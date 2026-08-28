package com.finnfreitag.teleportchainconveyor.mixin;

import com.finnfreitag.teleportchainconveyor.attachment.ITeleportPackage;
import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainConnectionInfo;
import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainData;
import com.finnfreitag.teleportchainconveyor.handler.TeleportChainManager;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainAttachments;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity.ConnectionStats;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRoutingTable;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorShape;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(value = ChainConveyorBlockEntity.class, remap = false)
public abstract class ChainConveyorBlockEntityMixin {

    @Shadow
    public Map<BlockPos, ConnectionStats> connectionStats;

    @Shadow
    public Map<BlockPos, List<ChainConveyorPackage>> travellingPackages;

    @Shadow
    public abstract void prepareStats();

    @Shadow
    public abstract void calculateConnectionStats(BlockPos target);

    @Inject(method = "prepareStats", at = @At("TAIL"))
    private void ensureAllStatsPresent(CallbackInfo ci) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        TeleportChainData data = self.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
        if (data != null) {
            if (connectionStats == null) {
                connectionStats = new HashMap<>();
            }
            for (TeleportChainConnectionInfo info : data.getConnections()) {
                BlockPos virtualPos = info.virtualRelativePos();
                BlockPos recKey = TeleportChainManager.getReceiverKey(virtualPos);
                if (!connectionStats.containsKey(virtualPos) || !connectionStats.containsKey(recKey)) {
                    customConnectionStatsDirect(virtualPos, info);
                }
            }
        }
        if (self.connections != null) {
            if (connectionStats == null) {
                connectionStats = new HashMap<>();
            }
            for (BlockPos target : self.connections) {
                if (!connectionStats.containsKey(target)) {
                    this.calculateConnectionStats(target);
                }
            }
        }
    }

    @Inject(method = "addTravellingPackage", at = @At("HEAD"), cancellable = true)
    private void preventSourceTeleportReentry(ChainConveyorPackage box, BlockPos connection, CallbackInfoReturnable<Boolean> cir) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        Optional<TeleportChainConnectionInfo> infoOpt = TeleportChainManager.getTeleportConnection(self, connection);
        if (infoOpt.isPresent()) {
            TeleportChainConnectionInfo info = infoOpt.get();
            if (box instanceof ITeleportPackage tp) {
                if (info.connectionId().equals(tp.getTeleportSourceConnectionId())) {
                    ConnectionStats stats = connectionStats != null ? connectionStats.get(connection) : null;
                    if (stats != null) {
                        box.chainPosition = stats.tangentAngle();
                    }
                    self.addLoopingPackage(box);
                    cir.setReturnValue(false);
                }
            }
        } else {
            if (box instanceof ITeleportPackage tp) {
                tp.setTeleportSourceConnectionId(null);
            }
        }
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            ordinal = 2
        )
    )
    private BlockEntity preventDefaultTeleportTransfer(Level level, BlockPos pos) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        BlockPos relativePos = pos.subtract(self.getBlockPos());
        if (TeleportChainManager.isTeleportConnection(self, relativePos)) {
            return null;
        }
        return level.getBlockEntity(pos);
    }

    @Inject(method = "calculateConnectionStats", at = @At("HEAD"), cancellable = true)
    private void customConnectionStats(BlockPos relativePos, CallbackInfo ci) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        Optional<TeleportChainConnectionInfo> infoOpt = TeleportChainManager.getTeleportConnection(self, relativePos);

        if (infoOpt.isPresent()) {
            customConnectionStatsDirect(relativePos, infoOpt.get());
            ci.cancel();
        }
    }

    @Inject(method = "addPropagationLocations", at = @At("HEAD"), cancellable = true)
    private void filterTeleportPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours, CallbackInfoReturnable<List<BlockPos>> cir) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        if (self.connections != null) {
            for (BlockPos p : self.connections) {
                if (!TeleportChainManager.isTeleportConnection(self, p)) {
                    neighbours.add(self.getBlockPos().offset(p));
                }
            }
        }
        cir.setReturnValue(neighbours);
    }

    @Inject(method = "propagateRotationTo", at = @At("HEAD"), cancellable = true)
    private void filterTeleportRotationPropagation(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs, CallbackInfoReturnable<Float> cir) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        if (TeleportChainManager.isTeleportConnection(self, diff)) {
            cir.setReturnValue(0f);
        }
    }

    @Inject(method = "removeInvalidConnections", at = @At("HEAD"), cancellable = true)
    private void protectTeleportConnectionsFromRemoval(CallbackInfo ci) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) return;

        for (java.util.Iterator<BlockPos> iterator = self.connections.iterator(); iterator.hasNext(); ) {
            BlockPos next = iterator.next();
            if (TeleportChainManager.isTeleportConnection(self, next)) {
                continue;
            }
            BlockPos target = self.getBlockPos().offset(next);
            if (!level.isLoaded(target))
                continue;
            if (level.getBlockEntity(target) instanceof ChainConveyorBlockEntity ccbe
                    && ccbe.connections.contains(next.multiply(-1)))
                continue;
            iterator.remove();
        }
        ci.cancel();
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorRoutingTable;getExitFor(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/core/BlockPos;"
        )
    )
    private BlockPos allowTeleportChainExit(ChainConveyorRoutingTable routingTable, ItemStack stack) {
        BlockPos exit = routingTable.getExitFor(stack);
        if (exit != null && !exit.equals(BlockPos.ZERO)) {
            return exit;
        }

        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        TeleportChainData data = self.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
        if (data != null && !data.getConnections().isEmpty()) {
            for (TeleportChainConnectionInfo info : data.getConnections()) {
                if (self.connections.contains(info.virtualRelativePos())) {
                    return info.virtualRelativePos();
                }
            }
        }

        return exit != null ? exit : BlockPos.ZERO;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickTeleportPackages(CallbackInfo ci) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        Level level = self.getLevel();

        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        MinecraftServer server = serverLevel.getServer();

        // Sync kinetic speed across teleport chain connections
        TeleportChainData data = self.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
        if (data != null && !data.getConnections().isEmpty()) {
            for (TeleportChainConnectionInfo info : data.getConnections()) {
                ServerLevel targetLevel = server.getLevel(info.targetDimension());
                if (targetLevel != null) {
                    targetLevel.getChunkSource().getChunk(info.targetPos().getX() >> 4, info.targetPos().getZ() >> 4, ChunkStatus.FULL, true);
                    BlockEntity targetTile = targetLevel.getBlockEntity(info.targetPos());
                    if (targetTile instanceof ChainConveyorBlockEntity targetBE) {
                        com.finnfreitag.teleportchainconveyor.handler.TeleportChainKineticHelper.syncKineticSpeed(self, targetBE, targetLevel);
                    }
                }
            }
        }

        // Advertise routing tables across teleport chain connections
        if (self.routingTable != null && self.routingTable.shouldAdvertise()) {
            if (data != null) {
                for (TeleportChainConnectionInfo info : data.getConnections()) {
                    ServerLevel targetLevel = server.getLevel(info.targetDimension());
                    if (targetLevel != null) {
                        targetLevel.getChunkSource().getChunk(info.targetPos().getX() >> 4, info.targetPos().getZ() >> 4, ChunkStatus.FULL, true);
                        BlockEntity targetTile = targetLevel.getBlockEntity(info.targetPos());
                        if (targetTile instanceof ChainConveyorBlockEntity targetBE) {
                            TeleportChainData targetData = targetBE.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
                            Optional<TeleportChainConnectionInfo> targetConnOpt = targetData.getConnectionById(info.connectionId());
                            if (targetConnOpt.isPresent()) {
                                BlockPos targetVirtualPos = targetConnOpt.get().virtualRelativePos();
                                self.routingTable.advertiseTo(targetVirtualPos.multiply(-1), targetBE.routingTable);
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<BlockPos, List<ChainConveyorPackage>> entry : travellingPackages.entrySet()) {
            BlockPos relPos = entry.getKey();
            Optional<TeleportChainConnectionInfo> infoOpt = TeleportChainManager.getTeleportConnection(self, relPos);

            if (infoOpt.isEmpty()) {
                continue;
            }

            TeleportChainConnectionInfo info = infoOpt.get();
            List<ChainConveyorPackage> packages = entry.getValue();
            List<ChainConveyorPackage> finishedPackages = new ArrayList<>();

            boolean isReceiver = TeleportChainManager.isReceiverKey(relPos);

            for (ChainConveyorPackage pkg : packages) {
                if (pkg.chainPosition >= 1.5f) {
                    finishedPackages.add(pkg);
                }
            }

            for (ChainConveyorPackage pkg : finishedPackages) {
                packages.remove(pkg);

                if (!isReceiver) {
                    // --- SENDER STUB COMPLETED: TELEPORT TO TARGET CONVEYOR'S RECEIVER STUB ---
                    ServerLevel targetLevel = server.getLevel(info.targetDimension());
                    if (targetLevel != null) {
                        targetLevel.getChunkSource().getChunk(info.targetPos().getX() >> 4, info.targetPos().getZ() >> 4, ChunkStatus.FULL, true);
                        BlockEntity targetBE = targetLevel.getBlockEntity(info.targetPos());

                        if (targetBE instanceof ChainConveyorBlockEntity targetConveyor) {
                            TeleportChainData targetData = targetConveyor.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
                            Optional<TeleportChainConnectionInfo> targetConnOpt = targetData.getConnectionById(info.connectionId());

                            pkg.chainPosition = 0f;
                            if (pkg instanceof ITeleportPackage tp) {
                                tp.setTeleportSourceConnectionId(info.connectionId());
                            }

                            if (targetConnOpt.isPresent()) {
                                TeleportChainConnectionInfo targetInfo = targetConnOpt.get();
                                BlockPos targetRecKey = TeleportChainManager.getReceiverKey(targetInfo.virtualRelativePos());

                                Vec3 targetStartCenter = Vec3.atBottomCenterOf(info.targetPos()).add(0, 6 / 16f, 0);
                                Vec3 sourceWorldPos = Vec3.atBottomCenterOf(self.getBlockPos()).add(0, 6 / 16f, 0);
                                Vec3 targetDir = targetLevel.dimension().equals(level.dimension()) ? sourceWorldPos.subtract(targetStartCenter) : new Vec3(0, 10, 0);
                                if (targetDir.lengthSqr() > 1e-4) {
                                    targetDir = targetDir.normalize();
                                } else {
                                    targetDir = new Vec3(0, 1, 0);
                                }

                                float targetDirAngle = (float) (Mth.atan2(targetDir.x, targetDir.z) * Mth.RAD_TO_DEG);
                                boolean targetReversed = targetConveyor.getSpeed() < 0;
                                float targetAngle2 = targetDirAngle + 35f * (targetReversed ? -1 : 1);
                                Vec3 targetOffset2 = VecHelper.rotate(new Vec3(0, 0, 1.25), targetAngle2, Axis.Y);
                                Vec3 targetStart2 = targetStartCenter.add(targetOffset2);
                                Vec3 targetEnd2 = targetStart2.add(targetDir.scale(1.5f));

                                pkg.worldPosition = targetEnd2;
                                pkg.yaw = Mth.wrapDegrees(targetDirAngle + 180);

                                com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage.ChainConveyorPackagePhysicsData physicsData = pkg.physicsData(targetLevel);
                                if (physicsData != null) {
                                    physicsData.pos = targetEnd2;
                                    physicsData.prevPos = targetEnd2;
                                    physicsData.targetPos = targetEnd2;
                                    physicsData.prevTargetPos = targetEnd2;
                                    physicsData.yaw = pkg.yaw;
                                    physicsData.prevYaw = pkg.yaw;
                                }

                                targetConveyor.prepareStats();
                                targetConveyor.getTravellingPackages().computeIfAbsent(targetRecKey, $ -> new ArrayList<>()).add(pkg);
                            } else {
                                targetConveyor.addLoopingPackage(pkg);
                            }

                            targetConveyor.notifyUpdate();

                            // Sound and particle effects at source and target
                            serverLevel.playSound(null, self.getBlockPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8f, 1.2f);
                            serverLevel.sendParticles(ParticleTypes.PORTAL, self.getBlockPos().getX() + 0.5, self.getBlockPos().getY() + 0.9, self.getBlockPos().getZ() + 0.5, 25, 0.2, 0.2, 0.2, 0.1);

                            targetLevel.playSound(null, info.targetPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8f, 1.2f);
                            targetLevel.sendParticles(ParticleTypes.PORTAL, info.targetPos().getX() + 0.5, info.targetPos().getY() + 0.9, info.targetPos().getZ() + 0.5, 25, 0.2, 0.2, 0.2, 0.1);
                        } else {
                            // Drop package entity if target no longer exists
                            if (pkg.worldPosition != null) {
                                serverLevel.addFreshEntity(PackageEntity.fromItemStack(serverLevel, pkg.worldPosition.subtract(0, 0.5, 0), pkg.item));
                            }
                        }
                    }
                } else {
                    // --- RECEIVER STUB COMPLETED: HAND OFF TO WHEEL LOOP ---
                    ConnectionStats stats2 = connectionStats.get(relPos);
                    if (stats2 != null) {
                        pkg.chainPosition = stats2.tangentAngle();
                    }
                    self.addLoopingPackage(pkg);
                }
            }

            if (!finishedPackages.isEmpty()) {
                self.notifyUpdate();
            }
        }
    }

    private static Vec3 getMappedTargetWorldPos(Level level, BlockPos targetPos, ResourceKey<Level> targetDimension) {
        Vec3 rawTargetCenter = Vec3.atBottomCenterOf(targetPos).add(0, 6 / 16f, 0);
        if (level == null || level.dimension().equals(targetDimension)) {
            return rawTargetCenter;
        }

        ResourceKey<Level> currentDim = level.dimension();
        double scaleX = 1.0;
        double scaleZ = 1.0;

        if (currentDim.equals(Level.NETHER) && targetDimension.equals(Level.OVERWORLD)) {
            scaleX = 1.0 / 8.0;
            scaleZ = 1.0 / 8.0;
        } else if (currentDim.equals(Level.OVERWORLD) && targetDimension.equals(Level.NETHER)) {
            scaleX = 8.0;
            scaleZ = 8.0;
        }

        double mappedX = (targetPos.getX() + 0.5) * scaleX;
        double mappedY = targetPos.getY() + 6 / 16f;
        double mappedZ = (targetPos.getZ() + 0.5) * scaleZ;

        return new Vec3(mappedX, mappedY, mappedZ);
    }

    public void customConnectionStatsDirect(BlockPos relativePos, TeleportChainConnectionInfo info) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        BlockPos tilePos = self.getBlockPos();
        Vec3 startCenter = Vec3.atBottomCenterOf(tilePos).add(0, 6 / 16f, 0);
        Vec3 targetWorldPos = getMappedTargetWorldPos(self.getLevel(), info.targetPos(), info.targetDimension());

        Vec3 dir = targetWorldPos.subtract(startCenter);
        if (dir.lengthSqr() > 1e-4) {
            dir = dir.normalize();
        } else {
            dir = new Vec3(0, 1, 0);
        }

        float direction = (float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG);
        float offBranchDistance = 35f;
        boolean reversed = self.getSpeed() < 0;

        // Stub 1 (Outgoing / Sender Stub): from start1 (wheel) to end1 (portal)
        float angle1 = direction - offBranchDistance * (reversed ? -1 : 1);
        Vec3 offset1 = VecHelper.rotate(new Vec3(0, 0, 1.25), angle1, Axis.Y);
        Vec3 start1 = startCenter.add(offset1);
        float stubLength = 1.5f;
        Vec3 end1 = start1.add(dir.scale(stubLength));
        ConnectionStats stats1 = new ConnectionStats(angle1, stubLength, start1, end1);
        connectionStats.put(relativePos, stats1);

        // Stub 2 (Incoming / Receiver Stub): from end2 (portal) to start2 (wheel)
        float angle2 = direction + offBranchDistance * (reversed ? -1 : 1);
        Vec3 offset2 = VecHelper.rotate(new Vec3(0, 0, 1.25), angle2, Axis.Y);
        Vec3 start2 = startCenter.add(offset2);
        Vec3 end2 = start2.add(dir.scale(stubLength));
        ConnectionStats stats2 = new ConnectionStats(angle2, stubLength, end2, start2);
        BlockPos recKey = TeleportChainManager.getReceiverKey(relativePos);
        connectionStats.put(recKey, stats2);
    }

    @Inject(method = "updateChainShapes", at = @At("TAIL"))
    private void addReceiverStubShapes(CallbackInfo ci) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level != null && level.isClientSide()) {
            List<ChainConveyorShape> shapes =
                    ChainConveyorInteractionHandler.loadedChains.get(level).asMap().get(self.getBlockPos());
            if (shapes != null) {
                TeleportChainData data = self.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
                for (TeleportChainConnectionInfo info : data.getConnections()) {
                    BlockPos recKey = TeleportChainManager.getReceiverKey(info.virtualRelativePos());
                    if (shapes.stream().noneMatch(s -> s instanceof ChainConveyorShape.ChainConveyorOBB obb && ((ChainConveyorOBBAccessor) obb).getConnection().equals(recKey))) {
                        ConnectionStats stats = connectionStats != null ? connectionStats.get(recKey) : null;
                        if (stats == null) {
                            customConnectionStatsDirect(info.virtualRelativePos(), info);
                            stats = connectionStats != null ? connectionStats.get(recKey) : null;
                        }
                        if (stats != null) {
                            Vec3 localStart = stats.end().subtract(Vec3.atLowerCornerOf(self.getBlockPos()));
                            Vec3 localEnd = stats.start().subtract(Vec3.atLowerCornerOf(self.getBlockPos()));
                            shapes.add(new ChainConveyorShape.ChainConveyorOBB(recKey, localStart, localEnd));
                        }
                    }
                }
            }
        }
    }
}


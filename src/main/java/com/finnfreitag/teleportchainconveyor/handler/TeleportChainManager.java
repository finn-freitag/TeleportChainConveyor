package com.finnfreitag.teleportchainconveyor.handler;

import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainConnectionInfo;
import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainData;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainAttachments;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.Optional;
import java.util.UUID;

public class TeleportChainManager {

    public static boolean createConnection(Level targetLevel, ChainConveyorBlockEntity targetBE, BlockPos sourcePos, ResourceKey<Level> sourceDim, boolean isInterdimensional, Player player) {
        if (!(targetLevel instanceof ServerLevel currentServerLevel)) {
            return true;
        }

        MinecraftServer server = currentServerLevel.getServer();
        ServerLevel sourceServerLevel = server.getLevel(sourceDim);

        if (sourceServerLevel == null) {
            if (player != null) {
                player.displayClientMessage(Component.literal("Source dimension is not loaded!").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        // Retrieve or load source block entity
        sourceServerLevel.getChunkSource().getChunk(sourcePos.getX() >> 4, sourcePos.getZ() >> 4, ChunkStatus.FULL, true);
        BlockEntity sourceTile = sourceServerLevel.getBlockEntity(sourcePos);

        if (!(sourceTile instanceof ChainConveyorBlockEntity sourceBE)) {
            if (player != null) {
                player.displayClientMessage(Component.literal("Source conveyor no longer exists at [" + sourcePos.getX() + ", " + sourcePos.getY() + ", " + sourcePos.getZ() + "]!").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        // Check 4-connection cap
        if (sourceBE.connections.size() >= 4) {
            if (player != null) {
                player.displayClientMessage(Component.literal("Source conveyor already has maximum allowed connections (4)!").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        if (targetBE.connections.size() >= 4) {
            if (player != null) {
                player.displayClientMessage(Component.literal("Target conveyor already has maximum allowed connections (4)!").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        UUID connectionId = UUID.randomUUID();
        BlockPos targetPos = targetBE.getBlockPos();
        ResourceKey<Level> targetDim = targetLevel.dimension();

        BlockPos relPosFromSource;
        BlockPos relPosFromTarget;

        if (sourceDim.equals(targetDim)) {
            relPosFromSource = targetPos.subtract(sourcePos);
            relPosFromTarget = sourcePos.subtract(targetPos);
        } else {
            // Synthetic offsets for cross-dimensional links
            int hash = Math.abs(connectionId.hashCode() % 200) + 100;
            relPosFromSource = new BlockPos(hash, hash, hash);
            relPosFromTarget = new BlockPos(-hash, -hash, -hash);
        }

        // Add to Create's connections set
        sourceBE.connections.add(relPosFromSource);
        targetBE.connections.add(relPosFromTarget);

        // Add to our attachment data
        TeleportChainData sourceData = sourceBE.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
        TeleportChainData targetData = targetBE.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());

        sourceData.addConnection(new TeleportChainConnectionInfo(connectionId, targetPos, targetDim, isInterdimensional, relPosFromSource));
        targetData.addConnection(new TeleportChainConnectionInfo(connectionId, sourcePos, sourceDim, isInterdimensional, relPosFromTarget));

        sourceBE.prepareStats();
        targetBE.prepareStats();

        sourceBE.notifyUpdate();
        targetBE.notifyUpdate();

        if (player != null) {
            player.displayClientMessage(Component.literal("Teleport Chain connected!").withStyle(ChatFormatting.GREEN), true);
        }

        return true;
    }

    public static boolean removeConnection(ServerLevel level, ChainConveyorBlockEntity sourceBE, BlockPos connection, Player player) {
        Optional<TeleportChainConnectionInfo> infoOpt = getTeleportConnection(sourceBE, connection);
        if (infoOpt.isEmpty()) {
            return false;
        }

        TeleportChainConnectionInfo info = infoOpt.get();
        UUID connectionId = info.connectionId();

        // 1. Remove connection on sourceBE
        TeleportChainData sourceData = sourceBE.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
        BlockPos sourceVirtualPos = info.virtualRelativePos();
        BlockPos sourceRecKey = getReceiverKey(sourceVirtualPos);

        sourceBE.connections.remove(sourceVirtualPos);
        sourceBE.connections.remove(sourceRecKey);
        if (sourceBE.connectionStats != null) {
            sourceBE.connectionStats.remove(sourceVirtualPos);
            sourceBE.connectionStats.remove(sourceRecKey);
        }
        if (sourceBE.getTravellingPackages() != null) {
            sourceBE.getTravellingPackages().remove(sourceVirtualPos);
            sourceBE.getTravellingPackages().remove(sourceRecKey);
        }
        sourceData.removeConnectionById(connectionId);
        sourceBE.notifyUpdate();

        // Sound on source side
        level.playSound(null, sourceBE.getBlockPos(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);

        // 2. Remove connection on targetBE (across dimensions if needed)
        MinecraftServer server = level.getServer();
        ServerLevel targetLevel = server.getLevel(info.targetDimension());
        if (targetLevel != null) {
            targetLevel.getChunkSource().getChunk(info.targetPos().getX() >> 4, info.targetPos().getZ() >> 4, ChunkStatus.FULL, true);
            BlockEntity targetTile = targetLevel.getBlockEntity(info.targetPos());

            if (targetTile instanceof ChainConveyorBlockEntity targetBE) {
                TeleportChainData targetData = targetBE.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
                Optional<TeleportChainConnectionInfo> targetInfoOpt = targetData.getConnectionById(connectionId);

                if (targetInfoOpt.isPresent()) {
                    TeleportChainConnectionInfo targetInfo = targetInfoOpt.get();
                    BlockPos targetVirtualPos = targetInfo.virtualRelativePos();
                    BlockPos targetRecKey = getReceiverKey(targetVirtualPos);

                    targetBE.connections.remove(targetVirtualPos);
                    targetBE.connections.remove(targetRecKey);
                    if (targetBE.connectionStats != null) {
                        targetBE.connectionStats.remove(targetVirtualPos);
                        targetBE.connectionStats.remove(targetRecKey);
                    }
                    if (targetBE.getTravellingPackages() != null) {
                        targetBE.getTravellingPackages().remove(targetVirtualPos);
                        targetBE.getTravellingPackages().remove(targetRecKey);
                    }
                    targetData.removeConnectionById(connectionId);
                    targetBE.notifyUpdate();

                    targetLevel.playSound(null, targetBE.getBlockPos(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
        }

        // 3. Refund item to player if not in creative mode
        if (player != null && !player.isCreative()) {
            ItemStack refund = info.isInterdimensional()
                    ? new ItemStack(TeleportChainItems.INTERDIMENSIONAL_CHAIN.get())
                    : new ItemStack(TeleportChainItems.TELEPORTATION_CHAIN.get());
            player.getInventory().placeItemBackInInventory(refund);
        }

        return true;
    }

    public static BlockPos getReceiverKey(BlockPos virtualRelativePos) {
        return virtualRelativePos.offset(0, 1000, 0);
    }

    public static boolean isReceiverKey(BlockPos relativePos) {
        return relativePos != null && relativePos.getY() >= 500;
    }

    public static boolean isTeleportConnection(ChainConveyorBlockEntity be, BlockPos relativePos) {
        return getTeleportConnection(be, relativePos).isPresent();
    }

    public static Optional<TeleportChainConnectionInfo> getTeleportConnection(ChainConveyorBlockEntity be, BlockPos relativePos) {
        if (be == null || relativePos == null) {
            return Optional.empty();
        }
        TeleportChainData data = be.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
        if (data == null) {
            return Optional.empty();
        }
        for (TeleportChainConnectionInfo info : data.getConnections()) {
            if (info.virtualRelativePos().equals(relativePos) || getReceiverKey(info.virtualRelativePos()).equals(relativePos)) {
                return Optional.of(info);
            }
        }
        return Optional.empty();
    }
}

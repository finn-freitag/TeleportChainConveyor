package com.finnfreitag.teleportchainconveyor.handler;

import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainConnectionInfo;
import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainData;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainAttachments;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public class TeleportChainKineticHelper {

    private static final Map<BlockPos, Boolean> clientTeleportPoweredMap = new HashMap<>();

    public static void setClientTeleportPowered(KineticBlockEntity be, boolean powered) {
        if (be != null && be.getBlockPos() != null) {
            if (powered) {
                clientTeleportPoweredMap.put(be.getBlockPos(), true);
            } else {
                clientTeleportPoweredMap.remove(be.getBlockPos());
            }
        }
    }

    public static boolean isClientTeleportPowered(KineticBlockEntity be) {
        if (be == null || be.getBlockPos() == null) return false;
        return clientTeleportPoweredMap.getOrDefault(be.getBlockPos(), false);
    }

    public static void syncKineticSpeed(ChainConveyorBlockEntity source, ChainConveyorBlockEntity target, ServerLevel targetLevel) {
        boolean sourceLocallyPowered = isLocallyPowered(source);
        boolean targetLocallyPowered = isLocallyPowered(target);

        float sourceSpeed = source.getTheoreticalSpeed();
        float targetSpeed = target.getTheoreticalSpeed();

        if (sourceLocallyPowered && !targetLocallyPowered) {
            if (Math.abs(targetSpeed - sourceSpeed) > 1e-4f) {
                setSpeedAndPropagate(target, targetLevel, sourceSpeed, getProvidedCapacity(source));
            }
        } else if (targetLocallyPowered && !sourceLocallyPowered) {
            ServerLevel sourceLevel = (ServerLevel) source.getLevel();
            if (sourceLevel != null && Math.abs(sourceSpeed - targetSpeed) > 1e-4f) {
                setSpeedAndPropagate(source, sourceLevel, targetSpeed, getProvidedCapacity(target));
            }
        } else if (sourceLocallyPowered && targetLocallyPowered) {
            if (Math.abs(sourceSpeed) > Math.abs(targetSpeed)) {
                if (Math.abs(targetSpeed - sourceSpeed) > 1e-4f) {
                    setSpeedAndPropagate(target, targetLevel, sourceSpeed, getProvidedCapacity(source));
                }
            } else if (Math.abs(targetSpeed) > Math.abs(sourceSpeed)) {
                ServerLevel sourceLevel = (ServerLevel) source.getLevel();
                if (sourceLevel != null && Math.abs(sourceSpeed - targetSpeed) > 1e-4f) {
                    setSpeedAndPropagate(source, sourceLevel, targetSpeed, getProvidedCapacity(target));
                }
            }
        } else {
            if (sourceSpeed != 0 || targetSpeed != 0) {
                if (sourceSpeed != 0) {
                    ServerLevel sourceLevel = (ServerLevel) source.getLevel();
                    if (sourceLevel != null) {
                        clearSpeedAndPropagate(source, sourceLevel);
                    }
                }
                if (targetSpeed != 0) {
                    clearSpeedAndPropagate(target, targetLevel);
                }
            }
        }
    }

    private static float getProvidedCapacity(ChainConveyorBlockEntity be) {
        if (be == null) return 1000000f;
        if (be.hasNetwork() && be.getOrCreateNetwork() != null) {
            float cap = be.getOrCreateNetwork().calculateCapacity();
            if (cap > 0) return cap;
        }
        return 1000000f;
    }

    public static boolean isLocallyPowered(ChainConveyorBlockEntity be) {
        if (be == null || be.getLevel() == null || be.getLevel().isClientSide) return false;
        if (!be.hasSource() || be.source == null) return false;
        if (be.getTheoreticalSpeed() == 0) return false;

        Level level = be.getLevel();
        if (!level.isLoaded(be.source)) return false;

        BlockEntity sourceTile = level.getBlockEntity(be.source);
        if (!(sourceTile instanceof KineticBlockEntity kbe)) return false;

        return kbe.getTheoreticalSpeed() != 0;
    }

    public static boolean isTeleportGenerator(ChainConveyorBlockEntity self) {
        if (self == null || self.getLevel() == null) return false;
        if (self.getLevel().isClientSide) {
            return isClientTeleportPowered(self);
        }
        if (self.getTheoreticalSpeed() == 0) return false;
        if (isLocallyPowered(self)) return false;

        TeleportChainData data = self.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
        if (data == null || data.getConnections().isEmpty()) return false;

        ServerLevel serverLevel = (ServerLevel) self.getLevel();
        MinecraftServer server = serverLevel.getServer();

        for (TeleportChainConnectionInfo info : data.getConnections()) {
            ServerLevel targetLevel = server.getLevel(info.targetDimension());
            if (targetLevel == null) continue;

            BlockEntity targetTile = targetLevel.getBlockEntity(info.targetPos());
            if (targetTile instanceof ChainConveyorBlockEntity targetBE) {
                if (Math.abs(targetBE.getTheoreticalSpeed() - self.getTheoreticalSpeed()) <= 1e-4f && targetBE.getTheoreticalSpeed() != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float getTeleportCapacity(ChainConveyorBlockEntity self) {
        if (self == null || self.getLevel() == null) return 1000000f;
        if (self.getLevel().isClientSide) return 1000000f;

        TeleportChainData data = self.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
        if (data == null || data.getConnections().isEmpty()) return 1000000f;

        ServerLevel serverLevel = (ServerLevel) self.getLevel();
        MinecraftServer server = serverLevel.getServer();

        for (TeleportChainConnectionInfo info : data.getConnections()) {
            ServerLevel targetLevel = server.getLevel(info.targetDimension());
            if (targetLevel == null) continue;

            BlockEntity targetTile = targetLevel.getBlockEntity(info.targetPos());
            if (targetTile instanceof ChainConveyorBlockEntity targetBE) {
                if (targetBE.hasNetwork() && targetBE.getOrCreateNetwork() != null) {
                    float cap = targetBE.getOrCreateNetwork().calculateCapacity();
                    if (cap > 0) return cap;
                }
            }
        }
        return 1000000f;
    }

    public static void setSpeedAndPropagate(ChainConveyorBlockEntity be, ServerLevel level, float speed, float capacity) {
        float prevSpeed = be.getTheoreticalSpeed();
        be.setSpeed(speed);
        if (!be.hasNetwork() && speed != 0) {
            be.setNetwork(be.getBlockPos().asLong());
        }
        if (be.hasNetwork() && speed != 0) {
            float addedCapacity = capacity > 0 ? capacity : 1000000f;
            be.getOrCreateNetwork().updateCapacityFor(be, addedCapacity);
        }
        be.onSpeedChanged(prevSpeed);
        be.sendData();
        be.notifyUpdate();
        RotationPropagator.handleAdded(level, be.getBlockPos(), be);
    }

    public static void clearSpeedAndPropagate(ChainConveyorBlockEntity be, ServerLevel level) {
        float prevSpeed = be.getTheoreticalSpeed();
        if (be.hasNetwork() && be.getOrCreateNetwork() != null) {
            be.getOrCreateNetwork().updateCapacityFor(be, 0f);
        }
        be.setSpeed(0f);
        be.removeSource();
        be.onSpeedChanged(prevSpeed);
        be.sendData();
        be.notifyUpdate();
        RotationPropagator.handleRemoved(level, be.getBlockPos(), be);
    }

    public static boolean isPoweredByTeleportConnection(ChainConveyorBlockEntity self) {
        return isTeleportGenerator(self);
    }
}

package com.finnfreitag.teleportchainconveyor.handler;

import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainConnectionInfo;
import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainData;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainAttachments;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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
            int hash = Math.abs(connectionId.hashCode() % 8000) + 2000;
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

        sourceBE.notifyUpdate();
        targetBE.notifyUpdate();

        if (player != null) {
            player.displayClientMessage(Component.literal("Teleport Chain connected!").withStyle(ChatFormatting.GREEN), true);
        }

        return true;
    }

    public static BlockPos getReceiverKey(BlockPos virtualRelativePos) {
        return virtualRelativePos.offset(10000, 10000, 10000);
    }

    public static boolean isReceiverKey(BlockPos relativePos) {
        return relativePos.getY() >= 5000;
    }

    public static boolean isTeleportConnection(ChainConveyorBlockEntity be, BlockPos relativePos) {
        return getTeleportConnection(be, relativePos).isPresent();
    }

    public static Optional<TeleportChainConnectionInfo> getTeleportConnection(ChainConveyorBlockEntity be, BlockPos relativePos) {
        TeleportChainData data = be.getData(TeleportChainAttachments.TELEPORT_CHAIN_DATA.get());
        Optional<TeleportChainConnectionInfo> info = data.getConnectionByVirtualPos(relativePos);
        if (info.isPresent()) {
            return info;
        }
        BlockPos basePos = relativePos.offset(-10000, -10000, -10000);
        return data.getConnectionByVirtualPos(basePos);
    }
}

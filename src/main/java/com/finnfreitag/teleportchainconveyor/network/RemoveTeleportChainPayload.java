package com.finnfreitag.teleportchainconveyor.network;

import com.finnfreitag.teleportchainconveyor.Teleportchainconveyor;
import com.finnfreitag.teleportchainconveyor.handler.TeleportChainManager;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RemoveTeleportChainPayload(BlockPos conveyorPos, BlockPos connection) implements CustomPacketPayload {

    public static final Type<RemoveTeleportChainPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Teleportchainconveyor.MODID, "remove_teleport_chain")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveTeleportChainPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemoveTeleportChainPayload::conveyorPos,
            BlockPos.STREAM_CODEC, RemoveTeleportChainPayload::connection,
            RemoveTeleportChainPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.level().getBlockEntity(conveyorPos) instanceof ChainConveyorBlockEntity conveyorBE) {
                    TeleportChainManager.removeConnection(player.serverLevel(), conveyorBE, connection, player);
                }
            }
        });
    }
}

package com.finnfreitag.teleportchainconveyor.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record TeleportChainLink(BlockPos pos, ResourceKey<Level> dimension, UUID linkId) {

    public static final Codec<TeleportChainLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(TeleportChainLink::pos),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(TeleportChainLink::dimension),
            UUIDUtil.CODEC.fieldOf("link_id").forGetter(TeleportChainLink::linkId)
    ).apply(instance, TeleportChainLink::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportChainLink> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TeleportChainLink::pos,
            ResourceKey.streamCodec(Registries.DIMENSION), TeleportChainLink::dimension,
            UUIDUtil.STREAM_CODEC, TeleportChainLink::linkId,
            TeleportChainLink::new
    );
}

package com.finnfreitag.teleportchainconveyor.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record TeleportChainConnectionInfo(
        UUID connectionId,
        BlockPos targetPos,
        ResourceKey<Level> targetDimension,
        boolean isInterdimensional,
        BlockPos virtualRelativePos
) {
    public static final Codec<TeleportChainConnectionInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("connection_id").forGetter(TeleportChainConnectionInfo::connectionId),
            BlockPos.CODEC.fieldOf("target_pos").forGetter(TeleportChainConnectionInfo::targetPos),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("target_dimension").forGetter(TeleportChainConnectionInfo::targetDimension),
            Codec.BOOL.fieldOf("is_interdimensional").forGetter(TeleportChainConnectionInfo::isInterdimensional),
            BlockPos.CODEC.fieldOf("virtual_relative_pos").forGetter(TeleportChainConnectionInfo::virtualRelativePos)
    ).apply(instance, TeleportChainConnectionInfo::new));
}

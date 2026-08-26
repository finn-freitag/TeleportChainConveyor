package com.finnfreitag.teleportchainconveyor.registry;

import com.finnfreitag.teleportchainconveyor.Teleportchainconveyor;
import com.finnfreitag.teleportchainconveyor.component.TeleportChainLink;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TeleportChainDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Teleportchainconveyor.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TeleportChainLink>> CHAIN_LINK =
            DATA_COMPONENT_TYPES.register("chain_link", () -> DataComponentType.<TeleportChainLink>builder()
                    .persistent(TeleportChainLink.CODEC)
                    .networkSynchronized(TeleportChainLink.STREAM_CODEC)
                    .build());
}

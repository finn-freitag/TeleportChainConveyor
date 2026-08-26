package com.finnfreitag.teleportchainconveyor.registry;

import com.finnfreitag.teleportchainconveyor.Teleportchainconveyor;
import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class TeleportChainAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Teleportchainconveyor.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TeleportChainData>> TELEPORT_CHAIN_DATA =
            ATTACHMENT_TYPES.register("teleport_chain_data", () -> AttachmentType.builder((Supplier<TeleportChainData>) TeleportChainData::new)
                    .serialize(TeleportChainData.CODEC)
                    .build());
}

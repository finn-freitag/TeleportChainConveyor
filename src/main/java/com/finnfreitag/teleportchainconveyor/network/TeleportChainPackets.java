package com.finnfreitag.teleportchainconveyor.network;

import com.finnfreitag.teleportchainconveyor.Teleportchainconveyor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Teleportchainconveyor.MODID)
public class TeleportChainPackets {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                RemoveTeleportChainPayload.TYPE,
                RemoveTeleportChainPayload.STREAM_CODEC,
                RemoveTeleportChainPayload::handle
        );
    }
}

package com.finnfreitag.teleportchainconveyor.handler;

import com.finnfreitag.teleportchainconveyor.Teleportchainconveyor;
import com.finnfreitag.teleportchainconveyor.network.RemoveTeleportChainPayload;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Teleportchainconveyor.MODID, value = Dist.CLIENT)
public class ClientWrenchHandler {

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (event.isAttack()) { // Left click
            if (ChainConveyorInteractionHandler.selectedLift != null && ChainConveyorInteractionHandler.selectedConnection != null) {
                Player player = mc.player;
                if (player.isHolding(stack -> stack.is(Tags.Items.TOOLS_WRENCH)) && player.isShiftKeyDown()) {
                    BlockEntity be = mc.level.getBlockEntity(ChainConveyorInteractionHandler.selectedLift);
                    if (be instanceof ChainConveyorBlockEntity conveyorBE) {
                        if (TeleportChainManager.isTeleportConnection(conveyorBE, ChainConveyorInteractionHandler.selectedConnection)) {
                            PacketDistributor.sendToServer(new RemoveTeleportChainPayload(
                                    ChainConveyorInteractionHandler.selectedLift,
                                    ChainConveyorInteractionHandler.selectedConnection
                            ));
                            event.setCanceled(true);
                            event.setSwingHand(false);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (ChainConveyorInteractionHandler.selectedLift != null && ChainConveyorInteractionHandler.selectedConnection != null) {
            Player player = mc.player;
            if (player.isHolding(stack -> stack.is(Tags.Items.TOOLS_WRENCH)) && player.isShiftKeyDown()) {
                BlockEntity be = mc.level.getBlockEntity(ChainConveyorInteractionHandler.selectedLift);
                if (be instanceof ChainConveyorBlockEntity conveyorBE) {
                    if (TeleportChainManager.isTeleportConnection(conveyorBE, ChainConveyorInteractionHandler.selectedConnection)) {
                        PacketDistributor.sendToServer(new RemoveTeleportChainPayload(
                                ChainConveyorInteractionHandler.selectedLift,
                                ChainConveyorInteractionHandler.selectedConnection
                        ));
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}

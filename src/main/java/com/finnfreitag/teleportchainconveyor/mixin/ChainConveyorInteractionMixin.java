package com.finnfreitag.teleportchainconveyor.mixin;

import com.finnfreitag.teleportchainconveyor.handler.TeleportChainManager;
import com.finnfreitag.teleportchainconveyor.network.RemoveTeleportChainPayload;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorShape;
import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.createmod.catnip.outliner.Outliner;

@Mixin(value = ChainConveyorInteractionHandler.class, remap = false)
public class ChainConveyorInteractionMixin {

    @Shadow
    public static BlockPos selectedLift;

    @Shadow
    public static BlockPos selectedConnection;

    @Shadow
    public static ChainConveyorShape selectedShape;

    @Shadow
    public static Vec3 selectedBakedPosition;

    @Inject(method = "clientTick", at = @At("TAIL"))
    private static void disableHitboxWhenHoldingPackage(CallbackInfo ci) {
        if (selectedLift != null && selectedConnection != null) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player != null) {
                boolean isHoldingPackage = PackageItem.isPackage(player.getMainHandItem()) || PackageItem.isPackage(player.getOffhandItem());
                if (isHoldingPackage) {
                    Level level = player.level();
                    if (level != null) {
                        BlockEntity be = level.getBlockEntity(selectedLift);
                        if (be instanceof ChainConveyorBlockEntity conveyorBE) {
                            if (TeleportChainManager.isTeleportConnection(conveyorBE, selectedConnection)) {
                                selectedLift = null;
                                selectedConnection = null;
                                selectedShape = null;
                                selectedBakedPosition = null;
                                Outliner.getInstance().remove("ChainPointSelection");
                            }
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private static void handleTeleportChainUse(CallbackInfoReturnable<Boolean> cir) {
        if (selectedConnection != null && selectedLift != null) {
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                BlockEntity be = level.getBlockEntity(selectedLift);
                if (be instanceof ChainConveyorBlockEntity conveyorBE) {
                    if (TeleportChainManager.isTeleportConnection(conveyorBE, selectedConnection)) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null && mc.player.isHolding(stack -> stack.is(Tags.Items.TOOLS_WRENCH)) && mc.player.isShiftKeyDown()) {
                            PacketDistributor.sendToServer(new RemoveTeleportChainPayload(selectedLift, selectedConnection));
                            cir.setReturnValue(true);
                        } else {
                            cir.setReturnValue(false);
                        }
                    }
                }
            }
        }
    }
}


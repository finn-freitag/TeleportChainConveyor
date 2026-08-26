package com.finnfreitag.teleportchainconveyor.mixin;

import com.finnfreitag.teleportchainconveyor.handler.TeleportChainManager;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChainConveyorInteractionHandler.class, remap = false)
public class ChainConveyorInteractionMixin {

    @Shadow
    public static BlockPos selectedLift;

    @Shadow
    public static BlockPos selectedConnection;

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private static void cancelUseOnTeleportChain(CallbackInfoReturnable<Boolean> cir) {
        if (selectedConnection != null && selectedLift != null) {
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                BlockEntity be = level.getBlockEntity(selectedLift);
                if (be instanceof ChainConveyorBlockEntity conveyorBE) {
                    if (TeleportChainManager.isTeleportConnection(conveyorBE, selectedConnection)) {
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }
}

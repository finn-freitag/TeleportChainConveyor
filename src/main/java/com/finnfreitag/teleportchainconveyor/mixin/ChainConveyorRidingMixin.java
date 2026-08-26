package com.finnfreitag.teleportchainconveyor.mixin;

import com.finnfreitag.teleportchainconveyor.handler.TeleportChainManager;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChainConveyorRidingHandler.class, remap = false)
public class ChainConveyorRidingMixin {

    @Inject(method = "embark", at = @At("HEAD"), cancellable = true)
    private static void cancelEmbarkOnTeleportChain(BlockPos conveyorPos, float chainPos, BlockPos connection, CallbackInfo ci) {
        Level level = Minecraft.getInstance().level;
        if (level != null && conveyorPos != null && connection != null) {
            BlockEntity be = level.getBlockEntity(conveyorPos);
            if (be instanceof ChainConveyorBlockEntity conveyorBE) {
                if (TeleportChainManager.isTeleportConnection(conveyorBE, connection)) {
                    ci.cancel();
                }
            }
        }
    }
}

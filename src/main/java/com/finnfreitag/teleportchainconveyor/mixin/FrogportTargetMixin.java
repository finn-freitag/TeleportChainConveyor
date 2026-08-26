package com.finnfreitag.teleportchainconveyor.mixin;

import com.finnfreitag.teleportchainconveyor.handler.TeleportChainManager;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PackagePortTarget.ChainConveyorFrogportTarget.class, remap = false)
public class FrogportTargetMixin {

    @Shadow
    public BlockPos connection;

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private void cancelRegisterOnTeleportChain(PackagePortBlockEntity port, LevelAccessor level, BlockPos pos, CallbackInfo ci) {
        if (this.connection != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChainConveyorBlockEntity conveyorBE) {
                if (TeleportChainManager.isTeleportConnection(conveyorBE, this.connection)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
    private void cancelSetupOnTeleportChain(PackagePortBlockEntity port, LevelAccessor level, BlockPos pos, CallbackInfo ci) {
        if (this.connection != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChainConveyorBlockEntity conveyorBE) {
                if (TeleportChainManager.isTeleportConnection(conveyorBE, this.connection)) {
                    ci.cancel();
                }
            }
        }
    }
}

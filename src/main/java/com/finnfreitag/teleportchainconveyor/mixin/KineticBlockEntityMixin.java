package com.finnfreitag.teleportchainconveyor.mixin;

import com.finnfreitag.teleportchainconveyor.handler.TeleportChainKineticHelper;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityMixin {

    @Shadow
    protected boolean overStressed;

    @Inject(method = "validateKinetics", at = @At("HEAD"), cancellable = true)
    private void protectTeleportKinetics(CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        if (self instanceof ChainConveyorBlockEntity ccbe) {
            if (TeleportChainKineticHelper.isPoweredByTeleportConnection(ccbe)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "getGeneratedSpeed", at = @At("HEAD"), cancellable = true)
    private void customGeneratedSpeed(CallbackInfoReturnable<Float> cir) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        if (self instanceof ChainConveyorBlockEntity ccbe) {
            if (TeleportChainKineticHelper.isTeleportGenerator(ccbe)) {
                cir.setReturnValue(ccbe.getTheoreticalSpeed());
            }
        }
    }

    @Inject(method = "calculateAddedStressCapacity", at = @At("HEAD"), cancellable = true)
    private void customAddedStressCapacity(CallbackInfoReturnable<Float> cir) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        if (self instanceof ChainConveyorBlockEntity ccbe) {
            if (TeleportChainKineticHelper.isTeleportGenerator(ccbe)) {
                cir.setReturnValue(TeleportChainKineticHelper.getTeleportCapacity(ccbe));
            }
        }
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void writeTeleportPowered(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (clientPacket) {
            KineticBlockEntity self = (KineticBlockEntity) (Object) this;
            if (self instanceof ChainConveyorBlockEntity ccbe) {
                compound.putBoolean("IsTeleportPowered", TeleportChainKineticHelper.isTeleportGenerator(ccbe));
            }
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void readTeleportPowered(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (clientPacket) {
            KineticBlockEntity self = (KineticBlockEntity) (Object) this;
            if (self instanceof ChainConveyorBlockEntity ccbe) {
                if (compound.contains("IsTeleportPowered")) {
                    boolean powered = compound.getBoolean("IsTeleportPowered");
                    TeleportChainKineticHelper.setClientTeleportPowered(ccbe, powered);
                    if (powered) {
                        this.overStressed = false;
                    }
                }
            }
        }
    }
}

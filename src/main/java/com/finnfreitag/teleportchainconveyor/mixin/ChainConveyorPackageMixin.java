package com.finnfreitag.teleportchainconveyor.mixin;

import com.finnfreitag.teleportchainconveyor.attachment.ITeleportPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = ChainConveyorPackage.class, remap = false)
public class ChainConveyorPackageMixin implements ITeleportPackage {

    private UUID teleportSourceConnectionId;

    @Override
    public UUID getTeleportSourceConnectionId() {
        return teleportSourceConnectionId;
    }

    @Override
    public void setTeleportSourceConnectionId(UUID id) {
        this.teleportSourceConnectionId = id;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void writeTeleportData(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
        if (teleportSourceConnectionId != null) {
            cir.getReturnValue().putUUID("TeleportSourceConnId", teleportSourceConnectionId);
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void readTeleportData(CompoundTag compoundTag, HolderLookup.Provider registries, CallbackInfoReturnable<ChainConveyorPackage> cir) {
        if (compoundTag.hasUUID("TeleportSourceConnId")) {
            ChainConveyorPackage pkg = cir.getReturnValue();
            if (pkg instanceof ITeleportPackage tp) {
                tp.setTeleportSourceConnectionId(compoundTag.getUUID("TeleportSourceConnId"));
            }
        }
    }
}

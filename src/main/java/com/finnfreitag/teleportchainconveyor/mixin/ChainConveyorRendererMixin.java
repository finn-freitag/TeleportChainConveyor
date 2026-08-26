package com.finnfreitag.teleportchainconveyor.mixin;

import com.finnfreitag.teleportchainconveyor.attachment.TeleportChainConnectionInfo;
import com.finnfreitag.teleportchainconveyor.handler.TeleportChainManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity.ConnectionStats;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Optional;

@Mixin(value = ChainConveyorRenderer.class, remap = false)
public class ChainConveyorRendererMixin {

    @Inject(
        method = "renderSafe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorRenderer;renderChains(Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            shift = At.Shift.AFTER
        )
    )
    private void renderTeleportStubsAndParticles(ChainConveyorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        BlockPos tilePos = be.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(level) / (360f / Math.max(1f, Math.abs(be.getSpeed())));
        time %= 1;
        if (time < 0)
            time += 1;
        float animation = time - 0.5f;

        RandomSource random = level.getRandom();

        for (Map.Entry<BlockPos, ConnectionStats> entry : be.connectionStats.entrySet()) {
            BlockPos relPos = entry.getKey();
            if (TeleportChainManager.isReceiverKey(relPos)) {
                continue;
            }
            if (TeleportChainManager.isTeleportConnection(be, relPos)) {
                ConnectionStats stats1 = entry.getValue();
                if (stats1 == null)
                    continue;

                Optional<TeleportChainConnectionInfo> infoOpt = TeleportChainManager.getTeleportConnection(be, relPos);
                if (infoOpt.isEmpty())
                    continue;

                TeleportChainConnectionInfo info = infoOpt.get();

                Vec3 startCenter = Vec3.atBottomCenterOf(tilePos).add(0, 6 / 16f, 0);
                Vec3 targetWorldPos;
                if (level.dimension().equals(info.targetDimension())) {
                    targetWorldPos = Vec3.atBottomCenterOf(info.targetPos()).add(0, 6 / 16f, 0);
                } else {
                    targetWorldPos = startCenter.add(0, 10, 0);
                }

                Vec3 dir = targetWorldPos.subtract(startCenter);
                if (dir.lengthSqr() > 1e-4) {
                    dir = dir.normalize();
                } else {
                    dir = new Vec3(0, 1, 0);
                }

                float direction = (float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG);
                float offBranchDistance = 35f;
                boolean reversed = be.getSpeed() < 0;
                float angle2 = direction + offBranchDistance * (reversed ? -1 : 1);

                Vec3 offset2 = VecHelper.rotate(new Vec3(0, 0, 1.25), angle2, Axis.Y);
                Vec3 start2 = startCenter.add(offset2);
                float stubLength = stats1.chainLength();
                Vec3 end1 = stats1.end();
                Vec3 end2 = start2.add(dir.scale(stubLength));

                // --- Render Stub 2 (Incoming chain animating towards wheel start2) ---
                Vec3 diff2 = end2.subtract(start2);
                double yaw = Mth.RAD_TO_DEG * Mth.atan2(diff2.x, diff2.z);
                double pitch = Mth.RAD_TO_DEG * Mth.atan2(diff2.y, diff2.multiply(1, 0, 1).length());
                Vec3 startOffset2 = start2.subtract(Vec3.atCenterOf(tilePos));

                ms.pushPose();
                var chain = TransformStack.of(ms);
                chain.center();
                chain.translate(startOffset2);
                chain.rotateYDegrees((float) yaw);
                chain.rotateXDegrees(90 - (float) pitch);
                chain.rotateYDegrees(45);
                chain.translate(0, 8 / 16f, 0);
                chain.uncenter();

                int light1 = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, tilePos),
                        level.getBrightness(LightLayer.SKY, tilePos));
                int light2 = light1;

                boolean far = Minecraft.getInstance().level == level && !Minecraft.getInstance()
                        .getBlockEntityRenderDispatcher().camera.getPosition()
                        .closerThan(Vec3.atCenterOf(tilePos), ChainConveyorRenderer.MIP_DISTANCE);

                ChainConveyorRenderer.renderChain(ms, buffer, -animation, stubLength, light1, light2, far);

                ms.popPose();

                // --- Render Particles at end1 (Stub 1 portal) and end2 (Stub 2 portal) ---
                if (level.isClientSide) {
                    if (random.nextFloat() < 0.4f) {
                        level.addParticle(ParticleTypes.PORTAL, end1.x, end1.y, end1.z,
                                (random.nextDouble() - 0.5) * 0.1,
                                (random.nextDouble() - 0.5) * 0.1,
                                (random.nextDouble() - 0.5) * 0.1);
                        level.addParticle(ParticleTypes.PORTAL, end2.x, end2.y, end2.z,
                                (random.nextDouble() - 0.5) * 0.1,
                                (random.nextDouble() - 0.5) * 0.1,
                                (random.nextDouble() - 0.5) * 0.1);
                    }

                    if (random.nextFloat() < 0.2f) {
                        level.addParticle(ParticleTypes.END_ROD, end1.x, end1.y, end1.z, 0, 0.01, 0);
                        level.addParticle(ParticleTypes.END_ROD, end2.x, end2.y, end2.z, 0, 0.01, 0);
                    }
                }
            }
        }
    }
}


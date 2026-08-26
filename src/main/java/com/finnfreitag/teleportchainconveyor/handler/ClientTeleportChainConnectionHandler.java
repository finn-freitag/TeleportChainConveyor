package com.finnfreitag.teleportchainconveyor.handler;

import com.finnfreitag.teleportchainconveyor.Teleportchainconveyor;
import com.finnfreitag.teleportchainconveyor.component.TeleportChainLink;
import com.finnfreitag.teleportchainconveyor.item.AbstractTeleportChainItem;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainDataComponents;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlock;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Teleportchainconveyor.MODID, value = Dist.CLIENT)
public class ClientTeleportChainConnectionHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        ItemStack stack = player.getMainHandItem();
        AbstractTeleportChainItem chainItem;
        if (stack.getItem() instanceof AbstractTeleportChainItem item) {
            chainItem = item;
        } else {
            stack = player.getOffhandItem();
            if (stack.getItem() instanceof AbstractTeleportChainItem item) {
                chainItem = item;
            } else {
                return;
            }
        }

        TeleportChainLink link = stack.get(TeleportChainDataComponents.CHAIN_LINK.get());
        if (link == null) return;

        BlockPos firstPos = link.pos();
        ResourceKey<Level> firstDim = link.dimension();

        boolean isFirstInSameDim = level.dimension().equals(firstDim);
        HitResult hitResult = mc.hitResult;

        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            if (isFirstInSameDim) {
                highlightConveyor(firstPos, 0xFFFFFF, "teleport_chain_connect");
            }
            return;
        }

        BlockHitResult bhr = (BlockHitResult) hitResult;
        BlockPos pos = bhr.getBlockPos();
        BlockState hitState = level.getBlockState(pos);

        if (pos.equals(firstPos) && isFirstInSameDim) {
            highlightConveyor(firstPos, 0xFFFFFF, "teleport_chain_connect");
            return;
        }

        if (!(hitState.getBlock() instanceof ChainConveyorBlock)) {
            if (isFirstInSameDim) {
                highlightConveyor(firstPos, 0xFFFFFF, "teleport_chain_connect");
            }
            return;
        }

        // Validate connection to targeted conveyor
        boolean success = validateConnection(level, firstPos, firstDim, pos, chainItem);
        int color = success ? 0x95CD41 : 0xEA5C2B;

        if (isFirstInSameDim) {
            highlightConveyor(firstPos, color, "teleport_chain_connect");
        }
        highlightConveyor(pos, color, "teleport_chain_connect_to");

        if (isFirstInSameDim) {
            Vec3 from = Vec3.atCenterOf(pos);
            Vec3 to = Vec3.atCenterOf(firstPos);
            Vec3 diff = from.subtract(to);

            if (diff.length() >= 1) {
                from = from.subtract(diff.normalize().scale(0.5));
                to = to.add(diff.normalize().scale(0.5));

                Vec3 normal = diff.cross(new Vec3(0, 1, 0));
                if (normal.lengthSqr() < 1e-4) {
                    normal = new Vec3(1, 0, 0);
                } else {
                    normal = normal.normalize().scale(0.875);
                }

                Outliner.getInstance().showLine("teleport_chain_connect_line", from.add(normal), to.add(normal))
                        .lineWidth(1 / 16f)
                        .colored(color);
                Outliner.getInstance().showLine("teleport_chain_connect_line_1", from.subtract(normal), to.subtract(normal))
                        .lineWidth(1 / 16f)
                        .colored(color);
            }
        }
    }

    private static boolean validateConnection(Level level, BlockPos firstPos, ResourceKey<Level> firstDim, BlockPos pos, AbstractTeleportChainItem chainItem) {
        if (pos.equals(firstPos) && level.dimension().equals(firstDim)) {
            return false;
        }

        if (!chainItem.isInterdimensional() && !firstDim.equals(level.dimension())) {
            return false;
        }

        BlockEntity targetTile = level.getBlockEntity(pos);
        if (!(targetTile instanceof ChainConveyorBlockEntity targetBE)) {
            return false;
        }

        if (targetBE.connections.size() >= 4) {
            return false;
        }

        return true;
    }

    private static void highlightConveyor(BlockPos pos, int color, String key) {
        for (int y : Iterate.zeroAndOne) {
            Vec3 prevV = VecHelper.rotate(new Vec3(0, .125 + y * .75, 1.25), -22.5, Axis.Y)
                    .add(Vec3.atBottomCenterOf(pos));
            for (int i = 0; i < 8; i++) {
                Vec3 v = VecHelper.rotate(new Vec3(0, .125 + y * .75, 1.25), 22.5 + i * 45, Axis.Y)
                        .add(Vec3.atBottomCenterOf(pos));
                Outliner.getInstance().showLine(key + y + i, prevV, v)
                        .lineWidth(1 / 16f)
                        .colored(color);
                prevV = v;
            }
        }
    }
}

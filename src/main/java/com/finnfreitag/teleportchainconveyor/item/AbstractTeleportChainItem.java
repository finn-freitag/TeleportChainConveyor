package com.finnfreitag.teleportchainconveyor.item;

import com.finnfreitag.teleportchainconveyor.component.TeleportChainLink;
import com.finnfreitag.teleportchainconveyor.handler.TeleportChainManager;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainDataComponents;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.UUID;

public abstract class AbstractTeleportChainItem extends Item {

    public AbstractTeleportChainItem(Properties properties) {
        super(properties);
    }

    public abstract boolean isInterdimensional();

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(clickedPos);

        if (!(be instanceof ChainConveyorBlockEntity conveyorBE)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        ResourceKey<Level> currentDim = level.dimension();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        TeleportChainLink link = stack.get(TeleportChainDataComponents.CHAIN_LINK.get());

        if (link == null) {
            // First click: Bind to this conveyor
            TeleportChainLink newLink = new TeleportChainLink(clickedPos, currentDim, UUID.randomUUID());
            stack.set(TeleportChainDataComponents.CHAIN_LINK.get(), newLink);

            if (player != null) {
                player.displayClientMessage(
                        Component.literal("Chain linked to conveyor at [" + clickedPos.getX() + ", " + clickedPos.getY() + ", " + clickedPos.getZ() + "] in " + currentDim.location().getPath())
                                .withStyle(ChatFormatting.GREEN), true);
            }
            level.playSound(null, clickedPos, SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8f, 1.2f);
            return InteractionResult.CONSUME;
        } else {
            // Second click: Connect first conveyor to this conveyor
            BlockPos sourcePos = link.pos();
            ResourceKey<Level> sourceDim = link.dimension();

            if (sourcePos.equals(clickedPos) && sourceDim.equals(currentDim)) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("Cannot connect a conveyor to itself!").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.FAIL;
            }

            if (!isInterdimensional() && !sourceDim.equals(currentDim)) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("Teleportation Chains cannot cross dimensions! Use an Interdimensional Chain instead.").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.FAIL;
            }

            boolean success = TeleportChainManager.createConnection(level, conveyorBE, sourcePos, sourceDim, isInterdimensional(), player);

            if (success) {
                if (player != null && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                stack.remove(TeleportChainDataComponents.CHAIN_LINK.get());
                level.playSound(null, clickedPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            } else {
                return InteractionResult.FAIL;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        TeleportChainLink link = stack.get(TeleportChainDataComponents.CHAIN_LINK.get());

        if (link != null) {
            tooltipComponents.add(Component.literal("Linked to: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("[" + link.pos().getX() + ", " + link.pos().getY() + ", " + link.pos().getZ() + "]")
                            .withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" (" + link.dimension().location().getPath() + ")")
                            .withStyle(ChatFormatting.AQUA)));
            tooltipComponents.add(Component.literal("Right-click another conveyor to complete connection.")
                    .withStyle(ChatFormatting.DARK_GREEN));
            tooltipComponents.add(Component.literal("Place in crafting grid to clear link.")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltipComponents.add(Component.literal("Unlinked. Right-click a Chain Conveyor to bind.")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}

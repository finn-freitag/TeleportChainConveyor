package com.finnfreitag.teleportchainconveyor.recipe;

import com.finnfreitag.teleportchainconveyor.item.AbstractTeleportChainItem;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainDataComponents;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class TeleportChainUnlinkRecipe extends CustomRecipe {

    public TeleportChainUnlinkRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int count = 0;
        ItemStack targetStack = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                count++;
                if (stack.getItem() instanceof AbstractTeleportChainItem && stack.has(TeleportChainDataComponents.CHAIN_LINK.get())) {
                    targetStack = stack;
                }
            }
        }
        return count == 1 && !targetStack.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof AbstractTeleportChainItem && stack.has(TeleportChainDataComponents.CHAIN_LINK.get())) {
                ItemStack result = stack.copyWithCount(1);
                result.remove(TeleportChainDataComponents.CHAIN_LINK.get());
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TeleportChainRecipes.UNLINK_RECIPE_SERIALIZER.get();
    }
}

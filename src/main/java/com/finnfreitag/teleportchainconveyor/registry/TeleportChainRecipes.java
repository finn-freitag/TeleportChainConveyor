package com.finnfreitag.teleportchainconveyor.registry;

import com.finnfreitag.teleportchainconveyor.Teleportchainconveyor;
import com.finnfreitag.teleportchainconveyor.recipe.TeleportChainUnlinkRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TeleportChainRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Teleportchainconveyor.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<TeleportChainUnlinkRecipe>> UNLINK_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("unlink_chain", () -> new SimpleCraftingRecipeSerializer<>(TeleportChainUnlinkRecipe::new));
}

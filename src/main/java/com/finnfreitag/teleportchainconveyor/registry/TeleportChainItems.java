package com.finnfreitag.teleportchainconveyor.registry;

import com.finnfreitag.teleportchainconveyor.Teleportchainconveyor;
import com.finnfreitag.teleportchainconveyor.item.InterdimensionalChainItem;
import com.finnfreitag.teleportchainconveyor.item.TeleportationChainItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TeleportChainItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Teleportchainconveyor.MODID);

    public static final DeferredItem<TeleportationChainItem> TELEPORTATION_CHAIN = ITEMS.register("teleportation_chain",
            () -> new TeleportationChainItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<InterdimensionalChainItem> INTERDIMENSIONAL_CHAIN = ITEMS.register("interdimensional_chain",
            () -> new InterdimensionalChainItem(new Item.Properties().stacksTo(16)));
}

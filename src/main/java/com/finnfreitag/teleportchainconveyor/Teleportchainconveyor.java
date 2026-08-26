package com.finnfreitag.teleportchainconveyor;

import com.finnfreitag.teleportchainconveyor.registry.TeleportChainAttachments;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainDataComponents;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainItems;
import com.finnfreitag.teleportchainconveyor.registry.TeleportChainRecipes;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Teleportchainconveyor.MODID)
public class Teleportchainconveyor {
    public static final String MODID = "teleportchainconveyor";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TELEPORT_TAB = CREATIVE_MODE_TABS.register("teleport_chain_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.teleportchainconveyor"))
                    .withTabsBefore(CreativeModeTabs.REDSTONE_BLOCKS)
                    .icon(() -> TeleportChainItems.TELEPORTATION_CHAIN.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(TeleportChainItems.TELEPORTATION_CHAIN.get());
                        output.accept(TeleportChainItems.INTERDIMENSIONAL_CHAIN.get());
                    })
                    .build());

    public Teleportchainconveyor(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        TeleportChainItems.ITEMS.register(modEventBus);
        TeleportChainDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        TeleportChainAttachments.ATTACHMENT_TYPES.register(modEventBus);
        TeleportChainRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Create: Teleport Chain Conveyor initialized!");
    }
}

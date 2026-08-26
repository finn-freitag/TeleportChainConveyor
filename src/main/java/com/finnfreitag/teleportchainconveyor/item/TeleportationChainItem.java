package com.finnfreitag.teleportchainconveyor.item;

public class TeleportationChainItem extends AbstractTeleportChainItem {

    public TeleportationChainItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isInterdimensional() {
        return false;
    }
}

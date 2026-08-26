package com.finnfreitag.teleportchainconveyor.item;

public class InterdimensionalChainItem extends AbstractTeleportChainItem {

    public InterdimensionalChainItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isInterdimensional() {
        return true;
    }
}

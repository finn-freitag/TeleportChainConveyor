package com.finnfreitag.teleportchainconveyor.attachment;

import java.util.UUID;

public interface ITeleportPackage {
    UUID getTeleportSourceConnectionId();
    void setTeleportSourceConnectionId(UUID id);
}

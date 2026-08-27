package com.finnfreitag.teleportchainconveyor.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TeleportChainData {

    public static final Codec<TeleportChainData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TeleportChainConnectionInfo.CODEC.listOf().fieldOf("connections").forGetter(TeleportChainData::getConnections)
    ).apply(instance, TeleportChainData::new));

    private final List<TeleportChainConnectionInfo> connections = new ArrayList<>();

    public TeleportChainData() {
    }

    public TeleportChainData(List<TeleportChainConnectionInfo> connections) {
        this.connections.addAll(connections);
    }

    public List<TeleportChainConnectionInfo> getConnections() {
        return connections;
    }

    public void addConnection(TeleportChainConnectionInfo info) {
        removeConnectionByVirtualPos(info.virtualRelativePos());
        connections.add(info);
    }

    public boolean removeConnectionByVirtualPos(BlockPos virtualRelativePos) {
        return connections.removeIf(c -> c.virtualRelativePos().equals(virtualRelativePos));
    }

    public boolean removeConnectionById(UUID connectionId) {
        return connections.removeIf(c -> c.connectionId().equals(connectionId));
    }

    public Optional<TeleportChainConnectionInfo> getConnectionById(UUID connectionId) {
        return connections.stream()
                .filter(c -> c.connectionId().equals(connectionId))
                .findFirst();
    }
}

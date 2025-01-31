package com.github.nalamodikk.common.network.mana_net;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ConduitNetwork {

    private final UUID networkId;
    private final Set<BlockPos> conduitPositions = new HashSet<>();
    private long storedMana = 0;

    public ConduitNetwork(UUID id) {
        this.networkId = id;
    }

    public UUID getId() {
        return networkId;
    }

    public Set<BlockPos> getConduitPositions() {
        return conduitPositions;
    }

    public void addConduit(BlockPos pos) {
        conduitPositions.add(pos);
    }

    public void removeConduit(BlockPos pos) {
        conduitPositions.remove(pos);
    }

    public void addAll(Set<BlockPos> positions) {
        conduitPositions.addAll(positions);
    }

    public long getStoredMana() {
        return storedMana;
    }

    public long requestMana(long amount) {
        long provided = Math.min(storedMana, amount);
        storedMana -= provided;
        return provided;
    }

    public void insertMana(long amount) {
        this.storedMana += amount;
    }
}

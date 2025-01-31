package com.github.nalamodikk.common.network.mana_net;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class ManaNetwork {
    private final Level level;
    private final Set<BlockPos> conduits = ConcurrentHashMap.newKeySet();
    private final Map<BlockPos, Integer> connectedMachines = new ConcurrentHashMap<>();

    public ManaNetwork(Level level) {
        this.level = level;
    }

    public boolean isEmpty() {
        return conduits.isEmpty() && connectedMachines.isEmpty();
    }

    public void addConduit(BlockPos pos) {
        conduits.add(pos);
        updateNetwork();
    }

    public void removeConduit(BlockPos pos) {
        conduits.remove(pos);
        updateNetwork();
    }

    public void registerMachine(BlockPos pos, int priority) {
        connectedMachines.put(pos, priority);
        updateNetwork();
    }

    public void removeMachine(BlockPos pos) {
        connectedMachines.remove(pos);
        updateNetwork();
    }

    public void updateNetwork() {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toVisit = new LinkedList<>();

        BlockPos start = !connectedMachines.isEmpty() ?
                connectedMachines.keySet().iterator().next() :
                conduits.stream().findFirst().orElse(null);
        if (start != null) toVisit.add(start);

        while (!toVisit.isEmpty()) {
            BlockPos current = toVisit.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (conduits.contains(neighbor) && !visited.contains(neighbor)) {
                    toVisit.add(neighbor);
                }
                if (connectedMachines.containsKey(neighbor) && !visited.contains(neighbor)) {
                    toVisit.add(neighbor);
                }
            }
        }

        conduits.retainAll(visited);
        connectedMachines.keySet().retainAll(visited);

        MagicalIndustryMod.LOGGER.info("Updated mana network, {} conduits active.", conduits.size());
    }

    public LazyOptional<IUnifiedManaHandler> getManaHandler(BlockPos pos, @Nullable Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            return blockEntity.getCapability(ModCapabilities.MANA, side);
        }
        return LazyOptional.empty();
    }
}

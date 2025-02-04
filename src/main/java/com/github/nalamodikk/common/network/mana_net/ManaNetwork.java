package com.github.nalamodikk.common.network.mana_net;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ManaNetwork {
    private final Level level;
    private final Set<BlockPos> conduits = ConcurrentHashMap.newKeySet();
    private final Map<BlockPos, Integer> connectedMachines = new ConcurrentHashMap<>();
    private final Set<BlockPos> visitedPositions = new HashSet<>();
    private static final Map<Level, ManaNetwork> networks = new ConcurrentHashMap<>();

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

    /**
     * 更新網絡結構，刪除無效的節點。
     */
    public void updateNetwork() {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toVisit = new LinkedList<>();

        // 找到網絡起點
        BlockPos start = !connectedMachines.isEmpty() ?
                connectedMachines.keySet().iterator().next() :
                conduits.stream().findFirst().orElse(null);

        if (start != null) toVisit.add(start);

        // 使用佇列遍歷網絡
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

        // 保留有效節點
        conduits.retainAll(visited);
        connectedMachines.keySet().retainAll(visited);

        MagicalIndustryMod.LOGGER.info("Updated mana network, {} conduits and {} machines active.", conduits.size(), connectedMachines.size());
    }

    /**
     * 獲取 Mana 處理器，防止遞迴。
     */
    public LazyOptional<IUnifiedManaHandler> getManaHandler(BlockPos pos, Direction direction) {
        if (visitedPositions.contains(pos)) {
            return LazyOptional.empty(); // 防止無限遞迴
        }

        visitedPositions.add(pos); // 標記位置已訪問

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            return blockEntity.getCapability(ModCapabilities.MANA, direction);
        }

        return LazyOptional.empty();
    }

    /**
     * 將 Mana 分配到所有機器。
     */
    public void distributeMana(int totalMana) {
        if (connectedMachines.isEmpty()) return;

        // 使用 AtomicInteger 來追蹤剩餘 Mana
        AtomicInteger remainingMana = new AtomicInteger(totalMana);

        // 按優先級排序
        List<Map.Entry<BlockPos, Integer>> sortedMachines = new ArrayList<>(connectedMachines.entrySet());
        sortedMachines.sort(Map.Entry.comparingByValue());

        for (Map.Entry<BlockPos, Integer> entry : sortedMachines) {
            BlockPos machinePos = entry.getKey();
            BlockEntity machineEntity = level.getBlockEntity(machinePos);

            if (machineEntity != null) {
                LazyOptional<IUnifiedManaHandler> manaOpt = machineEntity.getCapability(ModCapabilities.MANA);
                manaOpt.ifPresent(manaHandler -> {
                    int neededMana = manaHandler.getNeeded();
                    int toTransfer = Math.min(neededMana, remainingMana.get());

                    if (toTransfer > 0) {
                        manaHandler.addMana(toTransfer, ManaAction.EXECUTE);
                        remainingMana.addAndGet(-toTransfer); // 減少剩餘 Mana

                        if (remainingMana.get() <= 0) {
                            return; // 結束迴圈
                        }
                    }
                });
            }
        }

        // 最後更新 totalMana
        totalMana = remainingMana.get();
    }
}

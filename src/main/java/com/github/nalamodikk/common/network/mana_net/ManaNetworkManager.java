package com.github.nalamodikk.common.network.mana_net;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.blockentity.Conduit.ManaConduitBlockEntity;
import com.github.nalamodikk.common.mana.ManaAction;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ManaNetworkManager {
    // 全局網絡映射
    private static final Map<Level, ManaNetworkManager> managers = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    // 當前世界的導管與機器數據
    private final Level level;
    private final Set<BlockPos> conduits = new HashSet<>();
    private final Map<BlockPos, Integer> connectedMachines = new HashMap<>();

    private ManaNetworkManager(Level level) {
        this.level = level;
    }

    /**
     * 獲取或創建 ManaNetworkManager 實例
     */
    public static ManaNetworkManager getInstance(Level level) {
        return managers.computeIfAbsent(level, ManaNetworkManager::new);
    }

    /**
     * 移除指定 Level 的 ManaNetworkManager（通常在世界卸載時調用）
     */
    public static void removeInstance(Level level) {
        managers.remove(level);
    }

    /**
     * 註冊導管
     */
    public void registerConduit(BlockPos pos) {
        conduits.add(pos);
        updateNetwork();
    }

    /**
     * 移除導管
     */
    public void removeConduit(BlockPos pos) {
        conduits.remove(pos);
        updateNetwork();
    }

    /**
     * 註冊機器
     */
    public void registerMachine(BlockPos pos, int priority) {
        connectedMachines.put(pos, priority);
        updateNetwork();
    }

    /**
     * 移除機器
     */
    public void removeMachine(BlockPos pos) {
        connectedMachines.remove(pos);
        updateNetwork();
    }

    /**
     * 插入魔力到連接的機器
     */
    public long insertMana(BlockPos pos, long mana) {
        LOGGER.debug("Trying to insert {} mana at {}", mana, pos);

        return handleManaTransfer(pos, mana, true, new HashSet<>(), 0);

    }

    /**
     * 從連接的機器中提取魔力
     */
    public long requestMana(BlockPos pos, long amount) {
        return amount - handleManaTransfer(pos, amount, false, new HashSet<>(), 0);
    }

    /**
     * 更新網絡結構
     */
    protected void updateNetwork() {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toVisit = new LinkedList<>();

        if (!connectedMachines.isEmpty()) {
            toVisit.add(connectedMachines.keySet().iterator().next());
        } else if (!conduits.isEmpty()) {
            toVisit.add(conduits.iterator().next());
        }

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
    }

    /**
     * 魔力傳輸邏輯
     */
    private long handleManaTransfer(BlockPos pos, long mana, boolean isInsertMode, Set<BlockPos> visited, int depth) {
        if (visited.contains(pos)) return mana; // 防止循環
        visited.add(pos);

        long remainingMana = mana;

        // **1️⃣ 嘗試將魔力送入機器**
        for (Map.Entry<BlockPos, Integer> entry : connectedMachines.entrySet()) {
            BlockPos machinePos = entry.getKey();
            LazyOptional<IUnifiedManaHandler> cap = getCapability(machinePos);
            if (cap.isPresent()) {
                IUnifiedManaHandler handler = cap.orElseThrow(() -> new IllegalStateException("Missing mana capability at " + machinePos));
                long processed = isInsertMode
                        ? handler.insertMana((int) remainingMana, ManaAction.EXECUTE)
                        : handler.extractMana((int) remainingMana, ManaAction.EXECUTE);
                remainingMana -= processed;
                if (remainingMana <= 0) return 0;
            }
        }

        // **2️⃣ 嘗試將魔力送入相鄰導管**
        for (BlockPos conduit : conduits) {
            if (!conduit.equals(pos)) {
                BlockEntity conduitEntity = level.getBlockEntity(conduit);
                if (conduitEntity instanceof ManaConduitBlockEntity neighborConduit) {
                    LazyOptional<IUnifiedManaHandler> neighborManaOpt = conduitEntity.getCapability(ModCapabilities.MANA);
                    if (neighborManaOpt.isPresent()) {
                        IUnifiedManaHandler neighborMana = neighborManaOpt.orElseThrow(() -> new IllegalStateException("Mana capability missing at " + conduit));
                        int manaToTransfer = Math.min((int) remainingMana, neighborMana.getNeeded());
                        int accepted = neighborMana.insertMana(manaToTransfer, ManaAction.EXECUTE);
                        remainingMana -= accepted;
                    }
                }
                MagicalIndustryMod.LOGGER.debug("Transferring mana from {} to {}: {}", pos, conduit, remainingMana);

                remainingMana = handleManaTransfer(conduit, remainingMana, isInsertMode, visited, depth + 1);
                if (remainingMana <= 0) return 0;
            }
        }

        return remainingMana;
    }

    /**
     * 獲取方塊的 Mana 能力
     */
    private LazyOptional<IUnifiedManaHandler> getCapability(BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            return blockEntity.getCapability(ModCapabilities.MANA);
        }
        return LazyOptional.empty();
    }

    /**
     *
     * @param pos
     * @param side
     * @return
     */
    public LazyOptional<IUnifiedManaHandler> getManaHandler(BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            return blockEntity.getCapability(ModCapabilities.MANA, side);
        }
        return LazyOptional.empty();
    }

}

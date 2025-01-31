package com.github.nalamodikk.common.network.mana_net;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NetworkManager {

    // 靜態變量，用於管理所有 Level 的 NetworkManager 實例
    private static final Map<Level, NetworkManager> managers = new HashMap<>();

    private final Level level; // 當前世界
    private final Map<BlockPos, Integer> connectedMachines = new HashMap<>(); // 儲存機器位置與優先級
    private final Set<BlockPos> conduits = new HashSet<>(); // 儲存所有導管位置

    private static final int MAX_RECURSION_DEPTH = 1000;

    // 私有構造函數，禁止外部直接調用
    private NetworkManager(Level level) {
        this.level = level;
    }

    /**
     * 獲取或創建對應 Level 的 NetworkManager 實例
     */
    public static NetworkManager getInstance(Level level) {
        return managers.computeIfAbsent(level, NetworkManager::new);
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
     * 註冊設備
     */
    public void registerMachine(BlockPos pos, int priority) {
        connectedMachines.put(pos, priority);
        updateNetwork();
    }

    /**
     * 移除設備
     */
    public void removeMachine(BlockPos pos) {
        connectedMachines.remove(pos);
        updateNetwork();
    }

    /**
     * 插入魔力到連接的設備中
     */
    public long insertMana(BlockPos pos, long mana) {
        return handleManaTransfer(pos, mana, true, new HashSet<>(), 0);
    }

    /**
     * 從連接的設備中提取魔力
     */
    public long requestMana(BlockPos requester, long amount) {
        return amount - handleManaTransfer(requester, amount, false, new HashSet<>(), 0);
    }

    /**
     * 更新網路結構
     */
    public void updateNetwork() {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toVisit = new LinkedList<>();

        // 從設備或導管開始遍歷
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

        // 只保留真正連接的節點
        conduits.retainAll(visited);
        connectedMachines.keySet().retainAll(visited);

        MagicalIndustryMod.LOGGER.info("Updated mana network: {} conduits, {} machines.", conduits.size(), connectedMachines.size());
    }

    /**
     * 魔力傳輸邏輯
     */
    private long handleManaTransfer(BlockPos pos, long mana, boolean isInsertMode, Set<BlockPos> visited, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            MagicalIndustryMod.LOGGER.error("Recursion limit exceeded at: " + pos);
            return mana;
        }
        if (visited.contains(pos)) return mana;
        visited.add(pos);

        long remainingMana = mana;

        // 與設備交換魔力
        for (Map.Entry<BlockPos, Integer> entry : connectedMachines.entrySet()) {
            BlockPos machinePos = entry.getKey();
            LazyOptional<IUnifiedManaHandler> cap = getCapability(machinePos);

            if (cap.isPresent()) {
                IUnifiedManaHandler handler = cap.orElseThrow(() -> new IllegalStateException("Mana capability missing at " + machinePos));
                long processed = isInsertMode
                        ? handler.insertMana((int) remainingMana, ManaAction.EXECUTE)
                        : handler.extractMana((int) remainingMana, ManaAction.EXECUTE);
                remainingMana -= processed;

                MagicalIndustryMod.LOGGER.debug("Transferred {} mana to machine at {}", processed, machinePos);

                if (remainingMana <= 0) return 0;
            }
        }

        // 與相鄰導管傳輸魔力
        for (BlockPos conduit : conduits) {
            if (!conduit.equals(pos) && isConnected(pos, conduit)) {
                remainingMana = handleManaTransfer(conduit, remainingMana, isInsertMode, visited, depth + 1);
                if (remainingMana <= 0) return 0;
            }
        }

        return remainingMana;
    }

    /**
     * 檢查兩個位置是否相連
     */
    private boolean isConnected(BlockPos a, BlockPos b) {
        if (!conduits.contains(a) || !conduits.contains(b)) return false;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(a);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (current.equals(b)) return true;
            if (visited.contains(current)) continue;
            visited.add(current);

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (conduits.contains(neighbor) && !visited.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    /**
     * 獲取方塊的 Mana 能力
     */
    private LazyOptional<IUnifiedManaHandler> getCapability(BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            return be.getCapability(ModCapabilities.MANA);
        }
        return LazyOptional.empty();
    }
}

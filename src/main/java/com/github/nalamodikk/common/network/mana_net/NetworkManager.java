package com.github.nalamodikk.common.network.mana_net;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

import java.util.*;

public class NetworkManager {
    // 靜態變量，用於管理所有 Level 的 NetworkManager 實例
    private static final Map<Level, NetworkManager> managers = new HashMap<>();

    private final Level level;
    private final Map<BlockPos, Integer> connectedMachines = new HashMap<>(); // 儲存位置與權重
    private final Set<BlockPos> conduits = new HashSet<>(); // 所有導管位置

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
     * 插入魔力
     */
    /**
     * 插入魔力到連接的設備中
     */
    // 🔥 讓 `insertMana()` 和 `requestMana()` 呼叫時傳入新的 `visited` 集合
    public long insertMana(BlockPos pos, long mana) {
        return handleManaTransfer(pos, mana, true, new HashSet<>()); // 插入模式
    }

    /**
     * 處理魔力請求，從連接的設備中提取魔力
     */


    public int requestMana(BlockPos requester, int amount) {
        return (int) (amount - handleManaTransfer(requester, amount, false, new HashSet<>())); // 提取模式
    }
    /**
     * 處理魔力的插入和提取邏輯
     *
     * @param pos          發起插入/請求的位置
     * @param mana         插入/請求的魔力量
     * @param isInsertMode 是否為插入模式（true：插入；false：請求）
     * @return 剩餘未處理的魔力量
     */
    private long handleManaTransfer(BlockPos pos, long mana, boolean isInsertMode, Set<BlockPos> visited) {
        if (visited.contains(pos)) return mana; // 避免無限循環
        visited.add(pos);

        List<Map.Entry<BlockPos, Integer>> sortedMachines = new ArrayList<>(connectedMachines.entrySet());
        sortedMachines.sort(Map.Entry.comparingByValue()); // 根據優先級排序

        long remainingMana = mana;

        // **1️⃣ 先嘗試與設備交換魔力**
        for (Map.Entry<BlockPos, Integer> entry : sortedMachines) {
            BlockPos machinePos = entry.getKey();
            LazyOptional<IUnifiedManaHandler> cap = getCapability(machinePos);

            if (cap.isPresent()) {
                IUnifiedManaHandler handler = cap.orElseThrow(() ->
                        new IllegalStateException("Mana capability missing at " + machinePos)
                );

                long processed = isInsertMode
                        ? handler.insertMana((int) remainingMana, ManaAction.get(false))
                        : handler.extractMana((int) remainingMana, ManaAction.get(false));

                remainingMana -= processed;

                if (remainingMana <= 0) return 0; // 魔力已完全處理，結束
            }
        }

        // **2️⃣ 傳輸魔力到導管內部**
        for (BlockPos conduit : conduits) {
            if (!conduit.equals(pos) && isConnected(pos, conduit)) {
                remainingMana = handleManaTransfer(conduit, remainingMana, isInsertMode, visited);
                if (remainingMana <= 0) return 0;
            }
        }

        return remainingMana;
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
     * 更新網路結構
     */
    public void updateNetwork() {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toVisit = new LinkedList<>();

        if (!connectedMachines.isEmpty()) {
            BlockPos start = connectedMachines.keySet().iterator().next();
            toVisit.add(start);
        } else if (!conduits.isEmpty()) {
            BlockPos start = conduits.iterator().next();
            toVisit.add(start);
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

        // 只保留已訪問的導管，刪除不連接的導管
        conduits.retainAll(visited);
    }


    /**
     * 檢查是否相連
     */
    private boolean isConnected(BlockPos a, BlockPos b) {
        return a.distSqr(b) <= 1; // 檢查兩個方塊是否相鄰
    }

    /**
     * 獲取方塊的 Mana 能力
     */
    private LazyOptional<IUnifiedManaHandler> getCapability(BlockPos pos) {
        return level.getBlockEntity(pos).getCapability(ModCapabilities.MANA);
    }

    public static void onConduitPlaced(Level level, BlockPos pos) {
        // 確保 NetworkManager 的實例存在
        NetworkManager manager = getInstance(level);
        if (manager != null) {
            // 將新放置的導管加入到網路中
            manager.registerConduit(pos);
        }
    }

}

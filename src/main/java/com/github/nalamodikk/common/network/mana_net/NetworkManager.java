package com.github.nalamodikk.common.network.mana_net;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraft.core.BlockPos;
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
    public long insertMana(BlockPos pos, long mana) {
        return handleManaTransfer(pos, mana, true); // 插入模式
    }

    /**
     * 處理魔力請求，從連接的設備中提取魔力
     */
    public int requestMana(BlockPos requester, int amount) {
        return (int) (amount - handleManaTransfer(requester, amount, false)); // 提取模式
    }

    /**
     * 處理魔力的插入和提取邏輯
     *
     * @param pos          發起插入/請求的位置
     * @param mana         插入/請求的魔力量
     * @param isInsertMode 是否為插入模式（true：插入；false：請求）
     * @return 剩餘未處理的魔力量
     */
    private long handleManaTransfer(BlockPos pos, long mana, boolean isInsertMode) {
        List<Map.Entry<BlockPos, Integer>> sortedMachines = new ArrayList<>(connectedMachines.entrySet());
        sortedMachines.sort(Map.Entry.comparingByValue()); // 根據權重排序

        long remainingMana = mana;

        for (Map.Entry<BlockPos, Integer> entry : sortedMachines) {
            BlockPos machinePos = entry.getKey();
            LazyOptional<IUnifiedManaHandler> cap = getCapability(machinePos);

            if (cap.isPresent()) {
                IUnifiedManaHandler handler = cap.orElseThrow(() ->
                        new IllegalStateException("Mana capability missing at " + machinePos)
                );

                long processed = isInsertMode
                        ? handler.insertMana((int) remainingMana, ManaAction.get(false)) // 插入模式
                        : handler.extractMana((int) remainingMana, ManaAction.get(false)); // 提取模式

                remainingMana -= processed;

                // 日誌記錄（可選）
                MagicalIndustryMod.LOGGER.info((isInsertMode ? "Inserted" : "Extracted") + " " + processed + " mana at " + machinePos);

                if (remainingMana <= 0) {
                    break; // 已完成處理，退出
                }
            }
        }

        return remainingMana; // 返回未處理的剩餘魔力量
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
        }

        while (!toVisit.isEmpty()) {
            BlockPos current = toVisit.poll();

            if (visited.contains(current)) continue;
            visited.add(current);

            for (BlockPos conduit : conduits) {
                if (isConnected(current, conduit) && !visited.contains(conduit)) {
                    toVisit.add(conduit);
                }
            }
        }

        // 移除孤立的導管
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

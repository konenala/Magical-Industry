package com.github.nalamodikk.common.network.ManaNetwork;

import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

public class ManaNetworkNode {
    private final BlockPos position;
    private final Level level;
    private int manaBuffer = 0;
    private static final int MAX_BUFFER = 100;
    private final Set<ManaNetworkNode> neighbors = new HashSet<>();


    public ManaNetworkNode(BlockPos position, Level level) {
        this.position = position;
        this.level = level;
    }

    // 獲取節點位置
    public BlockPos getPosition() {
        return position;
    }

    // 獲取節點所在世界
    public Level getLevel() {
        return level;
    }

    // 獲取當前存儲的魔力
    public int getManaStored() {
        return manaBuffer;
    }

    // 接收魔力
    public int receiveMana(int amount) {
        int receivedAmount = Math.min(amount, MAX_BUFFER - manaBuffer); // 計算可接收的魔力
        manaBuffer += receivedAmount; // 更新魔力緩衝
        return receivedAmount; // 返回實際接收的數量
    }


    public int transferMana(int amount) {
        int transferable = Math.min(amount, manaBuffer); // 計算可傳輸的魔力
        manaBuffer -= transferable; // 更新緩衝區
        return transferable; // 返回實際傳輸的魔力數量
    }


    // 提取魔力
    public int extractMana(int amount) {
        int extracted = Math.min(amount, manaBuffer);
        manaBuffer -= extracted;
        return extracted;
    }

    // 是否能接受更多魔力
    public boolean canAcceptMana() {
        return manaBuffer < MAX_BUFFER;
    }

    // 是否為存儲節點（預設為否）
    public boolean isStorageNode() {
        return false;
    }

    // 是否為消費節點（預設為否）
    public boolean isConsumerNode() {
        return false;
    }

    // 鄰居管理：添加鄰居節點
    public void addNeighbor(ManaNetworkNode neighbor) {
        neighbors.add(neighbor);
    }

    // 鄰居管理：移除鄰居節點
    public void removeNeighbor(ManaNetworkNode neighbor) {
        neighbors.remove(neighbor);
    }

    // 獲取鄰居節點
    public Set<ManaNetworkNode> getNeighbors() {
        return neighbors;
    }

    // 驗證節點是否有效
    public boolean isValid() {
        return level != null && position != null;
    }

    // 自動分配魔力至鄰居
    public void distributeManaToNeighbors() {
        for (ManaNetworkNode neighbor : neighbors) {
            if (neighbor.canAcceptMana()) {
                int transferAmount = Math.min(manaBuffer, 10); // 每次最多傳輸 10 單位
                int accepted = neighbor.receiveMana(transferAmount);
                manaBuffer -= accepted;

                // 日誌或調試信息
                MagicalIndustryMod.LOGGER.info("Transferred {} mana to neighbor at {}", accepted, neighbor.getPosition());
            }
        }
    }


    // 用於處理事件更新，例如重新構建網路後自動更新
    public void onNetworkUpdate() {
        distributeManaToNeighbors();
    }

    public int getRequestedMana() {
        if (isConsumerNode()) {
            return Math.max(0, MAX_BUFFER - manaBuffer); // 返回消費節點缺少的魔力數量
        }
        return 0; // 不是消費節點時請求為 0
    }

}

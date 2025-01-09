package com.github.nalamodikk.common.network.ManaNetwork;

import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class ManaConduitNode extends ManaNetworkNode {
    private int manaBuffer = 0;
    private static int maxBuffer = 100; // 改為靈活的靜態變量
    private static final float BASE_LOSS_RATE = 0.03f; // 預設傳輸損耗
    private Float customLossRate = null; // 自定義損耗率

    public ManaConduitNode(BlockPos position, Level level) {
        super(position, level);
    }

    @Override
    public boolean isStorageNode() {
        return false; // 管道不是存儲節點
    }

    @Override
    public boolean isConsumerNode() {
        return false; // 管道也不是消費節點
    }

    @Override
    public int getManaStored() {
        return manaBuffer;
    }

    @Override
    public int getRequestedMana() {
        return Math.max(0, maxBuffer - manaBuffer); // 如果有空間就請求魔力
    }

    @Override
    public int receiveMana(int amount) {
        int receivedAmount = (int) (amount * (1.0f - getTransmissionLossRate()));
        int acceptedAmount = Math.min(receivedAmount, maxBuffer - manaBuffer);
        manaBuffer += acceptedAmount;
        MagicalIndustryMod.LOGGER.info("Received mana: {} at {}", acceptedAmount, getPosition());
        return acceptedAmount;
    }

    @Override
    public int transferMana(int amount) {
        int transferable = Math.min(amount, manaBuffer);
        manaBuffer -= transferable;
        return transferable;
    }

    @Override
    public Level getLevel() {
        return super.getLevel();
    }

    public void transferManaToNeighbors() {
        Level currentLevel = this.getLevel(); // 通過 getter 獲取 Level
        if (currentLevel == null) return;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = getPosition().relative(direction); // 使用 getPosition()
            ManaNetworkNode neighbor = ManaNetworkManager.getInstance().getNode(currentLevel, neighborPos);

            if (neighbor != null && neighbor.canAcceptMana()) {
                int transferAmount = Math.min(this.manaBuffer, 50);
                int accepted = neighbor.receiveMana(transferAmount);
                this.manaBuffer -= accepted;
                System.out.println("Transferred mana: " + accepted + " from " + getPosition() + " to " + neighbor.getPosition());
            }
        }
    }


    public float getTransmissionLossRate() {
        return (customLossRate != null) ? customLossRate : BASE_LOSS_RATE;
    }

    public void setCustomLossRate(float newLossRate) {
        if (newLossRate >= 0.0f && newLossRate <= 1.0f) {
            this.customLossRate = newLossRate;
        }
    }

    public static void setMaxBuffer(int newMaxBuffer) {
        maxBuffer = newMaxBuffer;
    }

    public static int getMaxBuffer() {
        return maxBuffer;
    }

    public void onNeighborUpdate() {
        transferManaToNeighbors();
    }


}

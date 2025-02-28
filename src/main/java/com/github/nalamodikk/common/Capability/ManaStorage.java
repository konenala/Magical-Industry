package com.github.nalamodikk.common.Capability;

import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * ManaStorage 是一個魔力存儲類，支持插入、提取以及同步功能。
 */
public class ManaStorage implements IUnifiedManaHandler , INBTSerializable<CompoundTag>  {
    public static final Capability<IUnifiedManaHandler> MANA = CapabilityManager.get(new CapabilityToken<>() {});
    private int storedMana;

    private int mana; // 當前魔力存儲量
    private final int capacity; // 最大魔力存儲量


    private Level level; // 添加 Level
    private BlockPos worldPosition; // 添加 BlockPos

    public ManaStorage(int capacity) {
        this.capacity = capacity;
        this.mana = 0;

    }

    /**
     * 檢查當前是否已滿。
     */
    public boolean isFull() {
        return mana >= capacity;
    }

    /**
     * 檢查當前是否為空。
     */
    public boolean isEmpty() {
        return mana <= 0;
    }



    @Override
    public void consumeMana(int amount) {
        mana = Math.max(mana - amount, 0);
        onChanged(); // 通知數據變更
    }

    /**
     * 獲取當前儲存的魔力值
     *
     * @return 當前魔力值
     */
    public int getManaStored() {
        return mana;
    }

    @Override
    public int getMana() {
        return mana;
    }

    @Override
    public void addMana(int amount, ManaAction action) {
        insertMana(0, amount, action); // 單槽存儲，使用容器索引 0
    }


    @Override
    public void setMana(int amount) {
        mana = Math.min(Math.max(amount, 0), capacity);
        onChanged(); // 通知數據變更
    }

    @Override
    public int getMaxMana() {
        return capacity;
    }

    @Override
    public int getManaContainerCount() {
        return 1; // 單槽存儲
    }

    @Override
    public int getMana(int container) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);
        return mana;
    }

    @Override
    public void setMana(int container, int mana) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);
        this.mana = Math.min(Math.max(mana, 0), capacity);
        onChanged();
    }

    @Override
    public int getMaxMana(int container) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);
        return capacity;
    }

    @Override
    public int getNeededMana(int container) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);
        return capacity - mana;
    }

    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);

        // 計算可以插入的量
        int manaToInsert = Math.min(amount, capacity - mana);
        if (action.execute()) {
            mana += manaToInsert;
            onChanged(); // 通知數據變更
        }
        return manaToInsert;
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);

        // 計算可以提取的量
        int manaToExtract = Math.min(amount, mana);
        if (action.execute()) {
            mana -= manaToExtract;
            onChanged(); // 通知數據變更
        }
        return manaToExtract;
    }

    /**
     * 當魔力變化時調用，用於同步或觸發事件。
     * 可以在這裡實現數據同步或其他邏輯。
     */
    @Override
    public void onChanged() {
        if (level != null && !level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(worldPosition);
            if (blockEntity != null) {
                BlockState state = level.getBlockState(worldPosition);
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("StoredMana", storedMana); // 存入魔力數值
        tag.putInt("Capacity", capacity);     // 存入魔力容量（可選）
        return tag;
    }

    // ✅ **從 NBT 讀取魔力數據**
    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.storedMana = tag.getInt("StoredMana"); // 讀取魔力數值
    }
}

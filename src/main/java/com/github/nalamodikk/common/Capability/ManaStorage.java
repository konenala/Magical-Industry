package com.github.nalamodikk.common.Capability;

import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ManaStorage implements IUnifiedManaHandler {
    public static final Capability<ManaStorage> MANA = CapabilityManager.get(new CapabilityToken<>() {});

    private int mana;
    private final int capacity;

    public ManaStorage(int capacity) {
        this.capacity = capacity;
        this.mana = 0;
    }

    public boolean isFull() {
        return this.getMana() >= this.getMaxMana();
    }


    @Override
    public void addMana(int amount) {
        this.mana = Math.min(this.mana + amount, capacity);
        onChanged(); // 添加魔力時通知變化
    }

    @Override
    public void consumeMana(int amount) {
        this.mana = Math.max(this.mana - amount, 0);
        onChanged(); // 消耗魔力時通知變化
    }

    @Override
    public int getMana() {
        return mana;
    }

    @Override
    public void setMana(int amount) {
        this.mana = Math.min(amount, capacity);
        onChanged(); // 設置魔力時通知變化
    }

    @Override
    public void onChanged() {
        // 可以加入一些狀態同步的邏輯，如果需要的話
    }

    @Override
    public int getMaxMana() {
        return capacity;
    }

    @Override
    public int getManaContainerCount() {
        return 0;
    }

    @Override
    public int getMana(int container) {
        return 0;
    }

    @Override
    public void setMana(int container, int mana) {

    }

    @Override
    public int getMaxMana(int container) {
        return 0;
    }

    @Override
    public int getNeededMana(int container) {
        return 0;
    }

    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        // 如果当前魔力已满，则无法插入
        if (this.mana >= this.capacity) {
            return 0;
        }

        // 计算实际可以插入的魔力
        int manaToInsert = Math.min(amount, this.capacity - this.mana);

        // 如果是执行操作，实际插入魔力
        if (action.execute()) {
            this.mana += manaToInsert;
            onChanged(); // 通知数据已更改
        }

        return manaToInsert;
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        // 如果当前没有足够的魔力，则只能提取现有魔力
        int manaToExtract = Math.min(amount, this.mana);

        // 如果是执行操作，实际提取魔力
        if (action.execute()) {
            this.mana -= manaToExtract;
            onChanged(); // 通知数据已更改
        }

        return manaToExtract;
    }

}


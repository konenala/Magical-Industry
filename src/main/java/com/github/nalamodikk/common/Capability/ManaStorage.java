package com.github.nalamodikk.common.Capability;

public class ManaStorage implements IMana {
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
}


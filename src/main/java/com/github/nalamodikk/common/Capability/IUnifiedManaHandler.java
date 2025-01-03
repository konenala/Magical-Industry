package com.github.nalamodikk.common.Capability;

import com.github.nalamodikk.common.mana.ManaAction;

public interface IUnifiedManaHandler {

    // 單一槽位管理
    int getMana();
    void addMana(int amount);
    void consumeMana(int amount);
    void setMana(int amount);
    void onChanged();
    int getMaxMana();

    default int getNeeded() {
        return Math.max(0, getMaxMana() - getMana());
    }

    /**
     * Checks if this handler can receive mana.
     * @return True if mana can be received.
     */
    default boolean canReceive() {
        return getNeeded() > 0; // 如果還有空間則表示可以接收
    }

    /**
     * Receives mana from an external source.
     * @param amount Mana to receive.
     * @param action The action to perform, either {@link ManaAction#EXECUTE} or {@link ManaAction#SIMULATE}.
     * @return The amount of mana that was actually received.
     */
    default int receiveMana(int amount, ManaAction action) {
        if (amount <= 0) {
            return 0;
        }
        int toReceive = Math.min(amount, getNeeded());
        if (action.execute()) {
            addMana(toReceive);
            onChanged(); // 接收成功後通知變化
        }
        return toReceive; // 返回實際接收的量
    }

    /**
     * Inserts mana into the storage.
     * @param amount Mana to insert.
     * @param action The action to perform, either {@link ManaAction#EXECUTE} or {@link ManaAction#SIMULATE}.
     * @return The amount of mana actually inserted.
     */
    default int insertMana(int amount, ManaAction action) {
        if (amount <= 0) {
            return 0;
        }
        int toAdd = Math.min(amount, getNeeded());
        if (action.execute()) {
            addMana(toAdd);
            onChanged(); // 插入成功後通知變化
        }
        return toAdd; // 返回實際插入的量
    }

    /**
     * Extracts mana from the storage.
     * @param amount Mana to extract.
     * @param action The action to perform, either {@link ManaAction#EXECUTE} or {@link ManaAction#SIMULATE}.
     * @return The amount of mana actually extracted.
     */
    default int extractMana(int amount, ManaAction action) {
        if (amount <= 0 || getMana() == 0) {
            return 0;
        }
        int extracted = Math.min(amount, getMana());
        if (action.execute()) {
            consumeMana(extracted);
            onChanged(); // 提取成功後通知變化
        }
        return extracted; // 返回實際提取的量
    }

    // 多槽位管理
    int getManaContainerCount();

    int getMana(int container);

    void setMana(int container, int mana);

    int getMaxMana(int container);

    int getNeededMana(int container);

    int insertMana(int container, int amount, ManaAction action);

    int extractMana(int container, int amount, ManaAction action);

    /**
     * Attempts to insert mana into all containers sequentially.
     * @param amount Total mana to insert.
     * @param action The action to perform, either {@link ManaAction#EXECUTE} or {@link ManaAction#SIMULATE}.
     * @return The remaining mana that could not be inserted.
     */
    default int insertManaToAllContainers(int amount, ManaAction action) {
        int remaining = amount;
        for (int i = 0; i < getManaContainerCount(); i++) {
            remaining = insertMana(i, remaining, action);
            if (remaining == 0) {
                break;
            }
        }
        return remaining; // 返回剩餘未插入的量
    }

    /**
     * Attempts to extract mana from all containers sequentially.
     * @param amount Total mana to extract.
     * @param action The action to perform, either {@link ManaAction#EXECUTE} or {@link ManaAction#SIMULATE}.
     * @return The total amount of mana actually extracted.
     */
    default int extractManaFromAllContainers(int amount, ManaAction action) {
        int extracted = 0;
        for (int i = 0; i < getManaContainerCount(); i++) {
            extracted += extractMana(i, amount - extracted, action);
            if (extracted >= amount) {
                break;
            }
        }
        return extracted; // 返回實際提取的量
    }
}

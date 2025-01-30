package com.github.nalamodikk.core.block;

import net.minecraft.world.item.Item;

public interface IItemBlock {
    /**
     * 返回與該方塊相關聯的物品。
     *
     * @return 該方塊對應的物品
     */
    Item toItem();

    /**
     * 檢查該方塊是否與物品關聯。
     *
     * @return 如果已關聯則返回 true，否則返回 false
     */
    boolean hasItem();

    /**
     * 設置該方塊與物品的關聯。
     *
     * @param item 要關聯的物品
     */
    void setItem(Item item);
}

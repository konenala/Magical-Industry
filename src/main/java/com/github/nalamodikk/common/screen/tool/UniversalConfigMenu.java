package com.github.nalamodikk.common.screen.tool;


import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.screen.ModMenusTypes;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumMap;
import java.util.Objects;


public class UniversalConfigMenu extends AbstractContainerMenu {
    private final EnumMap<Direction, Boolean> currentConfig = new EnumMap<>(Direction.class);
    private final Player player;

    private final BlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public UniversalConfigMenu(int id, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenusTypes.UNIVERSAL_CONFIG.get(), id);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;

        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (blockEntity instanceof IConfigurableBlock configurableBlock) {
            // 從 BlockEntity 獲取當前的方向配置
            for (Direction direction : Direction.values()) {
                this.currentConfig.put(direction, configurableBlock.isOutput(direction));
            }
        }

    }

    public Player getPlayer() {
        return player;
    }

    public void updateConfig(Direction direction, boolean isOutput) {
        currentConfig.put(direction, isOutput);

        if (this.blockEntity instanceof IConfigurableBlock configurableBlock) {
            configurableBlock.setDirectionConfig(direction, isOutput);
            this.blockEntity.setChanged(); // 標記方塊為已更新，以便後續保存
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            if (this.blockEntity instanceof IConfigurableBlock configurableBlock) {
                // 保存當前界面的配置數據到 BlockEntity
                for (Direction direction : Direction.values()) {
                    boolean newConfig = this.currentConfig.get(direction);
                    configurableBlock.setDirectionConfig(direction, newConfig);
                    // 打印日誌檢查更新
                    MagicalIndustryMod.LOGGER.info("Direction {} now set as {}", direction, newConfig ? "Output" : "Input");
                }
                // 標記 BlockEntity 為已變更以保存數據
                this.blockEntity.setChanged();
            }
        }
    }



    public UniversalConfigMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, Objects.requireNonNull(playerInventory.player.level().getBlockEntity(buf.readBlockPos())));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    public BlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
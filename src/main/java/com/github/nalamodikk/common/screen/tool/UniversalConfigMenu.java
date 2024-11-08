package com.github.nalamodikk.common.screen.tool;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.NetworkHandler;
import com.github.nalamodikk.common.screen.ModMenusTypes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class UniversalConfigMenu extends AbstractContainerMenu {
    private final EnumMap<Direction, Boolean> currentConfig = new EnumMap<>(Direction.class);
    private final Player player;
    private final BlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ItemStack wandItem;

    public UniversalConfigMenu(int id, Inventory playerInventory, BlockEntity blockEntity, ItemStack wandItem) {
        super(ModMenusTypes.UNIVERSAL_CONFIG.get(), id);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.wandItem = wandItem;

        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (blockEntity instanceof IConfigurableBlock configurableBlock) {
            // 从 BlockEntity 获取当前的方向配置
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
            this.blockEntity.setChanged(); // 标记方块为已更新，以便后续保存
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            if (this.blockEntity instanceof IConfigurableBlock configurableBlock) {
                boolean configChanged = false;

                for (Direction direction : Direction.values()) {
                    boolean newConfig = this.currentConfig.get(direction);
                    if (configurableBlock.isOutput(direction) != newConfig) {
                        configurableBlock.setDirectionConfig(direction, newConfig);
                        configChanged = true;

                        // 每次配置更改時，發送封包來同步方向配置
                        NetworkHandler.NETWORK_CHANNEL.sendToServer(new ConfigDirectionUpdatePacket(this.blockEntity.getBlockPos(), direction, newConfig));
                    }
                }

                if (configChanged) {
                    MagicalIndustryMod.LOGGER.info("Updated configuration for block at {}", this.blockEntity.getBlockPos());
                    this.blockEntity.setChanged(); // 標記方塊為已變更以保存數據
                }
            }
        }

        // 保存配置到物品的 NBT 數據
        if (wandItem != null) {
            CompoundTag tag = wandItem.getOrCreateTag();
            for (Direction direction : Direction.values()) {
                tag.putBoolean("Direction_" + direction.getName(), currentConfig.get(direction));
            }
            wandItem.setTag(tag);
        }
    }




    public UniversalConfigMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, Objects.requireNonNull(playerInventory.player.level().getBlockEntity(buf.readBlockPos())), buf.readItem());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    public BlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public ItemStack getWandItem() {
        return this.wandItem;
    }
}

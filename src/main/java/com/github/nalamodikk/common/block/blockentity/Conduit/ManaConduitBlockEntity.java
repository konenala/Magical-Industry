package com.github.nalamodikk.common.block.blockentity.Conduit;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.block.block.Conduit.ManaConduitBlock;
import com.github.nalamodikk.common.config.ManaConduitConfigLoader;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.network.mana_net.ManaNetworkManager;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.EnumMap;

public class ManaConduitBlockEntity extends BlockEntity implements IUnifiedManaHandler {
    private final EnumMap<Direction, ConduitMode> directionModes = new EnumMap<>(Direction.class);
    private int manaStored = 0;
    private static final int MAX_MANA = 1000;

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
        for (Direction direction : Direction.values()) {
            directionModes.put(direction, ConduitMode.NONE);
        }
    }

    public ConduitMode toggleDirectionMode(Direction direction) {
        ConduitMode newMode = directionModes.get(direction).next();
        directionModes.put(direction, newMode);
        setChanged();
        updateBlockState(direction, newMode != ConduitMode.NONE);
        return newMode;
    }

    private void updateBlockState(Direction direction, boolean isConnected) {
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            state = state.setValue(ManaConduitBlock.DIRECTION_PROPERTIES.get(direction), isConnected);
            level.setBlock(worldPosition, state, 3);
        }
    }

    public void transferMana() {
        if (level == null || level.isClientSide || manaStored <= 0) return;

        int transferRate = ManaConduitConfigLoader.getTransferRate();

        for (Direction direction : Direction.values()) {
            if (directionModes.get(direction) == ConduitMode.OUTPUT) {
                BlockEntity neighborEntity = level.getBlockEntity(worldPosition.relative(direction));
                if (neighborEntity != null) {
                    neighborEntity.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(neighborManaStorage -> {
                        int manaToTransfer = Math.min(transferRate, neighborManaStorage.getNeededMana(0));
                        if (manaToTransfer > 0) {
                            int accepted = neighborManaStorage.insertMana(0, manaToTransfer, ManaAction.EXECUTE);
                            manaStored -= accepted;
                            setChanged();
                        }
                    });
                }
            }
        }
    }

    @Override
    public int getMana() {
        return manaStored;
    }

    @Override
    public void addMana(int amount, ManaAction action) {
        if (action.execute()) {
            manaStored = Math.min(manaStored + amount, MAX_MANA);
            setChanged();
            updateClient();
        }
    }

    @Override
    public void consumeMana(int amount) {
        manaStored = Math.max(manaStored - amount, 0);
        setChanged();
        updateClient();
    }

    @Override
    public void setMana(int amount) {
        manaStored = Math.min(Math.max(amount, 0), MAX_MANA);
        setChanged();
        updateClient();
    }

    @Override
    public void onChanged() {
        setChanged();
        updateClient();
    }

    @Override
    public int getMaxMana() {
        return MAX_MANA;
    }

    @Override
    public int getManaContainerCount() {
        return 1; // 這個導管只有一個魔力存儲槽
    }

    @Override
    public int getMana(int container) {
        return manaStored;
    }

    @Override
    public void setMana(int container, int mana) {
        if (container == 0) {
            this.manaStored = Math.min(Math.max(mana, 0), MAX_MANA);
            setChanged();
            updateClient();
        }
    }

    @Override
    public int getMaxMana(int container) {
        return MAX_MANA;
    }

    @Override
    public int getNeededMana(int container) {
        return MAX_MANA - manaStored;
    }

    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        if (container != 0) return 0; // 只有第一個槽可以接受魔力

        int manaToInsert = Math.min(amount, getNeededMana(container));
        if (action.execute()) {
            manaStored += manaToInsert;
            setChanged();
            updateClient();
        }
        return manaToInsert;
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        if (container != 0) return 0; // 只有第一個槽可以輸出魔力

        int manaToExtract = Math.min(amount, manaStored);
        if (action.execute()) {
            manaStored -= manaToExtract;
            setChanged();
            updateClient();
        }
        return manaToExtract;
    }


    public enum ConduitMode {
        NONE, INPUT, OUTPUT;

        public ConduitMode next() {
            return switch (this) {
                case NONE -> INPUT;
                case INPUT -> OUTPUT;
                case OUTPUT -> NONE;
            };
        }
    }

    /**
     * 同步客戶端數據，確保方塊狀態更新
     */
    private void updateClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

}

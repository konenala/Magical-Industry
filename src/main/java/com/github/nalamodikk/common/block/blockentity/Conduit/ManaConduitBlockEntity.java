package com.github.nalamodikk.common.block.blockentity.Conduit;

import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ManaConduitBlockEntity extends BlockEntity {

    private int manaStored = 0;
    private static final int MAX_MANA = 1000;

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
    }

    public int getManaStored() {
        return manaStored;
    }

    public int getMaxMana() {
        return MAX_MANA;
    }

    public void addMana(int amount) {
        this.manaStored = Math.min(this.manaStored + amount, MAX_MANA);
    }

    public void consumeMana(int amount) {
        this.manaStored = Math.max(this.manaStored - amount, 0);
    }

    public void transferMana() {
        if (level == null) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor instanceof ManaConduitBlockEntity conduit) {
                int transferAmount = Math.min(50, this.manaStored); // 每次傳輸 50 單位魔力
                conduit.addMana(transferAmount);
                this.consumeMana(transferAmount);
            }
        }
    }
}

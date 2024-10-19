package com.github.nalamodikk.block.custom.mana_crafting_table;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.block.entity.mana_crafting_table.AdvancedManaCraftingTableBlockEntity;
import com.github.nalamodikk.block.entity.mana_crafting_table.BaseManaCraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class AdvancedManaCraftingTableBlock extends BaseEntityBlock {
    public AdvancedManaCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide && player instanceof ServerPlayer) {
            // 打開合成界面
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof AdvancedManaCraftingTableBlockEntity) {
                NetworkHooks.openScreen((ServerPlayer) player, (AdvancedManaCraftingTableBlockEntity) blockEntity, pos);
            } else {
                throw new IllegalStateException("Our container provider is missing!");
            }
        }
        return InteractionResult.SUCCESS;  // 返回成功表示成功處理交互
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedManaCraftingTableBlockEntity(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AdvancedManaCraftingTableBlockEntity) {
            return ((AdvancedManaCraftingTableBlockEntity) blockEntity).getManaStored();
        }
        return super.getAnalogOutputSignal(blockState, level, pos);
    }
}
package com.github.nalamodikk.common.block.custom;

import com.github.nalamodikk.common.block.entity.ManaGenerator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.entity.ModBlockEntities;
import com.github.nalamodikk.common.util.helpers.FacingHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class ManaGeneratorBlock extends BaseEntityBlock {
    public ManaGeneratorBlock(Properties properties) {
        super(properties);
    }
    private final FacingHandler facingHandler = new FacingHandler();
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }




    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            // 如果是 ManaCraftingTableBlockEntity，掉落物品
            if (blockEntity instanceof ManaGeneratorBlockEntity) {
                ((ManaGeneratorBlockEntity) blockEntity).drops();  // 掉落方塊內的物品
            }

            // 如果是 ManaGeneratorBlockEntity，移除方塊實體
            if (blockEntity instanceof ManaGeneratorBlockEntity) {
                level.removeBlockEntity(pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }



    //    state, level, pos, newState, isMovin
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide && player instanceof ServerPlayer) {
            // 打開界面
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ManaGeneratorBlockEntity) {
                NetworkHooks.openScreen((ServerPlayer) player, (ManaGeneratorBlockEntity) blockEntity, pos);
            } else {
                throw new IllegalStateException("Our container provider is missing!");
            }
        }
        return InteractionResult.SUCCESS;  // 返回成功表示成功處理交互
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaGeneratorBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorBlockEntity::serverTick);
    }


    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;

    }


}

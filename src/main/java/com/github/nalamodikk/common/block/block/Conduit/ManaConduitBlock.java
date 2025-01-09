package com.github.nalamodikk.common.block.block.Conduit;

import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.block.blockentity.Conduit.ManaConduitBlockEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ManaConduitBlock extends BaseEntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public static final VoxelShape CENTER_SHAPE = Block.box(6, 6, 6, 10, 10, 10);
    public static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 0, 10, 10, 6);
    public static final VoxelShape EAST_SHAPE = Block.box(10, 6, 6, 16, 10, 10);
    public static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 10, 10, 10, 16);
    public static final VoxelShape WEST_SHAPE = Block.box(0, 6, 6, 6, 10, 10);
    public static final VoxelShape UP_SHAPE = Block.box(6, 10, 6, 10, 16, 10);
    public static final VoxelShape DOWN_SHAPE = Block.box(6, 0, 6, 10, 6, 10);


    public ManaConduitBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        // 添加多行自定義工具提示
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.magical_industry.mana_conduit.line1"));
            tooltip.add(Component.translatable("tooltip.magical_industry.mana_conduit.line2"));
            tooltip.add(Component.translatable("tooltip.magical_industry.mana_conduit.line3"));

        } else {
            tooltip.add(Component.translatable("tooltip.magical_industry.mana_conduit.hold_shift"));

        }
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ManaConduitBlockEntity conduit) {
            VoxelShape cachedShape = conduit.getCachedShape(state);
            return cachedShape != null ? cachedShape : CENTER_SHAPE;
        }
        return CENTER_SHAPE; // 預設返回中心形狀
    }


    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
        BlockState updatedState = updateConnections(level, pos, state);
        if (!state.equals(updatedState)) {
            level.setBlock(pos, updatedState, 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ManaConduitBlockEntity conduit) {
                conduit.invalidateShapeCache(); // 當連接狀態確實改變時，才重置形狀緩存
            }
        }
    }




    private BlockState updateConnections(Level level, BlockPos pos, BlockState state) {
        return state.setValue(NORTH, canConnect(level, pos.relative(Direction.NORTH)))
                .setValue(EAST, canConnect(level, pos.relative(Direction.EAST)))
                .setValue(SOUTH, canConnect(level, pos.relative(Direction.SOUTH)))
                .setValue(WEST, canConnect(level, pos.relative(Direction.WEST)))
                .setValue(UP, canConnect(level, pos.relative(Direction.UP)))
                .setValue(DOWN, canConnect(level, pos.relative(Direction.DOWN)));
    }

    private boolean canConnect(Level level, BlockPos neighborPos) {
        BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
        if (neighborEntity instanceof ManaConduitBlockEntity) {
            return true;
        }

        // 支持其他有魔力能力的機器
        return neighborEntity != null && neighborEntity.getCapability(ModCapabilities.MANA).isPresent();
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaConduitBlockEntity(pos, state);
    }
}

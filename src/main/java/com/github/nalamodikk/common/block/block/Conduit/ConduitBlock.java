package com.github.nalamodikk.common.block.block.Conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * 抽象的「導管」父類，主要負責：
 * 1. 方向屬性 (UP, DOWN, NORTH, SOUTH, EAST, WEST) 與 WATERLOGGED。
 * 2. 根據 canConnectTo(...) 決定方塊狀態要不要「連接」。
 * 3. 動態生成導管形狀（中心 + 若干延伸）。
 * 4. 初始化及更新方塊狀態。
 * 5. 透過 newBlockEntity(...) 供子類建立具體的 BlockEntity 實例。
 */
public abstract class ConduitBlock extends Block implements EntityBlock {

    // 六個方向的連接屬性
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");

    // 水淹屬性
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // 對應方向→BooleanProperty的映射，方便存取
    protected static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
    static {
        PROPERTY_BY_DIRECTION.put(Direction.DOWN,  DOWN);
        PROPERTY_BY_DIRECTION.put(Direction.UP,    UP);
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.WEST,  WEST);
        PROPERTY_BY_DIRECTION.put(Direction.EAST,  EAST);
    }

    // 儲存 64 種形狀對應 (因為 6 個方向可連接與否，2^6 = 64)
    private final VoxelShape[] shapeByIndex;

    // ------------------------------------------------------------------------
    // 建構子
    // ------------------------------------------------------------------------
    protected ConduitBlock(BlockBehaviour.Properties properties, float apothem) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(WATERLOGGED, false));

        // 建立所有組合形狀
        this.shapeByIndex = makeShapes(apothem);
    }

    // ------------------------------------------------------------------------
    // 抽象方法：由子類決定如何判斷是否能連接
    // ------------------------------------------------------------------------
    protected abstract boolean canConnectTo(LevelAccessor world, BlockPos pos, Direction direction);

    // 若你需要判斷是否同一種類型的導管，可在子類實作
    protected abstract boolean isSameConduit(LevelAccessor world, BlockPos pos, Direction direction);

    // 由子類回傳對應的 BlockEntity
    @Nullable
    protected abstract BlockEntity createConduitEntity(BlockPos pos, BlockState state);

    @Override
    public final BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createConduitEntity(pos, state);
    }

    // ------------------------------------------------------------------------
    // 狀態 & 水淹處理
    // ------------------------------------------------------------------------
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluid = level.getFluidState(pos);

        // 根據周圍六面檢查能否連接
        BlockState state = this.defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), canConnectTo(level, pos, dir));
        }

        // 水淹屬性
        state = state.setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor world, BlockPos currentPos, BlockPos neighborPos) {
        // 如果水淹則排程流動
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        // 自動更新指定方向的連接狀態
        boolean connected = canConnectTo(world, currentPos, direction);
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connected);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos neighborPos,
                                boolean isMoving) {
        super.neighborChanged(state, world, pos, block, neighborPos, isMoving);
        // 你也可以在這裡呼叫 BlockEntity 的 updateConnections()，或透過以上 updateShape 自動處理
        // 以下示範做個簡單的重新檢查 (可視需求保留或移除)
        BlockState newState = state;
        for (Direction dir : Direction.values()) {
            newState = newState.setValue(PROPERTY_BY_DIRECTION.get(dir), canConnectTo(world, pos, dir));
        }
        if (!newState.equals(state)) {
            world.setBlock(pos, newState, 2);
        }
    }

    // ------------------------------------------------------------------------
    // 顯示狀態 / 互動
    // ------------------------------------------------------------------------
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                // 自行定義要顯示的訊息，例如「魔力導管狀態」等
                player.displayClientMessage(Component.literal("Conduit State: " + be.toString()), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------------
    // 形狀處理：根據 6 個方向是否連接，動態組合 shape
    // ------------------------------------------------------------------------
    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return shapeByIndex[getShapeIndex(state)];
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 若之後想用客製 BakedModel，也可改成 RenderShape.MODEL
        return RenderShape.MODEL;
    }

    private int getShapeIndex(BlockState state) {
        int index = 0;
        for (Direction direction : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(direction))) {
                index |= 1 << direction.ordinal();
            }
        }
        return index;
    }

    private VoxelShape[] makeShapes(float apothem) {
        // apothem：導管「半徑」，控制粗細
        float min = 8 - (apothem * 8);
        float max = 8 + (apothem * 8);

        // 中心方塊(導管本體)
        VoxelShape core = Block.box(min, min, min, max, max, max);

        // 各方向延伸部分
        VoxelShape[] arms = new VoxelShape[Direction.values().length];
        arms[Direction.DOWN.ordinal()]  = Block.box(min, 0,   min,  max, min,  max);
        arms[Direction.UP.ordinal()]    = Block.box(min, max, min,  max, 16,  max);
        arms[Direction.NORTH.ordinal()] = Block.box(min, min, 0,    max, max, min);
        arms[Direction.SOUTH.ordinal()] = Block.box(min, min, max,  max, max, 16);
        arms[Direction.WEST.ordinal()]  = Block.box(0,   min, min,  min, max,  max);
        arms[Direction.EAST.ordinal()]  = Block.box(max, min, min,  16, max,  max);

        // 預先生成 64 種組合
        VoxelShape[] result = new VoxelShape[64];
        for (int i = 0; i < 64; i++) {
            VoxelShape combined = core;
            for (Direction dir : Direction.values()) {
                if ((i & (1 << dir.ordinal())) != 0) {
                    combined = Shapes.or(combined, arms[dir.ordinal()]);
                }
            }
            result[i] = combined.optimize();
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // 建立方塊狀態定義
    // ------------------------------------------------------------------------
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, WATERLOGGED);
    }
}

package com.github.nalamodikk.common.block.block.Conduit;

import com.github.nalamodikk.common.block.blockentity.Conduit.ManaConduitBlockEntity;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ManaConduitBlock extends ConduitBlock {

    public static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = Map.of(
            Direction.UP, BooleanProperty.create("up"),
            Direction.DOWN, BooleanProperty.create("down"),
            Direction.NORTH, BooleanProperty.create("north"),
            Direction.SOUTH, BooleanProperty.create("south"),
            Direction.EAST, BooleanProperty.create("east"),
            Direction.WEST, BooleanProperty.create("west")
    );

    public ManaConduitBlock() {
        super(Properties.copy(Blocks.IRON_BLOCK).noOcclusion().strength(1.0F), 0.25F);
    }

    @Override
    protected boolean canConnectTo(LevelAccessor world, BlockPos pos, Direction dir) {
        BlockEntity neighborEntity = world.getBlockEntity(pos.relative(dir));

        return neighborEntity instanceof ManaConduitBlockEntity ||
                (neighborEntity != null && neighborEntity.getCapability(ModCapabilities.MANA, dir.getOpposite()).isPresent());
    }

    @Override
    protected boolean isSameConduit(LevelAccessor world, BlockPos pos, Direction direction) {
        return world.getBlockState(pos.relative(direction)).getBlock() instanceof ManaConduitBlock;
    }

    @Override
    protected @Nullable BlockEntity createConduitEntity(BlockPos pos, BlockState state) {
        return new ManaConduitBlockEntity(pos, state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty prop = DIRECTION_PROPERTIES.get(direction);
        return state.setValue(prop, canConnectTo(level, pos, direction));
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        if (!world.isClientSide) {
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, world, pos, newState, isMoving);
        if (!world.isClientSide) {
        }
    }
}

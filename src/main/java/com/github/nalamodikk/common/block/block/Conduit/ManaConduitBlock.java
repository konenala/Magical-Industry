package com.github.nalamodikk.common.block.block.Conduit;

import com.github.nalamodikk.common.block.blockentity.Conduit.ManaConduitBlockEntity;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.network.mana_net.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ManaConduitBlock extends ConduitBlock {

    public ManaConduitBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().strength(1.0F), 0.25F);
    }

    @Override
    protected boolean canConnectTo(LevelAccessor world, BlockPos pos, Direction dir) {
        // 檢查鄰居是否擁有 ModCapabilities.MANA
        BlockPos neighborPos = pos.relative(dir);
        BlockEntity be = world.getBlockEntity(neighborPos);
        if (be != null) {
            // 如果鄰居具備 MANA capability，就表示可連
            return be.getCapability(ModCapabilities.MANA, dir.getOpposite()).isPresent();
        }
        return false;
    }

    @Override
    protected boolean isSameConduit(LevelAccessor world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        // 如果該方塊也是 ManaConduitBlock，就回傳 true
        return (neighborState.getBlock() instanceof ManaConduitBlock);
    }


    @Override
    protected @Nullable BlockEntity createConduitEntity(BlockPos pos, BlockState state) {
        return new ManaConduitBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        if (!world.isClientSide) {
            NetworkManager manager = NetworkManager.getInstance(world);
            manager.registerConduit(pos);
        }
    }



    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, world, pos, newState, isMoving);
        if (!world.isClientSide) {
            NetworkManager manager = NetworkManager.getInstance(world);
            manager.removeConduit(pos);
        }
    }

}

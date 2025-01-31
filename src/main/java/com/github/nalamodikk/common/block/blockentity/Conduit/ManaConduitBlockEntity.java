package com.github.nalamodikk.common.block.blockentity.Conduit;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.network.mana_net.ManaNetworkManager;
import com.github.nalamodikk.common.network.mana_net.NetworkManager;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * 魔力導管方塊實體，用於處理魔力儲存、傳輸邏輯。
 * 可以在伺服端每 tick 執行 transferMana() 等操作。
 */
public class ManaConduitBlockEntity extends ConduitBlockEntity {

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
    }
    private final Set<Direction> outputDirections = EnumSet.noneOf(Direction.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int BASE_TRANSFER_RATE = 50; // 替換為實際的傳輸速率

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            NetworkManager.getInstance(level).registerConduit(worldPosition);
            ManaNetworkManager.registerConduit(level, worldPosition);


        }
    }

    public boolean isOutput(Direction direction) {
        return outputDirections.contains(direction);
    }

    public void setOutput(Direction direction, boolean isOutput) {
        if (isOutput) {
            outputDirections.add(direction);
        } else {
            outputDirections.remove(direction);
        }
        setChanged(); // 標記數據已變更
    }


    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.getBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }


    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!level.isClientSide) {
            NetworkManager.getInstance(level).removeConduit(worldPosition);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.MANA) {
            if (level != null) {
                return ManaNetworkManager.getNetwork(level) // 獲取對應的 ManaNetwork
                        .getManaHandler(worldPosition, side) // 呼叫新增的 getManaHandler 方法
                        .cast(); // 將其轉型為泛型
            }
        }
        return super.getCapability(cap, side);
    }

}



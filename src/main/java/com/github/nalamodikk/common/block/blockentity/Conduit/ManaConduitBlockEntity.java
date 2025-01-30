package com.github.nalamodikk.common.block.blockentity.Conduit;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * 魔力導管方塊實體，用於處理魔力儲存、傳輸邏輯。
 * 可以在伺服端每 tick 執行 transferMana() 等操作。
 */
public class ManaConduitBlockEntity extends ConduitBlockEntity {

    private static final int MAX_MANA = 1000;
    private static final int BASE_TRANSFER_RATE = 50;

    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA);
    private final LazyOptional<IUnifiedManaHandler> manaOptional = LazyOptional.of(() -> manaStorage);

    // 哪些方向正在「抽取」（extract）魔力，可以自行定義規則
    private final Set<Direction> extractingDirections = EnumSet.noneOf(Direction.class);

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
    }

    // 伺服端每 tick 執行
    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaConduitBlockEntity be) {
        be.transferMana();

        // 若需要定期檢查連接狀態，可呼叫 be.updateConnections()
        // be.updateConnections();
    }

    public void transferMana() {
        if (level == null || manaStorage.getManaStored() <= 0) return;

        for (Direction direction : Direction.values()) {
            if (isExtracting(direction)) {
                BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
                if (neighbor != null) {
                    neighbor.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(target -> {
                        // 進行傳輸
                        int extracted = manaStorage.extractMana(BASE_TRANSFER_RATE, ManaAction.get(false));
                        int inserted = target.receiveMana(extracted, ManaAction.get(false));
                        // 若有剩餘沒插入（插不進去），再加回自己
                        manaStorage.addMana(extracted - inserted);
                    });
                }
            }
        }
    }

    // 是否從此方向抽取魔力
    public boolean isExtracting(Direction direction) {
        return extractingDirections.contains(direction);
    }

    public void setExtracting(Direction direction, boolean extracting) {
        if (extracting) {
            extractingDirections.add(direction);
        } else {
            extractingDirections.remove(direction);
        }
        setChanged();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        manaOptional.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,
                                             @Nullable Direction side) {
        if (cap == ModCapabilities.MANA) {
            return manaOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    // 例如用於顯示在玩家訊息
    public String getManaInfo() {
        return "Mana: " + manaStorage.getManaStored() + "/" + MAX_MANA;
    }

    // 這裡可實作「updateConnections()」，把是否能連接的狀態同步給 BlockState
    // 或者直接使用 Block 的自動 updateShape()。
}

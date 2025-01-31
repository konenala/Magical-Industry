package com.github.nalamodikk.common.network.mana_net;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ManaNetworkManager {
    // 使用 ConcurrentHashMap 以處理潛在的併發問題
    private static final Map<Level, ManaNetwork> networks = new ConcurrentHashMap<>();

    /**
     * 獲取或創建對應 Level 的 ManaNetwork 實例
     */
    public static ManaNetwork getNetwork(Level level) {
        return networks.computeIfAbsent(level, ManaNetwork::new);
    }

    /**
     * 移除世界的 ManaNetwork（通常在世界卸載時呼叫）
     */
    public static void removeNetwork(Level level) {
        networks.remove(level);
    }

    /**
     * 註冊導管
     */
    public static void registerConduit(Level level, BlockPos pos) {
        getNetwork(level).addConduit(pos);
    }

    /**
     * 移除導管
     */
    public static void removeConduit(Level level, BlockPos pos) {
        ManaNetwork network = getNetwork(level);
        network.removeConduit(pos);
        if (network.isEmpty()) {
            removeNetwork(level);
        }
    }

    /**
     * 獲取特定位置的 Mana 能力
     */
    public LazyOptional<IUnifiedManaHandler> getManaHandler(Level level, BlockPos pos, Direction side) {
        if (level == null) return LazyOptional.empty();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            return blockEntity.getCapability(ModCapabilities.MANA, side);
        }
        return LazyOptional.empty();
    }
}

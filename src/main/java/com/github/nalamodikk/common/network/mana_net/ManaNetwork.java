package com.github.nalamodikk.common.network.mana_net;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

import java.util.*;

/**
 * 魔力網路：
 * - 追蹤所有已連接的導管與機器
 * - 能夠根據請求自動分配魔力
 */
public class ManaNetwork {
    private final Set<BlockPos> conduits = new HashSet<>();
    private final Set<BlockEntity> connectedMachines = new HashSet<>();

    // ✅ 加入節點
    public void addNode(BlockPos pos) {
        conduits.add(pos);
    }

    // ✅ 移除節點
    public void removeNode(BlockPos pos) {
        conduits.remove(pos);
        if (conduits.isEmpty()) {
            connectedMachines.clear();
        }
    }

    // ✅ 是否包含該節點
    public boolean contains(BlockPos pos) {
        return conduits.contains(pos);
    }

    // ✅ 取得所有機器
    public List<BlockEntity> getConnectedMachines() {
        return new ArrayList<>(connectedMachines);
    }

    // ✅ 魔力分配
    public int distributeMana(BlockPos requestPos, int requestedAmount) {
        int remaining = requestedAmount;

        // 嘗試從導管獲取魔力
        for (BlockEntity be : connectedMachines) {
            LazyOptional<IUnifiedManaHandler> cap = be.getCapability(ModCapabilities.MANA);
            if (cap.isPresent()) {
                IUnifiedManaHandler manaStorage = cap.orElse(new ManaStorage(0)); // 默認返回一個空的 ManaStorage
                int extracted = manaStorage.extractMana(remaining, ManaAction.EXECUTE);
                remaining -= extracted;
                if (remaining <= 0) break;
            }
        }

        return requestedAmount - remaining;
    }
}

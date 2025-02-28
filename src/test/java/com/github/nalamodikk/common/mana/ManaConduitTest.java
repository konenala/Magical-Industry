package com.github.nalamodikk.common.mana;

import com.github.nalamodikk.common.block.blockentity.Conduit.ManaConduitBlockEntity;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.mana.ManaAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ManaConduitTest {

    @Test
    public void testPushMana() {
        // ✅ 1. 建立測試用的 BlockState
        BlockPos pos = new BlockPos(0, 64, 0);
        BlockState state = Mockito.mock(BlockState.class);

        // ✅ 2. 建立 ManaConduitBlockEntity
        ManaConduitBlockEntity conduit = new ManaConduitBlockEntity(pos, state);
        conduit.getManaStorage().insertMana(500, ManaAction.get(false)); // 先存入 500 魔力

        // ✅ 3. 設定 NORTH 為輸出方向
        conduit.toggleOutput(Direction.NORTH);

        // ✅ 4. 測試 pushMana() 方法
        conduit.pushMana();

        // ✅ 5. 驗證魔力是否被傳輸
        int remainingMana = conduit.getManaStorage().getManaStored();
        Assertions.assertTrue(remainingMana < 500, "魔力應該要被傳輸！");

        // ✅ 6. 驗證方向設定是否正確
        Assertions.assertTrue(conduit.getOutputDirections().get(Direction.NORTH), "NORTH 方向應該允許輸出！");
        Assertions.assertFalse(conduit.getOutputDirections().getOrDefault(Direction.SOUTH, false), "SOUTH 方向應該不能輸出！");
    }
}

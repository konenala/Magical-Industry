package com.github.nalamodikk.common.network.toolpacket;

import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ModeChangePacket {
    private final int modeIndex;

    // 構造函數，接受模式索引
    public ModeChangePacket(int modeIndex) {
        this.modeIndex = modeIndex;
    }

    // 解碼封包（從位元緩衝區讀取封包數據）
    public ModeChangePacket(FriendlyByteBuf buf) {
        this.modeIndex = buf.readInt();
    }

    // 將封包的數據編碼到位元緩衝區
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(modeIndex);
    }

    // 處理封包
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 確保這個處理邏輯只在伺服器端執行
            Player player = ctx.get().getSender();
            if (player != null) {
                ItemStack heldItem = player.getMainHandItem();

                // 檢查玩家主手中的道具是否為 ManaDebugToolItem
                if (heldItem.getItem() instanceof ManaDebugToolItem) {
                    // 將新的 modeIndex 寫入 NBT，使用常量 TAG_MODE_INDEX 確保一致性
                    heldItem.getOrCreateTag().putInt(ManaDebugToolItem.TAG_MODE_INDEX, modeIndex);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

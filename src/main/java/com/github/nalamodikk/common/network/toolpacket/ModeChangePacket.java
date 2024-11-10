package com.github.nalamodikk.common.network.toolpacket;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;



public class ModeChangePacket {
    private final int modeIndex;

    // 構造函數
    public ModeChangePacket(int modeIndex) {
        this.modeIndex = modeIndex;
    }

    // 解碼
    public ModeChangePacket(FriendlyByteBuf buf) {
        this.modeIndex = buf.readInt();
    }

    // 編碼
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(modeIndex);
    }

    // 處理封包
    // 在封包處理時，僅在伺服器端執行必要的邏輯
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            // 確保在伺服器端處理
            if (context.getDirection().getReceptionSide().isServer()) {
                Player player = context.getSender();
                if (player != null) {
                    ItemStack heldItem = player.getMainHandItem();

                    // 確保玩家持有的物品不為空，並且確定是 ManaDebugToolItem
                    if (!heldItem.isEmpty() && heldItem.getItem() instanceof ManaDebugToolItem) {
                        // 使用玩家持有的物品更新 NBT 標籤
                        heldItem.getOrCreateTag().putInt(ManaDebugToolItem.TAG_MODE_INDEX, modeIndex);

                        // 日誌記錄以確認更新操作
                        MagicalIndustryMod.LOGGER.info("Updated mode index for ManaDebugToolItem held by player: {} to {}", player.getName().getString(), modeIndex);
                    }
                }
            }
        });

        context.setPacketHandled(true);
    }

}



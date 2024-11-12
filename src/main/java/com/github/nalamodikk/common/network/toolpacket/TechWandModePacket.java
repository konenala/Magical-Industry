package com.github.nalamodikk.common.network.toolpacket;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TechWandModePacket {
    private final boolean forward;

    public TechWandModePacket(boolean forward) {
        this.forward = forward;
    }

    public boolean isForward() {
        return forward;
    }

    // 編碼和解碼方法保持不變
    public static void encode(TechWandModePacket msg, FriendlyByteBuf buffer) {
        buffer.writeBoolean(msg.forward);
    }

    public static TechWandModePacket decode(FriendlyByteBuf buffer) {
        return new TechWandModePacket(buffer.readBoolean());
    }

    // 封包處理方法
    public static void handle(TechWandModePacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 確保處理邏輯在伺服器端進行
            if (context.getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
                    if (heldItem.getItem() instanceof BasicTechWandItem) {
                        BasicTechWandItem wand = (BasicTechWandItem) heldItem.getItem();
                        BasicTechWandItem.TechWandMode currentMode = wand.getMode(heldItem);
                        BasicTechWandItem.TechWandMode newMode = msg.forward ? currentMode.next() : currentMode.previous();
                        wand.setMode(heldItem, newMode);

                        // 調試信息，確認模式切換被處理
                        MagicalIndustryMod.LOGGER.debug("TechWandMode changed to: " + newMode);

                        // 向玩家發送顯示消息
                        player.displayClientMessage(Component.translatable(
                                "message.magical_industry.mode_changed",
                                Component.translatable("mode.magical_industry." + newMode.name().toLowerCase())), true);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }


}

package com.github.nalamodikk.common.item.debug;

import com.github.nalamodikk.common.Capability.ManaCapability;

import com.github.nalamodikk.common.network.handler.NetworkHandler;
import com.github.nalamodikk.common.network.toolpacket.ManaUpdatePacket;
import com.github.nalamodikk.common.network.toolpacket.ModeChangePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber
public class ManaDebugToolItem extends Item {
    public static final String TAG_MODE_INDEX = "ModeIndex";


    private static final int[] MANA_AMOUNTS = {10, 100, 1000};
    private static final String[] MODES = {"message.magical_industry.mana_mode_add_10", "message.magical_industry.mana_mode_add_100", "message.magical_industry.mana_mode_add_1000"};

    public ManaDebugToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos();
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity != null && blockEntity.getCapability(ManaCapability.MANA).isPresent()) {
                blockEntity.getCapability(ManaCapability.MANA).ifPresent(manaStorage -> {
                    // 增加魔力
                    ItemStack stack = context.getItemInHand();
                    int modeIndex = stack.getOrCreateTag().getInt(TAG_MODE_INDEX);
                    int manaToAdd = MANA_AMOUNTS[modeIndex];
                    manaStorage.addMana(manaToAdd);

                    // 向玩家顯示訊息
                    Player player = context.getPlayer();
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(Component.translatable("message.magical_industry.mana_added", manaToAdd, manaStorage.getMana()), true);
                    }

                    // 確保狀態已更新並同步到所有客戶端
                    blockEntity.setChanged();
                    BlockState state = level.getBlockState(pos);
                    level.sendBlockUpdated(pos, state, state, 3);

                    // 發送同步封包到所有玩家
                    ManaUpdatePacket packet = new ManaUpdatePacket(pos, manaStorage.getMana());
                    NetworkHandler.NETWORK_CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), packet);
                });

                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (heldItem.getItem() instanceof ManaDebugToolItem && player.isCrouching()) {
                ManaDebugToolItem manaDebugToolItem = (ManaDebugToolItem) heldItem.getItem();
                manaDebugToolItem.cycleMode(heldItem, event.getScrollDelta() > 0);

                // 發送封包同步到伺服器
                int modeIndex = heldItem.getOrCreateTag().getInt(TAG_MODE_INDEX);
                NetworkHandler.NETWORK_CHANNEL.sendToServer(new ModeChangePacket(modeIndex));

                player.displayClientMessage(Component.translatable("message.magical_industry.mana_mode_changed", manaDebugToolItem.getCurrentModeDescription(heldItem)), true);
                event.setCanceled(true); // 阻止玩家切換物品欄位
            }
        }
    }

    private void cycleMode(ItemStack stack, boolean forward) {
        int modeIndex = stack.getOrCreateTag().getInt(TAG_MODE_INDEX);
        if (forward) {
            modeIndex = (modeIndex + 1) % MANA_AMOUNTS.length;
        } else {
            modeIndex = (modeIndex - 1 + MANA_AMOUNTS.length) % MANA_AMOUNTS.length;
        }
        stack.getOrCreateTag().putInt(TAG_MODE_INDEX, modeIndex);
    }

    private String getCurrentModeDescription(ItemStack stack) {
        int modeIndex = stack.getOrCreateTag().getInt(TAG_MODE_INDEX);
        return Component.translatable(MODES[modeIndex]).getString();
    }
}

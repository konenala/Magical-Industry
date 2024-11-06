package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.network.NetworkHandler;
import com.github.nalamodikk.common.network.TechWandModePacket;
import com.github.nalamodikk.common.screen.tool.UniversalConfigMenu;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber
public class BasicTechWandItem extends Item {

    public BasicTechWandItem(Properties properties) {
        super(properties);
    }

    // 定義科技魔杖的模式
    public enum TechWandMode {
        CONFIGURE_IO, // 合併輸入和輸出配置
        ADD_MANA,
        ROTATE;

        public TechWandMode next() {
            TechWandMode[] modes = values();
            return modes[(this.ordinal() + 1) % modes.length];
        }

        // 獲取上一個模式（循環切換）
        public TechWandMode previous() {
            TechWandMode[] modes = values();
            return modes[(this.ordinal() - 1 + modes.length) % modes.length];
        }
    }

    // 從物品中獲取當前模式
    public TechWandMode getMode(ItemStack stack) {
        return TechWandMode.values()[stack.getOrCreateTag().getInt("TechWandMode")];
    }

    // 設置物品的模式
    public void setMode(ItemStack stack, TechWandMode mode) {
        stack.getOrCreateTag().putInt("TechWandMode", mode.ordinal());
    }

    // 伺服器端處理切換模式
    public void changeMode(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            TechWandMode currentMode = getMode(stack);
            TechWandMode newMode = currentMode.next();
            setMode(stack, newMode);
            serverPlayer.displayClientMessage(Component.literal("Mode: " + newMode.name()), true);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!level.isClientSide && player != null && player.isCrouching()) {
            TechWandMode mode = getMode(stack);

            if (blockEntity != null) {
                LazyOptional<IUnifiedManaHandler> manaCap = blockEntity.getCapability(ManaCapability.MANA);

                if (manaCap.isPresent()) {
                    switch (mode) {
                        case CONFIGURE_IO:
                            // 配置輸入和輸出
                            NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider(
                                    (id, playerInventory, playerEntity) -> new UniversalConfigMenu(id, playerInventory, blockEntity),
                                    Component.translatable("screen.magical_industry.configure_io")
                            ), pos);
                            return InteractionResult.SUCCESS;

                        case ADD_MANA:
                            // 增加魔力，並消耗魔杖的魔力
                            LazyOptional<IUnifiedManaHandler> wandManaCap = stack.getCapability(ManaCapability.MANA);
                            if (wandManaCap.isPresent()) {
                                wandManaCap.ifPresent(wandMana -> {
                                    int manaToConsume = 10;
                                    if (wandMana.extractMana(manaToConsume, ManaAction.get(true)) >= manaToConsume) {
                                        wandMana.extractMana(manaToConsume, ManaAction.get(false));
                                        manaCap.ifPresent(mana -> mana.receiveMana(manaToConsume, ManaAction.get(false)));
                                        player.displayClientMessage(Component.literal("Added 10 Mana!"), true);
                                    } else {
                                        player.displayClientMessage(Component.literal("Not enough mana in wand!"), true);
                                    }
                                });
                                return InteractionResult.SUCCESS;
                            }
                            break;


                        case ROTATE:
                            // 獲取方塊的當前狀態
                            BlockState state = level.getBlockState(pos);

                            // 1. 首先檢查是否具有 FACING 屬性
                            if (state.hasProperty(BlockStateProperties.FACING)) {
                                Direction currentDirection = state.getValue(BlockStateProperties.FACING);
                                Direction newDirection = currentDirection.getClockWise(); // 順時針旋轉
                                BlockState newState = state.setValue(BlockStateProperties.FACING, newDirection);

                                // 更新方塊狀態
                                level.setBlock(pos, newState, 3);
                                player.displayClientMessage(Component.literal("Block rotated! (FACING)"), true);
                                return InteractionResult.SUCCESS;
                            }

                            // 2. 如果沒有 FACING 屬性，則檢查是否具有 HORIZONTAL_FACING 屬性
                            else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                                Direction currentDirection = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                                Direction newDirection = currentDirection.getClockWise(); // 順時針旋轉
                                BlockState newState = state.setValue(BlockStateProperties.HORIZONTAL_FACING, newDirection);

                                // 更新方塊狀態
                                level.setBlock(pos, newState, 3);
                                player.displayClientMessage(Component.literal("Block rotated! (HORIZONTAL_FACING)"), true);
                                return InteractionResult.SUCCESS;
                            }

                            // 3. 如果既沒有 FACING 也沒有 HORIZONTAL_FACING，則顯示無法旋轉的消息
                            else {
                                player.displayClientMessage(Component.literal("Block cannot be rotated!"), true);
                            }
                            break;

                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    // 處理玩家的蹲下滾輪滾動來切換模式
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && player.isCrouching()) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (heldItem.getItem() instanceof BasicTechWandItem) {
                BasicTechWandItem wand = (BasicTechWandItem) heldItem.getItem();

                // 強制類型轉換，將滾輪方向變量從 double 轉為 float
                float scrollDelta = (float) event.getScrollDelta();

                // 發送切換模式封包，根據滾輪方向決定是前進還是後退
                NetworkHandler.NETWORK_CHANNEL.sendToServer(new TechWandModePacket(scrollDelta > 0));

                event.setCanceled(true); // 阻止玩家切換物品欄位
            }
        }
    }


    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(Component.literal("Mode: " + getMode(stack).name()));
    }

}

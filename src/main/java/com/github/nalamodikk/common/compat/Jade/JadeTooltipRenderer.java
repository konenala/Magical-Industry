package com.github.nalamodikk.common.compat.Jade;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.compat.energy.SimpleEnergy;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.ITargetRedirector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import net.minecraft.network.chat.Component;
import com.github.nalamodikk.common.Capability.ManaStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import snownee.jade.api.ui.ITooltipRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import snownee.jade.impl.Tooltip;
import snownee.jade.api.ui.IElementHelper;

public class JadeTooltipRenderer {

    public static class ManaEnergyDataProvider implements IServerDataProvider<BlockAccessor> {

        static final ManaEnergyDataProvider INSTANCE = new ManaEnergyDataProvider();
        private static final ResourceLocation BLOCK_DATA = new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_energy_data");

        @Override
        public ResourceLocation getUid() {
            return BLOCK_DATA;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
            BlockEntity blockEntity = blockAccessor.getBlockEntity();
            if (blockEntity != null) {
                // 獲取 Mana 數據並添加到 data 中
                blockEntity.getCapability(ManaStorage.MANA).ifPresent(manaStorage -> {
                    data.putInt("mana", manaStorage.getMana());
                });
                // 獲取 FE 數據並添加到 data 中
                blockEntity.getCapability(ForgeCapabilities.ENERGY).ifPresent(energyStorage -> {
                    data.putInt("fe", energyStorage.getEnergyStored());
                });
            }
        }
    }

    public static class JadePlugin implements IWailaPlugin {
        private static final ResourceLocation CUSTOM_TOOLTIP = new ResourceLocation(MagicalIndustryMod.MOD_ID, "custom_tooltip");

        @Override
        public void register(IWailaCommonRegistration registration) {
            registration.registerBlockDataProvider(ManaEnergyDataProvider.INSTANCE, BlockEntity.class);
        }

        @Override
        public void registerClient(snownee.jade.api.IWailaClientRegistration registration) {
            registration.registerBlockComponent(new CustomTooltipRenderer(), Block.class);
        }
    }

    public static class CustomTooltipRenderer implements IBlockComponentProvider, ITooltipRenderer, snownee.jade.api.IBlockComponentProvider {

        private IElement icon;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("mana") && data.contains("fe")) {
                int mana = data.getInt("mana");
                String fe = data.getString("fe");
                tooltip.add(Component.literal("Mana: " + mana + " | FE: " + fe));

            } else {
                tooltip.add(Component.literal("No Data"));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return new ResourceLocation(MagicalIndustryMod.MOD_ID, "custom_tooltip");
        }

        @Override
        public IElement getIcon() {
            if (icon == null) {
                IElementHelper helper = IElementHelper.get();
                icon = helper.text(Component.literal("Custom Icon"));
            }
            return icon;
        }

        @Override
        public boolean hasIcon() {
            return icon != null;
        }

        @Override
        public void setIcon(IElement icon) {
            this.icon = icon;
        }

        @Override
        public Rect2i getPosition() {
            return null; // 可以實現返回具體位置的邏輯
        }

        @Override
        public Vec2 getSize() {
            return new Vec2(16, 16); // 默認大小，可以根據需求調整
        }

        @Override
        public void setSize(Vec2 size) {
            // 設置大小的實現
        }

        @Override
        public void recalculateSize() {
            // 如果需要重新計算大小，可以在這裡實現
        }

        @Override
        public float getRealScale() {
            return 1.0f; // 默認比例
        }

        @Override
        public @Nullable Rect2i getRealRect() {
            return null; // 如果需要返回具體矩形，可以在這裡實現
        }

        @Override
        public void recalculateRealRect() {
            // 如果需要重新計算矩形，可以在這裡實現
        }

        @Override
        public int getPadding(int side) {
            return 2; // 默認的邊距設置，可以根據需要進行調整
        }

        @Override
        public void setPadding(int side, int value) {
            // 這裡可以實現自定義的邊距設置邏輯
        }

        @Override
        public Tooltip getTooltip() {
            return null; // 如果需要返回具體的 Tooltip，可以在這裡實現
        }

        @Override
        public ITargetRedirector.@Nullable Result redirect(ITargetRedirector redirect, IBlockAccessor accessor, mcp.mobius.waila.api.IPluginConfig config) {
            return IBlockComponentProvider.super.redirect(redirect, accessor, config);
        }
    }
}
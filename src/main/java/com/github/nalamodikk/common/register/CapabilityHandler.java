package com.github.nalamodikk.common.register;


import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.block.entity.mana_crafting.ManaCraftingTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.github.nalamodikk.common.MagicalIndustryMod.MOD_ID;

public class CapabilityHandler {

    public static void register() {
        // 註冊到 MinecraftForge 事件總線
        MinecraftForge.EVENT_BUS.register(new CapabilityHandler());
    }

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof ManaCraftingTableBlockEntity blockEntity) {
            // 判斷是否已經具有 MANA 能力，避免重複附加
            LazyOptional<IUnifiedManaHandler> manaCap = blockEntity.getCapability(ManaCapability.MANA);

        }
    }
}

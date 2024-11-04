package com.github.nalamodikk.common.register;


import com.github.nalamodikk.common.block.entity.mana_crafting.ManaCraftingTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.github.nalamodikk.common.MagicalIndustryMod.MOD_ID;

public class CapabilityHandler {

    public static void register() {
        // 註冊到 MinecraftForge 事件總線
        MinecraftForge.EVENT_BUS.register(new CapabilityHandler());
    }

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<?> event) {
        if (event.getObject() instanceof ManaCraftingTableBlockEntity blockEntity) {
            event.addCapability(new ResourceLocation(MOD_ID, "mana"), new ManaCraftingTableBlockEntity.Provider(blockEntity));
        }
    }
}

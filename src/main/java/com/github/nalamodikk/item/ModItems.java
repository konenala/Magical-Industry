package com.github.nalamodikk.item;

import com.github.nalamodikk.MagicalIndustryMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MagicalIndustryMod.MOD_ID);

    public static final RegistryObject<Item> MANA_DUST = ITEMS.register("mana_dust",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MANA_INGOT = ITEMS.register("mana_ingot",
            () -> new Item(new Item.Properties()));



  public static void register(IEventBus eventBus) {
      ITEMS.register(eventBus);
  }

}

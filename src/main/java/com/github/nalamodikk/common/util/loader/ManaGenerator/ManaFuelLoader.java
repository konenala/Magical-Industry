package com.github.nalamodikk.common.util.loader.ManaGenerator;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class ManaFuelLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final Map<ResourceLocation, Integer> MANA_FUELS = new HashMap<>();

    public ManaFuelLoader() {
        super(GSON, "mana_fuels");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        MANA_FUELS.clear();

        resources.forEach((key, value) -> {
            if (value.isJsonObject()) {
                JsonObject jsonObject = value.getAsJsonObject();

                jsonObject.entrySet().forEach(entry -> {
                    try {
                        ResourceLocation fuelId = new ResourceLocation(entry.getKey());
                        int manaValue = entry.getValue().getAsInt();
                        MANA_FUELS.put(fuelId, manaValue);

                        // 成功加载魔力燃料时发出日志
                        MagicalIndustryMod.LOGGER.info("Loaded mana fuel: {} with mana value: {}", fuelId, manaValue);
                    } catch (Exception e) {
                        MagicalIndustryMod.LOGGER.error("Failed to parse mana fuel for key: {} in file: {}", entry.getKey(), key, e);
                    }
                });
            } else {
                MagicalIndustryMod.LOGGER.warn("Skipping non-JsonObject value for resource: {}", key);
            }
        });

        // 加载完成后发出总结日志
        MagicalIndustryMod.LOGGER.info("Successfully loaded {} mana fuels.", MANA_FUELS.size());
    }




    public static int getManaFuelValue(ResourceLocation item) {
        return MANA_FUELS.getOrDefault(item, 0);
    }
}

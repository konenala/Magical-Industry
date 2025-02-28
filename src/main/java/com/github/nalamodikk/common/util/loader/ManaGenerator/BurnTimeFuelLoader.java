package com.github.nalamodikk.common.util.loader.ManaGenerator;

import com.github.nalamodikk.common.NeoMagnaMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class BurnTimeFuelLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final Map<ResourceLocation, Integer> BURN_TIME_FUELS = new HashMap<>();

    public BurnTimeFuelLoader() {
        super(GSON, "burn_time_fuels");
    }


    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        BURN_TIME_FUELS.clear();

        resources.forEach((key, value) -> {
            if (value.isJsonObject()) {
                JsonObject jsonObject = value.getAsJsonObject();

                jsonObject.entrySet().forEach(entry -> {
                    try {
                        ResourceLocation fuelId = new ResourceLocation(entry.getKey());
                        int burnTime = entry.getValue().getAsInt();
                        BURN_TIME_FUELS.put(fuelId, burnTime);

                        // 在每次成功加载一个燃料时发出日志
                        NeoMagnaMod.LOGGER.info("Loaded burn time for fuel: {} with burn time: {}", fuelId, burnTime);
                    } catch (Exception e) {
                        NeoMagnaMod.LOGGER.error("Loaded burn time for fuel {} in file: {}", entry.getKey(), key, e);
                    }
                });
            } else {
                NeoMagnaMod.LOGGER.warn("Skipping non-JsonObject value for resource: {}", key);
            }
        });

        // 加载完成后发出总结日志
        NeoMagnaMod.LOGGER.info("SSkipping non-JsonObject value fod {} burn time fuels.", BURN_TIME_FUELS.size());
    }


    public static int getBurnTime(ResourceLocation itemId) {
        return BURN_TIME_FUELS.getOrDefault(itemId, -1);
    }
}

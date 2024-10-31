package com.github.nalamodikk.common.util.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class FuelRateLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FuelRateLoader.class);
    private static final Gson GSON = new Gson();
    private static final Map<String, FuelRate> FUEL_RATES = new HashMap<>();

    public FuelRateLoader() {
        super(GSON, "recipes/fuel_rate");  // 修改此處，使其加載 fuel_rate 目錄下的 JSON 文件
    }


    // Event for adding reload listener
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new FuelRateLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        FUEL_RATES.clear(); // Clear old data

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(entry.getValue(), "fuel_rate");
                JsonObject ingredientObject = GsonHelper.getAsJsonObject(jsonObject, "ingredient");
                String itemId = GsonHelper.getAsString(ingredientObject, "item");
                int manaRate = GsonHelper.getAsInt(jsonObject, "mana");
                int burnTime = GsonHelper.getAsInt(jsonObject, "burn_time", 200); // 默認燃燒時間 200
                int energyRate = GsonHelper.getAsInt(jsonObject, "energy");

                FuelRate fuelRate = new FuelRate(manaRate, burnTime, energyRate);
                FUEL_RATES.put(itemId, fuelRate);

            } catch (IllegalArgumentException | JsonParseException e) {
                LOGGER.error("Couldn't parse fuel rate for {}", id, e);
            }
        }
        LOGGER.info("Loaded {} fuel rates", FUEL_RATES.size());
    }

    // Method to get fuel rate for an item
    public static FuelRate getFuelRateForItem(ResourceLocation itemId) {
        return FUEL_RATES.getOrDefault(itemId.toString(), new FuelRate(0, 0, 0));
    }

    // Class representing fuel rate
    public static class FuelRate {
        public final int manaRate;
        public final int burnTime;
        public final int energyRate;

        public FuelRate(int manaRate, int burnTime, int energyRate) {
            this.manaRate = manaRate;
            this.burnTime = burnTime;
            this.energyRate = energyRate;
        }

        public int getManaRate() {
            return manaRate;
        }

        public int getBurnTime() {
            return burnTime;
        }

        public int getEnergyRate() {
            return energyRate;
        }
    }
}

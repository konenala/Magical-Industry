package com.github.nalamodikk.common.config;

import com.github.nalamodikk.common.NeoMagnaMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ManaConduitConfigLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static int transferRate = -1;
    private static final Path CONFIG_PATH = Paths.get("config/magical_industry/mana_conduit_transfer.json");

    public ManaConduitConfigLoader() {
        super(GSON, "config");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (objectMap.isEmpty()) {
            NeoMagnaMod.LOGGER.warn("ManaConduitConfig 未成功載入，將嘗試從本地 JSON 文件讀取！");
            loadConfigFromFile();
            return;
        }

        for (Map.Entry<ResourceLocation, JsonElement> entry : objectMap.entrySet()) {
            if (entry.getKey().getPath().equals("mana_conduit_transfer")) {
                JsonObject json = entry.getValue().getAsJsonObject();
                transferRate = json.has("transfer_rate") ? json.get("transfer_rate").getAsInt() : -1;
                NeoMagnaMod.LOGGER.info("成功載入 ManaConduitConfig，傳輸速率: {}", transferRate);
            }
        }
    }

    public static void generateDefaultConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                Files.createDirectories(CONFIG_PATH.getParent());
                JsonObject defaultConfig = new JsonObject();
                defaultConfig.addProperty("transfer_rate", ManaConduitConfig.getDefaultManaTransferRate());
                Files.writeString(CONFIG_PATH, GSON.toJson(defaultConfig));
                NeoMagnaMod.LOGGER.info("ManaConduitConfig 預設 JSON 文件已創建！");
            }
        } catch (IOException e) {
            NeoMagnaMod.LOGGER.error("無法生成 mana_conduit_transfer.json 配置文件！", e);
        }
    }

    public static void loadConfigFromFile() {
        if (!Files.exists(CONFIG_PATH)) {
            generateDefaultConfig();
        }

        try {
            String jsonString = Files.readString(CONFIG_PATH);
            JsonObject json = GSON.fromJson(jsonString, JsonObject.class);
            transferRate = json.has("transfer_rate") ? json.get("transfer_rate").getAsInt() : -1;
            NeoMagnaMod.LOGGER.info("從 mana_conduit_transfer.json 成功載入配置，傳輸速率: {}", transferRate);
            NeoMagnaMod.LOGGER.debug("Mana Conduit Transfer Rate loaded: {}", transferRate);
            NeoMagnaMod.LOGGER.debug("ManaConduitConfigLoader getTransferRate() -> {}", transferRate);
        } catch (IOException e) {
            NeoMagnaMod.LOGGER.error("無法讀取 mana_conduit_transfer.json！", e);
        }
    }

    public static int getTransferRate() {
        return transferRate;
    }

    public static boolean hasJsonConfig() {
        return transferRate != -1;
    }
}

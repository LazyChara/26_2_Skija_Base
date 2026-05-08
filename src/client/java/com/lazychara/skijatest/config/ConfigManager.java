package com.lazychara.skijatest.config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lazychara.skijatest.client.MainMenuRenderer;
import com.lazychara.skijatest.client.SkijaTestClient;
import com.lazychara.skijatest.client.SkijaTestScreen;
import com.lazychara.skijatest.module.BooleanSetting;
import com.lazychara.skijatest.module.Module;
import com.lazychara.skijatest.module.ModuleManager;
import com.lazychara.skijatest.module.Setting;
import com.lazychara.skijatest.module.render.HUD;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path LAZYCHARA_DIR = FabricLoader.getInstance().getGameDir().resolve("lazychara");
    private static final File CONFIG_FILE = LAZYCHARA_DIR.resolve("skijatest.json").toFile();
    static {
        try { Files.createDirectories(LAZYCHARA_DIR); } catch (Exception ignored) {}
    }
    public static void save() {
        try {
            JsonObject root = new JsonObject();
            JsonObject ui = new JsonObject();
            ui.addProperty("cR", SkijaTestScreen.cR);
            ui.addProperty("cG", SkijaTestScreen.cG);
            ui.addProperty("cB", SkijaTestScreen.cB);
            ui.addProperty("font", SkijaTestScreen.selectedFontName);
            ui.addProperty("mainMenuBg", MainMenuRenderer.selectedBgId);
            root.add("ui", ui);
            JsonObject modulesObj = new JsonObject();
            for (Module mod : ModuleManager.modules) {
                JsonObject mObj = new JsonObject();
                mObj.addProperty("enabled", mod.enabled);
                mObj.addProperty("keybind", mod.keybind);
                if (mod instanceof HUD hud) {
                    JsonObject posObj = new JsonObject();
                    posObj.addProperty("mainX", hud.mainX);
                    posObj.addProperty("mainY", hud.mainY);
                    posObj.addProperty("xyzX", hud.xyzX);
                    posObj.addProperty("xyzY", hud.xyzY);
                    posObj.addProperty("potionsX", hud.potionsX);
                    posObj.addProperty("potionsY", hud.potionsY);
                    mObj.add("positions", posObj);
                }
                JsonObject settingsObj = new JsonObject();
                for (Setting s : mod.settings) {
                    if (s instanceof BooleanSetting) {
                        BooleanSetting bs = (BooleanSetting) s;
                        settingsObj.addProperty(s.name, bs.value);
                    }
                }
                mObj.add("settings", settingsObj);
                modulesObj.add(mod.name, mObj);
            }
            root.add("modules", modulesObj);
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[SkijaTest] Failed to save config", e);
        }
    }
    public static void load() {
        if (!CONFIG_FILE.exists()) return;
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root.has("ui")) {
                JsonObject ui = root.getAsJsonObject("ui");
                if (ui.has("cR")) SkijaTestScreen.cR = ui.get("cR").getAsInt();
                if (ui.has("cG")) SkijaTestScreen.cG = ui.get("cG").getAsInt();
                if (ui.has("cB")) SkijaTestScreen.cB = ui.get("cB").getAsInt();
                if (ui.has("font")) SkijaTestScreen.selectedFontName = ui.get("font").getAsString();
                if (ui.has("mainMenuBg")) MainMenuRenderer.selectedBgId = ui.get("mainMenuBg").getAsString();
            }
            if (root.has("modules")) {
                JsonObject modulesObj = root.getAsJsonObject("modules");
                for (Module mod : ModuleManager.modules) {
                    if (modulesObj.has(mod.name)) {
                        JsonObject mObj = modulesObj.getAsJsonObject(mod.name);
                        if (mObj.has("enabled")) mod.enabled = mObj.get("enabled").getAsBoolean();
                        if (mObj.has("keybind")) mod.keybind = mObj.get("keybind").getAsInt();
                        if (mod instanceof HUD hud && mObj.has("positions")) {
                            JsonObject posObj = mObj.getAsJsonObject("positions");
                            if (posObj.has("mainX")) hud.mainX = posObj.get("mainX").getAsInt();
                            if (posObj.has("mainY")) hud.mainY = posObj.get("mainY").getAsInt();
                            if (posObj.has("xyzX")) hud.xyzX = posObj.get("xyzX").getAsInt();
                            if (posObj.has("xyzY")) hud.xyzY = posObj.get("xyzY").getAsInt();
                            if (posObj.has("potionsX")) hud.potionsX = posObj.get("potionsX").getAsInt();
                            if (posObj.has("potionsY")) hud.potionsY = posObj.get("potionsY").getAsInt();
                        }
                        if (mObj.has("settings")) {
                            JsonObject settingsObj = mObj.getAsJsonObject("settings");
                            for (Setting s : mod.settings) {
                                if (s instanceof BooleanSetting && settingsObj.has(s.name)) {
                                    BooleanSetting bs = (BooleanSetting) s;
                                    bs.value = settingsObj.get(s.name).getAsBoolean();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[SkijaTest] Failed to load config", e);
        }
    }
}

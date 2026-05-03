package com.lazychara.skijatest.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lazychara.skijatest.module.ModuleManager;

public class SkijaTestClient implements ClientModInitializer {

    public static final String MOD_ID = "skija-test";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.skija-test.open_gui",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.MISC
    );

    @Override
    public void onInitializeClient() {
        LOGGER.info("[SkijaTest] log print test, Ciallo !");

        ModuleManager.init();
        com.lazychara.skijatest.config.ConfigManager.load();
        HudManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI_KEY.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new SkijaTestScreen());
                }
            }

            if (client.getWindow() != null && client.gui.screen() == null) {
                long window = client.getWindow().handle();
                for (com.lazychara.skijatest.module.Module mod : ModuleManager.modules) {
                    if (mod.keybind != -1) {
                        boolean isDown = GLFW.glfwGetKey(window, mod.keybind) == GLFW.GLFW_PRESS;
                        if (isDown && !mod.wasKeyPressed) {
                            mod.toggle();
                        }
                        mod.wasKeyPressed = isDown;
                    }
                }
            }
        });

        LOGGER.info("[SkijaTest] loaded ");
    }
}

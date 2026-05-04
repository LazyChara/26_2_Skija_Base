package com.lazychara.skijatest.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.lazychara.skijatest.module.ModuleManager;
public class SkijaTestClient implements ClientModInitializer {
    public static final String MOD_ID = "skija-test";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.skija-test.open_gui",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.MISC
    );
    private static final List<DelayedClientTask> DELAYED_CLIENT_TASKS = new ArrayList<>();
    public static void runAfterClientTicks(int ticks, Runnable task) {
        DELAYED_CLIENT_TASKS.add(new DelayedClientTask(Math.max(1, ticks), task));
    }
    private static void runDelayedClientTasks() {
        Iterator<DelayedClientTask> iterator = DELAYED_CLIENT_TASKS.iterator();
        while (iterator.hasNext()) {
            DelayedClientTask delayed = iterator.next();
            delayed.ticks--;
            if (delayed.ticks <= 0) {
                iterator.remove();
                try {
                    delayed.task.run();
                } catch (Exception e) {
                    LOGGER.error("[SkijaTest] Delayed client task failed", e);
                }
            }
        }
    }
    private static final class DelayedClientTask {
        private int ticks;
        private final Runnable task;
        private DelayedClientTask(int ticks, Runnable task) {
            this.ticks = ticks;
            this.task = task;
        }
    }
    @Override
    public void onInitializeClient() {
        LOGGER.info("[SkijaTest] log print test, Ciallo !");
        ModuleManager.init();
        com.lazychara.skijatest.config.ConfigManager.load();
        HudManager.init();
        EspManager.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            runDelayedClientTasks();
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

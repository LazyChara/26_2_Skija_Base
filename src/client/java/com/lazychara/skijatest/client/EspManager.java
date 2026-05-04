package com.lazychara.skijatest.client;
import com.lazychara.skijatest.client.esp.EspRender2D;
import com.lazychara.skijatest.client.esp.EspRender3D;
import com.lazychara.skijatest.client.esp.EspRenderUtil;
import com.lazychara.skijatest.client.esp.EspTarget;
import com.lazychara.skijatest.module.render.ESP;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import java.util.ArrayList;
import java.util.List;
public class EspManager {
    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            ESP esp = EspRenderUtil.getEspModule();
            if (esp == null || !esp.enabled) {
                EspRender2D.clear();
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || context.levelState() == null || context.levelState().cameraRenderState == null) {
                EspRender2D.clear();
                return;
            }
            var camera = context.levelState().cameraRenderState;
            List<EspTarget> targets = collectTargets(mc, esp);
            if (esp.render3D.value) {
                EspRender3D.submit(targets, context);
            }
            EspRender2D.collect(mc, esp, targets, camera.pos, camera.viewRotationMatrix, camera.projectionMatrix);
        });
        HudElementRegistry.addLast(Identifier.parse("skija-test:esp_2d"), (g, deltaTracker) -> {
            ESP esp = EspRenderUtil.getEspModule();
            if (esp == null || !esp.enabled) {
                EspRender2D.clear();
                return;
            }
            EspRender2D.renderHud(g, esp, Minecraft.getInstance());
        });
    }
    private static List<EspTarget> collectTargets(Minecraft mc, ESP esp) {
        List<EspTarget> targets = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!EspRenderUtil.shouldRenderEntity(esp, mc, entity)) continue;
            targets.add(new EspTarget(entity, entity.getBoundingBox().inflate(0.04), EspRenderUtil.entityColor(entity)));
        }
        return targets;
    }
}

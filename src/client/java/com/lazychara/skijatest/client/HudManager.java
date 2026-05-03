package com.lazychara.skijatest.client;

import com.lazychara.skijatest.module.ModuleManager;
import com.lazychara.skijatest.module.NotificationManager;
import com.lazychara.skijatest.module.render.HUD;
import io.github.humbleui.skija.Typeface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;

public class HudManager {
    private static SkijaRenderer hudR;
    private static int lastGuiScale = -1;
    private static final int GUI_W = 260;
    private static final int GUI_H = 40;
    private static long lastTime = 0;

    private static float currentW = 80f;
    private static float currentH = 22f;
    
    private static boolean isDirty = true;
    private static int lastFps = -1;
    private static float lastNotifTimer = -1f;
    private static String lastNotifText = "";

    public static void init() {
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
            net.minecraft.resources.Identifier.parse("skija-test:hud"),
            (g, deltaTracker) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getWindow() == null) return;

                HUD hudModule = (HUD) ModuleManager.modules.stream()
                        .filter(m -> m.name.equalsIgnoreCase("HUD"))
                        .findFirst()
                        .orElse(null);
                if (hudModule == null || !hudModule.enabled) return;

                int guiScale = Math.max(1, mc.getWindow().getGuiScale());
                int texW = GUI_W * guiScale;
                int texH = GUI_H * guiScale;
                if (hudR == null || lastGuiScale != guiScale) {
                    if (hudR != null) hudR.close();
                    hudR = new SkijaRenderer("hud", texW, texH);
                    lastGuiScale = guiScale;
                }

                long now = System.currentTimeMillis();
                if (lastTime == 0) lastTime = now;
                float dt = Math.min((now - lastTime) / 1000f, 0.05f);
                lastTime = now;

                if (NotificationManager.notifTimer > 0f) {
                    NotificationManager.notifTimer -= dt;
                    if (NotificationManager.notifTimer < 0f)
                        NotificationManager.notifTimer = 0f;
                    isDirty = true;
                }

                int currentFps = mc.getFps();
                boolean showingNotif = NotificationManager.notifTimer > 0f;
                float targetW = showingNotif ? 220f : 80f;
                float targetH = showingNotif ? 28f : 22f;
                
                float speed = 28f;
                float newW = currentW + (targetW - currentW) * speed * dt;
                float newH = currentH + (targetH - currentH) * speed * dt;
                
                if (Math.abs(newW - targetW) < 0.5f) newW = targetW;
                if (Math.abs(newH - targetH) < 0.5f) newH = targetH;
                
                boolean animChanged = Math.abs(newW - currentW) > 0.01f || Math.abs(newH - currentH) > 0.01f;
                currentW = newW;
                currentH = newH;
                
                if (lastFps != currentFps || Math.abs(lastNotifTimer - NotificationManager.notifTimer) > 0.01f || animChanged) {
                    isDirty = true;
                    lastFps = currentFps;
                    lastNotifTimer = NotificationManager.notifTimer;
                }
                
                if (isDirty) {
                    renderHud(guiScale);
                    isDirty = false;
                }

                int screenW = mc.getWindow().getGuiScaledWidth();
                int x = (screenW - GUI_W) / 2;
                int y = 6;

                try {
                    var pose = g.pose();
                    int fullTexW = hudR.getWidth();
                    int fullTexH = hudR.getHeight();
                    float inv = 1f / guiScale;

                    pose.pushMatrix();
                    pose.translate(x, y);
                    pose.scale(inv, inv);
                    g.blit(RenderPipelines.GUI_TEXTURED, hudR.textureId(),
                            0, 0, 0f, 0f, fullTexW, fullTexH, fullTexW, fullTexH);
                    pose.popMatrix();
                } catch (Exception e) {
                    SkijaTestClient.LOGGER.error("Failed to render HUD", e);
                }
            }
        );
    }

    private static Typeface cachedTf = null;

    private static void renderHud(int guiScale) {
        if (hudR == null) return;
        
        hudR.clear(0x00000000);
        var c = hudR.canvas();
        c.save();
        c.scale(guiScale, guiScale);

        float centerX = GUI_W / 2f;
        float topY = 2f;

        float left = centerX - currentW / 2f;
        float right = centerX + currentW / 2f;
        float bottom = topY + currentH;

        float cornerRadius = currentH / 2f;
        hudR.drawRoundedRect(left, topY, currentW, currentH, cornerRadius, 0x40FFFFFF);
        hudR.drawRoundedRectStroke(left, topY, currentW, currentH, cornerRadius, 1.2f, 0x66FFFFFF);

        c.save();
        c.clipRect(io.github.humbleui.types.Rect.makeLTRB(left + 6, topY, right - 6, bottom));

        Typeface tf = SkijaTestScreen.curTf;
        if (tf == null) {
            if (cachedTf == null) {
                tf = hudR.getDefaultTypeface();
                if (tf == null) {
                    var bundled = SkijaRenderer.loadAllBundledFonts();
                    if (!bundled.isEmpty()) cachedTf = bundled.values().iterator().next();
                }
                tf = cachedTf;
            } else {
                tf = cachedTf;
            }
        }

        float fontSize = 11f;
        float textY = topY + (currentH - fontSize) / 2f;

        int baseTextCol = (255 << 24) | (Math.max(0, Math.min(255, SkijaTestScreen.cR)) << 16) | (Math.max(0, Math.min(255, SkijaTestScreen.cG)) << 8) | Math.max(0, Math.min(255, SkijaTestScreen.cB));

        boolean showingNotif = NotificationManager.notifTimer > 0f;
        if (!showingNotif || currentW < 130f) {
            hudR.drawTextCentered("FPS " + lastFps, centerX, textY, tf, fontSize, baseTextCol);
        } else {
            float alpha = Math.min(1f, (currentW - 130f) / 60f);
            int a = Math.round(alpha * 255f);
            int textCol = (a << 24) | (baseTextCol & 0xFFFFFF);
            hudR.drawTextCentered(NotificationManager.currentNotification, centerX, textY, tf, fontSize, textCol);
        }

        c.restore();
        c.restore();
        hudR.upload();
    }
}
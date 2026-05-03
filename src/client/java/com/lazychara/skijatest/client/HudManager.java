package com.lazychara.skijatest.client;

import com.lazychara.skijatest.module.ModuleManager;
import com.lazychara.skijatest.module.NotificationManager;
import com.lazychara.skijatest.module.render.HUD;
import io.github.humbleui.skija.Typeface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HudManager {
    private static SkijaRenderer mainR;
    private static SkijaRenderer mainFpsTextR;
    private static SkijaRenderer mainNotifTextR;
    private static SkijaRenderer xyzR;
    private static SkijaRenderer potionR;
    private static int lastGuiScale = -1;

    private static final int MAIN_W = 260;
    private static final int MAIN_H = 40;
    private static final int XYZ_W = 160;
    private static final int XYZ_H = 24;
    private static final int POTION_W = 180;
    private static final int POTION_ROW_H = 28;
    private static final int POTION_PAD = 4;
    private static final int SNAP_DISTANCE = 5;
    private static final float MAIN_CENTER_X = MAIN_W / 2f;
    private static final float MAIN_TOP_Y = 2f;
    private static final float MAIN_COLLAPSED_W = 80f;
    private static final float MAIN_COLLAPSED_H = 22f;
    private static final float MAIN_EXPANDED_W = 220f;
    private static final float MAIN_EXPANDED_H = 28f;
    private static final int MAIN_TEXT_H = 16;
    private static final long MAIN_FPS_UPDATE_INTERVAL_MS = 250L;

    private static long lastTime = 0;
    private static float currentW = MAIN_COLLAPSED_W;
    private static float currentH = MAIN_COLLAPSED_H;

    private static boolean mainBgDirty = true;
    private static boolean mainFpsTextDirty = true;
    private static boolean mainNotifTextDirty = true;
    private static boolean xyzDirty = true;
    private static boolean potionDirty = true;
    private static int displayedFps = -1;
    private static String lastNotificationText = "";
    private static int lastMainTextColor = 0;
    private static boolean lastEditing = false;
    private static long lastMainFpsTextureUpdateMs = 0L;
    private static String lastXyzText = "";
    private static String lastPotionKey = "";
    private static int lastPotionRows = 1;
    private static boolean wasLeftDown = false;
    private static String dragging = null;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static int activeGuideX = -1;
    private static int activeGuideY = -1;

    public static void init() {
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
            net.minecraft.resources.Identifier.parse("skija-test:hud"),
            (g, deltaTracker) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getWindow() == null) return;

                HUD hudModule = getHudModule();
                if (hudModule == null || !hudModule.enabled) return;

                int guiScale = Math.max(1, mc.getWindow().getGuiScale());
                ensureRenderers(guiScale, Math.max(1, lastPotionRows));
                ensureDefaultPositions(hudModule, mc);

                boolean editing = mc.gui.screen() instanceof ChatScreen;
                if (editing) handleEditorMouse(hudModule, mc);
                else if (wasLeftDown || dragging != null) {
                    wasLeftDown = false;
                    dragging = null;
                }

                long now = System.currentTimeMillis();
                if (lastTime == 0) lastTime = now;
                float dt = Math.min((now - lastTime) / 1000f, 0.05f);
                lastTime = now;

                if (NotificationManager.notifTimer > 0f) {
                    NotificationManager.notifTimer -= dt;
                    if (NotificationManager.notifTimer < 0f) NotificationManager.notifTimer = 0f;
                }

                int currentFps = mc.getFps();
                boolean fpsCanRefresh = displayedFps < 0 || now - lastMainFpsTextureUpdateMs >= MAIN_FPS_UPDATE_INTERVAL_MS;
                if (fpsCanRefresh) {
                    if (displayedFps != currentFps) {
                        displayedFps = currentFps;
                        mainFpsTextDirty = true;
                    }
                    lastMainFpsTextureUpdateMs = now;
                }

                boolean showingNotif = NotificationManager.notifTimer > 0f;
                float targetW = showingNotif ? MAIN_EXPANDED_W : MAIN_COLLAPSED_W;
                float targetH = showingNotif ? MAIN_EXPANDED_H : MAIN_COLLAPSED_H;

                float speed = 28f;
                float step = Math.min(1f, speed * dt);
                float newW = currentW + (targetW - currentW) * step;
                float newH = currentH + (targetH - currentH) * step;

                if (Math.abs(newW - targetW) < 0.5f) newW = targetW;
                if (Math.abs(newH - targetH) < 0.5f) newH = targetH;

                currentW = newW;
                currentH = newH;

                int currentTextColor = textColor();
                if (currentTextColor != lastMainTextColor) {
                    mainFpsTextDirty = true;
                    mainNotifTextDirty = true;
                    lastMainTextColor = currentTextColor;
                }
                if (!NotificationManager.currentNotification.equals(lastNotificationText)) {
                    mainNotifTextDirty = true;
                    lastNotificationText = NotificationManager.currentNotification;
                }
                if (editing != lastEditing) {
                    mainBgDirty = true;
                    lastEditing = editing;
                }

                String xyzText = makeXyzText(mc);
                if (!xyzText.equals(lastXyzText) || editing) {
                    xyzDirty = true;
                    lastXyzText = xyzText;
                }

                List<MobEffectInstance> effects = getVisibleEffects(mc);
                String potionKey = makePotionKey(effects, editing);
                int potionRows = Math.max(1, effects.size());
                if (potionRows != lastPotionRows) {
                    lastPotionRows = potionRows;
                    recreatePotionRenderer(guiScale, potionRows);
                    potionDirty = true;
                }
                if (!potionKey.equals(lastPotionKey) || editing) {
                    potionDirty = true;
                    lastPotionKey = potionKey;
                }

                if (mainBgDirty) {
                    renderMainBackground(guiScale, editing);
                    mainBgDirty = false;
                }
                if (mainFpsTextDirty) {
                    renderMainText(mainFpsTextR, guiScale, "FPS " + displayedFps, currentTextColor);
                    mainFpsTextDirty = false;
                }
                if (mainNotifTextDirty) {
                    renderMainText(mainNotifTextR, guiScale, NotificationManager.currentNotification, currentTextColor);
                    mainNotifTextDirty = false;
                }
                if (xyzDirty) {
                    renderXyz(guiScale, editing, hudModule.xyz.value, xyzText);
                    xyzDirty = false;
                }
                if (potionDirty) {
                    renderPotions(guiScale, editing, hudModule.potions.value, effects);
                    potionDirty = false;
                }

                try {
                    blitMainBackground(g, mainR, hudModule.mainX, hudModule.mainY);
                    blitMainText(g, showingNotif && currentW >= 130f ? mainNotifTextR : mainFpsTextR, hudModule.mainX, hudModule.mainY);
                    if (hudModule.xyz.value || editing) blit(g, xyzR, hudModule.xyzX, hudModule.xyzY, XYZ_W, XYZ_H);
                    if (hudModule.potions.value || editing) {
                        int potionH = getPotionHeight(potionRows);
                        blit(g, potionR, hudModule.potionsX, hudModule.potionsY, POTION_W, potionH);
                        if (hudModule.potions.value) blitPotionIcons(g, effects, hudModule.potionsX, hudModule.potionsY);
                    }
                    if (editing) drawAlignmentGuides(g, mc);
                } catch (Exception e) {
                    SkijaTestClient.LOGGER.error("Failed to render HUD", e);
                }
            }
        );
    }

    public static boolean shouldHideVanillaEffects() {
        HUD hud = getHudModule();
        return hud != null && hud.enabled && hud.potions.value;
    }

    private static HUD getHudModule() {
        return (HUD) ModuleManager.modules.stream()
                .filter(m -> m.name.equalsIgnoreCase("HUD"))
                .findFirst()
                .orElse(null);
    }

    private static void ensureRenderers(int guiScale, int potionRows) {
        if (mainR == null || lastGuiScale != guiScale) {
            closeRenderers();
            mainR = new SkijaRenderer("hud_main_bg", MAIN_W * guiScale, MAIN_H * guiScale);
            mainFpsTextR = new SkijaRenderer("hud_main_fps_text", MAIN_W * guiScale, MAIN_TEXT_H * guiScale);
            mainNotifTextR = new SkijaRenderer("hud_main_notif_text", MAIN_W * guiScale, MAIN_TEXT_H * guiScale);
            xyzR = new SkijaRenderer("hud_xyz", XYZ_W * guiScale, XYZ_H * guiScale);
            potionR = new SkijaRenderer("hud_potions", POTION_W * guiScale, getPotionHeight(potionRows) * guiScale);
            lastGuiScale = guiScale;
            mainBgDirty = true;
            mainFpsTextDirty = true;
            mainNotifTextDirty = true;
            xyzDirty = true;
            potionDirty = true;
        }
    }

    private static void recreatePotionRenderer(int guiScale, int rows) {
        if (potionR != null) potionR.close();
        potionR = new SkijaRenderer("hud_potions", POTION_W * guiScale, getPotionHeight(rows) * guiScale);
    }

    private static void closeRenderers() {
        if (mainR != null) mainR.close();
        if (mainFpsTextR != null) mainFpsTextR.close();
        if (mainNotifTextR != null) mainNotifTextR.close();
        if (xyzR != null) xyzR.close();
        if (potionR != null) potionR.close();
        mainR = null;
        mainFpsTextR = null;
        mainNotifTextR = null;
        xyzR = null;
        potionR = null;
    }

    private static void ensureDefaultPositions(HUD hud, Minecraft mc) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        if (hud.mainX < 0) hud.mainX = (screenW - MAIN_W) / 2;
        if (hud.xyzX < 0) hud.xyzX = screenW - XYZ_W - 6;
        if (hud.xyzY < 0) hud.xyzY = screenH - XYZ_H - 6;
        if (hud.potionsX < 0) hud.potionsX = screenW - POTION_W - 6;
        clampPositions(hud, mc);
    }

    private static void clampPositions(HUD hud, Minecraft mc) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        hud.mainX = clamp(hud.mainX, 0, Math.max(0, screenW - MAIN_W));
        hud.mainY = clamp(hud.mainY, 0, Math.max(0, screenH - MAIN_H));
        hud.xyzX = clamp(hud.xyzX, 0, Math.max(0, screenW - XYZ_W));
        hud.xyzY = clamp(hud.xyzY, 0, Math.max(0, screenH - XYZ_H));
        hud.potionsX = clamp(hud.potionsX, 0, Math.max(0, screenW - POTION_W));
        hud.potionsY = clamp(hud.potionsY, 0, Math.max(0, screenH - getPotionHeight(lastPotionRows)));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void handleEditorMouse(HUD hud, Minecraft mc) {
        long window = mc.getWindow().handle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        int mx = (int) Math.round(mc.mouseHandler.getScaledXPos(mc.getWindow()));
        int my = (int) Math.round(mc.mouseHandler.getScaledYPos(mc.getWindow()));

        if (leftDown && !wasLeftDown) {
            dragging = hitTest(hud, mx, my);
            if (dragging != null) {
                int[] pos = getPos(hud, dragging);
                dragOffsetX = mx - pos[0];
                dragOffsetY = my - pos[1];
            }
        }

        if (leftDown && dragging != null) {
            int[] snapped = snapPosition(hud, dragging, mx - dragOffsetX, my - dragOffsetY, mc);
            setPos(hud, dragging, snapped[0], snapped[1]);
            clampPositions(hud, mc);
        }

        if (!leftDown && wasLeftDown && dragging != null) {
            dragging = null;
            activeGuideX = -1;
            activeGuideY = -1;
            com.lazychara.skijatest.config.ConfigManager.save();
        } else if (!leftDown && dragging == null) {
            activeGuideX = -1;
            activeGuideY = -1;
        }
        wasLeftDown = leftDown;
    }

    private static String hitTest(HUD hud, int mx, int my) {
        if (inside(mx, my, hud.potionsX, hud.potionsY, POTION_W, getPotionHeight(lastPotionRows))) return "potions";
        if (inside(mx, my, hud.xyzX, hud.xyzY, XYZ_W, XYZ_H)) return "xyz";
        if (inside(mx, my, hud.mainX, hud.mainY, MAIN_W, MAIN_H)) return "main";
        return null;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int[] getPos(HUD hud, String id) {
        if ("xyz".equals(id)) return new int[] { hud.xyzX, hud.xyzY };
        if ("potions".equals(id)) return new int[] { hud.potionsX, hud.potionsY };
        return new int[] { hud.mainX, hud.mainY };
    }

    private static void setPos(HUD hud, String id, int x, int y) {
        if ("xyz".equals(id)) {
            hud.xyzX = x;
            hud.xyzY = y;
        } else if ("potions".equals(id)) {
            hud.potionsX = x;
            hud.potionsY = y;
        } else {
            hud.mainX = x;
            hud.mainY = y;
        }
    }

    private static int[] snapPosition(HUD hud, String id, int x, int y, Minecraft mc) {
        int w = widgetWidth(id);
        int h = widgetHeight(id);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        activeGuideX = -1;
        activeGuideY = -1;

        int bestDx = SNAP_DISTANCE + 1;
        int bestX = x;
        int[] xAnchors = new int[] { x, x + w / 2, x + w };
        int[] screenXTargets = new int[] { 0, screenW / 2, screenW };
        for (int anchorIndex = 0; anchorIndex < xAnchors.length; anchorIndex++) {
            for (int target : screenXTargets) {
                int dx = target - xAnchors[anchorIndex];
                if (Math.abs(dx) < Math.abs(bestDx)) {
                    bestDx = dx;
                    bestX = x + dx;
                    activeGuideX = target;
                }
            }
        }
        for (WidgetBounds other : widgetBounds(hud)) {
            if (other.id.equals(id)) continue;
            int[] otherTargets = new int[] { other.x, other.x + other.w / 2, other.x + other.w };
            for (int anchorIndex = 0; anchorIndex < xAnchors.length; anchorIndex++) {
                for (int target : otherTargets) {
                    int dx = target - xAnchors[anchorIndex];
                    if (Math.abs(dx) < Math.abs(bestDx)) {
                        bestDx = dx;
                        bestX = x + dx;
                        activeGuideX = target;
                    }
                }
            }
        }
        if (Math.abs(bestDx) <= SNAP_DISTANCE) x = bestX;
        else activeGuideX = -1;

        int bestDy = SNAP_DISTANCE + 1;
        int bestY = y;
        int[] yAnchors = new int[] { y, y + h / 2, y + h };
        int[] screenYTargets = new int[] { 0, screenH / 2, screenH };
        for (int anchorIndex = 0; anchorIndex < yAnchors.length; anchorIndex++) {
            for (int target : screenYTargets) {
                int dy = target - yAnchors[anchorIndex];
                if (Math.abs(dy) < Math.abs(bestDy)) {
                    bestDy = dy;
                    bestY = y + dy;
                    activeGuideY = target;
                }
            }
        }
        for (WidgetBounds other : widgetBounds(hud)) {
            if (other.id.equals(id)) continue;
            int[] otherTargets = new int[] { other.y, other.y + other.h / 2, other.y + other.h };
            for (int anchorIndex = 0; anchorIndex < yAnchors.length; anchorIndex++) {
                for (int target : otherTargets) {
                    int dy = target - yAnchors[anchorIndex];
                    if (Math.abs(dy) < Math.abs(bestDy)) {
                        bestDy = dy;
                        bestY = y + dy;
                        activeGuideY = target;
                    }
                }
            }
        }
        if (Math.abs(bestDy) <= SNAP_DISTANCE) y = bestY;
        else activeGuideY = -1;

        return new int[] { x, y };
    }

    private static void drawAlignmentGuides(net.minecraft.client.gui.GuiGraphicsExtractor g, Minecraft mc) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int lineColor = 0xAAFFFFFF;
        if (activeGuideX >= 0) g.fill(activeGuideX, 0, activeGuideX + 1, screenH, lineColor);
        if (activeGuideY >= 0) g.fill(0, activeGuideY, screenW, activeGuideY + 1, lineColor);
    }

    private static int widgetWidth(String id) {
        if ("xyz".equals(id)) return XYZ_W;
        if ("potions".equals(id)) return POTION_W;
        return MAIN_W;
    }

    private static int widgetHeight(String id) {
        if ("xyz".equals(id)) return XYZ_H;
        if ("potions".equals(id)) return getPotionHeight(lastPotionRows);
        return MAIN_H;
    }

    private static List<WidgetBounds> widgetBounds(HUD hud) {
        List<WidgetBounds> bounds = new ArrayList<>();
        bounds.add(new WidgetBounds("main", hud.mainX, hud.mainY, MAIN_W, MAIN_H));
        bounds.add(new WidgetBounds("xyz", hud.xyzX, hud.xyzY, XYZ_W, XYZ_H));
        bounds.add(new WidgetBounds("potions", hud.potionsX, hud.potionsY, POTION_W, getPotionHeight(lastPotionRows)));
        return bounds;
    }

    private record WidgetBounds(String id, int x, int y, int w, int h) {}

    private static void blit(net.minecraft.client.gui.GuiGraphicsExtractor g, SkijaRenderer r, int x, int y, int guiW, int guiH) {
        int regionW = guiW * lastGuiScale;
        int regionH = guiH * lastGuiScale;
        int fullTexW = r.getWidth();
        int fullTexH = r.getHeight();
        float inv = 1f / lastGuiScale;

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, r.textureId(), 0, 0, 0f, 0f, regionW, regionH, fullTexW, fullTexH);
        pose.popMatrix();
    }

    private static void blitMainBackground(net.minecraft.client.gui.GuiGraphicsExtractor g, SkijaRenderer r, int x, int y) {
        if (r == null) return;
        float srcLeft = MAIN_CENTER_X - MAIN_EXPANDED_W / 2f;
        float srcTop = MAIN_TOP_Y;
        float srcCap = MAIN_EXPANDED_H / 2f;
        float srcCenterW = MAIN_EXPANDED_W - srcCap * 2f;

        float dstLeft = x + MAIN_CENTER_X - currentW / 2f;
        float dstTop = y + MAIN_TOP_Y;
        float dstCap = currentH / 2f;
        float dstCenterW = Math.max(0f, currentW - dstCap * 2f);

        blitSubRect(g, r, dstLeft, dstTop, dstCap, currentH, srcLeft, srcTop, srcCap, MAIN_EXPANDED_H);
        if (dstCenterW > 0.5f) {
            blitSubRect(g, r, dstLeft + dstCap, dstTop, dstCenterW, currentH, srcLeft + srcCap, srcTop, srcCenterW, MAIN_EXPANDED_H);
        }
        blitSubRect(g, r, dstLeft + dstCap + dstCenterW, dstTop, dstCap, currentH, srcLeft + srcCap + srcCenterW, srcTop, srcCap, MAIN_EXPANDED_H);
    }

    private static void blitMainText(net.minecraft.client.gui.GuiGraphicsExtractor g, SkijaRenderer r, int x, int y) {
        if (r == null) return;
        float visibleW = Math.max(1f, currentW - 12f);
        float srcX = MAIN_CENTER_X - visibleW / 2f;
        float dstX = x + MAIN_CENTER_X - visibleW / 2f;
        float dstY = y + MAIN_TOP_Y + (currentH - MAIN_TEXT_H) / 2f;
        blitSubRect(g, r, dstX, dstY, visibleW, MAIN_TEXT_H, srcX, 0f, visibleW, MAIN_TEXT_H);
    }

    private static void blitSubRect(net.minecraft.client.gui.GuiGraphicsExtractor g, SkijaRenderer r,
                                    float dstX, float dstY, float dstW, float dstH,
                                    float srcX, float srcY, float srcW, float srcH) {
        int srcPxX = Math.round(srcX * lastGuiScale);
        int srcPxY = Math.round(srcY * lastGuiScale);
        int srcPxW = Math.max(1, Math.round(srcW * lastGuiScale));
        int srcPxH = Math.max(1, Math.round(srcH * lastGuiScale));
        int dstPxW = Math.max(1, Math.round(dstW * lastGuiScale));
        int dstPxH = Math.max(1, Math.round(dstH * lastGuiScale));
        float inv = 1f / lastGuiScale;

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(dstX, dstY);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, r.textureId(), 0, 0, (float) srcPxX, (float) srcPxY, dstPxW, dstPxH, srcPxW, srcPxH, r.getWidth(), r.getHeight());
        pose.popMatrix();
    }

    private static Typeface cachedTf = null;

    private static Typeface font(SkijaRenderer r) {
        SkijaTestScreen.ensureFontLoaded();
        Typeface tf = SkijaTestScreen.curTf;
        if (tf == null) {
            if (cachedTf == null) {
                tf = r.getDefaultTypeface();
                if (tf == null) {
                    var bundled = SkijaRenderer.loadAllBundledFonts();
                    if (!bundled.isEmpty()) cachedTf = bundled.values().iterator().next();
                } else {
                    cachedTf = tf;
                }
            }
            tf = cachedTf;
        }
        return tf;
    }

    private static int textColor() {
        return (255 << 24) | (Math.max(0, Math.min(255, SkijaTestScreen.cR)) << 16) | (Math.max(0, Math.min(255, SkijaTestScreen.cG)) << 8) | Math.max(0, Math.min(255, SkijaTestScreen.cB));
    }

    private static void renderMainBackground(int guiScale, boolean editing) {
        if (mainR == null) return;
        mainR.clear(0x00000000);
        var c = mainR.canvas();
        c.save();
        c.scale(guiScale, guiScale);

        float left = MAIN_CENTER_X - MAIN_EXPANDED_W / 2f;
        float cornerRadius = MAIN_EXPANDED_H / 2f;
        int bg = editing ? 0x55FFFFFF : 0x40FFFFFF;
        int border = editing ? 0xAAFFFFFF : 0x66FFFFFF;

        mainR.drawRoundedRect(left, MAIN_TOP_Y, MAIN_EXPANDED_W, MAIN_EXPANDED_H, cornerRadius, bg);
        mainR.drawRoundedRectStroke(left, MAIN_TOP_Y, MAIN_EXPANDED_W, MAIN_EXPANDED_H, cornerRadius, 1.2f, border);

        c.restore();
        mainR.upload();
    }

    private static void renderMainText(SkijaRenderer r, int guiScale, String text, int color) {
        if (r == null) return;
        r.clear(0x00000000);
        var c = r.canvas();
        c.save();
        c.scale(guiScale, guiScale);

        Typeface tf = font(r);
        float fontSize = 11f;
        float textY = (MAIN_TEXT_H - fontSize) / 2f;
        r.drawTextCentered(text == null ? "" : text, MAIN_CENTER_X, textY, tf, fontSize, color);

        c.restore();
        r.upload();
    }

    private static void renderXyz(int guiScale, boolean editing, boolean enabled, String xyzText) {
        if (xyzR == null) return;
        xyzR.clear(0x00000000);
        var c = xyzR.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        if (enabled || editing) {
            int bg = enabled ? 0x40FFFFFF : 0x22FFFFFF;
            int border = editing ? (enabled ? 0xAAFFFFFF : 0x66FFFFFF) : 0x66FFFFFF;
            xyzR.drawRoundedRect(2, 2, XYZ_W - 4, XYZ_H - 4, 9f, bg);
            xyzR.drawRoundedRectStroke(2, 2, XYZ_W - 4, XYZ_H - 4, 9f, 1.1f, border);
        }
        String text = enabled ? xyzText : "XYZ: hidden";
        int col = enabled ? textColor() : 0x88FFFFFF;
        Typeface tf = font(xyzR);
        float textW = xyzR.measureText(text, tf, 11f);
        xyzR.drawText(text, (XYZ_W - textW) / 2f, 6f, tf, 11f, col);
        c.restore();
        xyzR.upload();
    }

    private static void renderPotions(int guiScale, boolean editing, boolean enabled, List<MobEffectInstance> effects) {
        if (potionR == null) return;
        potionR.clear(0x00000000);
        var c = potionR.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        int rows = Math.max(1, effects.size());
        int bg = enabled ? 0x40FFFFFF : 0x22FFFFFF;
        int border = editing ? (enabled ? 0xAAFFFFFF : 0x66FFFFFF) : 0x66FFFFFF;
        int h = getPotionHeight(rows) - 4;
        potionR.drawRoundedRect(2, 2, POTION_W - 4, h, 10f, bg);
        potionR.drawRoundedRectStroke(2, 2, POTION_W - 4, h, 10f, 1.1f, border);

        Typeface tf = font(potionR);
        if (!enabled) {
            potionR.drawTextCentered("Potions: hidden", POTION_W / 2f, 8f, tf, 11f, 0x88FFFFFF);
        } else if (effects.isEmpty()) {
            potionR.drawTextCentered(editing ? "Potions" : "No Effects", POTION_W / 2f, 8f, tf, 11f, editing ? textColor() : 0x88FFFFFF);
        } else {
            float y = POTION_PAD + 2;
            for (MobEffectInstance effect : effects) {
                String name = net.minecraft.network.chat.Component.translatable(effect.getDescriptionId()).getString();
                String duration = MobEffectUtil.formatDuration(effect, 1f, 20f).getString();
                if (effect.getAmplifier() > 0) name = name + " " + (effect.getAmplifier() + 1);
                if (name.length() > 18) name = name.substring(0, 16) + "..";
                potionR.drawText(name, 30, y + 1, tf, 10f, textColor());
                potionR.drawText(duration, 30, y + 14, tf, 8.5f, 0xB3FFFFFF);
                y += POTION_ROW_H;
            }
        }
        c.restore();
        potionR.upload();
    }

    private static void blitPotionIcons(net.minecraft.client.gui.GuiGraphicsExtractor g, List<MobEffectInstance> effects, int x, int y) {
        int rowY = y + POTION_PAD + 4;
        for (MobEffectInstance effect : effects) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect.getEffect()), x + 8, rowY, 18, 18);
            rowY += POTION_ROW_H;
        }
    }

    private static String makeXyzText(Minecraft mc) {
        if (mc.player == null) return "XYZ: 0 0 0";
        int x = (int) Math.floor(mc.player.getX());
        int y = (int) Math.floor(mc.player.getY());
        int z = (int) Math.floor(mc.player.getZ());
        return "XYZ: " + x + " " + y + " " + z;
    }

    private static List<MobEffectInstance> getVisibleEffects(Minecraft mc) {
        List<MobEffectInstance> list = new ArrayList<>();
        if (mc.player == null) return list;
        Collection<MobEffectInstance> active = mc.player.getActiveEffects();
        for (MobEffectInstance effect : active) {
            if (effect.isVisible() && effect.showIcon()) list.add(effect);
        }
        list.sort(null);
        return list;
    }

    private static String makePotionKey(List<MobEffectInstance> effects, boolean editing) {
        StringBuilder sb = new StringBuilder();
        sb.append(editing).append('|');
        for (MobEffectInstance effect : effects) {
            sb.append(effect.getDescriptionId()).append(':')
                    .append(effect.getAmplifier()).append(':')
                    .append(effect.getDuration() / 20).append(';');
        }
        return sb.toString();
    }

    private static int getPotionHeight(int rows) {
        return POTION_PAD * 2 + Math.max(1, rows) * POTION_ROW_H;
    }
}

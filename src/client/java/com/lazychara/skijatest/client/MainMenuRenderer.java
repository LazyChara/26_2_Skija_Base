package com.lazychara.skijatest.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Typeface;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainMenuRenderer {
    private static final int MENU_X = 26;
    private static final int BUTTON_W = 178;
    private static final int BUTTON_H = 30;
    private static final int BUTTON_GAP = 10;
    private static final int BUTTON_RADIUS = 12;
    private static final int BG_SELECTOR_W = 150;
    private static final int BG_SELECTOR_H = 24;
    private static final int BG_OPTION_H = 22;
    private static final int BG_SELECTOR_PAD = 14;
    private static final int AVATAR_SIZE = 24;
    private static final String[] MENU_KEYS = {
            "menu.singleplayer",
            "menu.multiplayer",
            "menu.online",
            "menu.options",
            "menu.quit"
    };

    public static String selectedBgId = "builtin:1.png";

    private static SkijaRenderer renderer;
    private static SkijaRenderer menuHoverR;
    private static SkijaRenderer bgSelectorHoverR;
    private static SkijaRenderer bgOptionHoverR;
    private static SkijaRenderer avatarR;
    private static int lastAvatarGuiScale = -1;
    private static int lastGuiScale = -1;
    private static int lastW = -1;
    private static int lastH = -1;
    private static DynamicTexture backgroundTexture;
    private static Identifier backgroundTextureId;
    private static int backgroundW = 1;
    private static int backgroundH = 1;
    private static String loadedBgId = "";
    private static Typeface cachedTf;
    private static String avatarProfileKey = "";
    private static boolean avatarLoadFailed = false;
    private static boolean bgSelectorOpen = false;
    private static boolean uiDirty = true;
    private static int lastHoverIndex = -2;
    private static boolean lastSelectorHover = false;
    private static int lastSelectorRowHover = -2;
    private static String lastSelectorHoverLabel = "";
    private static int lastSelectorHoverColor = 0;
    private static String lastSelectorRowHoverName = "";
    private static String lastRenderedBgId = "";
    private static String lastRenderedPlayerName = "";
    private static int lastTextColor = 0;
    private static List<BgOption> bgOptions = new ArrayList<>();
    private static long lastBgScan = 0L;

    public static void layoutVanillaButtons(TitleScreen screen) {
        Map<String, AbstractWidget> widgets = findMenuWidgets(screen);
        int y = menuStartY(screen.height);
        for (String key : MENU_KEYS) {
            AbstractWidget widget = widgets.get(key);
            if (widget != null) {
                widget.setRectangle(MENU_X, y, BUTTON_W, BUTTON_H);
                widget.visible = true;
                widget.active = false;
                widget.setAlpha(0f);
                y += BUTTON_H + BUTTON_GAP;
            }
        }
    }

    public static void render(TitleScreen screen, GuiGraphicsExtractor g, int mx, int my, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        int guiScale = Math.max(1, mc.getWindow().getGuiScale());
        if (renderer == null || lastGuiScale != guiScale || lastW != screen.width || lastH != screen.height) {
            closeRenderer();
            renderer = new SkijaRenderer("main_menu", Math.max(1, screen.width * guiScale), Math.max(1, screen.height * guiScale));
            menuHoverR = new SkijaRenderer("main_menu_hover", BUTTON_W * guiScale, BUTTON_H * guiScale);
            bgSelectorHoverR = new SkijaRenderer("main_menu_bg_selector_hover", BG_SELECTOR_W * guiScale, BG_SELECTOR_H * guiScale);
            bgOptionHoverR = new SkijaRenderer("main_menu_bg_option_hover", BG_SELECTOR_W * guiScale, BG_OPTION_H * guiScale);

            lastGuiScale = guiScale;
            lastW = screen.width;
            lastH = screen.height;
            uiDirty = true;
        }

        int hoverIndex = hoverMenuIndex(mx, my, screen.height);
        boolean selectorHover = isInsideBgSelector(mx, my, screen.width, screen.height);
        int selectorRowHover = bgSelectorOpen ? hoverBgOptionIndex(mx, my, screen.width, screen.height) : -1;
        updateUiDirtyState(screen, mc);
        if (hoverIndex != lastHoverIndex) {
            lastHoverIndex = hoverIndex;
            if (hoverIndex >= 0) renderMenuHover(guiScale, hoverIndex);
        }
        int color = textColor();
        String selectorLabel = bgSelectorLabel();
        if (selectorHover != lastSelectorHover || (selectorHover && (!selectorLabel.equals(lastSelectorHoverLabel) || color != lastSelectorHoverColor))) {
            lastSelectorHover = selectorHover;
            lastSelectorHoverLabel = selectorLabel;
            lastSelectorHoverColor = color;
            if (selectorHover) renderBgSelectorHover(guiScale, selectorLabel, color);
        }
        String selectorRowName = selectorRowHover >= 0 && selectorRowHover < bgOptions.size() ? bgOptions.get(selectorRowHover).displayName : "";
        if (selectorRowHover != lastSelectorRowHover || (selectorRowHover >= 0 && !selectorRowName.equals(lastSelectorRowHoverName))) {
            lastSelectorRowHover = selectorRowHover;
            lastSelectorRowHoverName = selectorRowName;
            if (selectorRowHover >= 0) renderBgOptionHover(guiScale, selectorRowHover);
        }
        if (uiDirty) {
            renderer.clear(0x00000000);
            var c = renderer.canvas();
            c.save();
            c.scale(guiScale, guiScale);

            drawMenu(screen, mx, my);
            drawPlayerInfo(mc, screen.width);
            drawBgSelector(screen.width, screen.height, mx, my);

            c.restore();
            renderer.upload();
            uiDirty = false;
        }

        drawBackground(g, mx, my, screen.width, screen.height);
        blitFull(g);
        if (hoverIndex >= 0) blitMenuHover(g, MENU_X, menuStartY(screen.height) + hoverIndex * (BUTTON_H + BUTTON_GAP));
        if (selectorHover && !bgSelectorOpen) blitBgSelectorHover(g, screen.width, screen.height);
        if (selectorRowHover >= 0 && !isBgOptionSelected(selectorRowHover)) blitBgOptionHover(g, screen.width, screen.height, selectorRowHover);
        drawPlayerHead(g, mc, screen.width);
    }

    private static void updateUiDirtyState(TitleScreen screen, Minecraft mc) {
        String playerName = mc.getUser() == null ? "Player" : mc.getUser().getName();
        int color = textColor();
        if (!selectedBgId.equals(lastRenderedBgId) || !playerName.equals(lastRenderedPlayerName) || color != lastTextColor) {
            lastRenderedBgId = selectedBgId;
            lastRenderedPlayerName = playerName;
            lastTextColor = color;
            uiDirty = true;
        }
    }

    private static int hoverMenuIndex(double mx, double my, int screenH) {
        int y = menuStartY(screenH);
        for (int i = 0; i < MENU_KEYS.length; i++) {
            if (mx >= MENU_X && mx <= MENU_X + BUTTON_W && my >= y && my <= y + BUTTON_H) return i;
            y += BUTTON_H + BUTTON_GAP;
        }
        return -1;
    }

    private static boolean isInsideBgSelector(double mx, double my, int screenW, int screenH) {
        int x = screenW - BG_SELECTOR_W - BG_SELECTOR_PAD;
        int y = screenH - BG_SELECTOR_H - BG_SELECTOR_PAD;
        return mx >= x && mx <= x + BG_SELECTOR_W && my >= y && my <= y + BG_SELECTOR_H;
    }

    private static int hoverBgOptionIndex(double mx, double my, int screenW, int screenH) {
        int x = screenW - BG_SELECTOR_W - BG_SELECTOR_PAD;
        int y = screenH - BG_SELECTOR_H - BG_SELECTOR_PAD;
        int optionCount = Math.min(8, bgOptions.size());
        int listH = optionCount * BG_OPTION_H;
        int listY = y - listH - 6;
        if (mx < x || mx > x + BG_SELECTOR_W || my < listY || my > listY + listH) return -1;
        int index = (int) ((my - listY) / BG_OPTION_H);
        return index >= 0 && index < optionCount ? index : -1;
    }

    public static boolean mouseClicked(TitleScreen screen, MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();
        int screenW = screen.width;
        int screenH = screen.height;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        layoutVanillaButtons(screen);
        ensureBgOptions();
        int x = screenW - BG_SELECTOR_W - BG_SELECTOR_PAD;
        int y = screenH - BG_SELECTOR_H - BG_SELECTOR_PAD;
        int optionCount = Math.min(8, bgOptions.size());
        int listH = optionCount * BG_OPTION_H;
        int listY = y - listH - 6;

        if (bgSelectorOpen) {
            if (mx >= x && mx <= x + BG_SELECTOR_W && my >= listY && my <= listY + listH) {
                int index = (int) ((my - listY) / BG_OPTION_H);
                if (index >= 0 && index < bgOptions.size()) {
                    selectBackground(bgOptions.get(index).id);
                }
                bgSelectorOpen = false;
                uiDirty = true;
                return true;
            }
            if (!(mx >= x && mx <= x + BG_SELECTOR_W && my >= y && my <= y + BG_SELECTOR_H)) {
                bgSelectorOpen = false;
                uiDirty = true;
                return true;
            }
        }

        if (mx >= x && mx <= x + BG_SELECTOR_W && my >= y && my <= y + BG_SELECTOR_H) {
            bgSelectorOpen = !bgSelectorOpen;
            uiDirty = true;
            return true;
        }

        String menuKey = hitMenu(mx, my, screenH);
        if (menuKey == null && (mx > screenW || my > screenH)) {
            int guiScale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
            menuKey = hitMenu(mx / guiScale, my / guiScale, screenH);
        }
        if (menuKey != null) {
            AbstractWidget widget = findMenuWidgets(screen).get(menuKey);
            if (widget != null) {
                widget.playDownSound(Minecraft.getInstance().getSoundManager());
                widget.onClick(event, doubleClick);
                return true;
            }
        }
        return false;
    }

    private static String hitMenu(double mx, double my, int screenH) {
        int y = menuStartY(screenH);
        for (String key : MENU_KEYS) {
            if (mx >= MENU_X && mx <= MENU_X + BUTTON_W && my >= y && my <= y + BUTTON_H) {
                return key;
            }
            y += BUTTON_H + BUTTON_GAP;
        }
        return null;
    }

    public static void close() {
        closeRenderer();
        closeBackground();
        uiDirty = true;
    }

    private static Map<String, AbstractWidget> findMenuWidgets(TitleScreen screen) {
        Map<String, AbstractWidget> result = new HashMap<>();
        Map<String, String> localized = localizedLabels();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget) {
                String text = widget.getMessage().getString();
                String key = keyByLocalizedText(localized, text);
                if (key != null && !result.containsKey(key)) {
                    result.put(key, widget);
                }
            }
        }
        return result;
    }

    private static String keyByLocalizedText(Map<String, String> localized, String text) {
        for (Map.Entry<String, String> entry : localized.entrySet()) {
            if (entry.getValue().equals(text)) return entry.getKey();
        }
        return null;
    }

    private static Map<String, String> localizedLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (String key : MENU_KEYS) {
            labels.put(key, Component.translatable(key).getString());
        }
        return labels;
    }

    private static void drawBackground(GuiGraphicsExtractor g, int mx, int my, int w, int h) {
        if (!ensureBackgroundTexture()) {
            g.fill(0, 0, w, h, 0xFF101018);
            return;
        }

        float zoom = 1.08f;
        float targetRatio = (float) w / Math.max(1f, h);
        float bgRatio = backgroundW / Math.max(1f, (float) backgroundH);
        float drawW;
        float drawH;
        if (bgRatio > targetRatio) {
            drawH = h * zoom;
            drawW = drawH * bgRatio;
        } else {
            drawW = w * zoom;
            drawH = drawW / bgRatio;
        }

        float mouseX = w <= 0 ? 0.5f : mx / (float) w;
        float mouseY = h <= 0 ? 0.5f : my / (float) h;
        float maxX = Math.max(0f, drawW - w);
        float maxY = Math.max(0f, drawH - h);
        int x = Math.round(-maxX * mouseX);
        int y = Math.round(-maxY * mouseY);
        int dw = Math.round(drawW);
        int dh = Math.round(drawH);

        g.blit(RenderPipelines.GUI_TEXTURED, backgroundTextureId, x, y, 0f, 0f, dw, dh, backgroundW, backgroundH, backgroundW, backgroundH);
        g.fill(0, 0, w, h, 0x66000000);
    }

    private static void drawMenu(TitleScreen screen, int mx, int my) {
        Typeface tf = font();
        int y = menuStartY(screen.height);
        Map<String, String> labels = localizedLabels();
        for (String key : MENU_KEYS) {
            renderer.drawRoundedRect(MENU_X, y, BUTTON_W, BUTTON_H, BUTTON_RADIUS, 0x35FFFFFF);
            renderer.drawRoundedRectStroke(MENU_X, y, BUTTON_W, BUTTON_H, BUTTON_RADIUS, 1.2f, 0x66FFFFFF);
            renderer.drawText(labels.getOrDefault(key, key), MENU_X + 16, y + 8, tf, 13f, textColor());
            y += BUTTON_H + BUTTON_GAP;
        }
    }

    private static void renderMenuHover(int guiScale, int hoverIndex) {
        if (menuHoverR == null || hoverIndex < 0 || hoverIndex >= MENU_KEYS.length) return;
        menuHoverR.clear(0x00000000);
        var c = menuHoverR.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        Typeface tf = font();
        String label = localizedLabels().getOrDefault(MENU_KEYS[hoverIndex], MENU_KEYS[hoverIndex]);
        menuHoverR.drawRoundedRect(0, 0, BUTTON_W, BUTTON_H, BUTTON_RADIUS, 0x55FFFFFF);
        menuHoverR.drawRoundedRectStroke(0, 0, BUTTON_W, BUTTON_H, BUTTON_RADIUS, 1.2f, 0xCCFFFFFF);
        menuHoverR.drawText(label, 16, 8, tf, 13f, 0xFFFFFFFF);
        c.restore();
        menuHoverR.upload();
    }

    private static void renderBgSelectorHover(int guiScale, String label, int color) {
        if (bgSelectorHoverR == null) return;
        bgSelectorHoverR.clear(0x00000000);
        var c = bgSelectorHoverR.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        Typeface tf = font();
        bgSelectorHoverR.drawRoundedRect(0, 0, BG_SELECTOR_W, BG_SELECTOR_H, 10f, 0x55FFFFFF);
        bgSelectorHoverR.drawRoundedRectStroke(0, 0, BG_SELECTOR_W, BG_SELECTOR_H, 10f, 1.1f, 0xCCFFFFFF);
        bgSelectorHoverR.drawText(label, 10, 6, tf, 10.5f, color);
        c.restore();
        bgSelectorHoverR.upload();
    }

    private static void renderBgOptionHover(int guiScale, int rowIndex) {
        if (bgOptionHoverR == null || rowIndex < 0 || rowIndex >= bgOptions.size()) return;
        bgOptionHoverR.clear(0x00000000);
        var c = bgOptionHoverR.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        Typeface tf = font();
        String name = bgOptions.get(rowIndex).displayName;
        if (name.length() > 20) name = name.substring(0, 18) + "..";
        bgOptionHoverR.drawRoundedRect(4, 2, BG_SELECTOR_W - 8, BG_OPTION_H - 4, 7f, 0x22FFFFFF);
        bgOptionHoverR.drawText(name, 10, 6, tf, 10f, 0xB3FFFFFF);
        c.restore();
        bgOptionHoverR.upload();
    }

    private static boolean isBgOptionSelected(int index) {
        return index >= 0 && index < bgOptions.size() && bgOptions.get(index).id.equals(selectedBgId);
    }

    private static String bgSelectorLabel() {
        String label = "BG: " + selectedBgName();
        return label.length() > 20 ? label.substring(0, 18) + ".." : label;
    }

    private static void drawPlayerInfo(Minecraft mc, int screenW) {
        Typeface tf = font();
        String name = mc.getUser() == null ? "Player" : mc.getUser().getName();
        float textW = renderer.measureText(name, tf, 13f);
        float boxW = Math.max(116f, textW + 56f);
        float x = screenW - boxW - 18f;
        float y = 18f;
        renderer.drawRoundedRect(x, y, boxW, 34f, 14f, 0x40FFFFFF);
        renderer.drawRoundedRectStroke(x, y, boxW, 34f, 14f, 1.1f, 0x66FFFFFF);
        renderer.drawText(name, x + 44f, y + 10f, tf, 13f, textColor());
    }

    private static void drawBgSelector(int screenW, int screenH, int mx, int my) {
        ensureBgOptions();
        Typeface tf = font();
        int x = screenW - BG_SELECTOR_W - BG_SELECTOR_PAD;
        int y = screenH - BG_SELECTOR_H - BG_SELECTOR_PAD;
        String label = bgSelectorLabel();

        renderer.drawRoundedRect(x, y, BG_SELECTOR_W, BG_SELECTOR_H, 10f, bgSelectorOpen ? 0x55FFFFFF : 0x35FFFFFF);
        renderer.drawRoundedRectStroke(x, y, BG_SELECTOR_W, BG_SELECTOR_H, 10f, 1.1f, bgSelectorOpen ? 0xCCFFFFFF : 0x66FFFFFF);
        renderer.drawText(label, x + 10, y + 6, tf, 10.5f, textColor());

        if (!bgSelectorOpen) return;

        int optionCount = Math.min(8, bgOptions.size());
        int listH = optionCount * BG_OPTION_H;
        int listY = y - listH - 6;
        renderer.drawRoundedRect(x, listY, BG_SELECTOR_W, listH, 10f, 0x55FFFFFF);
        renderer.drawRoundedRectStroke(x, listY, BG_SELECTOR_W, listH, 10f, 1.1f, 0xAAFFFFFF);
        for (int i = 0; i < optionCount; i++) {
            BgOption option = bgOptions.get(i);
            int rowY = listY + i * BG_OPTION_H;
            boolean selected = option.id.equals(selectedBgId);
            if (selected) {
                renderer.drawRoundedRect(x + 4, rowY + 2, BG_SELECTOR_W - 8, BG_OPTION_H - 4, 7f, 0x40FFFFFF);
            }
            String name = option.displayName;
            if (name.length() > 20) name = name.substring(0, 18) + "..";
            renderer.drawText(name, x + 10, rowY + 6, tf, 10f, selected ? 0xFFFFFFFF : 0xB3FFFFFF);
        }
    }

    private static void drawPlayerHead(GuiGraphicsExtractor g, Minecraft mc, int screenW) {
        String name = mc.getUser() == null ? "Player" : mc.getUser().getName();
        float textW = renderer.measureText(name, font(), 13f);
        int boxW = Math.round(Math.max(116f, textW + 56f));
        int x = screenW - boxW - 18 + 8;
        int y = 23;
        ensureAvatarRenderer(mc);
        if (avatarR != null) {
            blitAvatar(g, x, y);
        }
    }

    private static void ensureAvatarRenderer(Minecraft mc) {
        int guiScale = Math.max(1, mc.getWindow().getGuiScale());
        String key = mc.getUser() == null ? "" : mc.getUser().getProfileId().toString();
        if (avatarR != null && key.equals(avatarProfileKey) && guiScale == lastAvatarGuiScale) return;
        if (avatarLoadFailed && key.equals(avatarProfileKey) && guiScale == lastAvatarGuiScale) return;

        closeAvatarRenderer();
        avatarProfileKey = key;
        lastAvatarGuiScale = guiScale;
        avatarLoadFailed = false;

        String skinUrl = findSkinUrl(mc);
        if (skinUrl == null || skinUrl.isEmpty()) {
            String name = mc.getUser() == null ? "Steve" : mc.getUser().getName();
            skinUrl = "https://minotar.net/skin/" + name;
        }

        try (InputStream is = URI.create(skinUrl).toURL().openStream()) {
            byte[] bytes = is.readAllBytes();
            try (var skinImage = io.github.humbleui.skija.Image.makeDeferredFromEncodedBytes(bytes)) {
                avatarR = new SkijaRenderer("main_menu_avatar", AVATAR_SIZE * guiScale, AVATAR_SIZE * guiScale);
                avatarR.clear(0x00000000);
                var c = avatarR.canvas();
                c.save();
                c.scale(guiScale, guiScale);

                try (var builder = new io.github.humbleui.skija.PathBuilder()) {
                    builder.addCircle(AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, AVATAR_SIZE / 2f - 0.3f);
                    try (var circle = builder.detach()) {
                        c.clipPath(circle, true);
                    }
                }

                SkinPixels skinPixels = readSkinPixels(skinImage);
                drawSkinRegion(c, skinPixels, 8, 8, 8, 0, 0, AVATAR_SIZE);
                drawSkinRegion(c, skinPixels, 40, 8, 8, 0, 0, AVATAR_SIZE);

                c.restore();
                var c2 = avatarR.canvas();
                c2.save();
                c2.scale(guiScale, guiScale);
                try (Paint stroke = new Paint()) {
                    stroke.setColor(0xCCFFFFFF);
                    stroke.setAntiAlias(true);
                    stroke.setMode(io.github.humbleui.skija.PaintMode.STROKE);
                    stroke.setStrokeWidth(1.2f);
                    c2.drawCircle(AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, AVATAR_SIZE / 2f - 0.8f, stroke);
                }
                c2.restore();
                avatarR.upload();
            }
        } catch (Exception e) {
            avatarLoadFailed = true;
            createFallbackAvatar(guiScale);
            SkijaTestClient.LOGGER.warn("[SkijaTest] Failed to load circular avatar skin", e);
        }
    }

    private record SkinPixels(int width, int height, byte[] bgra) {}

    private static SkinPixels readSkinPixels(Image image) {
        ImageInfo info = image.getImageInfo();
        int w = info.getWidth();
        int h = info.getHeight();
        ImageInfo readInfo = ImageInfo.makeS32(w, h, ColorAlphaType.UNPREMUL);
        try (Bitmap bitmap = new Bitmap()) {
            bitmap.allocPixels(readInfo);
            image.readPixels(bitmap, 0, 0);
            byte[] bytes = bitmap.readPixels(readInfo, w * 4, 0, 0);
            if (bytes == null) throw new IllegalStateException("Failed to read skin pixels");
            return new SkinPixels(w, h, bytes);
        }
    }

    private static void drawSkinRegion(io.github.humbleui.skija.Canvas c, SkinPixels pixels,
                                       int srcX, int srcY, int srcSize,
                                       float dstX, float dstY, float dstSize) {
        float cell = dstSize / srcSize;
        try (Paint paint = new Paint()) {
            paint.setAntiAlias(false);
            for (int y = 0; y < srcSize; y++) {
                int py = srcY + y;
                if (py < 0 || py >= pixels.height()) continue;
                for (int x = 0; x < srcSize; x++) {
                    int px = srcX + x;
                    if (px < 0 || px >= pixels.width()) continue;
                    int argb = skinPixelArgb(pixels, px, py);
                    if (((argb >>> 24) & 0xFF) == 0) continue;
                    paint.setColor(argb);
                    c.drawRect(io.github.humbleui.types.Rect.makeXYWH(
                            dstX + x * cell,
                            dstY + y * cell,
                            cell + 0.01f,
                            cell + 0.01f), paint);
                }
            }
        }
    }

    private static int skinPixelArgb(SkinPixels pixels, int x, int y) {
        int offset = (y * pixels.width() + x) * 4;
        byte[] data = pixels.bgra();
        int b = data[offset] & 0xFF;
        int g = data[offset + 1] & 0xFF;
        int r = data[offset + 2] & 0xFF;
        int a = data[offset + 3] & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void createFallbackAvatar(int guiScale) {
        try {
            avatarR = new SkijaRenderer("main_menu_avatar", AVATAR_SIZE * guiScale, AVATAR_SIZE * guiScale);
            avatarR.clear(0x00000000);
            var c = avatarR.canvas();
            c.save();
            c.scale(guiScale, guiScale);
            avatarR.drawCircle(AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, AVATAR_SIZE / 2f - 0.5f, 0x55666666);
            try (Paint stroke = new Paint()) {
                stroke.setColor(0xCCFFFFFF);
                stroke.setAntiAlias(true);
                stroke.setMode(io.github.humbleui.skija.PaintMode.STROKE);
                stroke.setStrokeWidth(1.2f);
                c.drawCircle(AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, AVATAR_SIZE / 2f - 0.8f, stroke);
            }
            c.restore();
            avatarR.upload();
        } catch (Exception ignored) {
            closeAvatarRenderer();
        }
    }

    private static String findSkinUrl(Minecraft mc) {
        try {
            var profile = mc.getGameProfile();
            var props = profile.properties().get("textures");
            if (props == null || props.isEmpty()) return null;
            var prop = props.iterator().next();
            String json = new String(Base64.getDecoder().decode(prop.value()), java.nio.charset.StandardCharsets.UTF_8);
            var root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("textures")) return null;
            var textures = root.getAsJsonObject("textures");
            if (!textures.has("SKIN")) return null;
            var skin = textures.getAsJsonObject("SKIN");
            return skin.has("url") ? skin.get("url").getAsString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void blitAvatar(GuiGraphicsExtractor g, int x, int y) {
        float inv = 1f / lastAvatarGuiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, avatarR.textureId(), 0, 0, 0f, 0f, avatarR.getWidth(), avatarR.getHeight(), avatarR.getWidth(), avatarR.getHeight());
        pose.popMatrix();
    }

    private static void closeAvatarRenderer() {
        if (avatarR != null) {
            avatarR.close();
            avatarR = null;
        }
    }
    private static boolean ensureBackgroundTexture() {
        ensureBgOptions();
        BgOption option = selectedBgOption();
        if (option == null && !bgOptions.isEmpty()) option = bgOptions.getFirst();
        if (option == null) return false;
        if (backgroundTexture != null && option.id.equals(loadedBgId)) return true;

        closeBackground();
        loadedBgId = option.id;
        try {
            NativeImage image = readBackgroundNativeImage(option);
            if (image == null) return false;
            backgroundW = image.getWidth();
            backgroundH = image.getHeight();
            backgroundTexture = new DynamicTexture(() -> "skija-main-menu-bg", image);
            backgroundTextureId = Identifier.parse("skija-test:main_menu_bg");
            Minecraft.getInstance().getTextureManager().register(backgroundTextureId, backgroundTexture);
            return true;
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[SkijaTest] Failed to load main menu background: {}", option.displayName, e);
            return false;
        }
    }

    private static NativeImage readBackgroundNativeImage(BgOption option) throws Exception {
        byte[] bytes;
        if (option.resourcePath != null) {
            try (InputStream is = MainMenuRenderer.class.getResourceAsStream(option.resourcePath)) {
                if (is == null) return null;
                bytes = is.readAllBytes();
            }
        } else if (option.filePath != null) {
            bytes = Files.readAllBytes(option.filePath);
        } else {
            return null;
        }

        try {
            return NativeImage.read(bytes);
        } catch (Exception ignored) {
            return decodeBackgroundWithSkija(bytes);
        }
    }

    private static NativeImage decodeBackgroundWithSkija(byte[] bytes) throws Exception {
        try (Image image = Image.makeDeferredFromEncodedBytes(bytes)) {
            if (image == null) return null;
            ImageInfo info = image.getImageInfo();
            int w = info.getWidth();
            int h = info.getHeight();
            ImageInfo readInfo = ImageInfo.makeN32(w, h, ColorAlphaType.UNPREMUL);
            try (Bitmap bitmap = new Bitmap()) {
                bitmap.allocPixels(readInfo);
                image.readPixels(bitmap, 0, 0);
                byte[] pixelBytes = bitmap.readPixels(readInfo, w * 4, 0, 0);
                if (pixelBytes == null) return null;
                NativeImage nativeImage = new NativeImage(w, h, true);
                int offset = 0;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int b = pixelBytes[offset] & 0xFF;
                        int g = pixelBytes[offset + 1] & 0xFF;
                        int r = pixelBytes[offset + 2] & 0xFF;
                        int a = pixelBytes[offset + 3] & 0xFF;
                        nativeImage.setPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                        offset += 4;
                    }
                }
                return nativeImage;
            }
        }
    }

    private static void selectBackground(String id) {
        selectedBgId = id;
        closeBackground();
        uiDirty = true;
        com.lazychara.skijatest.config.ConfigManager.save();
    }

    private static String selectedBgName() {
        BgOption option = selectedBgOption();
        return option == null ? "None" : option.displayName;
    }

    private static BgOption selectedBgOption() {
        ensureBgOptions();
        for (BgOption option : bgOptions) {
            if (option.id.equals(selectedBgId)) return option;
        }
        return null;
    }

    private static void ensureBgOptions() {
        long now = System.currentTimeMillis();
        if (!bgOptions.isEmpty() && now - lastBgScan < 3000L) return;
        lastBgScan = now;

        List<BgOption> options = new ArrayList<>();
        scanBundledBackgrounds(options);
        scanCustomBackgrounds(options);
        bgOptions = options;

        if (selectedBgOptionNoScan() == null && !bgOptions.isEmpty()) {
            selectedBgId = bgOptions.getFirst().id;
            uiDirty = true;
        }
    }

    private static BgOption selectedBgOptionNoScan() {
        for (BgOption option : bgOptions) {
            if (option.id.equals(selectedBgId)) return option;
        }
        return null;
    }

    private static void scanBundledBackgrounds(List<BgOption> options) {
        String basePath = "/skija-test/bg/";
        try {
            var url = MainMenuRenderer.class.getResource(basePath);
            if (url == null) return;

            Path dirPath;
            java.nio.file.FileSystem jarFs = null;
            if ("file".equals(url.getProtocol())) {
                dirPath = Path.of(url.toURI());
            } else if ("jar".equals(url.getProtocol())) {
                var jarUri = new java.net.URI(url.toString().split("!")[0] + "!/");
                try {
                    jarFs = java.nio.file.FileSystems.getFileSystem(jarUri);
                } catch (Exception e) {
                    jarFs = java.nio.file.FileSystems.newFileSystem(jarUri, java.util.Map.of());
                }
                dirPath = jarFs.getPath(basePath);
            } else {
                return;
            }

            try (var stream = Files.list(dirPath)) {
                stream.filter(p -> isImageName(p.getFileName().toString()))
                        .sorted()
                        .forEach(p -> {
                            String fileName = p.getFileName().toString();
                            options.add(new BgOption("builtin:" + fileName, fileName, basePath + fileName, null));
                        });
            }
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[SkijaTest] Failed to scan bundled main menu backgrounds", e);
        }
    }

    private static void scanCustomBackgrounds(List<BgOption> options) {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("skija-test/bg");
            Files.createDirectories(dir);
            try (var stream = Files.list(dir)) {
                stream.filter(p -> Files.isRegularFile(p) && isImageName(p.getFileName().toString()))
                        .sorted()
                        .forEach(p -> {
                            String fileName = p.getFileName().toString();
                            options.add(new BgOption("custom:" + fileName, fileName, null, p));
                        });
            }
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[SkijaTest] Failed to scan custom main menu backgrounds", e);
        }
    }

    private static boolean isImageName(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp");
    }

    private static Typeface font() {
        SkijaTestScreen.ensureFontLoaded();
        if (SkijaTestScreen.curTf != null) return SkijaTestScreen.curTf;
        if (cachedTf == null) {
            var bundled = SkijaRenderer.loadAllBundledFonts();
            if (!bundled.isEmpty()) cachedTf = bundled.values().iterator().next();
            if (cachedTf == null && renderer != null) cachedTf = renderer.getDefaultTypeface();
        }
        return cachedTf;
    }

    private static int textColor() {
        return (255 << 24) | (Math.max(0, Math.min(255, SkijaTestScreen.cR)) << 16) | (Math.max(0, Math.min(255, SkijaTestScreen.cG)) << 8) | Math.max(0, Math.min(255, SkijaTestScreen.cB));
    }

    private static int menuStartY(int screenH) {
        int totalH = MENU_KEYS.length * BUTTON_H + (MENU_KEYS.length - 1) * BUTTON_GAP;
        return Math.max(42, (screenH - totalH) / 2);
    }

    private static void blitFull(GuiGraphicsExtractor g) {
        float inv = 1f / lastGuiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, renderer.textureId(), 0, 0, 0f, 0f, renderer.getWidth(), renderer.getHeight(), renderer.getWidth(), renderer.getHeight());
        pose.popMatrix();
    }

    private static void blitMenuHover(GuiGraphicsExtractor g, int x, int y) {
        if (menuHoverR == null) return;
        float inv = 1f / lastGuiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, menuHoverR.textureId(), 0, 0, 0f, 0f, menuHoverR.getWidth(), menuHoverR.getHeight(), menuHoverR.getWidth(), menuHoverR.getHeight());
        pose.popMatrix();
    }

    private static void blitBgSelectorHover(GuiGraphicsExtractor g, int screenW, int screenH) {
        if (bgSelectorHoverR == null) return;
        int x = screenW - BG_SELECTOR_W - BG_SELECTOR_PAD;
        int y = screenH - BG_SELECTOR_H - BG_SELECTOR_PAD;
        float inv = 1f / lastGuiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, bgSelectorHoverR.textureId(), 0, 0, 0f, 0f, bgSelectorHoverR.getWidth(), bgSelectorHoverR.getHeight(), bgSelectorHoverR.getWidth(), bgSelectorHoverR.getHeight());
        pose.popMatrix();
    }

    private static void blitBgOptionHover(GuiGraphicsExtractor g, int screenW, int screenH, int rowIndex) {
        if (bgOptionHoverR == null) return;
        int x = screenW - BG_SELECTOR_W - BG_SELECTOR_PAD;
        int y = screenH - BG_SELECTOR_H - BG_SELECTOR_PAD;
        int optionCount = Math.min(8, bgOptions.size());
        int listY = y - optionCount * BG_OPTION_H - 6;
        float inv = 1f / lastGuiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, listY + rowIndex * BG_OPTION_H);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, bgOptionHoverR.textureId(), 0, 0, 0f, 0f, bgOptionHoverR.getWidth(), bgOptionHoverR.getHeight(), bgOptionHoverR.getWidth(), bgOptionHoverR.getHeight());
        pose.popMatrix();
    }

    private static void closeRenderer() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
        if (menuHoverR != null) {
            menuHoverR.close();
            menuHoverR = null;
        }
        if (bgSelectorHoverR != null) {
            bgSelectorHoverR.close();
            bgSelectorHoverR = null;
        }
        if (bgOptionHoverR != null) {
            bgOptionHoverR.close();
            bgOptionHoverR = null;
        }
        lastHoverIndex = -2;
        lastSelectorHover = false;
        lastSelectorRowHover = -2;
        lastSelectorHoverLabel = "";
        lastSelectorRowHoverName = "";
        closeAvatarRenderer();
    }

    private static void closeBackground() {
        if (backgroundTexture != null) {
            backgroundTexture.close();
            backgroundTexture = null;
        }
        backgroundTextureId = null;
        backgroundW = 1;
        backgroundH = 1;
        loadedBgId = "";
    }

    private record BgOption(String id, String displayName, String resourcePath, Path filePath) {}
}

package com.lazychara.skijatest.client;
import io.github.humbleui.skija.Typeface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
public class SkijaTestScreen extends Screen {
    private static final int PANEL_W = 220, PANEL_H = 240;
    private static final float CORNER_R = 14f, PAD = 14f, SLIDER_H = 14f, THUMB_R = 6f;
    private static final int DD_W = 160, DD_COLLAPSED_H = 24, DD_ROW_H = 18, DD_MAX_ROWS = 8;
    private static final int DD_GAP = 10;
    private static final int DD_MAX_H = DD_COLLAPSED_H + DD_MAX_ROWS * DD_ROW_H + 6;
    private static final long OPEN_TRANSITION_MS = 320L;
    private static final long CLOSE_TRANSITION_MS = 260L;
    private static final long MUSIC_TRANSITION_MS = 360L;
    private static final int MODULE_W = 140, MODULE_H = 240;
    private static final int BG = 0x40FFFFFF, BORDER = 0x66FFFFFF;
    private static final int W100 = 0xFFFFFFFF, W70 = 0xB3FFFFFF, W50 = 0x80FFFFFF;
    private static final int W30 = 0x4DFFFFFF, W15 = 0x26FFFFFF, W08 = 0x14FFFFFF, SEP = 0x1AFFFFFF;
    private static SkijaRenderer panelR, ddR, moduleR, expandedModuleR;
    private static int cachedRendererGuiScale = -1;
    private static String[] fonts;
    private static java.util.LinkedHashMap<String, Typeface> bundled;
    private static int selIdx = 0;
    public static String selectedFontName = "";
    public static Typeface curTf;
    private int guiScale = 1;
    public static int cR = 255, cG = 255, cB = 255;
    private int dragSlider = -1;
    private boolean wasLeftDown = false, wasRightDown = false;
    private int panelX, panelY, ddX, ddY, moduleX, moduleY;
    private float settingScroll = 0f;
    private boolean dropdownOpen = false;
    private float ddAnim = 0f;
    private int ddScroll = 0;
    private long lastTime = 0;
    private long openTransitionStart = 0L;
    private boolean closeTransitioning = false;
    private long closeTransitionStart = 0L;
    private boolean musicTransitioning = false;
    private long musicTransitionStart = 0L;
    private boolean panelDirty = true;
    private boolean ddDirty = true;
    private boolean moduleDirty = true;
    private boolean expandedModuleDirty = true;
    private String lastExpandedModuleName = "";
    private boolean lastHoverHeader = false;
    private int lastHoverRow = -1;
    public SkijaTestScreen() {
        super(Component.literal("Skija Test"));
    }
    @Override
    protected void init() {
        super.init();
        guiScale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        if (panelR == null || ddR == null || moduleR == null || expandedModuleR == null || cachedRendererGuiScale != guiScale) {
            closeCachedRenderers();
            panelR = new SkijaRenderer("skija_panel", (PANEL_W + 4) * guiScale, (PANEL_H + 4) * guiScale);
            ddR = new SkijaRenderer("skija_dd", (DD_W + 4) * guiScale, (DD_MAX_H + 4) * guiScale);
            moduleR = new SkijaRenderer("skija_module", (MODULE_W + 4) * guiScale, (MODULE_H + 4) * guiScale);
            expandedModuleR = new SkijaRenderer("skija_expanded_module", (MODULE_W - 8) * guiScale, (MODULE_H - 8) * guiScale);
            cachedRendererGuiScale = guiScale;
        }
        if (fonts == null) {
            ensureFontLoaded();
        }
        int totalW = MODULE_W + DD_GAP + PANEL_W + DD_GAP + DD_W;
        moduleX = (this.width - totalW) / 2;
        moduleY = (this.height - PANEL_H) / 2;
        panelX = moduleX + MODULE_W + DD_GAP;
        panelY = moduleY;
        ddX = panelX + PANEL_W + DD_GAP;
        ddY = panelY;
        panelDirty = true;
        ddDirty = true;
        moduleDirty = true;
        expandedModuleDirty = true;
        lastExpandedModuleName = "";
        openTransitionStart = System.currentTimeMillis();
        closeTransitioning = false;
        closeTransitionStart = 0L;
        musicTransitioning = false;
        musicTransitionStart = 0L;
    }
    public static void ensureFontLoaded() {
        if (fonts != null) return;
        bundled = SkijaRenderer.loadAllBundledFonts();
        fonts = bundled.keySet().toArray(new String[0]);
        if (!selectedFontName.isEmpty()) {
            for (int i = 0; i < fonts.length; i++) {
                if (fonts[i].equals(selectedFontName)) {
                    selIdx = i;
                    break;
                }
            }
        }
        curTf = fonts.length == 0 ? null : bundled.get(fonts[selIdx]);
        if (selectedFontName.isEmpty() && fonts.length > 0) selectedFontName = fonts[selIdx];
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        g.fill(0, 0, this.width, this.height, 0x66000000);
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (panelR == null)
            return;
        try {
            long now = System.currentTimeMillis();
            if (lastTime == 0)
                lastTime = now;
            float dt = (now - lastTime) / 1000f;
            lastTime = now;
            if (musicTransitioning && now - musicTransitionStart >= MUSIC_TRANSITION_MS) {
                Minecraft.getInstance().gui.setScreen(new Musicpage());
                return;
            }
            if (closeTransitioning && now - closeTransitionStart >= CLOSE_TRANSITION_MS) {
                Minecraft.getInstance().gui.setScreen(null);
                return;
            }
            float speed = 7.0f;
            if (dropdownOpen) {
                if (ddAnim < 1f) {
                    ddAnim = Math.min(1f, ddAnim + speed * dt);
                    ddDirty = true;
                }
            } else {
                if (ddAnim > 0f) {
                    ddAnim = Math.max(0f, ddAnim - speed * dt);
                    ddDirty = true;
                }
            }
            boolean animatingModule = false;
            for (com.lazychara.skijatest.module.Module mod : com.lazychara.skijatest.module.ModuleManager.modules) {
                if (mod.expanded && mod.expandAnim < 1f) {
                    mod.expandAnim = Math.min(1f, mod.expandAnim + 7f * dt);
                    animatingModule = true;
                } else if (!mod.expanded && mod.expandAnim > 0f) {
                    mod.expandAnim = Math.max(0f, mod.expandAnim - 7f * dt);
                    animatingModule = true;
                }
            }
            if (animatingModule) moduleDirty = true;
            if (!musicTransitioning && !closeTransitioning) handleMouse(mx, my);
            if (panelDirty) {
                renderPanel();
                panelDirty = false;
            }
            if (ddDirty) {
                renderDropdown();
                ddDirty = false;
            }
            if (moduleDirty) {
                renderModule();
                moduleDirty = false;
            }
            com.lazychara.skijatest.module.Module expandedModule = currentExpandedModule();
            if (expandedModule != null) {
                if (!expandedModule.name.equals(lastExpandedModuleName)) {
                    expandedModuleDirty = true;
                    lastExpandedModuleName = expandedModule.name;
                }
                if (expandedModuleDirty) {
                    renderExpandedModule(expandedModule);
                    expandedModuleDirty = false;
                }
            } else {
                lastExpandedModuleName = "";
            }
            float openT = openTransitionProgress(now);
            float closeT = closeTransitioning ? closeTransitionProgress(now) : 0f;
            float musicT = musicTransitioning ? musicTransitionProgress(now) : 0f;
            pushScreenTransitionPose(g, openT, closeT, musicT);
            blitRegion(g, panelR, panelX - 2, panelY - 2, PANEL_W + 4, PANEL_H + 4);
            blitRegion(g, moduleR, moduleX - 2, moduleY - 2, MODULE_W + 4, MODULE_H + 4);
            if (expandedModule != null) {
                blitExpandedModule(g, expandedModule);
            }
            float t = 1f - (1f - ddAnim) * (1f - ddAnim) * (1f - ddAnim);
            int visRows = Math.min(DD_MAX_ROWS, fonts.length);
            int fullH = DD_COLLAPSED_H + visRows * DD_ROW_H + 6;
            float animH = DD_COLLAPSED_H + (fullH - DD_COLLAPSED_H) * t;
            blitRegion(g, ddR, ddX - 2, ddY - 2, DD_W + 4, Math.round(animH) + 4);
            popScreenTransitionPose(g);
            drawScreenTransitionOverlay(g, openT, closeT, musicT);
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[SkijaTest] Render error!", e);
        }
        super.extractRenderState(g, mx, my, delta);
    }
    private float openTransitionProgress(long now) {
        return Math.max(0f, Math.min(1f, (now - openTransitionStart) / (float) OPEN_TRANSITION_MS));
    }
    private float closeTransitionProgress(long now) {
        return Math.max(0f, Math.min(1f, (now - closeTransitionStart) / (float) CLOSE_TRANSITION_MS));
    }
    private float musicTransitionProgress(long now) {
        return Math.max(0f, Math.min(1f, (now - musicTransitionStart) / (float) MUSIC_TRANSITION_MS));
    }
    private void pushScreenTransitionPose(GuiGraphicsExtractor g, float openT, float closeT, float musicT) {
        float openE = easeOutCubic(openT);
        float closeE = easeInOut(closeT);
        float musicE = easeOutCubic(musicT);
        float scale = (0.965f + 0.035f * openE) * (1f - 0.075f * musicE) * (1f - 0.055f * closeE);
        float y = 22f * (1f - openE) - 44f * musicE + 18f * closeE;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(width * 0.5f, height * 0.5f + y);
        pose.scale(scale, scale);
        pose.translate(-width * 0.5f, -height * 0.5f);
    }
    private void popScreenTransitionPose(GuiGraphicsExtractor g) {
        g.pose().popMatrix();
    }
    private void drawScreenTransitionOverlay(GuiGraphicsExtractor g, float openT, float closeT, float musicT) {
        float openA = 150f * (1f - easeInOut(openT));
        float closeA = 205f * easeInOut(closeT);
        float musicA = 230f * easeInOut(musicT);
        int a = Math.max(0, Math.min(240, Math.round(Math.max(openA, Math.max(closeA, musicA)))));
        if (a > 0) g.fill(0, 0, width, height, a << 24);
    }
    private float easeOutCubic(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float u = 1f - t;
        return 1f - u * u * u;
    }
    private float easeInOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2f) * 0.5f;
    }
    private void blitRegion(GuiGraphicsExtractor g, SkijaRenderer r, int dx, int dy, int guiW, int guiH) {
        int regionW = guiW * guiScale;
        int regionH = guiH * guiScale;
        int fullTexW = r.getWidth();
        int fullTexH = r.getHeight();
        float inv = 1f / guiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(dx, dy);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, r.textureId(), 0, 0, 0f, 0f, regionW, regionH, fullTexW, fullTexH);
        pose.popMatrix();
    }
    private void renderPanel() {
        panelR.clear(0x00000000);
        var c = panelR.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        float ox = 2, oy = 2, iw = PANEL_W - PAD * 2, cx = ox + PANEL_W / 2f;
        float y = oy + 14;
        panelR.drawSquircle(ox, oy, PANEL_W, PANEL_H, CORNER_R, BG);
        panelR.drawSquircleStroke(ox, oy, PANEL_W, PANEL_H, CORNER_R, 1.5f, BORDER);
        if (curTf != null) {
            panelR.drawTextCentered("skija test", cx, y, curTf, 20f, textColor());
        }
        y += 28;
        panelR.drawRoundedRect(ox + PAD, y, iw, .5f, .25f, SEP);
        y += 8;
        panelR.drawText("Color", ox + PAD, y, curTf, 10f, W50);
        y += 14;
        float sx = ox + PAD, sw = iw - 32;
        drawSlider(panelR, sx, y, sw, cR, 0xFF4444FF, "R");
        panelR.drawText(String.valueOf(cR), sx + sw + 6, y + 2, curTf, 9f, 0xFF4444FF);
        y += 20;
        drawSlider(panelR, sx, y, sw, cG, 0xFF44FF44, "G");
        panelR.drawText(String.valueOf(cG), sx + sw + 6, y + 2, curTf, 9f, 0xFF44FF44);
        y += 20;
        drawSlider(panelR, sx, y, sw, cB, 0xFFFF4444, "B");
        panelR.drawText(String.valueOf(cB), sx + sw + 6, y + 2, curTf, 9f, 0xFFFF4444);
        y += 20;
        panelR.drawRoundedRect(ox + PAD, y, iw, .5f, .25f, SEP);
        y += 8;
        panelR.drawText("Preview", ox + PAD, y, curTf, 10f, W50);
        y += 14;
        panelR.drawRoundedRect(ox + PAD, y, 20, 20, 4f, textColor());
        panelR.drawRoundedRectStroke(ox + PAD, y, 20, 20, 4f, .5f, W30);
        panelR.drawText(String.format("#%02X%02X%02X", cR, cG, cB), ox + PAD + 26, y + 4, curTf, 11f, W70);
        y += 26;
        if (curTf != null) {
            panelR.drawTextCentered("Ciallo Skija,Vulkan", cx, y, curTf, 14f, textColor());
        }
        panelR.drawTextCentered("RShift to close", cx, oy + PANEL_H - 12, curTf, 9f, W30);
        c.restore();
        panelR.upload();
    }
    private void renderDropdown() {
        ddR.clear(0x00000000);
        var c = ddR.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        float ox = 2, oy = 2;
        float t = 1f - (1f - ddAnim) * (1f - ddAnim) * (1f - ddAnim);
        int visRows = Math.min(DD_MAX_ROWS, fonts.length);
        int fullH = DD_COLLAPSED_H + visRows * DD_ROW_H + 6;
        float animH = DD_COLLAPSED_H + (fullH - DD_COLLAPSED_H) * t;
        ddR.drawSquircle(ox, oy, DD_W, animH, 10f, BG);
        ddR.drawSquircleStroke(ox, oy, DD_W, animH, 10f, 1f, BORDER);
        ddR.drawText("Font", ox + 8, oy + 5, curTf, 9f, W50);
        String sel = fonts.length > 0 ? fonts[selIdx] : "None";
        if (sel.length() > 14)
            sel = sel.substring(0, 12) + "..";
        if (lastHoverHeader)
            ddR.drawRoundedRect(ox + 32, oy + 2, DD_W - 40, 18, 4f, W08);
        ddR.drawText(sel, ox + 36, oy + 5, curTf, 10f, W100);
        if (ddAnim > 0.01f) {
            float listY = oy + DD_COLLAPSED_H;
            ddR.drawRoundedRect(ox + 4, listY - 1, DD_W - 8, .5f, .25f, SEP);
            float clipBottom = Math.max(listY, oy + animH - 4);
            c.save();
            c.clipRect(io.github.humbleui.types.Rect.makeLTRB(ox, listY, ox + DD_W, clipBottom));
            for (int i = 0; i < visRows && (ddScroll + i) < fonts.length; i++) {
                int fi = ddScroll + i;
                float ry = listY + i * DD_ROW_H;
                boolean isSel = fi == selIdx;
                boolean isHov = i == lastHoverRow;
                if (isSel)
                    ddR.drawRoundedRect(ox + 4, ry + 1, DD_W - 8, DD_ROW_H - 2, 4f, W15);
                else if (isHov)
                    ddR.drawRoundedRect(ox + 4, ry + 1, DD_W - 8, DD_ROW_H - 2, 4f, W08);
                String fn = fonts[fi];
                if (fn.length() > 20)
                    fn = fn.substring(0, 18) + "..";
                Typeface tf = bundled.get(fonts[fi]);
                ddR.drawText(fn, ox + 10, ry + 3, tf, 9f, isSel ? W100 : W70);
            }
            if (fonts.length > DD_MAX_ROWS) {
                float listH = visRows * DD_ROW_H;
                float bH = Math.max(8, listH * DD_MAX_ROWS / fonts.length);
                int maxS = fonts.length - DD_MAX_ROWS;
                float bY = listY + (listH - bH) * ddScroll / maxS;
                ddR.drawRoundedRect(ox + DD_W - 6, bY, 2, bH, 1f, W30);
            }
            c.restore();
        }
        c.restore();
        ddR.upload();
    }
    private void renderModule() {
        moduleR.clear(0x00000000);
        var c = moduleR.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        float ox = 2, oy = 2, cx = ox + MODULE_W / 2f;
        moduleR.drawSquircle(ox, oy, MODULE_W, MODULE_H, CORNER_R, BG);
        moduleR.drawSquircleStroke(ox, oy, MODULE_W, MODULE_H, CORNER_R, 1.5f, BORDER);
        float itemY = oy + 12;
        if (curTf != null) {
            for (com.lazychara.skijatest.module.Category cat : com.lazychara.skijatest.module.Category.values()) {
                java.util.List<com.lazychara.skijatest.module.Module> mods = com.lazychara.skijatest.module.ModuleManager
                        .getModulesByCategory(cat);
                if (mods.isEmpty()) continue;
                moduleR.drawTextCentered(cat.name, cx, itemY + 8, curTf, 13f, W70);
                itemY += 28;
                for (com.lazychara.skijatest.module.Module mod : mods) {
                    mod.originX = ox + PAD;
                    mod.originY = itemY;
                    int color = mod.enabled ? textColor() : W50;
                    moduleR.drawText(mod.name, ox + PAD, itemY + 10, curTf, 11f, color);
                    itemY += 16;
                }
                itemY += 8;
            }
        }
        c.restore();
        moduleR.upload();
    }
    private void renderExpandedModule(com.lazychara.skijatest.module.Module mod) {
        if (expandedModuleR == null) return;
        expandedModuleR.clear(0x00000000);
        var c = expandedModuleR.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        float w = MODULE_W - 8;
        float h = MODULE_H - 8;
        expandedModuleR.drawRoundedRect(0, 0, w, h, CORNER_R - 2f, 0xFF2A2A2A);
        expandedModuleR.drawRoundedRectStroke(0, 0, w, h, CORNER_R - 2f, 1.5f, 0xFF444444);
        int textCol = textColor();
        expandedModuleR.drawTextCentered(mod.name, w / 2f, 14, curTf, 13f, textCol);
        c.save();
        c.clipRect(io.github.humbleui.types.Rect.makeXYWH(0, 28, w, h - 32));
        float sy = 28 - settingScroll;
        String bindText = mod.isBinding ? "..." : (mod.keybind == -1 ? "NONE" : org.lwjgl.glfw.GLFW.glfwGetKeyName(mod.keybind, 0));
        if (bindText == null) bindText = "K" + mod.keybind;
        expandedModuleR.drawText("Bind: " + bindText, 14, sy + 14, curTf, 11f, mod.isBinding ? textCol : 0xAAFFFFFF);
        sy += 22;
        for (com.lazychara.skijatest.module.Setting s : mod.settings) {
            if (s instanceof com.lazychara.skijatest.module.BooleanSetting bs) {
                expandedModuleR.drawText(s.name, 14, sy + 14, curTf, 11f, bs.value ? textCol : 0xAAFFFFFF);
            }
            sy += 22;
        }
        c.restore();
        c.restore();
        expandedModuleR.upload();
    }
    private com.lazychara.skijatest.module.Module currentExpandedModule() {
        for (com.lazychara.skijatest.module.Module mod : com.lazychara.skijatest.module.ModuleManager.modules) {
            if (mod.expandAnim > 0.01f || mod.expanded) return mod;
        }
        return null;
    }
    private void blitExpandedModule(GuiGraphicsExtractor g, com.lazychara.skijatest.module.Module mod) {
        if (expandedModuleR == null) return;
        float t = 1f - (1f - mod.expandAnim) * (1f - mod.expandAnim) * (1f - mod.expandAnim);
        float targetW = MODULE_W - 8;
        float targetH = MODULE_H - 8;
        float targetX = moduleX + 4;
        float targetY = moduleY + 4;
        float startX = moduleX + mod.originX;
        float startY = moduleY + mod.originY;
        float startW = MODULE_W - 2 * PAD;
        float startH = 16f;
        float currX = startX + (targetX - startX) * t;
        float currY = startY + (targetY - startY) * t;
        float currW = startW + (targetW - startW) * t;
        float currH = startH + (targetH - startH) * t;
        int texW = expandedModuleR.getWidth();
        int texH = expandedModuleR.getHeight();
        float scaleX = currW / targetW;
        float scaleY = currH / targetH;
        float inv = 1f / guiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(currX, currY);
        pose.scale(scaleX * inv, scaleY * inv);
        g.blit(RenderPipelines.GUI_TEXTURED, expandedModuleR.textureId(), 0, 0, 0f, 0f, texW, texH, texW, texH);
        pose.popMatrix();
    }
    private void drawSlider(SkijaRenderer r, float x, float y, float w, int val, int col, String lbl) {
        r.drawText(lbl, x, y + 1, curTf, 11f, col);
        float tx = x + 16, tw = w - 16;
        r.drawRoundedRect(tx, y + 3, tw, SLIDER_H - 6, 4f, W08);
        float fw = (val / 255f) * tw;
        if (fw > 1)
            r.drawRoundedRect(tx, y + 3, fw, SLIDER_H - 6, 4f, (col & 0x00FFFFFF) | 0x66000000);
        r.drawCircle(tx + fw, y + SLIDER_H / 2f, THUMB_R, W100);
        r.drawCircle(tx + fw, y + SLIDER_H / 2f, THUMB_R - 1.5f, col);
    }
    private void handleMouse(int mx, int my) {
        long w = Minecraft.getInstance().getWindow().handle();
        boolean leftDown = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean leftClick = leftDown && !wasLeftDown;
        boolean rightClick = rightDown && !wasRightDown;
        boolean down = leftDown || rightDown;
        float lx = mx - panelX - 2, ly = my - panelY - 2;
        boolean hHeader = isHoverHeader(mx, my);
        int hRow = -1;
        if (dropdownOpen && isInsideDD(mx, my)) {
            for (int i = 0; i < Math.min(DD_MAX_ROWS, fonts.length); i++) {
                if (isHoverRow(mx, my, i)) {
                    hRow = i;
                    break;
                }
            }
        }
        if (hHeader != lastHoverHeader || hRow != lastHoverRow) {
            lastHoverHeader = hHeader;
            lastHoverRow = hRow;
            ddDirty = true;
        }
        if ((leftClick || rightClick)) {
            if (hHeader && leftClick) {
                dropdownOpen = !dropdownOpen;
                ddDirty = true;
                if (dropdownOpen)
                    ensureVisible();
            } else if (dropdownOpen && leftClick) {
                boolean clicked = false;
                if (hRow != -1) {
                    int ni = ddScroll + hRow;
                    if (ni < fonts.length) {
                        selIdx = ni;
                        loadFont();
                        com.lazychara.skijatest.config.ConfigManager.save();
                    }
                    dropdownOpen = false;
                    ddDirty = true;
                    panelDirty = true;
                    moduleDirty = true;
                    expandedModuleDirty = true;
                    clicked = true;
                }
                if (!clicked && !isInsideDD(mx, my)) {
                    dropdownOpen = false;
                    ddDirty = true;
                }
            }
            boolean handledOverlay = false;
            for (com.lazychara.skijatest.module.Module mod : com.lazychara.skijatest.module.ModuleManager.modules) {
                if (mod.expandAnim > 0.1f) {
                    float targetW = MODULE_W - 8;
                    float targetH = MODULE_H - 8;
                    float targetX = moduleX + 4;
                    float targetY = moduleY + 4;
                    if (mx >= targetX && mx <= targetX + targetW && my >= targetY && my <= targetY + targetH) {
                        if (leftClick) {
                            float sy = targetY + 28 - settingScroll;
                            if (my >= sy && my <= sy + 20) {
                                mod.isBinding = !mod.isBinding;
                                expandedModuleDirty = true;
                                com.lazychara.skijatest.config.ConfigManager.save();
                            }
                            sy += 22;
                            for (com.lazychara.skijatest.module.Setting s : mod.settings) {
                                if (my >= sy && my <= sy + 20) {
                                    if (s instanceof com.lazychara.skijatest.module.BooleanSetting bs) {
                                        bs.toggle();
                                        expandedModuleDirty = true;
                                        com.lazychara.skijatest.config.ConfigManager.save();
                                    }
                                }
                                sy += 22;
                            }
                        }
                    } else if (leftClick || rightClick) {
                        mod.expanded = false;
                        mod.isBinding = false;
                        settingScroll = 0f;
                        expandedModuleDirty = true;
                    }
                    handledOverlay = true;
                    break;
                }
            }
            if (!handledOverlay) {
                float currentY = moduleY + 12;
                for (com.lazychara.skijatest.module.Category cat : com.lazychara.skijatest.module.Category.values()) {
                    java.util.List<com.lazychara.skijatest.module.Module> mods = com.lazychara.skijatest.module.ModuleManager
                            .getModulesByCategory(cat);
                    if (mods.isEmpty())
                        continue;
                    currentY += 28;
                    for (com.lazychara.skijatest.module.Module mod : mods) {
                        if (mx >= moduleX + PAD && mx <= moduleX + MODULE_W - PAD && my >= currentY
                                && my <= currentY + 16) {
                            if (rightClick) {
                                mod.expanded = true;
                                expandedModuleDirty = true;
                            } else if (leftClick) {
                                mod.toggle();
                                moduleDirty = true;
                            }
                            moduleDirty = true;
                            break;
                        }
                        currentY += 16;
                    }
                    currentY += 8;
                }
            }
            float sY = 52, sX = PAD + 16, sW = PANEL_W - PAD * 2 - 32 - 16;
            if (lx >= sX && lx <= sX + sW) {
                if (ly >= sY && ly <= sY + 14)
                    dragSlider = 0;
                else if (ly >= sY + 20 && ly <= sY + 34)
                    dragSlider = 1;
                else if (ly >= sY + 40 && ly <= sY + 54)
                    dragSlider = 2;
            }
        }
        if (!down)
            dragSlider = -1;
        if (leftDown && dragSlider >= 0) {
            float sX = PAD + 16, sW = PANEL_W - PAD * 2 - 32 - 16;
            int v = Math.max(0, Math.min(255, Math.round((lx - sX) / sW * 255)));
            int oldR = cR, oldG = cG, oldB = cB;
            switch (dragSlider) {
                case 0:
                    cR = v;
                    break;
                case 1:
                    cG = v;
                    break;
                case 2:
                    cB = v;
                    break;
            }
            if (cR != oldR || cG != oldG || cB != oldB) {
                panelDirty = true;
                moduleDirty = true;
                expandedModuleDirty = true;
            }
        }
        wasLeftDown = leftDown;
        wasRightDown = rightDown;
    }
    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (musicTransitioning || closeTransitioning) return true;
        for (com.lazychara.skijatest.module.Module mod : com.lazychara.skijatest.module.ModuleManager.modules) {
            if (mod.expandAnim > 0.5f) {
                float targetW = MODULE_W - 8;
                float targetH = MODULE_H - 8;
                float targetX = moduleX + 4;
                float targetY = moduleY + 4;
                if (mx >= targetX && mx <= targetX + targetW && my >= targetY && my <= targetY + targetH) {
                    float contentH = 28f + 22f + mod.settings.size() * 22f + 18f;
                    float maxScroll = Math.max(0, contentH - (targetH - 32f));
                    settingScroll -= sy * 20f;
                    settingScroll = Math.max(0, Math.min(maxScroll, settingScroll));
                    expandedModuleDirty = true;
                    return true;
                }
            }
        }
        if (dropdownOpen && isInsideDD((int) mx, (int) my)) {
            int max = Math.max(0, fonts.length - DD_MAX_ROWS);
            int oldS = ddScroll;
            ddScroll = Math.max(0, Math.min(max, ddScroll - (int) sy));
            if (oldS != ddScroll)
                ddDirty = true;
            return true;
        }
        if (isBlankAreaForMusicPage(mx, my) && sy < 0) {
            startMusicTransition();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }
    private void startMusicTransition() {
        if (musicTransitioning || closeTransitioning) return;
        musicTransitioning = true;
        musicTransitionStart = System.currentTimeMillis();
        dropdownOpen = false;
        ddDirty = true;
    }
    private void startCloseTransition() {
        if (closeTransitioning || musicTransitioning) return;
        closeTransitioning = true;
        closeTransitionStart = System.currentTimeMillis();
        dropdownOpen = false;
        ddDirty = true;
    }
    private boolean isBlankAreaForMusicPage(double mx, double my) {
        if (mx >= panelX && mx <= panelX + PANEL_W && my >= panelY && my <= panelY + PANEL_H) return false;
        if (mx >= moduleX && mx <= moduleX + MODULE_W && my >= moduleY && my <= moduleY + MODULE_H) return false;
        if (isInsideDD((int) mx, (int) my)) return false;
        for (com.lazychara.skijatest.module.Module mod : com.lazychara.skijatest.module.ModuleManager.modules) {
            if (mod.expandAnim > 0.1f) {
                float targetW = MODULE_W - 8;
                float targetH = MODULE_H - 8;
                float targetX = moduleX + 4;
                float targetY = moduleY + 4;
                if (mx >= targetX && mx <= targetX + targetW && my >= targetY && my <= targetY + targetH) return false;
            }
        }
        return true;
    }
    private void ensureVisible() {
        if (selIdx < ddScroll)
            ddScroll = selIdx;
        else if (selIdx >= ddScroll + DD_MAX_ROWS)
            ddScroll = selIdx - DD_MAX_ROWS + 1;
        ddDirty = true;
    }
    private void loadFont() {
        String n = fonts[selIdx];
        selectedFontName = n;
        curTf = bundled.get(n);
    }
    private int textColor() {
        return 0xFF000000 | (cR << 16) | (cG << 8) | cB;
    }
    private boolean isHoverHeader(int mx, int my) {
        return mx >= ddX && mx <= ddX + DD_W && my >= ddY && my <= ddY + DD_COLLAPSED_H;
    }
    private boolean isHoverRow(int mx, int my, int row) {
        float ry = ddY + DD_COLLAPSED_H + row * DD_ROW_H;
        return mx >= ddX && mx <= ddX + DD_W && my >= ry && my <= ry + DD_ROW_H;
    }
    private boolean isInsideDD(int mx, int my) {
        float t = 1f - (1f - ddAnim) * (1f - ddAnim) * (1f - ddAnim);
        int visRows = Math.min(DD_MAX_ROWS, fonts.length);
        float h = DD_COLLAPSED_H + (visRows * DD_ROW_H + 4) * t;
        return mx >= ddX && mx <= ddX + DD_W && my >= ddY && my <= ddY + h;
    }
    @Override
    public void onClose() {
        startCloseTransition();
    }
    @Override
    public void removed() {
        super.removed();
        SkijaTestClient.runAfterClientTicks(20, com.lazychara.skijatest.config.ConfigManager::save);
    }
    private static void closeCachedRenderers() {
        if (panelR != null) {
            panelR.close();
            panelR = null;
        }
        if (ddR != null) {
            ddR.close();
            ddR = null;
        }
        if (moduleR != null) {
            moduleR.close();
            moduleR = null;
        }
        if (expandedModuleR != null) {
            expandedModuleR.close();
            expandedModuleR = null;
        }
        cachedRendererGuiScale = -1;
    }
    @Override
    public boolean keyPressed(KeyEvent e) {
        if (musicTransitioning || closeTransitioning) return true;
        if (e.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            startCloseTransition();
            return true;
        }
        for (com.lazychara.skijatest.module.Module mod : com.lazychara.skijatest.module.ModuleManager.modules) {
            if (mod.isBinding) {
                if (e.key() == GLFW.GLFW_KEY_ESCAPE || e.key() == GLFW.GLFW_KEY_BACKSPACE) {
                    mod.keybind = -1;
                } else {
                    mod.keybind = e.key();
                }
                mod.isBinding = false;
                expandedModuleDirty = true;
                com.lazychara.skijatest.config.ConfigManager.save();
                return true;
            }
        }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
            boolean closedSecondary = false;
            for (com.lazychara.skijatest.module.Module mod : com.lazychara.skijatest.module.ModuleManager.modules) {
                if (mod.expanded) {
                    mod.expanded = false;
                    mod.isBinding = false;
                    expandedModuleDirty = true;
                    closedSecondary = true;
                }
            }
            if (closedSecondary) {
                return true;
            }
        }
        return super.keyPressed(e);
    }
    @SuppressWarnings("unused")
    private int blendAlpha(int color, float alphaMult) {
        int a = Math.round(((color >> 24) & 0xFF) * alphaMult);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}

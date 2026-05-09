package com.lazychara.skijatest.client;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterBlurMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.MaskFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Musicpage extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int W88 = 0xE0FFFFFF;
    private static final int W70 = 0xB3FFFFFF;
    private static final int W46 = 0x75FFFFFF;
    private static final int W30 = 0x4DFFFFFF;
    private static final int W18 = 0x2EFFFFFF;
    private static final int W12 = 0x1FFFFFFF;
    private static final long ENTRY_ANIM_MS = 420L;
    private static final long RETURN_ANIM_MS = 340L;
    private static final long CLOSE_ANIM_MS = 260L;
    private static final float BG_RENDER_SCALE = 0.58f;
    private static final int BG_RENDER_MIN_W = 360;
    private static final int BG_RENDER_MIN_H = 202;
    private static final int BG_RENDER_MAX_W = 1280;
    private static final int BG_RENDER_MAX_H = 780;
    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]\\s*(.*)");

    private SkijaRenderer renderer;
    private SkijaRenderer bgRenderer;
    private SkijaRenderer staticRenderer;
    private SkijaRenderer controlsRenderer;
    private SkijaRenderer lyricsRenderer;
    private int guiScale = 1;
    private long openedAt;
    private long entryStartedAt;
    private long exitStartedAt;
    private long lastFrame;
    private long lastAnimatedRender;
    private long lastProgressRender;
    private long lastBgRender;
    private int lastActiveLyric = -1;
    private int lastRenderedSecond = -1;
    private boolean dirty = true;
    private float lyricScroll;
    private float lyricScrollVelocity;
    private boolean lyricSpringMoving;
    private int trackIndex;
    private Image coverImage;
    private final AMLLFluidBackground fluidBackground = new AMLLFluidBackground();
    private List<LyricLine> lyricLines = List.of();
    private final List<CachedLyricLine> lyricCache = new ArrayList<>();
    private float lyricCacheWidth = -1f;
    private float lyricCacheScale = -1f;
    private MusicLoader.MusicTrack cachedTrack;
    private MusicLoader.MusicTrack currentUiTrack;

    private boolean shuffleMode = false;
    private boolean playlistOpen = false;
    private float volume = 0.78f;
    private boolean draggingVolume = false;
    private boolean draggingProgress = false;
    private boolean wasLeftDown = false;
    private boolean returningToMain = false;
    private boolean closingPage = false;

    private float volX, volY, volW, volH;
    private float progX, progY, progW, progH;
    private float prevX, prevY, nextX, nextY, playX, playY, btnR;
    private float modeX, modeY, listX, listY, listW, listH;
    private float controlsLayerX, controlsLayerY, controlsLayerW, controlsLayerH;
    private float lyricsLayerX, lyricsLayerY, lyricsLayerW, lyricsLayerH;
    private float layoutLeftX, layoutLeftW, layoutControlY;
    private float layoutRightX, layoutLyricTop, layoutRightW, layoutLyricH;

    private int paletteA = 0xFF9D5064;
    private int paletteB = 0xFF923823;
    private int paletteC = 0xFF604064;
    private int paletteDark = 0xFF24151B;

    public Musicpage() {
        super(Component.literal("Music"));
    }

    @Override
    protected void init() {
        super.init();
        guiScale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        closeRenderer();
        int[] bgSize = computeBackgroundTextureSize();
        bgRenderer = new SkijaRenderer("music_page_bg_anim", bgSize[0], bgSize[1]);
        staticRenderer = new SkijaRenderer("music_page_static", Math.max(1, width * guiScale), Math.max(1, height * guiScale));
        controlsLayerX = 0f;
        controlsLayerY = 0f;
        controlsLayerW = Math.max(1f, width * 0.46f);
        controlsLayerH = Math.max(1f, height);
        lyricsLayerX = Math.max(0f, width * 0.46f);
        lyricsLayerY = 0f;
        lyricsLayerW = Math.max(1f, width - lyricsLayerX);
        lyricsLayerH = Math.max(1f, height);
        controlsRenderer = new SkijaRenderer("music_page_controls", Math.max(1, Math.round(controlsLayerW * guiScale)), Math.max(1, Math.round(controlsLayerH * guiScale)));
        lyricsRenderer = new SkijaRenderer("music_page_lyrics", Math.max(1, Math.round(lyricsLayerW * guiScale)), Math.max(1, Math.round(lyricsLayerH * guiScale)));
        renderer = staticRenderer;
        openedAt = System.currentTimeMillis();
        entryStartedAt = openedAt;
        exitStartedAt = 0L;
        returningToMain = false;
        closingPage = false;
        lastFrame = openedAt;
        lastAnimatedRender = 0;
        lastProgressRender = 0;
        lastBgRender = 0;
        lastActiveLyric = -1;
        lastRenderedSecond = -1;
        dirty = true;
        clampTrackIndex();
        refreshTrackCache();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (bgRenderer == null || staticRenderer == null || controlsRenderer == null || lyricsRenderer == null) return;
        try {
            long now = System.currentTimeMillis();
            if (returningToMain && now - exitStartedAt >= RETURN_ANIM_MS) {
                Minecraft.getInstance().gui.setScreen(new SkijaTestScreen());
                return;
            }
            if (closingPage && now - exitStartedAt >= CLOSE_ANIM_MS) {
                Minecraft.getInstance().gui.setScreen(null);
                return;
            }
            if (!isExiting()) handleMouse(mx, my);
            boolean controlsDirty = false;
            boolean lyricsDirty = false;
            if (currentUiTrack != null && MusicLoader.isPlaying(currentUiTrack)) {
                int currentSecond = (int) MusicLoader.getCurrentSeconds();
                int active = activeLyricIndex(MusicLoader.getCurrentSeconds());
                lyricsDirty = active != lastActiveLyric || lyricSpringMoving;
                controlsDirty = currentSecond != lastRenderedSecond || now - lastProgressRender >= 250L;
                if (lyricsDirty) lastActiveLyric = active;
                if (controlsDirty) {
                    lastRenderedSecond = currentSecond;
                    lastProgressRender = now;
                }
            }
            float dt = Math.min(0.05f, Math.max(0f, (now - lastFrame) / 1000f));
            if (dirty) {
                lastFrame = now;
                lastAnimatedRender = now;
                renderStaticPage(now);
                renderControlsLayer(now);
                renderLyricsLayer(now, dt);
                dirty = false;
            } else {
                if (controlsDirty) renderControlsLayer(now);
                if (lyricsDirty) {
                    lastFrame = now;
                    lastAnimatedRender = now;
                    renderLyricsLayer(now, dt);
                }
            }
            if (now - lastBgRender >= 50L) {
                renderBackgroundLayer((now - openedAt) / 1000f);
                lastBgRender = now;
            }
            blitLayers(g);
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[Musicpage] Render error", e);
        }
        super.extractRenderState(g, mx, my, delta);
    }

    private void renderBackgroundLayer(float time) {
        if (bgRenderer == null) return;
        renderer = bgRenderer;
        int bw = bgRenderer.getWidth();
        int bh = bgRenderer.getHeight();
        renderer.clear(paletteDark);
        Canvas c = renderer.canvas();
        float lowFreqPulse = currentUiTrack != null && MusicLoader.isPlaying(currentUiTrack)
                ? 0.035f + 0.018f * (0.5f + 0.5f * sin(time * 2.2f))
                : 0f;
        boolean drawn = fluidBackground.draw(c, bw, bh, time, lowFreqPulse);
        if (!drawn) {
            drawAnimatedFluidLayer(time);
        }
        renderer.drawRoundedRect(0, 0, bw, bh, 0, 0x26000000);
        renderer.upload();
    }

    private void drawCircleBlurred(Canvas c, float cx, float cy, float radius, int color, float sigma) {
        try (Paint paint = new Paint();
             MaskFilter blur = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma, false)) {
            paint.setColor(color);
            paint.setAntiAlias(true);
            paint.setMaskFilter(blur);
            c.drawCircle(cx, cy, radius, paint);
        }
    }

    private void renderStaticPage(long now) {
        renderer = staticRenderer;
        renderer.clear(0x00000000);
        Canvas c = renderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);

        SkijaTestScreen.ensureFontLoaded();
        Typeface tf = SkijaTestScreen.curTf;
        List<MusicLoader.MusicTrack> tracks = MusicLoader.getTracks();

        if (!tracks.isEmpty()) {
            clampTrackIndex();
            MusicLoader.MusicTrack track = tracks.get(trackIndex);
            if (track != cachedTrack) {
                cachedTrack = track;
                lyricLines = parseLyrics(track.lyrics(), track.title(), track.artist());
                clearLyricCache();
                lyricScroll = 0f;
                lyricScrollVelocity = 0f;
                lyricSpringMoving = false;
                openedAt = now;
                rebuildCover(track);
            }
        }

        if (tracks.isEmpty()) {
            renderEmpty(tf);
        } else {
            renderAMLLStatic(tracks.get(trackIndex), tf, now);
        }

        c.restore();
        renderer.upload();
    }

    private void renderControlsLayer(long now) {
        if (controlsRenderer == null || currentUiTrack == null) return;
        renderer = controlsRenderer;
        renderer.clear(0x00000000);
        Canvas c = renderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        c.translate(-controlsLayerX, -controlsLayerY);
        SkijaTestScreen.ensureFontLoaded();
        renderControls(layoutLeftX, layoutControlY, layoutLeftW, pageScale(), SkijaTestScreen.curTf);
        renderProgress(layoutLeftX, layoutLeftW, currentUiTrack, SkijaTestScreen.curTf, now, pageScale());
        c.restore();
        renderer.upload();
    }

    private void renderLyricsLayer(long now, float dt) {
        if (lyricsRenderer == null || currentUiTrack == null) return;
        renderer = lyricsRenderer;
        renderer.clear(0x00000000);
        Canvas c = renderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        c.translate(-lyricsLayerX, -lyricsLayerY);
        SkijaTestScreen.ensureFontLoaded();
        renderLyrics(layoutRightX, layoutLyricTop, layoutRightW, layoutLyricH, SkijaTestScreen.curTf, elapsedSeconds(currentUiTrack, now), dt, pageScale());
        c.restore();
        renderer.upload();
    }

    private void drawFluidBackground(float time) {
        Image bg = fluidBackground.image();
        if (bg != null) {
            float scale = 1.12f + 0.018f * sin(time * 0.55f);
            float bw = width * scale;
            float bh = height * scale;
            float ox = (width - bw) * 0.5f + sin(time * 0.19f) * width * 0.035f;
            float oy = (height - bh) * 0.5f + cos(time * 0.23f) * height * 0.040f;
            renderer.canvas().drawImageRect(bg, Rect.makeXYWH(ox, oy, bw, bh));
            drawAnimatedFluidLayer(time);
            renderer.drawRoundedRect(0, 0, width, height, 0, 0x22000000);
        } else {
            renderer.drawRoundedRect(0, 0, width, height, 0, paletteDark);
            drawAnimatedFluidLayer(time);
        }
    }

    private void drawAnimatedFluidLayer(float time) {
        float w = width;
        float h = height;
        float r1 = Math.max(w, h) * 0.38f;
        float r2 = Math.max(w, h) * 0.30f;
        float r3 = Math.max(w, h) * 0.24f;

        drawBlob(w * (0.22f + 0.055f * sin(time * 0.72f)), h * (0.28f + 0.075f * cos(time * 0.61f)), r1, withAlpha(paletteA, 0.18f));
        drawBlob(w * (0.72f + 0.070f * cos(time * 0.53f)), h * (0.35f + 0.060f * sin(time * 0.80f)), r2, withAlpha(paletteB, 0.15f));
        drawBlob(w * (0.54f + 0.090f * sin(time * 0.44f + 1.7f)), h * (0.76f + 0.055f * cos(time * 0.67f)), r3, withAlpha(paletteC, 0.16f));
    }

    private void drawBlob(float cx, float cy, float radius, int color) {
        renderer.drawCircle(cx, cy, radius, color);
        renderer.drawCircle(cx - radius * 0.18f, cy + radius * 0.10f, radius * 0.74f, withAlpha(color, alphaOf(color) * 0.56f));
        renderer.drawCircle(cx + radius * 0.26f, cy - radius * 0.16f, radius * 0.58f, withAlpha(color, alphaOf(color) * 0.42f));
    }

    private void renderEmpty(Typeface tf) {
        float s = pageScale();
        float cx = width / 2f;
        float cy = height / 2f;
        renderer.drawTextCentered("Music", cx, cy - 48 * s, tf, 38 * s, WHITE);
        renderer.drawTextCentered("没有发现本地音乐", cx, cy - 8 * s, tf, 17 * s, W88);
        renderer.drawTextCentered("把 .mp3 / .flac 放到 lazychara/music，然后按 R 重新扫描", cx, cy + 22 * s, tf, 12 * s, W70);
        renderer.drawTextCentered("向上滚动返回 · RShift 关闭", cx, height - 26 * s, tf, 10 * s, W46);
    }

    private void renderAMLLStatic(MusicLoader.MusicTrack track, Typeface tf, long now) {
        float s = pageScale();
        float leftX = Math.max(22f * s, width * 0.075f);
        float leftW = Math.min(width * 0.36f, 430f * s);
        float coverSize = Math.min(leftW, height * 0.35f);
        coverSize = Math.max(82f * s, coverSize);
        float coverY = Math.max(22f * s, height * 0.070f);
        float infoY = coverY + coverSize + 18f * s;
        float controlY = 0f;

        float rightX = Math.max(width * 0.51f, leftX + leftW + 54f * s);
        float rightW = Math.max(140f * s, width - rightX - 42f * s);
        float lyricTop = Math.max(20f * s, height * 0.075f);
        float lyricH = height - lyricTop - 28f * s;

        currentUiTrack = track;
        layoutLeftX = leftX;
        layoutLeftW = leftW;
        layoutRightX = rightX;
        layoutRightW = rightW;
        layoutLyricTop = lyricTop;
        layoutLyricH = lyricH;

        float coverX = leftX + (leftW - coverSize) * 0.5f;
        renderCover(coverX, coverY, coverSize, s);
        float titleSize = fitTextSize(track.title(), tf, 16f * s, 8.5f * s, leftW);
        renderer.drawText(track.title(), leftX, infoY, tf, titleSize, WHITE);
        renderer.drawText(trimText(track.artist(), tf, 12f * s, leftW - 38f * s), leftX, infoY + 22f * s, tf, 12f * s, W70);

        float barY = infoY + 42f * s;

        float badgeW = leftW * 0.38f;
        float badgeH = 22f * s;
        float badgeX = leftX + (leftW - badgeW) * 0.5f;
        float badgeY = infoY + 42f * s + 34f * s;
        controlY = badgeY + badgeH + 42f * s;
        float maxControlY = height - 86f * s;
        if (controlY > maxControlY) {
            float shift = controlY - maxControlY;
            badgeY = Math.max(barY + 20f * s, badgeY - shift);
            controlY = Math.min(maxControlY, badgeY + badgeH + 38f * s);
        }
        layoutControlY = controlY;
        String qualityLabel = track.qualityLabel();
        if (qualityLabel != null && !qualityLabel.isBlank()) {
            renderLosslessBadge(badgeX, badgeY, badgeW, badgeH, s, tf, qualityLabel);
        }
        renderPlaylistButton(width - 54f * s, height - 48f * s, 34f * s, s, tf);
        if (playlistOpen) renderPlaylistPanel(tf, s);
    }

    private void renderProgress(float leftX, float leftW, MusicLoader.MusicTrack track, Typeface tf, long now, float s) {
        float coverSize = Math.min(leftW, height * 0.35f);
        coverSize = Math.max(82f * s, coverSize);
        float coverY = Math.max(22f * s, height * 0.070f);
        float infoY = coverY + coverSize + 18f * s;
        float elapsed = elapsedSeconds(track, now);
        float duration = Math.max(1, track.duration());
        float progress = clamp(elapsed / duration, 0f, 1f);
        float barY = infoY + 42f * s;
        renderer.drawRoundedRect(leftX, barY, leftW, 5f * s, 2.5f * s, W30);
        renderer.drawRoundedRect(leftX, barY, Math.max(5f * s, leftW * progress), 5f * s, 2.5f * s, W88);
        renderer.drawCircle(leftX + leftW * progress, barY + 2.5f * s, 4.2f * s, WHITE);
        renderer.drawText(formatTime((int) elapsed), leftX, barY + 14f * s, tf, 8.5f * s, W70);
        String remain = "-" + formatTime(Math.max(0, track.duration() - (int) elapsed));
        float remainW = renderer.measureText(remain, tf, 8.5f * s);
        renderer.drawText(remain, leftX + leftW - remainW, barY + 14f * s, tf, 8.5f * s, W70);
        progX = leftX;
        progY = barY;
        progW = leftW;
        progH = 16f * s;
    }

    private void renderCover(float x, float y, float size, float s) {
        renderer.drawSquircle(x, y + 8f * s, size, size, 6f * s, 0x24000000);
        renderer.drawSquircle(x, y, size, size, 6f * s, W12);
        if (coverImage != null) {
            renderer.canvas().save();
            renderer.canvas().clipRRect(io.github.humbleui.types.RRect.makeXYWH(x, y, size, size, 6f * s));
            renderer.canvas().drawImageRect(coverImage, Rect.makeXYWH(x, y, size, size));
            renderer.canvas().restore();
        } else {
            renderer.drawCircle(x + size / 2f, y + size / 2f, size * 0.32f, W18);
            renderer.drawCircle(x + size / 2f, y + size / 2f, size * 0.12f, W30);
        }
    }

    private void renderLosslessBadge(float x, float y, float w, float h, float s, Typeface tf, String label) {
        renderer.drawRoundedRect(x, y, w, h, 6f * s, W18);
        renderer.drawTextCentered(label, x + w / 2f, y + 5f * s, tf, 8.5f * s, W70);
    }

    private void renderControls(float x, float y, float w, float s, Typeface tf) {
        btnR = 20f * s;
        modeX = x + w * 0.04f;
        modeY = y;
        prevX = x + w * 0.30f;
        prevY = y;
        playX = x + w * 0.50f;
        playY = y;
        nextX = x + w * 0.70f;
        nextY = y;

        drawModeButton(modeX, modeY, 17f * s, shuffleMode);
        drawPrevNext(prevX, prevY, 19f * s, false);
        drawPlayPause(playX, playY, 24f * s, !MusicLoader.isPlaying(currentUiTrack));
        drawPrevNext(nextX, nextY, 19f * s, true);

        volume = MusicLoader.getVolume();
        volX = x + 44f * s;
        volY = y + 46f * s;
        volW = w - 88f * s;
        volH = 7f * s;
        drawSpeaker(x + 9f * s, volY + 3.5f * s, 13f * s);
        renderer.drawRoundedRect(volX, volY, volW, volH, volH / 2f, W30);
        renderer.drawRoundedRect(volX, volY, volW * volume, volH, volH / 2f, W88);
        renderer.drawCircle(volX + volW * volume, volY + volH / 2f, 4.2f * s, WHITE);
        drawSoundWaves(x + w - 16f * s, volY + 3.5f * s, 10f * s);
    }

    private void renderPlaylistPanel(Typeface tf, float s) {
        List<MusicLoader.MusicTrack> tracks = MusicLoader.getTracks();
        float w = Math.min(300f * s, width * 0.30f);
        float h = Math.min(260f * s, height * 0.48f);
        float x = width - w - 26f * s;
        float y = height - h - 88f * s;
        renderer.drawRoundedRect(x, y, w, h, 14f * s, 0x3AFFFFFF);
        renderer.drawRoundedRectStroke(x, y, w, h, 14f * s, 1.0f * s, 0x55FFFFFF);
        renderer.drawText("播放列表", x + 14f * s, y + 10f * s, tf, 12f * s, WHITE);
        int maxRows = Math.max(1, (int) ((h - 42f * s) / (26f * s)));
        int rows = Math.min(maxRows, tracks.size());
        for (int i = 0; i < rows; i++) {
            MusicLoader.MusicTrack track = tracks.get(i);
            float rowY = y + 34f * s + i * 26f * s;
            if (i == trackIndex) renderer.drawRoundedRect(x + 8f * s, rowY - 2f * s, w - 16f * s, 22f * s, 7f * s, 0x2AFFFFFF);
            String title = trimText(track.title(), tf, 10.5f * s, w - 28f * s);
            renderer.drawText(title, x + 14f * s, rowY, tf, 10.5f * s, i == trackIndex ? WHITE : W70);
        }
    }

    private void renderPlaylistButton(float x, float y, float size, float s, Typeface tf) {
        listX = x;
        listY = y;
        listW = size;
        listH = size;
        renderer.drawRoundedRect(x, y, size, size, 8f * s, W18);
        float lx = x + 9f * s;
        float ly = y + 10f * s;
        for (int i = 0; i < 3; i++) {
            renderer.drawCircle(lx, ly + i * 7f * s, 1.4f * s, W88);
            renderer.drawRoundedRect(lx + 5f * s, ly + i * 7f * s - 1f * s, 13f * s, 2f * s, 1f * s, W88);
        }
    }

    private void renderLyrics(float x, float y, float w, float h, Typeface tf, float elapsed, float dt, float s) {
        int active = activeLyricIndex(elapsed);
        float activePreferred = Math.max(22f, 30f * s);
        float inactivePreferred = Math.max(17f, 25f * s);
        ensureLyricCache(tf, w, activePreferred, inactivePreferred, s);

        float gap = 14f * s;
        float cumY = 0f;
        float scrollTarget = 0f;
        int count = lyricLines.size();
        float[] offsets = new float[count];
        for (int i = 0; i < count; i++) {
            offsets[i] = cumY;
            if (i == active) scrollTarget = cumY;
            cumY += getLineContentHeight(i, active, s) + gap;
        }

        float anchorY = y + h * 0.35f;
        updateLyricSpring(scrollTarget, dt);
        lyricSpringMoving = Math.abs(scrollTarget - lyricScroll) > 0.35f || Math.abs(lyricScrollVelocity) > 0.35f;

        Canvas c = renderer.canvas();
        c.save();
        c.clipRect(Rect.makeXYWH(x - 18f * s, y, w + 36f * s, h));
        float baseY = anchorY - lyricScroll;
        for (int i = 0; i < count; i++) {
            float lineY = baseY + offsets[i];
            CachedLyricLine cached = i < lyricCache.size() ? lyricCache.get(i) : null;
            if (cached == null) continue;

             boolean isActive = i == active;
             float lineH = getLineContentHeight(i, active, s);

             if (lineY + lineH + 30f * s < y || lineY - 30f * s > y + h) continue;

            float textX = x;
            if (isActive) {
                renderer.canvas().drawImageRect(cached.activeImage, Rect.makeXYWH(textX, lineY, cached.activeW, cached.activeH));
            } else {
                renderer.canvas().drawImageRect(cached.inactiveImage, Rect.makeXYWH(textX - cached.inactivePad, lineY - cached.inactivePad, cached.inactiveW, cached.inactiveH));
            }
        }
        c.restore();
    }

    private float getLineContentHeight(int index, int activeIndex, float s) {
        CachedLyricLine cached = index >= 0 && index < lyricCache.size() ? lyricCache.get(index) : null;
        if (cached == null) return 30f * s;
        if (index == activeIndex) {
            return cached.activeH;
        } else {
            return Math.max(1f, cached.inactiveH - cached.inactivePad * 2f);
        }
    }

    private void ensureLyricCache(Typeface tf, float maxWidth, float activePreferred, float inactivePreferred, float s) {
        if (tf == null) return;
        if (lyricCache.size() == lyricLines.size() && Math.abs(lyricCacheWidth - maxWidth) < 0.5f && Math.abs(lyricCacheScale - s) < 0.001f) return;
        clearLyricCache();
        lyricCacheWidth = maxWidth;
        lyricCacheScale = s;
        for (LyricLine line : lyricLines) {
            String text = line.text();
            float activeSize = fitTextSize(text, tf, activePreferred, Math.max(16f, 20f * s), maxWidth);
            float inactiveSize = fitTextSize(text, tf, inactivePreferred, Math.max(12f, 15f * s), maxWidth);
            TextImage active = createLyricTextImage(text, tf, activeSize, WHITE, 0f, false, maxWidth);
            TextImage inactive = createLyricTextImage(text, tf, inactiveSize, 0x68FFFFFF, 6.0f * s, true, maxWidth);
            lyricCache.add(new CachedLyricLine(active, inactive));
        }
    }

    private TextImage createLyricTextImage(String text, Typeface tf, float size, int color, float blurSigma, boolean drawFaintCore, float maxWidth) {
        if (text == null) text = "";
        String[] lines = wrapText(text, tf, size, maxWidth);
        float scale = Math.max(1f, guiScale);
        try (Font font = new Font(tf, size * scale)) {
            float lineH = Math.max(1f, (font.getMetrics().getDescent() - font.getMetrics().getAscent()) / scale);
            float maxLineW = 1f;
            for (String line : lines) maxLineW = Math.max(maxLineW, renderer.measureText(line, tf, size));
            float pad = blurSigma > 0f ? Math.max(10f, blurSigma * 3.0f) : 0f;
            float logicalW = maxLineW + pad * 2f + 4f;
            float logicalH = lineH * lines.length + pad * 2f + 4f;
            int imgW = Math.max(1, Math.round(logicalW * scale));
            int imgH = Math.max(1, Math.round(logicalH * scale));
            Surface surface = Surface.makeRasterN32Premul(imgW, imgH);
            Canvas sc = surface.getCanvas();
            sc.clear(0x00000000);
            sc.save();
            sc.scale(scale, scale);
            try (Font logicalFont = new Font(tf, size); Paint paint = new Paint()) {
                paint.setColor(color);
                paint.setAntiAlias(true);
                if (blurSigma > 0f) {
                    try (MaskFilter blur = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurSigma, false)) {
                        paint.setMaskFilter(blur);
                        drawTextLines(sc, lines, pad + 2f, pad + 2f, logicalFont, paint, lineH);
                    }
                    if (drawFaintCore) {
                        paint.setMaskFilter(null);
                        paint.setColor(0x18FFFFFF);
                        drawTextLines(sc, lines, pad + 2f, pad + 2f, logicalFont, paint, lineH);
                    }
                } else {
                    drawTextLines(sc, lines, pad + 2f, pad + 2f, logicalFont, paint, lineH);
                }
            }
            sc.restore();
            Image image = surface.makeImageSnapshot();
            surface.close();
            return new TextImage(image, logicalW, logicalH, pad + 2f);
        }
    }

    private void drawTextLines(Canvas c, String[] lines, float x, float y, Font font, Paint paint, float lineH) {
        float ascent = font.getMetrics().getAscent();
        for (int i = 0; i < lines.length; i++) {
            c.drawString(lines[i], x, y + i * lineH - ascent, font, paint);
        }
    }

    private String[] wrapText(String text, Typeface tf, float size, float maxWidth) {
        if (text == null || text.isBlank()) return new String[]{""};
        if (renderer.measureText(text, tf, size) <= maxWidth) return new String[]{text};
        ArrayList<String> lines = new ArrayList<>();
        String[] parts = text.contains(" ") ? text.split(" ") : text.split("");
        String cur = "";
        for (String raw : parts) {
            String part = text.contains(" ") ? raw : raw;
            String next = cur.isEmpty() ? part : (text.contains(" ") ? cur + " " + part : cur + part);
            if (!cur.isEmpty() && renderer.measureText(next, tf, size) > maxWidth) {
                lines.add(cur);
                cur = part;
            } else {
                cur = next;
            }
        }
        if (!cur.isEmpty()) lines.add(cur);
        return lines.toArray(String[]::new);
    }

    private void clearLyricCache() {
        for (CachedLyricLine line : lyricCache) line.close();
        lyricCache.clear();
        lyricCacheWidth = -1f;
        lyricCacheScale = -1f;
    }

    private void updateLyricSpring(float targetScroll, float dt) {
        float safeDt = clamp(dt, 0f, 0.05f);
        float stiffness = 95f;
        float damping = 18f;
        float displacement = targetScroll - lyricScroll;
        lyricScrollVelocity += displacement * stiffness * safeDt;
        lyricScrollVelocity *= (float) Math.exp(-damping * safeDt);
        lyricScroll += lyricScrollVelocity * safeDt;
        if (Math.abs(displacement) < 0.25f && Math.abs(lyricScrollVelocity) < 0.35f) {
            lyricScroll = targetScroll;
            lyricScrollVelocity = 0f;
        }
    }

    private void drawCheapMotionBlurredLyricText(String text, float x, float y, Typeface tf, float size, int color, float radius) {
        if (text == null || text.isEmpty()) return;
        float o1 = radius * 0.45f;
        float o2 = radius;
        int c1 = withAlpha(color, alphaOf(color) * 0.28f);
        int c2 = withAlpha(color, alphaOf(color) * 0.16f);
        renderer.drawText(text, x - o2, y, tf, size, c2);
        renderer.drawText(text, x + o2, y, tf, size, c2);
        renderer.drawText(text, x, y - o2, tf, size, c2);
        renderer.drawText(text, x, y + o2, tf, size, c2);
        renderer.drawText(text, x - o1, y - o1, tf, size, c1);
        renderer.drawText(text, x + o1, y - o1, tf, size, c1);
        renderer.drawText(text, x - o1, y + o1, tf, size, c1);
        renderer.drawText(text, x + o1, y + o1, tf, size, c1);
    }

    private void drawBlurredLyricText(String text, float x, float y, Typeface tf, float size, int color, float sigma) {
        if (text == null || text.isEmpty() || tf == null) return;
        try (Font font = new Font(tf, size);
             Paint paint = new Paint();
             MaskFilter blur = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma, false)) {
            paint.setColor(color);
            paint.setAntiAlias(true);
            paint.setMaskFilter(blur);
            float baseline = y - font.getMetrics().getAscent();
            renderer.canvas().drawString(text, x, baseline, font, paint);
        }
    }

    private void drawPlayPause(float cx, float cy, float r, boolean showPlay) {
        if (showPlay) {
            fillTriangle(cx - r * 0.34f, cy - r * 0.54f, cx - r * 0.34f, cy + r * 0.54f, cx + r * 0.52f, cy, WHITE);
        } else {
            renderer.drawRoundedRect(cx - r * 0.36f, cy - r * 0.58f, r * 0.24f, r * 1.16f, r * 0.08f, WHITE);
            renderer.drawRoundedRect(cx + r * 0.12f, cy - r * 0.58f, r * 0.24f, r * 1.16f, r * 0.08f, WHITE);
        }
    }

    private void drawPrevNext(float cx, float cy, float r, boolean next) {
        float dir = next ? 1f : -1f;
        fillTriangle(cx - dir * r * 0.05f, cy, cx - dir * r * 0.58f, cy - r * 0.52f, cx - dir * r * 0.58f, cy + r * 0.52f, WHITE);
        fillTriangle(cx + dir * r * 0.52f, cy, cx - dir * r * 0.01f, cy - r * 0.52f, cx - dir * r * 0.01f, cy + r * 0.52f, WHITE);
        renderer.drawRoundedRect(cx + dir * r * 0.60f - (next ? 0 : r * 0.10f), cy - r * 0.52f, r * 0.10f, r * 1.04f, r * 0.03f, WHITE);
    }

    private void drawModeButton(float cx, float cy, float r, boolean shuffle) {
        if (shuffle) {
            renderer.drawRoundedRect(cx - r * 0.58f, cy - r * 0.28f, r * 0.72f, r * 0.10f, r * 0.05f, WHITE);
            renderer.drawRoundedRect(cx - r * 0.58f, cy + r * 0.24f, r * 0.72f, r * 0.10f, r * 0.05f, WHITE);
            fillTriangle(cx + r * 0.55f, cy - r * 0.23f, cx + r * 0.30f, cy - r * 0.39f, cx + r * 0.30f, cy - r * 0.07f, WHITE);
            fillTriangle(cx + r * 0.55f, cy + r * 0.29f, cx + r * 0.30f, cy + r * 0.13f, cx + r * 0.30f, cy + r * 0.45f, WHITE);
        } else {
            renderer.drawRoundedRect(cx - r * 0.56f, cy - r * 0.36f, r * 1.02f, r * 0.10f, r * 0.05f, WHITE);
            renderer.drawRoundedRect(cx - r * 0.56f, cy + r * 0.26f, r * 1.02f, r * 0.10f, r * 0.05f, WHITE);
            fillTriangle(cx + r * 0.58f, cy - r * 0.31f, cx + r * 0.32f, cy - r * 0.47f, cx + r * 0.32f, cy - r * 0.15f, WHITE);
            fillTriangle(cx - r * 0.58f, cy + r * 0.31f, cx - r * 0.32f, cy + r * 0.15f, cx - r * 0.32f, cy + r * 0.47f, WHITE);
        }
    }

     private void drawSpeaker(float cx, float cy, float r) {
         float bodyL = cx - r * 0.60f;
         float bodyT = cy - r * 0.22f;
         float bodyW = r * 0.28f;
         float bodyH = r * 0.44f;
         renderer.drawRoundedRect(bodyL, bodyT, bodyW, bodyH, r * 0.04f, W88);
         fillTriangle(
                 bodyL + bodyW, bodyT,
                 bodyL + bodyW, bodyT + bodyH,
                 cx + r * 0.10f, cy, W88);
     }

     private void drawSoundWaves(float cx, float cy, float r) {
         drawSoundArc(cx - r * 0.08f, cy, r * 0.38f, r * 0.12f, W88);
         drawSoundArc(cx + r * 0.08f, cy, r * 0.58f, r * 0.10f, W70);
         drawSoundArc(cx + r * 0.24f, cy, r * 0.78f, r * 0.08f, W46);
     }

     private void drawSoundArc(float cx, float cy, float radius, float strokeW, int color) {
         try (PathBuilder pb = new PathBuilder(); Paint paint = new Paint()) {
             int segments = 12;
             float startAngle = (float) Math.toRadians(-50);
             float endAngle = (float) Math.toRadians(50);
             for (int i = 0; i <= segments; i++) {
                 float t = startAngle + (endAngle - startAngle) * i / segments;
                 float px = cx + (float) Math.cos(t) * radius;
                 float py = cy + (float) Math.sin(t) * radius;
                 if (i == 0) pb.moveTo(px, py);
                 else pb.lineTo(px, py);
             }
             try (Path path = pb.detach()) {
                 paint.setColor(color);
                 paint.setAntiAlias(true);
                 paint.setMode(PaintMode.STROKE);
                 paint.setStrokeWidth(strokeW);
                 paint.setStrokeCap(io.github.humbleui.skija.PaintStrokeCap.ROUND);
                 renderer.canvas().drawPath(path, paint);
             }
         }
     }

    private void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        try (PathBuilder pb = new PathBuilder(); Paint paint = new Paint()) {
            pb.moveTo(x1, y1);
            pb.lineTo(x2, y2);
            pb.lineTo(x3, y3);
            pb.closePath();
            try (Path path = pb.detach()) {
                paint.setColor(color);
                paint.setAntiAlias(true);
                renderer.canvas().drawPath(path, paint);
            }
        }
    }

    private void handleMouse(int mx, int my) {
        long window = Minecraft.getInstance().getWindow().handle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean leftClick = leftDown && !wasLeftDown;
        if (leftClick) handleControlClick(mx, my);
        if (!leftDown) {
            draggingVolume = false;
            draggingProgress = false;
        }
        if (draggingProgress && currentUiTrack != null && progW > 0) {
            float p = clamp((mx - progX) / progW, 0f, 1f);
            MusicLoader.seek(currentUiTrack.duration() * p);
            lastRenderedSecond = -1;
            lastActiveLyric = -1;
            renderControlsLayer(System.currentTimeMillis());
            renderLyricsLayer(System.currentTimeMillis(), 0.016f);
        }
        if (draggingVolume && volW > 0) {
            volume = clamp((mx - volX) / volW, 0f, 1f);
            MusicLoader.setVolume(volume);
            dirty = true;
        }
        wasLeftDown = leftDown;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleControlClick((float) event.x(), (float) event.y())) {
            wasLeftDown = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleControlClick(float mx, float my) {
        if (playlistOpen) {
            if (playlistContains(mx, my)) {
                int row = playlistRowAt(mx, my);
                if (row >= 0 && row < MusicLoader.getTracks().size()) {
                    trackIndex = row;
                    refreshTrackCache();
                    MusicLoader.play(MusicLoader.getTracks().get(trackIndex));
                    playlistOpen = false;
                    dirty = true;
                    return true;
                }
            } else if (!inside(mx, my, listX, listY, listW, listH)) {
                playlistOpen = false;
                dirty = true;
                return true;
            }
        }
        if (inside(mx, my, playX - btnR, playY - btnR, btnR * 2f, btnR * 2f)) {
            if (currentUiTrack != null) {
                MusicLoader.toggle(currentUiTrack);
                if (MusicLoader.isPlaying(currentUiTrack)) {
                    openedAt = System.currentTimeMillis();
                    lastFrame = openedAt;
                }
            }
            dirty = true;
            return true;
        }
        if (inside(mx, my, prevX - btnR, prevY - btnR, btnR * 2f, btnR * 2f)) {
            previousTrack();
            return true;
        }
        if (inside(mx, my, nextX - btnR, nextY - btnR, btnR * 2f, btnR * 2f)) {
            nextTrack();
            return true;
        }
        if (inside(mx, my, modeX - btnR, modeY - btnR, btnR * 2f, btnR * 2f)) {
            shuffleMode = !shuffleMode;
            dirty = true;
            return true;
        }
        if (inside(mx, my, listX, listY, listW, listH)) {
            playlistOpen = !playlistOpen;
            dirty = true;
            return true;
        }
        if (inside(mx, my, progX, progY - 8, progW, progH + 12)) {
            draggingProgress = true;
            if (currentUiTrack != null) MusicLoader.seek(currentUiTrack.duration() * clamp((mx - progX) / progW, 0f, 1f));
            lastRenderedSecond = -1;
            lastActiveLyric = -1;
            dirty = true;
            return true;
        }
        if (inside(mx, my, volX - 10, volY - 12, volW + 20, volH + 24)) {
            draggingVolume = true;
            volume = clamp((mx - volX) / volW, 0f, 1f);
            MusicLoader.setVolume(volume);
            dirty = true;
            return true;
        }
        return false;
    }

    private boolean playlistContains(float mx, float my) {
        float s = pageScale();
        float w = Math.min(300f * s, width * 0.30f);
        float h = Math.min(260f * s, height * 0.48f);
        float x = width - w - 26f * s;
        float y = height - h - 88f * s;
        return inside(mx, my, x, y, w, h);
    }

    private int playlistRowAt(float mx, float my) {
        float s = pageScale();
        float w = Math.min(300f * s, width * 0.30f);
        float h = Math.min(260f * s, height * 0.48f);
        float x = width - w - 26f * s;
        float y = height - h - 88f * s;
        if (!playlistContains(mx, my)) return -1;
        int row = (int) ((my - y - 34f * s) / (26f * s));
        return row;
    }

    private void nextTrack() {
        int count = MusicLoader.getTracks().size();
        if (count <= 0) return;
        boolean wasPlaying = !MusicLoader.isPaused();
        trackIndex = shuffleMode && count > 1 ? (trackIndex + 1 + Math.max(1, (int)(System.nanoTime() % (count - 1)))) % count : (trackIndex + 1) % count;
        refreshTrackCache();
        if (wasPlaying) MusicLoader.play(MusicLoader.getTracks().get(trackIndex));
        dirty = true;
    }

    private void previousTrack() {
        int count = MusicLoader.getTracks().size();
        if (count <= 0) return;
        boolean wasPlaying = !MusicLoader.isPaused();
        trackIndex = (trackIndex - 1 + count) % count;
        refreshTrackCache();
        if (wasPlaying) MusicLoader.play(MusicLoader.getTracks().get(trackIndex));
        dirty = true;
    }

    private boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void rebuildCover(MusicLoader.MusicTrack track) {
        if (coverImage != null) {
            coverImage.close();
            coverImage = null;
        }
        byte[] coverBytes = track.coverArt();
        if (coverBytes == null || coverBytes.length == 0) {
            derivePaletteFromText(track.title() + track.artist());
            rebuildFluidBackground(null, track);
            return;
        }
        try {
            coverImage = Image.makeFromEncoded(coverBytes);
            derivePaletteFromCover(coverBytes);
            rebuildFluidBackground(coverBytes, track);
        } catch (Exception e) {
            derivePaletteFromText(track.title() + track.artist());
            rebuildFluidBackground(null, track);
            SkijaTestClient.LOGGER.warn("[Musicpage] Failed to decode cover art for {}", track.title(), e);
        }
    }

    private void rebuildFluidBackground(byte[] coverBytes, MusicLoader.MusicTrack track) {
        String seed = track.title() + track.artist();
        if (bgRenderer != null) {
            fluidBackground.rebuild(coverBytes, seed, bgRenderer.getWidth(), bgRenderer.getHeight());
        } else {
            fluidBackground.rebuild(coverBytes, seed);
        }
    }

    private void derivePaletteFromCover(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) return;
            long r = 0, g = 0, b = 0, count = 0;
            int stepX = Math.max(1, img.getWidth() / 28);
            int stepY = Math.max(1, img.getHeight() / 28);
            for (int y = 0; y < img.getHeight(); y += stepY) {
                for (int x = 0; x < img.getWidth(); x += stepX) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    if (a < 64) continue;
                    r += (argb >>> 16) & 0xFF;
                    g += (argb >>> 8) & 0xFF;
                    b += argb & 0xFF;
                    count++;
                }
            }
            if (count <= 0) return;
            int avg = rgb((int)(r / count), (int)(g / count), (int)(b / count));
            paletteA = saturate(lighten(avg, 0.14f), 1.22f);
            paletteB = saturate(rotate(avg), 1.34f);
            paletteC = saturate(darken(avg, 0.16f), 1.10f);
            paletteDark = darken(avg, 0.58f);
        } catch (Exception e) {
            SkijaTestClient.LOGGER.warn("[Musicpage] Failed to derive cover palette", e);
        }
    }

    private void derivePaletteFromText(String seed) {
        int h = Math.abs(seed == null ? 0 : seed.hashCode());
        int r = 110 + (h & 0x5F);
        int g = 48 + ((h >>> 7) & 0x4F);
        int b = 62 + ((h >>> 14) & 0x5F);
        int base = rgb(r, g, b);
        paletteA = saturate(lighten(base, 0.12f), 1.22f);
        paletteB = saturate(rotate(base), 1.30f);
        paletteC = darken(base, 0.16f);
        paletteDark = darken(base, 0.58f);
    }

    private void refreshTrackCache() {
        cachedTrack = null;
        clearLyricCache();
        if (coverImage != null) {
            coverImage.close();
            coverImage = null;
        }
    }

    private List<LyricLine> parseLyrics(String rawLyrics, String title, String artist) {
        ArrayList<LyricLine> result = new ArrayList<>();
        if (rawLyrics != null && !rawLyrics.isBlank()) {
            String[] lines = rawLyrics.replace("\r", "").split("\n");
            int plainIndex = 0;
            for (String line : lines) {
                Matcher m = LRC_PATTERN.matcher(line);
                if (m.matches()) {
                    int min = parseInt(m.group(1));
                    int sec = parseInt(m.group(2));
                    int ms = parseMillis(m.group(3));
                    String text = m.group(4).isBlank() ? "♪" : m.group(4).trim();
                    result.add(new LyricLine(min * 60f + sec + ms / 1000f, text));
                } else if (!line.isBlank()) {
                    result.add(new LyricLine(plainIndex++ * 4.0f, line.trim()));
                }
            }
        }
        if (result.isEmpty()) {
            result.add(new LyricLine(0, title));
            result.add(new LyricLine(4, artist));
            result.add(new LyricLine(8, "把带歌词标签的 MP3 / FLAC 放到 lazychara/music"));
            result.add(new LyricLine(12, "之后接入真实播放进度即可同步歌词"));
        }
        result.sort(java.util.Comparator.comparingDouble(LyricLine::time));
        return result;
    }

    private int activeLyricIndex(float elapsed) {
        int active = 0;
        for (int i = 0; i < lyricLines.size(); i++) {
            if (elapsed + 0.05f >= lyricLines.get(i).time()) active = i;
            else break;
        }
        return active;
    }

    private float elapsedSeconds(MusicLoader.MusicTrack track, long now) {
        if (MusicLoader.getCurrentTrack() == track) {
            return MusicLoader.getCurrentSeconds();
        }
        return 0f;
    }

    private void blitLayers(GuiGraphicsExtractor g) {
        long now = System.currentTimeMillis();
        float entryT = entryProgress(now);
        float returnT = returningToMain ? returnProgress(now) : 0f;
        float closeT = closingPage ? closeProgress(now) : 0f;
        var pose = g.pose();
        pose.pushMatrix();
        float entryE = easeOutCubic(entryT);
        float returnE = easeInOut(returnT);
        float closeE = easeInOut(closeT);
        float scale = (0.985f + 0.015f * entryE) * (1f - 0.070f * returnE) * (1f - 0.055f * closeE);
        float y = 18f * (1f - entryE) + 42f * returnE + 18f * closeE;
        pose.translate(width * 0.5f, height * 0.5f + y);
        pose.scale(scale, scale);
        pose.translate(-width * 0.5f, -height * 0.5f);
        blitBackgroundRenderer(g);
        blitRenderer(g, staticRenderer, 0f, 0f);
        blitRenderer(g, controlsRenderer, controlsLayerX, controlsLayerY);
        blitRenderer(g, lyricsRenderer, lyricsLayerX, lyricsLayerY);
        pose.popMatrix();
        float entryA = 220f * (1f - easeInOut(entryT));
        float returnA = 225f * returnE;
        float closeA = 205f * closeE;
        int a = Math.max(0, Math.min(240, Math.round(Math.max(entryA, Math.max(returnA, closeA)))));
        if (a > 0) g.fill(0, 0, width, height, a << 24);
    }

    private float entryProgress(long now) {
        return Math.max(0f, Math.min(1f, (now - entryStartedAt) / (float) ENTRY_ANIM_MS));
    }

    private float returnProgress(long now) {
        return Math.max(0f, Math.min(1f, (now - exitStartedAt) / (float) RETURN_ANIM_MS));
    }

    private float closeProgress(long now) {
        return Math.max(0f, Math.min(1f, (now - exitStartedAt) / (float) CLOSE_ANIM_MS));
    }

    private float easeOutCubic(float t) {
        t = clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private float easeInOut(float t) {
        t = clamp(t, 0f, 1f);
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2f) * 0.5f;
    }

    private void blitBackgroundRenderer(GuiGraphicsExtractor g) {
        if (bgRenderer == null) return;
        float inv = 1f / guiScale;
        float sx = (width * guiScale) / (float) bgRenderer.getWidth();
        float sy = (height * guiScale) / (float) bgRenderer.getHeight();
        var pose = g.pose();
        pose.pushMatrix();
        pose.scale(inv, inv);
        pose.scale(sx, sy);
        g.blit(RenderPipelines.GUI_TEXTURED, bgRenderer.textureId(), 0, 0, 0f, 0f, bgRenderer.getWidth(), bgRenderer.getHeight(), bgRenderer.getWidth(), bgRenderer.getHeight());
        pose.popMatrix();
    }

    private void blitRenderer(GuiGraphicsExtractor g, SkijaRenderer r, float x, float y) {
        if (r == null) return;
        float inv = 1f / guiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.scale(inv, inv);
        int px = Math.round(x * guiScale);
        int py = Math.round(y * guiScale);
        g.blit(RenderPipelines.GUI_TEXTURED, r.textureId(), px, py, 0f, 0f, r.getWidth(), r.getHeight(), r.getWidth(), r.getHeight());
        pose.popMatrix();
    }

    private void blit(GuiGraphicsExtractor g) {
        float inv = 1f / guiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, renderer.textureId(), 0, 0, 0f, 0f, renderer.getWidth(), renderer.getHeight(), renderer.getWidth(), renderer.getHeight());
        pose.popMatrix();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (isExiting()) return true;
        if (sy > 0) {
            startReturnTransition();
            return true;
        }
        if (sy < 0 && !MusicLoader.getTracks().isEmpty()) {
            nextTrack();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (isExiting()) return true;
        if (e.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            startCloseTransition();
            return true;
        }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
            startReturnTransition();
            return true;
        }
        if (e.key() == GLFW.GLFW_KEY_R) {
            MusicLoader.rescan();
            clampTrackIndex();
            refreshTrackCache();
            dirty = true;
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public void onClose() {
        startCloseTransition();
    }

    private boolean isExiting() {
        return returningToMain || closingPage;
    }

    private void startReturnTransition() {
        if (isExiting()) return;
        returningToMain = true;
        exitStartedAt = System.currentTimeMillis();
        draggingVolume = false;
        draggingProgress = false;
        playlistOpen = false;
        dirty = true;
    }

    private void startCloseTransition() {
        if (isExiting()) return;
        closingPage = true;
        exitStartedAt = System.currentTimeMillis();
        draggingVolume = false;
        draggingProgress = false;
        playlistOpen = false;
        dirty = true;
    }

    @Override
    public void removed() {
        super.removed();
        closeRenderer();
    }

    private void closeRenderer() {
        if (bgRenderer != null) {
            bgRenderer.close();
            bgRenderer = null;
        }
        if (staticRenderer != null) {
            staticRenderer.close();
            staticRenderer = null;
        }
        if (controlsRenderer != null) {
            controlsRenderer.close();
            controlsRenderer = null;
        }
        if (lyricsRenderer != null) {
            lyricsRenderer.close();
            lyricsRenderer = null;
        }
        renderer = null;
        if (coverImage != null) {
            coverImage.close();
            coverImage = null;
        }
        fluidBackground.close();
        clearLyricCache();
    }

    private void clampTrackIndex() {
        int count = MusicLoader.getTracks().size();
        if (count <= 0) trackIndex = 0;
        else if (trackIndex < 0 || trackIndex >= count) trackIndex = 0;
    }

    private int[] computeBackgroundTextureSize() {
        int targetW = Math.max(1, Math.round(width * guiScale * BG_RENDER_SCALE));
        int targetH = Math.max(1, Math.round(height * guiScale * BG_RENDER_SCALE));
        float capScale = Math.min(1f, Math.min(BG_RENDER_MAX_W / (float) targetW, BG_RENDER_MAX_H / (float) targetH));
        targetW = Math.round(targetW * capScale);
        targetH = Math.round(targetH * capScale);
        targetW = Math.max(BG_RENDER_MIN_W, targetW);
        targetH = Math.max(BG_RENDER_MIN_H, targetH);
        return new int[]{targetW, targetH};
    }

    private float pageScale() {
        return clamp(Math.min(width / 620f, height / 300f), 0.62f, 1.35f);
    }

    private float fitTextSize(String text, Typeface tf, float preferredSize, float minSize, float maxWidth) {
        if (text == null || text.isEmpty()) return preferredSize;
        float size = preferredSize;
        while (size > minSize && renderer.measureText(text, tf, size) > maxWidth) {
            size -= 1.4f;
        }
        return Math.max(minSize, size);
    }

    private String trimText(String text, Typeface tf, float size, float maxWidth) {
        if (text == null) return "";
        if (renderer.measureText(text, tf, size) <= maxWidth) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && renderer.measureText(text.substring(0, end) + ellipsis, tf, size) > maxWidth) end--;
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return 0; }
    }

    private int parseMillis(String value) {
        if (value == null || value.isEmpty()) return 0;
        int raw = parseInt(value);
        if (value.length() == 1) return raw * 100;
        if (value.length() == 2) return raw * 10;
        return raw;
    }

    private String formatTime(int sec) {
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }

    private float sin(float v) { return (float) Math.sin(v); }
    private float cos(float v) { return (float) Math.cos(v); }
    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    private int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private float alphaOf(int color) { return ((color >>> 24) & 0xFF) / 255f; }

    private int rgb(int r, int g, int b) {
        return 0xFF000000 | (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b);
    }

    private int clamp8(int v) { return Math.max(0, Math.min(255, v)); }

    private int lighten(int color, float amount) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return rgb((int)(r + (255 - r) * amount), (int)(g + (255 - g) * amount), (int)(b + (255 - b) * amount));
    }

    private int darken(int color, float amount) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return rgb((int)(r * (1f - amount)), (int)(g * (1f - amount)), (int)(b * (1f - amount)));
    }

    private int rotate(int color) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return rgb(Math.max(r, b), Math.max(g - 8, 0), Math.max(r - 20, 0));
    }

    private int saturate(int color, float factor) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        float gray = (r + g + b) / 3f;
        return rgb((int)(gray + (r - gray) * factor), (int)(gray + (g - gray) * factor), (int)(gray + (b - gray) * factor));
    }

    private record LyricLine(float time, String text) {}

    private static final class TextImage implements AutoCloseable {
        final Image image;
        final float w;
        final float h;
        final float pad;

        TextImage(Image image, float w, float h, float pad) {
            this.image = image;
            this.w = w;
            this.h = h;
            this.pad = pad;
        }

        @Override
        public void close() {
            if (image != null) image.close();
        }
    }

    private static final class CachedLyricLine implements AutoCloseable {
        final Image activeImage;
        final float activeW;
        final float activeH;
        final Image inactiveImage;
        final float inactiveW;
        final float inactiveH;
        final float inactivePad;

        CachedLyricLine(TextImage active, TextImage inactive) {
            this.activeImage = active.image;
            this.activeW = active.w;
            this.activeH = active.h;
            this.inactiveImage = inactive.image;
            this.inactiveW = inactive.w;
            this.inactiveH = inactive.h;
            this.inactivePad = inactive.pad;
        }

        @Override
        public void close() {
            if (activeImage != null) activeImage.close();
            if (inactiveImage != null) inactiveImage.close();
        }
    }
}

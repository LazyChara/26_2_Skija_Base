package com.lazychara.skijatest.client.Musicpage;

import static com.lazychara.skijatest.client.Musicpage.MusicPageIcons.*;
import static com.lazychara.skijatest.client.Musicpage.MusicPageMath.*;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterBlurMode;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.MaskFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.joml.Matrix3x2f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import com.lazychara.skijatest.client.AMLLFluidBackground;
import com.lazychara.skijatest.client.MusicLoader;
import com.lazychara.skijatest.client.SkijaRenderer;
import com.lazychara.skijatest.client.SkijaTestClient;
import com.lazychara.skijatest.client.SkijaTestScreen;
import java.util.Map;
import java.util.Set;
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
    private static final int AMLL_VOLUME = 0x80FFFFFF;
    private static final int AMLL_VOLUME_BG = 0x26FFFFFF;
    private static final int AMLL_ACTIVE_BG = 0xE6FFFFFF;
    private static final int AMLL_ACTIVE_ICON = 0xCC000000;
    private static final int AMLL_BUTTON_HOVER = 0x24FFFFFF;
    private static final long ENTRY_ANIM_MS = 420L;
    private static final long RETURN_ANIM_MS = 340L;
    private static final long CLOSE_ANIM_MS = 260L;
    private static final long LYRIC_LAYOUT_ANIM_MS = 500L;
    private static final long LYRIC_HIDE_ANIM_MS = 250L;
    private static final long LYRIC_SHOW_DELAY_MS = 250L;
    private static final long LYRIC_SHOW_ANIM_MS = 500L;
    private static final long BG_INTERVAL_PLAYING_MS = 66L;
    private static final long BG_INTERVAL_IDLE_MS = 160L;
    private static final long BG_INTERVAL_EXIT_MS = 120L;
    private static final long DRAG_RENDER_INTERVAL_MS = 40L;
    private static final float BG_RENDER_SCALE = 0.50f;
    private static final int BG_RENDER_MIN_W = 360;
    private static final int BG_RENDER_MIN_H = 202;
    private static final int BG_RENDER_MAX_W = 1080;
    private static final int BG_RENDER_MAX_H = 660;
    private static final float LYRIC_OVERFLOW_PENALTY_MULTIPLIER = 1000f;
    private static final float LYRIC_CJK_BREAK_PENALTY_RATIO = 0.15f;
    private static final float LYRIC_NORMAL_BREAK_PENALTY_RATIO = 0.50f;
    private static final float LYRIC_SPACE_BREAK_REWARD_RATIO = 0.40f;
    private static final float LYRIC_PUNCTUATION_BREAK_REWARD_RATIO = 0.60f;
    private static final String LYRIC_BREAK_PUNCTUATION = ",.;:!?，。；：！？、）】》」』’”)[\\]}>~…";
    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]\\s*(.*)");
    private static final Pattern LYRIC_SPACE_PATTERN = Pattern.compile("\\s+");
    private SkijaRenderer renderer;
    private SkijaRenderer bgRenderer;
    private AMLLFluidBackground.GuiRenderer gpuBackground;
    private SkijaRenderer staticRenderer;
    private SkijaRenderer controlsRenderer;
    private SkijaRenderer lyricsRenderer;
    private int guiScale = 1;
    private long openedAt;
    private long entryStartedAt;
    private long exitStartedAt;
    private long lastFrame;
    private long lastProgressRender;
    private long lastBgRender;
    private long lastDragRender;
    private int lastActiveLyric = -1;
    private final Set<Integer> amllHotGroups = new HashSet<>();
    private final Set<Integer> amllBufferedGroups = new HashSet<>();
    private int amllScrollToIndex = 0;
    private float amllLastTimelineSeconds = -1f;
    private int lastRenderedSecond = -1;
    private boolean dirty = true;
    private boolean controlsLayerDirty = true;
    private boolean lyricsLayerDirty = true;
    private float lyricScroll;
    private float lyricScrollVelocity;
    private float lyricRenderScroll;
    private float lyricLayerBlitOffsetY;
    private boolean lyricSnapOnNextRender = true;
    private float backgroundRenderTime;
    private float backgroundLowFreqPulse;
    private float backgroundPulseVisual;
    private int trackIndex;
    private Image coverImage;
    private List<LyricLine> lyricLines = List.of();
    private final List<CachedLyricLine> lyricCache = new ArrayList<>();
    private final Map<String, Path> iconPathCache = new HashMap<>();
    private float lyricCacheWidth = -1f;
    private float lyricCacheScale = -1f;
    private float lyricCacheActiveSize = -1f;
    private float lyricCacheInactiveSize = -1f;
    private Typeface lyricCacheTypeface;
    private MusicLoader.MusicTrack cachedTrack;
    private MusicLoader.MusicTrack currentUiTrack;

    private boolean shuffleMode = false;
    private boolean repeatMode = false;
    private boolean lyricsVisible = true;
    private boolean playlistOpen = false;
    private float lyricLayoutVisual = 1f;
    private float lyricLayoutAnimStart = 1f;
    private float lyricOpacityVisual = 1f;
    private float lyricOpacityAnimStart = 1f;
    private long lyricViewAnimStartedAt = 0L;
    private float volume = 0.78f;
    private boolean draggingVolume = false;
    private boolean draggingProgress = false;
    private float progressDragSeconds = -1f;
    private boolean wasLeftDown = false;
    private boolean returningToMain = false;
    private boolean closingPage = false;
    private int hoveredControl = -1;
    private boolean hoveredProgress = false;
    private boolean hoveredVolume = false;
    private boolean hoveredLyrics = false;
    private final float[] controlPress = new float[5];
    private final float[] controlHover = new float[5];
    private float progressVisual;
    private float volumeVisual;
    private float progressBounceX;
    private float volumeBounceX;
    private SkijaRenderer interludeDotRenderer;
    private int bakedBlurBudgetThisFrame;
    private float interludeDotScale = -1f;
    private float interludeY;
    private float interludeVelocityY;
    private float interludeOpacity;
    private float interludeStartTime;
    private float interludeEndTime;
    private boolean interludeNextDuet;
    private float interludeCurrentTime;
    private int interludeAnchor = Integer.MIN_VALUE;

    private float volX, volY, volW, volH;
    private float progX, progY, progW, progH;
    private float prevX, prevY, nextX, nextY, playX, playY, btnR;
    private float modeX, modeY, repeatX, repeatY, listX, listY, listW, listH;
    private float lyricToggleX, lyricToggleY, lyricToggleW, lyricToggleH;
    private float thumbX, thumbY, thumbW, thumbH;
    private float menuX, menuY, menuW, menuH;
    private float controlsLayerX, controlsLayerY, controlsLayerW, controlsLayerH;
    private float lyricsLayerX, lyricsLayerY, lyricsLayerW, lyricsLayerH;
    private float layoutLeftX, layoutLeftW, layoutInfoY, layoutProgressY, layoutControlY, layoutVolumeY;
    private float layoutRightX, layoutLyricTop, layoutRightW, layoutLyricH, layoutLyricAnchorY;

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
        bgRenderer.clear(paletteDark);
        bgRenderer.upload();
        gpuBackground = new AMLLFluidBackground.GuiRenderer("music_page_bg_gpu");
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
        lastProgressRender = 0;
        lastBgRender = 0;
        lastDragRender = 0;
        lyricLayoutVisual = lyricsVisible ? 1f : 0f;
        lyricLayoutAnimStart = lyricLayoutVisual;
        lyricOpacityVisual = lyricLayoutVisual;
        lyricOpacityAnimStart = lyricOpacityVisual;
        lyricViewAnimStartedAt = openedAt;
        lyricRenderScroll = 0f;
        lyricLayerBlitOffsetY = 0f;
        lyricSnapOnNextRender = true;
        backgroundRenderTime = 0f;
        backgroundLowFreqPulse = 0f;
        backgroundPulseVisual = 0f;
        lastActiveLyric = -1;
        amllHotGroups.clear();
        amllBufferedGroups.clear();
        amllScrollToIndex = 0;
        amllLastTimelineSeconds = -1f;
        lastRenderedSecond = -1;
        interludeAnchor = Integer.MIN_VALUE;
        interludeOpacity = 0f;
        interludeNextDuet = false;
        interludeVelocityY = 0f;
        dirty = true;
        controlsLayerDirty = true;
        lyricsLayerDirty = true;
        syncTrackIndexToPlaying();
        clampTrackIndex();
        refreshTrackCache();
        ensureInterludeDotRenderer(pageScale());
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
            boolean exiting = isExiting();
            if (!exiting) handleMouse(mx, my);
            float dt = Math.min(0.05f, Math.max(0f, (now - lastFrame) / 1000f));
            lastFrame = now;
            if (!exiting) updateLyricViewAnimation(now);
            if (!exiting && updateControlAnimations(dt, pageScale())) controlsLayerDirty = true;
            if (dirty) {
                renderStaticPage(now);
                controlsLayerDirty = true;
                lyricsLayerDirty = true;
                dirty = false;
            }
            if (!exiting && currentUiTrack != null) {
                if (MusicLoader.consumePlaybackEnded(currentUiTrack)) {
                    handlePlaybackEnded();
                } else {
                    float currentSeconds = displayedElapsedSeconds(currentUiTrack, now);
                    int currentSecond = (int) currentSeconds;
                    int active = activeLyricIndex(currentSeconds);
                    if (active != lastActiveLyric) {
                        if (!useLineLyricTextures()) lyricsLayerDirty = true;
                        lastActiveLyric = active;
                    }
                    updateLyricAnimation(active, currentSeconds, pageScale(), dt);
                    if (MusicLoader.isPlaying(currentUiTrack) && (currentSecond != lastRenderedSecond || now - lastProgressRender >= 250L)) {
                        controlsLayerDirty = true;
                        lastRenderedSecond = currentSecond;
                        lastProgressRender = now;
                    }
                }
            }
            if (!exiting) {
                if (controlsLayerDirty) {
                    renderControlsLayer(now);
                    controlsLayerDirty = false;
                }
                if (lyricsLayerDirty) {
                    if (!useLineLyricTextures()) renderLyricsLayer(now, dt);
                    lyricsLayerDirty = false;
                }
            }
            boolean bgAnimating = currentUiTrack != null && MusicLoader.isPlaying(currentUiTrack) || entryProgress(now) < 1f;
            if (gpuBackground != null && gpuBackground.ready() && !exiting) {
                renderBackgroundLayer((now - openedAt) / 1000f);
                lastBgRender = now;
            } else {
                long bgInterval = exiting ? BG_INTERVAL_EXIT_MS : bgAnimating ? BG_INTERVAL_PLAYING_MS : BG_INTERVAL_IDLE_MS;
                if (lastBgRender == 0L || now - lastBgRender >= bgInterval) {
                    renderBackgroundLayer((now - openedAt) / 1000f);
                    lastBgRender = now;
                }
            }
            blitLayers(g);
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[Musicpage] Render error", e);
        }
        super.extractRenderState(g, mx, my, delta);
    }

    private void renderBackgroundLayer(float time) {
        if (bgRenderer == null) return;
        backgroundRenderTime = time;
        float targetPulse = currentUiTrack != null && MusicLoader.isPlaying(currentUiTrack)
                ? 0.035f + 0.018f * (0.5f + 0.5f * sin(time * 2.2f))
                : 0f;
        backgroundPulseVisual += (targetPulse - backgroundPulseVisual) * 0.06f;
        backgroundLowFreqPulse = backgroundPulseVisual;
        if (gpuBackground != null && gpuBackground.ready()) return;
        renderer = bgRenderer;
        int bw = bgRenderer.getWidth();
        int bh = bgRenderer.getHeight();
        renderer.clear(paletteDark);
        drawAnimatedFluidLayer(time, bw, bh);
        renderer.upload();
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
                resetAMLLTimeline();
                clearLyricCache();
                lyricScroll = 0f;
                lyricScrollVelocity = 0f;
                lyricRenderScroll = 0f;
                lyricLayerBlitOffsetY = 0f;
                lyricSnapOnNextRender = true;
                openedAt = now;
                rebuildCover(track);
            }
        }

        if (tracks.isEmpty()) {
            currentUiTrack = null;
            renderEmpty(tf);
        } else {
            renderAMLLStatic(tracks.get(trackIndex), tf, now);
        }

        c.restore();
        renderer.upload();
    }

    private void renderControlsLayer(long now) {
        if (controlsRenderer == null) return;
        renderer = controlsRenderer;
        renderer.clear(0x00000000);
        if (currentUiTrack == null) {
            renderer.upload();
            return;
        }
        Canvas c = renderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        c.translate(-controlsLayerX, -controlsLayerY);
        SkijaTestScreen.ensureFontLoaded();
        renderControls(layoutLeftX, layoutControlY, layoutVolumeY, layoutLeftW, SkijaTestScreen.curTf);
        renderProgress(layoutLeftX, layoutLeftW, currentUiTrack, SkijaTestScreen.curTf, now, pageScale());
        c.restore();
        renderer.upload();
    }

    private void renderLyricsLayer(long now, float dt) {
        if (lyricsRenderer == null) return;
        		renderer = lyricsRenderer;
        		renderer.clear(0x00000000);
        		bakedBlurBudgetThisFrame = 0;
        if (currentUiTrack == null) {
            renderer.upload();
            return;
        }
        Canvas c = renderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        c.translate(-lyricsLayerX, -lyricsLayerY);
        SkijaTestScreen.ensureFontLoaded();
        int active = activeLyricIndex(elapsedSeconds(currentUiTrack, now));
        float scrollBase = lyricScroll;
        renderLyrics(layoutRightX, layoutLyricTop, layoutRightW, layoutLyricH, SkijaTestScreen.curTf, active, scrollBase, pageScale());
        lyricRenderScroll = scrollBase;
        lyricLayerBlitOffsetY = 0f;
        c.restore();
        renderer.upload();
    }



    private void drawAnimatedFluidLayer(float time, float w, float h) {
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
        renderer.drawTextCentered("没音乐你听个集冒", cx, cy - 8 * s, tf, 17 * s, W88);
        renderer.drawTextCentered("把 .mp3 / .flac 放到 lazychara/music，然后按 R 重新扫描", cx, cy + 22 * s, tf, 12 * s, W70);
        renderer.drawTextCentered("向上滚动返回 · RShift 关闭", cx, height - 26 * s, tf, 10 * s, W46);
    }

    private void renderAMLLStatic(MusicLoader.MusicTrack track, Typeface tf, long now) {
        float s = pageScale();
        float infoSize = amllMusicInfoFontSize();
        float leftW = amllHorizontalLayoutMaxWidth();
        HorizontalGrid grid = amllHorizontalGrid(leftW);
        float infoColumnW = amllHorizontalInfoColumnWidth();
        float playerColumnW = amllHorizontalPlayerColumnWidth();
        float columnGap = amllHorizontalColumnGap();
        float baseLeftX = Math.max(0f, (infoColumnW - leftW) * 0.5f);
        float leftX = baseLeftX;
        float coverSize = leftW;
        float coverY = grid.coverY();
        float infoBlockH = amllMusicInfoBlockHeight(infoSize);
        float progressBlockH = amllProgressBlockHeight(track.qualityLabel());
        float mediaBlockH = amllMediaButtonSize(leftW);
        float volumeBlockH = amllVolumeControlHeight();
        float controlsH = Math.max(0f, grid.controlsH());
        float controlsFree = controlsH - infoBlockH - progressBlockH - mediaBlockH - volumeBlockH;
        float controlsGap = Math.max(0f, controlsFree / 3f);
        float infoY = grid.controlsY() + amllHorizontalMusicInfoPaddingTop();
        float progressY = grid.controlsY() + infoBlockH + controlsGap;
        float mediaY = progressY + progressBlockH + controlsGap;
        float volumeY = mediaY + mediaBlockH + controlsGap;

        float rightColumnX = infoColumnW + columnGap;
        float rightColumnW = playerColumnW;
        float rightW = Math.max(1f, rightColumnW * (1f - amllHorizontalLyricPaddingRightRatio()));
        float lyricTop = grid.lyricTop();
        float lyricH = Math.max(1f, grid.lyricH());

        currentUiTrack = track;
        layoutLeftX = leftX;
        layoutLeftW = leftW;
        layoutInfoY = infoY;
        layoutProgressY = progressY;
        layoutControlY = mediaY;
        layoutVolumeY = volumeY;
        layoutRightX = rightColumnX;
        layoutRightW = rightW;
        layoutLyricTop = lyricTop;
        layoutLyricH = lyricH;
        layoutLyricAnchorY = coverY + coverSize * 0.5f;

        float coverX = leftX + (leftW - coverSize) * 0.5f;
        renderControlThumb(leftX + leftW * 0.5f, coverY - amllHorizontalGap() - amllHorizontalThumbRowHeight() * 0.5f);
        renderCover(coverX, coverY, coverSize, s);
        float menuSize = amllMenuButtonSize();
        float menuMargin = amllMenuButtonMarginLeft();
        float infoTextW = Math.max(1f, leftW - menuSize - menuMargin);
        float titleSize = fitTextSize(track.title(), tf, infoSize, Math.max(amllCssPx(9f), infoSize * 0.72f), infoTextW);
        float infoLineH = amllMusicInfoLineHeight(infoSize);
        renderer.drawTextCenteredY(track.title(), leftX, layoutInfoY + infoLineH * 0.5f, tf, titleSize, W88);
        renderer.drawTextCenteredY(trimText(track.artist(), tf, infoSize, infoTextW), leftX, layoutInfoY + infoLineH * 1.5f, tf, infoSize, W46);
        renderMusicInfoMenuButton(leftX + leftW - menuSize, layoutInfoY + (infoLineH * 2f - menuSize) * 0.5f, menuSize);

        float bottomIcon = amllBottomToggleSize();
        float bottomPad = amllBottomControlsPadding();
        renderPlaylistButton(width - bottomPad - bottomIcon, height - bottomPad - bottomIcon, bottomIcon, s, tf);
        if (playlistOpen) renderPlaylistPanel(tf, s);
        updateLyricsLayerBounds(s);
    }

    private void updateLyricsLayerBounds(float s) {
        float padX = 36f * s;
        float padY = 88f * s;
        float x = clamp(layoutRightX - padX, 0f, width);
        float y = clamp(layoutLyricTop - padY, 0f, height);
        float w = clamp(layoutRightW + padX * 2f, 1f, width - x);
        float h = clamp(layoutLyricH + padY * 2f, 1f, height - y);
        int texW = Math.max(1, Math.round(w * guiScale));
        int texH = Math.max(1, Math.round(h * guiScale));
        boolean changed = lyricsRenderer == null || lyricsRenderer.getWidth() != texW || lyricsRenderer.getHeight() != texH || Math.abs(lyricsLayerX - x) > 0.5f || Math.abs(lyricsLayerY - y) > 0.5f;
        lyricsLayerX = x;
        lyricsLayerY = y;
        lyricsLayerW = w;
        lyricsLayerH = h;
        if (lyricsRenderer == null || lyricsRenderer.getWidth() != texW || lyricsRenderer.getHeight() != texH) {
            if (lyricsRenderer != null) lyricsRenderer.close();
            lyricsRenderer = new SkijaRenderer("music_page_lyrics", texW, texH);
        }
        if (changed) lyricsLayerDirty = true;
    }

    private void renderProgress(float leftX, float leftW, MusicLoader.MusicTrack track, Typeface tf, long now, float s) {
        float elapsed = displayedElapsedSeconds(track, now);
        float duration = Math.max(1, track.duration());
        float progress = clamp(elapsed / duration, 0f, 1f);
        float barY = amllProgressBarY(leftW);
        float minH = amllSliderInnerMinHeight();
        float maxH = amllSliderInnerMaxHeight();
        float visualH = minH + (maxH - minH) * progressVisual;
        float visualY = barY + (amllSliderContainerHeight() - visualH) * 0.5f;
        float visualX = leftX + progressBounceX;
        float labelSize = amllProgressLabelFontSize();
        renderer.drawRoundedRect(visualX, visualY, leftW, visualH, visualH * 0.5f, AMLL_VOLUME_BG);
        if (progress > 0f) renderer.drawRoundedRect(visualX, visualY, Math.max(visualH, leftW * progress), visualH, visualH * 0.5f, withAlpha(WHITE, 0.40f));
        String qualityLabel = track.qualityLabel();
        float labelY = barY + amllSliderContainerHeight() + amllProgressLabelMarginTop();
        float labelRowH = amllProgressLabelRowHeight(qualityLabel);
        float labelCy = labelY + labelRowH * 0.5f;
        renderer.drawTextCenteredY(formatTime((int) elapsed), leftX, labelCy, tf, labelSize, AMLL_VOLUME);
        if (qualityLabel != null && !qualityLabel.isBlank()) {
            float badgeH = amllQualityBadgeHeight();
            float badgeW = losslessBadgeWidth(qualityLabel, tf, s);
            float badgeX = leftX + (leftW - badgeW) * 0.5f;
            float badgeY = labelY + (labelRowH - badgeH) * 0.5f;
            renderLosslessBadge(badgeX, badgeY, badgeW, badgeH, s, tf, qualityLabel);
        }
        String remain = "-" + formatTime(Math.max(0, track.duration() - (int) elapsed));
        float remainW = renderer.measureText(remain, tf, labelSize);
        renderer.drawTextCenteredY(remain, leftX + leftW - remainW, labelCy, tf, labelSize, AMLL_VOLUME);
        progX = leftX;
        progY = barY;
        progW = leftW;
        progH = amllCssPx(24f);
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

    private void renderControlThumb(float cx, float cy) {
        thumbW = amllControlThumbWidth();
        thumbH = amllControlThumbHeight();
        thumbX = cx - thumbW * 0.5f;
        thumbY = cy - thumbH * 0.5f;
        renderer.drawRoundedRect(thumbX, thumbY, thumbW, thumbH, thumbH * 0.5f, W46);
    }

    private void renderMusicInfoMenuButton(float x, float y, float size) {
        menuX = x;
        menuY = y;
        menuW = size;
        menuH = size;
        renderer.drawCircle(x + size * 0.5f, y + size * 0.5f, size * 0.5f, 0x26FFFFFF);
        float dot = size * 0.056f;
        float gap = size * 0.207f;
        float cy = y + size * 0.5f;
        float cx = x + size * 0.5f;
        renderer.drawCircle(cx - gap, cy, dot, W88);
        renderer.drawCircle(cx, cy, dot, W88);
        renderer.drawCircle(cx + gap, cy, dot, W88);
    }

    private float losslessBadgeWidth(String label, Typeface tf, float s) {
        float scale = amllQualityTagScale();
        float textSize = amllCssPx(11f) * scale;
        float iconW = amllCssPx(16.5f) * scale;
        float gap = amllCssPx(4f) * scale;
        float padX = amllCssPx(8f) * scale;
        return renderer.measureText(label, tf, textSize) + iconW + gap + padX * 2f;
    }

    private void renderLosslessBadge(float x, float y, float w, float h, float s, Typeface tf, String label) {
        float scale = amllQualityTagScale();
        renderer.drawRoundedRect(x, y, w, h, h * 0.32f, 0x24FFFFFF);
        float textSize = amllCssPx(11f) * scale;
        float iconW = amllCssPx(16.5f) * scale;
        float gap = amllCssPx(4f) * scale;
        float padX = amllCssPx(8f) * scale;
        float iconCx = x + padX + iconW * 0.5f;
        float iconCy = y + h * 0.5f;
        drawIconRect(new String[]{ICON_LOSSLESS}, 30f, 20f, iconCx, iconCy, iconW, W88);
        renderer.drawTextCenteredY(label, x + padX + iconW + gap, y + h * 0.5f, tf, textSize, W88);
    }

    private void renderControls(float x, float y, float volumeY, float w, Typeface tf) {
        float buttonSize = amllMediaButtonSize(w);
        float hoverR = buttonSize * 0.5f;
        float toggleSize = amllMediaToggleIconSize();
        float transportSize = Math.min(amllMediaIconSize(32f, false), buttonSize * 0.92f);
        float playSize = Math.min(amllMediaIconSize(38f, true), buttonSize * 0.92f);
        toggleSize = Math.min(toggleSize, buttonSize * 0.58f);
        btnR = hoverR;
        float buttonsY = y + buttonSize * 0.5f;
        modeX = x + w * 0.09f;
        modeY = buttonsY;
        prevX = x + w * 0.295f;
        prevY = buttonsY;
        playX = x + w * 0.50f;
        playY = buttonsY;
        nextX = x + w * 0.705f;
        nextY = buttonsY;
        repeatX = x + w * 0.91f;
        repeatY = buttonsY;

        drawShuffleButton(modeX, modeY, toggleSize * controlScale(0), shuffleMode, controlHover[0] > 0.01f, hoverR * controlHoverRadius(0));
        drawPrevNext(prevX, prevY, transportSize * controlScale(1), false, controlHover[1] > 0.01f, hoverR * controlHoverRadius(1));
        drawPlayPause(playX, playY, playSize * controlScale(2), !MusicLoader.isPlaying(currentUiTrack), controlHover[2] > 0.01f, hoverR * controlHoverRadius(2));
        drawPrevNext(nextX, nextY, transportSize * controlScale(3), true, controlHover[3] > 0.01f, hoverR * controlHoverRadius(3));
        drawRepeatButton(repeatX, repeatY, toggleSize * controlScale(4), repeatMode, controlHover[4] > 0.01f, hoverR * controlHoverRadius(4));

        volume = MusicLoader.getVolume();
        float volumeMarginLeft = amllVolumeControlMarginLeft();
        float volumeMarginRight = amllVolumeControlMarginRight();
        float volumeLeftIconW = amllVolumeMinIconWidth();
        float volumeRightIconW = amllVolumeMaxIconWidth();
        float volumeIconH = amllVolumeIconHeight();
        float volumeControlX = x + volumeMarginLeft;
        float volumeControlW = w - volumeMarginLeft - volumeMarginRight;
        volX = volumeControlX + volumeLeftIconW;
        volY = volumeY;
        volW = Math.max(1f, volumeControlW - volumeLeftIconW - volumeRightIconW);
        volH = amllVolumeControlHeight();
        float volCy = volY + volH * 0.5f;
        drawSpeaker(volumeControlX + volumeLeftIconW * 0.5f, volCy, volumeIconH * 0.5f, AMLL_VOLUME);
        float minH = amllSliderInnerMinHeight();
        float maxH = amllSliderInnerMaxHeight();
        float volVisualH = minH + (maxH - minH) * volumeVisual;
        float volVisualY = volY + (volH - volVisualH) * 0.5f;
        float volVisualX = volX + volumeBounceX;
        renderer.drawRoundedRect(volVisualX, volVisualY, volW, volVisualH, volVisualH / 2f, AMLL_VOLUME_BG);
        if (volume > 0f) renderer.drawRoundedRect(volVisualX, volVisualY, Math.max(volVisualH, volW * volume), volVisualH, volVisualH / 2f, withAlpha(WHITE, 0.40f));
        drawSpeakerWithWaves(volumeControlX + volumeControlW - volumeRightIconW * 0.5f, volCy, volumeIconH * 0.5f, AMLL_VOLUME);
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
        float gap = amllBottomControlsGap();
        lyricToggleX = x - size - gap;
        lyricToggleY = y;
        lyricToggleW = size;
        lyricToggleH = size;
        listX = x;
        listY = y;
        listW = size;
        listH = size;
        drawLyricsToggleButton(lyricToggleX, lyricToggleY, size, s, lyricsVisible);
        drawPlaylistToggleButton(listX, listY, size, s, playlistOpen);
    }

    private void drawLyricsToggleButton(float x, float y, float size, float s, boolean active) {
        int color = active ? W88 : W46;
        String[] paths = active ? new String[]{ICON_LYRICS_ON_AMLL_0, ICON_LYRICS_ON_AMLL_1} : new String[]{ICON_LYRICS_OFF_AMLL};
        drawIconRect(paths, 64f, 64f, x + size * 0.5f, y + size * 0.5f, size, color);
    }

    private void drawPlaylistToggleButton(float x, float y, float size, float s, boolean active) {
        int color = active ? W88 : W46;
        String[] paths = active ? new String[]{ICON_PLAYLIST_ON_AMLL} : new String[]{ICON_PLAYLIST_OFF_AMLL};
        drawIcon(paths, 64f, x + size * 0.5f, y + size * 0.5f, size, color);
    }

    private void renderLyrics(float x, float y, float w, float h, Typeface tf, int active, float scrollBase, float s) {
        float activePreferred = amllLyricFontSize();
        float inactivePreferred = amllInactiveLyricFontSize(activePreferred);
        ensureLyricCache(tf, w, activePreferred, inactivePreferred, s);

        Canvas c = renderer.canvas();
        c.save();
        float padX = amllLyricLinePaddingX(activePreferred);
        c.clipRect(Rect.makeXYWH(x - padX, lyricsLayerY, w + padX * 2f, lyricsLayerH));
        for (int i = 0; i < lyricCache.size(); i++) {
            CachedLyricLine cached = lyricCache.get(i);
            if (cached == null || !cached.initialized) continue;
            drawAMLLLyricLineToLayer(c, cached, i, active, x, w);
        }
        blitInterludeDotsToLayer(c, s, x - padX, lyricsLayerY, w + padX * 2f, lyricsLayerH);
        c.restore();
    }

    private void drawAMLLLyricLineToLayer(Canvas c, CachedLyricLine line, int index, int active, float x, float w) {
        boolean isActive = isActiveLine(index, active);
        Image image = isActive ? line.activeImage : line.inactiveImage;
        if (image == null) return;
        float alpha = clamp(line.currentOpacity, 0f, 1f);
        if (alpha <= 0.01f) return;
        float scale = clamp(line.currentScale, 0.86f, 1.08f);
        float texW = isActive ? line.activeW : line.inactiveW;
        float texH = isActive ? line.activeH : line.inactiveH;
        float pad = isActive ? line.activePad : line.inactivePad;
        float contentH = Math.max(1f, texH - pad * 2f);
        float rawX = x - pad;
        if (line.lineRef != null && line.lineRef.isDuet()) rawX = x + w - texW + pad;
        float rawY = line.currentY - pad;
        float originX = x;
        float originY = line.currentY + contentH * 0.5f;
        float drawX = originX + (rawX - originX) * scale;
        float drawY = originY + (rawY - originY) * scale;
        float drawW = texW * scale;
        float drawH = texH * scale;
        alpha *= amllLyricEdgeMaskAlpha(drawY, drawH, layoutLyricTop, layoutLyricH);
        if (alpha <= 0.01f) return;
        float blurLevel = line.currentBlurVisual;
        try (Paint paint = new Paint()) {
            paint.setAntiAlias(true);
            paint.setColor(withAlpha(WHITE, alpha));
            			int targetStep = amllBlurStep(blurLevel);
            			boolean alreadyBaked = line.bakedBlurImage != null && line.bakedBlurStep == targetStep;
            			boolean canBake = alreadyBaked || bakedBlurBudgetThisFrame < 1;
            			if (shouldUseAMLLLineBlur(isActive, blurLevel) && canBake && ensureBakedBlur(line, image, texW, texH, blurLevel)) {
            				if (!alreadyBaked) bakedBlurBudgetThisFrame++;
            				float bakedPad = line.bakedBlurPad * scale;
            				c.drawImageRect(line.bakedBlurImage, Rect.makeXYWH(drawX - bakedPad, drawY - bakedPad, line.bakedBlurW * scale, line.bakedBlurH * scale), paint);
            			} else {
            				c.drawImageRect(image, Rect.makeXYWH(drawX, drawY, drawW, drawH), paint);
            			}
        }
    }

    private boolean ensureBakedBlur(CachedLyricLine line, Image source, float sourceW, float sourceH, float blurLevel) {
        if (line == null || source == null) return false;
        int step = amllBlurStep(blurLevel);
        if (step <= 0) return false;
        float radius = amllBlurRadiusForStep(step);
        float pad = Math.max(2f, radius * 3f);
        float logicalW = Math.max(1f, sourceW + pad * 2f);
        float logicalH = Math.max(1f, sourceH + pad * 2f);
        if (line.bakedBlurImage != null && line.bakedBlurRenderer != null && line.bakedBlurStep == step && Math.abs(line.bakedBlurW - logicalW) < 0.5f && Math.abs(line.bakedBlurH - logicalH) < 0.5f && Math.abs(line.bakedBlurPad - pad) < 0.05f) return true;

        line.clearBakedBlur();
        int imgW = Math.max(1, Math.round(logicalW * guiScale));
        int imgH = Math.max(1, Math.round(logicalH * guiScale));
        SkijaRenderer bakedRenderer = new SkijaRenderer("music_page_lyric_blur", imgW, imgH);
        boolean transferred = false;
        try {
            bakedRenderer.clear(0x00000000);
            Canvas bc = bakedRenderer.canvas();
            bc.save();
            bc.scale(guiScale, guiScale);
            try (Paint paint = new Paint(); ImageFilter blur = ImageFilter.makeBlur(radius, radius, FilterTileMode.DECAL)) {
                paint.setAntiAlias(true);
                paint.setColor(WHITE);
                paint.setImageFilter(blur);
                bc.drawImageRect(source, Rect.makeXYWH(pad, pad, sourceW, sourceH), paint);
            }
            bc.restore();
            bakedRenderer.upload();
            line.bakedBlurRenderer = bakedRenderer;
            line.bakedBlurImage = bakedRenderer.getSurface().makeImageSnapshot();
            line.bakedBlurW = logicalW;
            line.bakedBlurH = logicalH;
            line.bakedBlurPad = pad;
            line.bakedBlurStep = step;
            transferred = true;
            return true;
        } finally {
            if (!transferred) bakedRenderer.close();
        }
    }

    private float getLineContentHeight(int index, int activeIndex, float s) {
        CachedLyricLine cached = index >= 0 && index < lyricCache.size() ? lyricCache.get(index) : null;
        if (cached == null) return 30f * s;
        if (index == activeIndex) {
            return Math.max(1f, cached.activeH - cached.activePad * 2f);
        } else {
            return Math.max(1f, cached.inactiveH - cached.inactivePad * 2f);
        }
    }

    private boolean isAttachedBgLine(int index) {
        if (index <= 0 || index >= lyricLines.size()) return false;
        LyricLine line = lyricLines.get(index);
        LyricLine prev = lyricLines.get(index - 1);
        return line.isBG() && !prev.isBG() && Math.abs(line.startTime() - prev.startTime()) < 0.001f;
    }

    private int attachedBgIndex(int mainIndex) {
        int bgIndex = mainIndex + 1;
        return bgIndex < lyricLines.size() && isAttachedBgLine(bgIndex) ? bgIndex : -1;
    }

    private int groupMainIndex(int index) {
        return isAttachedBgLine(index) ? index - 1 : index;
    }

    private float getLyricGroupHeight(int index, int activeIndex, float s) {
        int mainIndex = groupMainIndex(index);
        float mainH = getLineContentHeight(mainIndex, activeIndex, s);
        int bgIndex = attachedBgIndex(mainIndex);
        if (bgIndex < 0) return mainH;
        float bgH = getLineContentHeight(bgIndex, activeIndex, s);
        boolean groupActive = isActiveLine(mainIndex, activeIndex) || isActiveLine(bgIndex, activeIndex);
        return mainH + (groupActive ? bgH : 0f);
    }

    private boolean ensureLyricCache(Typeface tf, float maxWidth, float activePreferred, float inactivePreferred, float s) {
        if (tf == null) return false;
        if (lyricCache.size() == lyricLines.size() && Math.abs(lyricCacheWidth - maxWidth) < 0.5f && Math.abs(lyricCacheScale - s) < 0.001f && Math.abs(lyricCacheActiveSize - activePreferred) < 0.001f && Math.abs(lyricCacheInactiveSize - inactivePreferred) < 0.001f && lyricCacheTypeface == tf) return false;
        clearLyricCache();
        lyricCacheWidth = maxWidth;
        lyricCacheScale = s;
        lyricCacheActiveSize = activePreferred;
        lyricCacheInactiveSize = inactivePreferred;
        lyricCacheTypeface = tf;
        for (LyricLine line : lyricLines) {
            String text = line.text();
            float baseSize = line.isBG() ? activePreferred * 0.7f : activePreferred;
            float activeSize = fitLyricTextSize(text, tf, baseSize, 12f, maxWidth);
            float inactiveSize = activeSize;
            TextImage active = createLyricTextImage(line, tf, activeSize, WHITE, 0f, false, maxWidth, false, 0f);
            TextImage inactive = createLyricTextImage(line, tf, inactiveSize, WHITE, 0f, false, maxWidth, false, 0f);
            TextImage hover = createLyricTextImage(line, tf, inactiveSize, WHITE, 0f, false, maxWidth, false, 0f);
            CachedLyricLine cached = new CachedLyricLine(active, inactive, hover);
            cached.lineRef = line;
            if (line.isDynamic()) {
                cached.setDynamicWordRanges(measureDynamicWordRanges(line, tf, activeSize, maxWidth));
                TextImage white = createLyricTextImage(line, tf, activeSize, WHITE, 0f, false, maxWidth, true, active.pad);
                cached.whiteImage = white.image;
            }
            lyricCache.add(cached);
        }
        return true;
    }

    private java.util.List<WordRange> measureDynamicWordRanges(LyricLine line, Typeface tf, float size, float maxWidth) {
        java.util.List<WordRange> ranges = new ArrayList<>();
        if (line.words() == null || line.words().isEmpty()) return ranges;
        float linePadX = amllLyricLinePaddingX(size);
        float linePadY = amllLyricLinePaddingY(size);
        boolean hasRuby = hasRubyWords(line);
        String romanText = effectiveRomanText(line);
        float rubySize = size * 0.5f;
        float transSize = size * 0.75f;
        float textMaxWidth = amllLyricTextMaxWidth(maxWidth, size);
        String[] wrappedLines = wrapText(line.text(), tf, size, textMaxWidth);
        String[] transLines = line.translation() == null || line.translation().isBlank() ? new String[0] : wrapText(line.translation(), tf, transSize, textMaxWidth);
        String[] romanLines = romanText.isEmpty() ? new String[0] : wrapText(romanText, tf, transSize, textMaxWidth);
        float baseLineH;
        float rubyLineH;
        float subLineH;
        try (Font font = new Font(tf, size); Font rubyFont = new Font(tf, rubySize); Font subFont = new Font(tf, transSize)) {
            baseLineH = Math.max(1f, (font.getMetrics().getDescent() - font.getMetrics().getAscent()) * 1.20f);
            rubyLineH = hasRuby ? Math.max(1f, (rubyFont.getMetrics().getDescent() - rubyFont.getMetrics().getAscent()) * 1.05f) : 0f;
            subLineH = Math.max(1f, (subFont.getMetrics().getDescent() - subFont.getMetrics().getAscent()) * 1.20f);
        }
        float mainBlockLineH = baseLineH + rubyLineH;
        float romanY = -1f;
        float romanH = 0f;
        float romanW = 0f;
        float maxLineW = 1f;
        for (String wrappedLine : wrappedLines) maxLineW = Math.max(maxLineW, renderer.measureText(wrappedLine, tf, size));
        for (String transLine : transLines) maxLineW = Math.max(maxLineW, renderer.measureText(transLine, tf, transSize));
        if (romanLines.length > 0) {
            romanY = linePadY + mainBlockLineH * wrappedLines.length + linePadY * 0.5f + subLineH * transLines.length;
            romanH = subLineH * romanLines.length;
            for (String romanLine : romanLines) {
                romanW = Math.max(romanW, renderer.measureText(romanLine, tf, transSize));
                maxLineW = Math.max(maxLineW, renderer.measureText(romanLine, tf, transSize));
            }
        }
        Map<Integer, RomanWordBox> romanWordBoxes = measureRomanWordBoxes(line, tf, transSize, linePadX, romanY, subLineH, textMaxWidth, maxLineW, romanLines);
        int lineIndex = 0;
        float x = linePadX;
        float currentLineWidth = wrappedLines.length > 0 ? renderer.measureText(wrappedLines[0], tf, size) : 0f;
        int wordIndex = 0;
        int wordCount = line.words().size();
        for (LyricWord word : line.words()) {
            String text = word.word() == null ? "" : word.word();
            float wordW = Math.max(0f, renderer.measureText(text, tf, size));
            if (lineIndex < wrappedLines.length && x > linePadX && x + wordW - linePadX > currentLineWidth + 0.5f) {
                lineIndex++;
                x = linePadX;
                currentLineWidth = lineIndex < wrappedLines.length ? renderer.measureText(wrappedLines[lineIndex], tf, size) : textMaxWidth;
            }
            float blockY = linePadY + mainBlockLineH * lineIndex;
            float rubyY = hasRuby ? blockY : -1f;
            float baseY = blockY + rubyLineH;
            boolean emphasize = shouldAMLLWordEmphasize(text, word.endTime() - word.startTime());
            RomanWordBox romanBox = nearestRomanWordBox(romanWordBoxes, wordIndex, wordCount);
            java.util.List<EmphasisSlice> emphasisSlices = measureEmphasisSlices(text, tf, size, x, x + wordW);
            ranges.add(new WordRange(
                    word.startTime(), word.endTime(), x, x + wordW,
                    blockY, mainBlockLineH, baseY, baseLineH, rubyY, rubyLineH,
                    romanY, romanH, linePadX, linePadX + romanW,
                    romanBox == null ? -1f : romanBox.startX(),
                    romanBox == null ? -1f : romanBox.endX(),
                    romanBox == null ? -1f : romanBox.y(),
                    romanBox == null ? 0f : romanBox.h(),
                    text, emphasisSlices,
                    lineIndex, wordIndex, wordCount, emphasize,
                    word.ruby() == null ? List.of() : word.ruby()));
            x += wordW;
            wordIndex++;
        }
        return ranges;
    }

    private java.util.List<EmphasisSlice> measureEmphasisSlices(String text, Typeface tf, float size, float startX, float endX) {
        ArrayList<EmphasisSlice> slices = new ArrayList<>();
        if (text == null || text.isBlank()) {
            slices.add(new EmphasisSlice(startX, endX));
            return slices;
        }
        ArrayList<EmphasisSlice> raw = new ArrayList<>();
        float cursor = 0f;
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            int len = Character.charCount(cp);
            String part = text.substring(i, i + len);
            float w = Math.max(0f, renderer.measureText(part, tf, size));
            if (!Character.isWhitespace(cp)) raw.add(new EmphasisSlice(cursor, cursor + w));
            cursor += w;
            i += len;
        }
        if (raw.isEmpty() || cursor <= 0f) {
            slices.add(new EmphasisSlice(startX, endX));
            return slices;
        }
        float scale = Math.max(0f, endX - startX) / cursor;
        for (EmphasisSlice slice : raw) {
            slices.add(new EmphasisSlice(startX + slice.startX() * scale, startX + slice.endX() * scale));
        }
        return slices;
    }

    private RomanWordBox nearestRomanWordBox(Map<Integer, RomanWordBox> boxes, int wordIndex, int wordCount) {
        RomanWordBox direct = boxes.get(wordIndex);
        if (direct != null) return direct;
        for (int d = 1; d < wordCount; d++) {
            RomanWordBox before = boxes.get(wordIndex - d);
            if (before != null) return before;
            RomanWordBox after = boxes.get(wordIndex + d);
            if (after != null) return after;
        }
        return null;
    }

    private Map<Integer, RomanWordBox> measureRomanWordBoxes(LyricLine line, Typeface tf, float romanSize, float linePadX, float romanY, float romanLineH, float textMaxWidth, float maxLineW, String[] romanLines) {
        Map<Integer, RomanWordBox> result = new HashMap<>();
        if (line == null || line.words() == null || romanLines == null || romanLines.length == 0 || romanY < 0f) return result;
        int romanLineIndex = 0;
        float x = 0f;
        float currentLineWidth = romanLines.length > 0 ? renderer.measureText(romanLines[0], tf, romanSize) : textMaxWidth;
        boolean hasRomanBefore = false;
        for (int i = 0; i < line.words().size(); i++) {
            LyricWord word = line.words().get(i);
            String roman = word.romanWord() == null ? "" : word.romanWord().trim();
            if (roman.isEmpty()) continue;
            boolean leadingSpace = hasRomanBefore;
            float wordW = renderer.measureText(roman, tf, romanSize);
            float spaceW = leadingSpace ? renderer.measureText(" ", tf, romanSize) : 0f;
            float tokenW = wordW + spaceW;
            if (romanLineIndex < romanLines.length && x > 0f && x + tokenW > currentLineWidth + 0.5f) {
                romanLineIndex++;
                x = 0f;
                leadingSpace = false;
                spaceW = 0f;
                tokenW = wordW;
                currentLineWidth = romanLineIndex < romanLines.length ? renderer.measureText(romanLines[romanLineIndex], tf, romanSize) : textMaxWidth;
            }
            float lineStartX = linePadX;
            if (line.isDuet()) lineStartX = maxLineW + linePadX - currentLineWidth;
            float startX = lineStartX + x + spaceW;
            float y = romanY + romanLineH * romanLineIndex;
            result.put(i, new RomanWordBox(startX, startX + wordW, y, romanLineH));
            x += tokenW;
            hasRomanBefore = true;
        }
        return result;
    }

    private String effectiveRomanText(LyricLine line) {
        if (line == null) return "";
        if (line.romanization() != null && !line.romanization().isBlank()) return line.romanization();
        if (line.words() == null || line.words().isEmpty()) return "";
        StringBuilder roman = new StringBuilder();
        for (LyricWord word : line.words()) {
            if (word.romanWord() == null || word.romanWord().isBlank()) continue;
            if (!roman.isEmpty()) roman.append(' ');
            roman.append(word.romanWord().trim());
        }
        return roman.toString();
    }

    private String effectiveRubyText(LyricLine line) {
        if (line == null || line.words() == null || line.words().isEmpty()) return "";
        StringBuilder ruby = new StringBuilder();
        for (LyricWord word : line.words()) {
            if (word.ruby() == null || word.ruby().isEmpty()) continue;
            for (RubyText rt : word.ruby()) {
                if (rt.text() == null || rt.text().isBlank()) continue;
                if (!ruby.isEmpty()) ruby.append(' ');
                ruby.append(rt.text().trim());
            }
        }
        return ruby.toString();
    }

    private boolean hasRubyWords(LyricLine line) {
        if (line == null || line.words() == null) return false;
        for (LyricWord word : line.words()) {
            if (word.ruby() != null && !word.ruby().isEmpty()) return true;
        }
        return false;
    }

    private void drawRubyForMainLine(Canvas sc, LyricLine line, Typeface tf, float rubySize, Font rubyFont, Paint paint, int targetLineIndex, float lineStartX, float rubyY, String[] mainLines, float textMaxWidth) {
        if (line.words() == null || line.words().isEmpty()) return;
        int lineIndex = 0;
        float x = 0f;
        float mainSize = rubySize * 2f;
        float currentLineWidth = mainLines.length > 0 ? renderer.measureText(mainLines[0], tf, mainSize) : textMaxWidth;
        int oldColor = paint.getColor();
        paint.setColor(withAlpha(oldColor, alphaOf(oldColor) * 0.8f));
        for (LyricWord word : line.words()) {
            String text = word.word() == null ? "" : word.word();
            float wordW = renderer.measureText(text, tf, mainSize);
            if (lineIndex < mainLines.length && x > 0f && x + wordW > currentLineWidth + 0.5f) {
                lineIndex++;
                x = 0f;
                currentLineWidth = lineIndex < mainLines.length ? renderer.measureText(mainLines[lineIndex], tf, mainSize) : textMaxWidth;
            }
            if (lineIndex == targetLineIndex && word.ruby() != null && !word.ruby().isEmpty()) {
                String ruby = rubyTextForWord(word);
                float rubyW = renderer.measureText(ruby, tf, rubySize);
                float rubyX = lineStartX + x + (wordW - rubyW) * 0.5f;
                sc.drawString(ruby, rubyX, rubyY - rubyFont.getMetrics().getAscent(), rubyFont, paint);
            }
            x += wordW;
        }
        paint.setColor(oldColor);
    }

    private String rubyTextForWord(LyricWord word) {
        if (word == null || word.ruby() == null || word.ruby().isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (RubyText rt : word.ruby()) {
            if (rt.text() == null || rt.text().isBlank()) continue;
            result.append(rt.text().trim());
        }
        return result.toString();
    }

    private boolean shouldAMLLWordEmphasize(String word, float durationSeconds) {
        if (word == null) return false;
        if (containsCjk(word)) return durationSeconds >= 1f;
        int len = word.trim().length();
        return durationSeconds >= 1f && len <= 7 && len > 1;
    }

    private TextImage createLyricTextImage(LyricLine lyricLine, Typeface tf, float size, int color, float blurSigma, boolean drawFaintCore, float maxWidth, boolean isWhiteMask, float activePadOverride) {
        String mainText = lyricLine.text() == null ? "" : lyricLine.text();
        String transText = lyricLine.translation() == null ? "" : lyricLine.translation();
        String romanText = effectiveRomanText(lyricLine);
        boolean hasRuby = hasRubyWords(lyricLine);
        float linePadX = amllLyricLinePaddingX(size);
        float linePadY = amllLyricLinePaddingY(size);
        float textMaxWidth = amllLyricTextMaxWidth(maxWidth, size);
        String[] mainLines = wrapText(mainText, tf, size, textMaxWidth);
        float rubySize = size * 0.5f;
        float transSize = size * 0.75f;
        String[] transLines = transText.isEmpty() ? new String[0] : wrapText(transText, tf, transSize, textMaxWidth);
        String[] romanLines = romanText.isEmpty() ? new String[0] : wrapText(romanText, tf, transSize, textMaxWidth);
        float scale = Math.max(1f, guiScale);
        try (Font font = new Font(tf, size * scale); Font rubyFont = new Font(tf, rubySize * scale); Font transFont = new Font(tf, transSize * scale)) {
            float lineH = Math.max(1f, (font.getMetrics().getDescent() - font.getMetrics().getAscent()) / scale * 1.20f);
            float rubyLineH = Math.max(1f, (rubyFont.getMetrics().getDescent() - rubyFont.getMetrics().getAscent()) / scale * 1.05f);
            float transLineH = Math.max(1f, (transFont.getMetrics().getDescent() - transFont.getMetrics().getAscent()) / scale * 1.20f);
            float maxLineW = 1f;
            for (String line : mainLines) maxLineW = Math.max(maxLineW, renderer.measureText(line, tf, size));
            for (String line : transLines) maxLineW = Math.max(maxLineW, renderer.measureText(line, tf, transSize));
            for (String line : romanLines) maxLineW = Math.max(maxLineW, renderer.measureText(line, tf, transSize));
            float blurPad = blurSigma > 0f ? Math.max(10f, blurSigma * (drawFaintCore ? 3.0f : 5.5f)) : 0f;
            float pad = activePadOverride > 0f ? activePadOverride : (blurPad + 2f);
            float textX = pad + linePadX;
            float textY = pad + linePadY;
            boolean alignRight = lyricLine.isDuet();
            float logicalW = maxLineW + linePadX * 2f + pad * 2f;
            float mainBlockLineH = lineH + (hasRuby ? rubyLineH : 0f);
            float logicalH = mainBlockLineH * mainLines.length + transLineH * (transLines.length + romanLines.length) + ((transLines.length + romanLines.length) > 0 ? linePadY * 0.5f : 0f) + linePadY * 2f + pad * 2f;
            int imgW = Math.max(1, Math.round(logicalW * scale));
            int imgH = Math.max(1, Math.round(logicalH * scale));
            SkijaRenderer textRenderer = new SkijaRenderer("music_page_lyric_line", imgW, imgH);
            textRenderer.clear(0x00000000);
            Canvas sc = textRenderer.canvas();
            sc.save();
            sc.scale(scale, scale);
            try (Font logicalFont = new Font(tf, size); Font logicalRubyFont = new Font(tf, rubySize); Font logicalTransFont = new Font(tf, transSize); Paint paint = new Paint()) {
                paint.setColor(color);
                paint.setAntiAlias(true);
                Runnable drawTexts = () -> {
                    float curY = textY;
                    for (int lineIndex = 0; lineIndex < mainLines.length; lineIndex++) {
                        String line = mainLines[lineIndex];
                        float lineStartX = textX;
                        if (alignRight) lineStartX = logicalW - pad - linePadX - renderer.measureText(line, tf, size);
                        if (hasRuby) {
                            drawRubyForMainLine(sc, lyricLine, tf, rubySize, logicalRubyFont, paint, lineIndex, lineStartX, curY, mainLines, textMaxWidth);
                            curY += rubyLineH;
                        }
                        sc.drawString(line, lineStartX, curY - logicalFont.getMetrics().getAscent(), logicalFont, paint);
                        curY += lineH;
                    }
                    if (transLines.length > 0 || romanLines.length > 0) {
                        curY += linePadY * 0.5f;
                        int oldColor = paint.getColor();
                        paint.setColor(withAlpha(oldColor, alphaOf(oldColor) * 0.8f));
                        for (String line : transLines) {
                            float curX = textX;
                            if (alignRight) curX = logicalW - pad - linePadX - renderer.measureText(line, tf, transSize);
                            sc.drawString(line, curX, curY - logicalTransFont.getMetrics().getAscent(), logicalTransFont, paint);
                            curY += transLineH;
                        }
                        for (String line : romanLines) {
                            float curX = textX;
                            if (alignRight) curX = logicalW - pad - linePadX - renderer.measureText(line, tf, transSize);
                            sc.drawString(line, curX, curY - logicalTransFont.getMetrics().getAscent(), logicalTransFont, paint);
                            curY += transLineH;
                        }
                        paint.setColor(oldColor);
                    }
                };
                if (isWhiteMask) {
                    drawTexts.run();
                } else if (blurSigma > 0f) {
                    if (!drawFaintCore) {
                        try (MaskFilter outerBlur = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurSigma * 2.15f, false)) {
                            paint.setMaskFilter(outerBlur);
                            paint.setColor(0x34FFFFFF);
                            drawTexts.run();
                        }
                    }
                    try (MaskFilter blur = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurSigma, false)) {
                        paint.setMaskFilter(blur);
                        paint.setColor(drawFaintCore ? color : 0xA6FFFFFF);
                        drawTexts.run();
                    }
                    paint.setMaskFilter(null);
                    paint.setColor(drawFaintCore ? 0x70FFFFFF : color);
                    drawTexts.run();
                } else {
                    drawTexts.run();
                }
            }
            sc.restore();
            textRenderer.upload();
            Image image = textRenderer.getSurface().makeImageSnapshot();
            return new TextImage(image, textRenderer, logicalW, logicalH, pad);
        }
    }

    private void drawTextLines(Canvas c, String[] lines, float x, float y, Font font, Paint paint, float lineH) {
        float ascent = font.getMetrics().getAscent();
        for (int i = 0; i < lines.length; i++) {
            c.drawString(lines[i], x, y + i * lineH - ascent, font, paint);
        }
    }

    private String[] wrapText(String text, Typeface tf, float size, float maxWidth) {
        String normalized = normalizeLyricText(text);
        if (normalized.isEmpty()) return new String[]{""};
        if (renderer.measureText(normalized, tf, size) <= maxWidth) return new String[]{normalized};
        List<TextSegment> segments = lyricTextSegments(normalized, tf, size);
        List<Integer> breaks = balancedLyricBreaks(segments, maxWidth);
        if (breaks.isEmpty()) return new String[]{normalized};
        Set<Integer> breakSet = new HashSet<>(breaks);
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (breakSet.contains(i) && !cur.isEmpty()) {
                String line = trimLyricDrawLine(cur.toString());
                if (!line.isEmpty()) lines.add(line);
                cur.setLength(0);
            }
            cur.append(segments.get(i).text());
        }
        String last = trimLyricDrawLine(cur.toString());
        if (!last.isEmpty()) lines.add(last);
        return lines.isEmpty() ? new String[]{normalized} : lines.toArray(String[]::new);
    }

    private float fitLyricTextSize(String text, Typeface tf, float preferredSize, float minSize, float maxWidth) {
        String normalized = normalizeLyricText(text);
        float size = preferredSize;
        while (size > minSize && longestLyricSegmentWidth(normalized, tf, size) > amllLyricTextMaxWidth(maxWidth, size)) {
            size -= 1.4f;
        }
        return Math.max(minSize, size);
    }

    private float longestLyricSegmentWidth(String text, Typeface tf, float size) {
        String normalized = normalizeLyricText(text);
        if (normalized.isEmpty()) return 0f;
        float result = 0f;
        for (TextSegment segment : lyricTextSegments(normalized, tf, size)) {
            if (!segment.isSpace()) result = Math.max(result, segment.width());
        }
        return result;
    }

    private String normalizeLyricText(String text) {
        if (text == null) return "";
        return LYRIC_SPACE_PATTERN.matcher(text.strip()).replaceAll(" ");
    }

    private String trimLyricDrawLine(String text) {
        if (text == null) return "";
        return text.strip();
    }

    private List<TextSegment> lyricTextSegments(String text, Typeface tf, float size) {
        ArrayList<TextSegment> result = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String part = text.substring(start, end);
            if (part.isEmpty()) continue;
            appendLyricTextSegment(result, part, tf, size);
        }
        if (result.isEmpty()) result.add(new TextSegment(text, renderer.measureText(text, tf, size), text.isBlank()));
        return result;
    }

    private void appendLyricTextSegment(List<TextSegment> result, String part, Typeface tf, float size) {
        if (part.isBlank()) {
            result.add(new TextSegment(part, renderer.measureText(part, tf, size), true));
            return;
        }
        if (!containsCjk(part)) {
            result.add(new TextSegment(part, renderer.measureText(part, tf, size), false));
            return;
        }
        StringBuilder pending = new StringBuilder();
        for (int offset = 0; offset < part.length();) {
            int cp = part.codePointAt(offset);
            int len = Character.charCount(cp);
            String unit = part.substring(offset, offset + len);
            if (isCjkCodePoint(cp)) {
                if (!pending.isEmpty()) {
                    String pendingText = pending.toString();
                    result.add(new TextSegment(pendingText, renderer.measureText(pendingText, tf, size), false));
                    pending.setLength(0);
                }
                result.add(new TextSegment(unit, renderer.measureText(unit, tf, size), false));
            } else {
                pending.append(unit);
            }
            offset += len;
        }
        if (!pending.isEmpty()) {
            String pendingText = pending.toString();
            result.add(new TextSegment(pendingText, renderer.measureText(pendingText, tf, size), false));
        }
    }

    private List<Integer> balancedLyricBreaks(List<TextSegment> segments, float maxWidth) {
        int n = segments.size();
        ArrayList<Integer> result = new ArrayList<>();
        if (n <= 1 || maxWidth <= 1f) return result;
        double[] prefixWidth = new double[n + 1];
        for (int i = 0; i < n; i++) prefixWidth[i + 1] = prefixWidth[i] + segments.get(i).width();
        if (prefixWidth[n] <= maxWidth) return result;
        double[] dp = new double[n + 1];
        int[] nextBreak = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            dp[i] = Double.POSITIVE_INFINITY;
            nextBreak[i] = -1;
        }
        dp[n] = 0.0;
        double cjkPenalty = Math.pow(maxWidth * LYRIC_CJK_BREAK_PENALTY_RATIO, 2.0);
        double normalPenalty = Math.pow(maxWidth * LYRIC_NORMAL_BREAK_PENALTY_RATIO, 2.0);
        for (int i = n - 1; i >= 0; i--) {
            for (int j = i + 1; j <= n; j++) {
                double lineW = prefixWidth[j] - prefixWidth[i];
                double lineCost;
                if (lineW > maxWidth) {
                    if (j == i + 1) lineCost = Math.pow(lineW - maxWidth, 2.0) * LYRIC_OVERFLOW_PENALTY_MULTIPLIER;
                    else continue;
                } else {
                    lineCost = Math.pow(maxWidth - lineW, 2.0);
                }
                double breakPenalty = 0.0;
                if (j < n) {
                    TextSegment prev = segments.get(j - 1);
                    if (endsWithLyricPunctuation(prev.text())) breakPenalty = -Math.pow(maxWidth * LYRIC_PUNCTUATION_BREAK_REWARD_RATIO, 2.0);
                    else if (prev.isSpace()) breakPenalty = -Math.pow(maxWidth * LYRIC_SPACE_BREAK_REWARD_RATIO, 2.0);
                    else if (isCjkBreakBoundary(segments, j)) breakPenalty = cjkPenalty;
                    else breakPenalty = normalPenalty;
                }
                double total = lineCost + breakPenalty + dp[j];
                if (total < dp[i]) {
                    dp[i] = total;
                    nextBreak[i] = j;
                }
            }
        }
        int cur = 0;
        while (cur < n) {
            int next = nextBreak[cur];
            if (next <= cur || next > n) break;
            if (next < n) result.add(next);
            cur = next;
        }
        return result;
    }

    private boolean endsWithLyricPunctuation(String text) {
        if (text == null || text.isEmpty()) return false;
        String stripped = text.stripTrailing();
        if (stripped.isEmpty()) return false;
        int cp = stripped.codePointBefore(stripped.length());
        return LYRIC_BREAK_PUNCTUATION.indexOf(cp) >= 0;
    }

    private boolean isCjkBreakBoundary(List<TextSegment> segments, int index) {
        if (index <= 0 || index >= segments.size()) return false;
        return containsCjk(segments.get(index - 1).text()) || containsCjk(segments.get(index).text());
    }

    private boolean containsCjk(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            if (isCjkCodePoint(cp)) return true;
            offset += Character.charCount(cp);
        }
        return false;
    }

    private boolean isCjkCodePoint(int cp) {
        return Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN || (cp >= 0x0800 && cp <= 0x9FFC);
    }

    private void clearLyricCache() {
        for (CachedLyricLine line : lyricCache) line.close();
        lyricCache.clear();
        lyricCacheWidth = -1f;
        lyricCacheScale = -1f;
        lyricCacheActiveSize = -1f;
        lyricCacheInactiveSize = -1f;
        lyricCacheTypeface = null;
    }

    private boolean useLineLyricTextures() {
        return true;
    }

    private void updateLyricAnimation(int active, float currentSeconds, float s, float dt) {
        SkijaTestScreen.ensureFontLoaded();
        Typeface tf = SkijaTestScreen.curTf;
        if (tf != null && layoutRightW > 1f) {
            float activePreferred = amllLyricFontSize();
            float inactivePreferred = amllInactiveLyricFontSize(activePreferred);
            if (ensureLyricCache(tf, layoutRightW, activePreferred, inactivePreferred, s)) {
                lyricSnapOnNextRender = true;
                if (!useLineLyricTextures()) lyricsLayerDirty = true;
            }
        }
        boolean snap = lyricSnapOnNextRender;
        InterludeState interlude = computeInterlude(active, currentSeconds);
        float targetScroll = lyricScrollTarget(active, s, interlude);
        if (snap) {
            lyricScroll = targetScroll;
            lyricScrollVelocity = 0f;
        } else {
            updateLyricSpring(active, targetScroll, dt, interlude.active());
        }
        lyricLayerBlitOffsetY = useLineLyricTextures() ? 0f : lyricRenderScroll - lyricScroll;
        updateLyricLineAnimations(active, currentSeconds, s, dt, snap, interlude);
        if (!useLineLyricTextures() && lyricLayerAnimating()) lyricsLayerDirty = true;
        lyricSnapOnNextRender = false;
    }

    private boolean lyricLayerAnimating() {
        if (Math.abs(lyricScrollVelocity) > 0.01f) return true;
        if (interludeOpacity > 0.001f && currentUiTrack != null && MusicLoader.isPlaying(currentUiTrack)) return true;
        for (CachedLyricLine line : lyricCache) {
            if (line == null) continue;
            if (Math.abs(line.velocityY) > 0.01f) return true;
            if (Math.abs(line.velocityScale) > 0.0005f) return true;
            if (Math.abs(line.velocityOpacity) > 0.0005f) return true;
            if (Math.abs(line.velocityBlur) > 0.001f) return true;
        }
        return false;
    }

    private float lyricScrollTarget(int active, float s, InterludeState interlude) {
        		float target = 0f;
        		int count = Math.min(Math.max(active, 0), lyricLines.size());
        		float slotHeight = interlude.active() ? interludeSlotHeight(s) : 0f;
        		for (int i = 0; i < count; i++) {
        			if (isAttachedBgLine(i)) continue;
        			target += getLyricGroupHeight(i, active, s);
        			if (interlude.active() && i == interlude.anchor()) target += slotHeight;
        		}
        if (active >= 0 && active < lyricLines.size()) target += getLyricGroupHeight(active, active, s) * 0.5f;
        return target;
    }

    private SpringParams lyricSpringParams(int active, boolean isInterludeActive) {
        if (isInterludeActive) return new SpringParams(90f, 15f);
        float stiffness = 90f;
        float damping = 15f;
        if (active > 0 && active < lyricLines.size()) {
            float intervalMs = (lyricLines.get(active).startTime() - lyricLines.get(active - 1).startTime()) * 1000f;
            float clampedInterval = clamp(intervalMs, 100f, 800f);
            float ratio = 1f - (clampedInterval - 100f) / 700f;
            ratio = (float) Math.pow(ratio, 0.2f);
            stiffness = 170f + ratio * 50f;
            damping = (float) Math.sqrt(stiffness) * 2.2f;
        }
        return new SpringParams(stiffness, damping);
    }

    private void updateLyricSpring(int active, float targetScroll, float dt, boolean isInterludeActive) {
        float safeDt = clamp(dt, 0f, 0.05f);
        SpringParams params = lyricSpringParams(active, isInterludeActive);
        float displacement = targetScroll - lyricScroll;
        lyricScrollVelocity += displacement * params.stiffness() * safeDt;
        lyricScrollVelocity *= (float) Math.exp(-params.damping() * safeDt);
        lyricScroll += lyricScrollVelocity * safeDt;
        if (Math.abs(displacement) < 0.25f && Math.abs(lyricScrollVelocity) < 0.35f) {
            lyricScroll = targetScroll;
            lyricScrollVelocity = 0f;
        }
    }

    private void updateLyricLineAnimations(int active, float currentSeconds, float s, float dt, boolean snap, InterludeState interlude) {
        if (lyricCache.size() != lyricLines.size()) return;
        float safeDt = clamp(dt, 0f, 0.05f);
        SpringParams params = lyricSpringParams(active, interlude.active());
        float slotHeight = interlude.active() ? interludeSlotHeight(s) : 0f;
        boolean showDots = interlude.active();
        float anchorY = layoutLyricAnchorY > 0f ? layoutLyricAnchorY : layoutLyricTop + layoutLyricH * 0.35f;
        float baseY = anchorY - lyricScroll;
        float offsetY = 0f;
        boolean dotsPlaced = false;
        for (int i = 0; i < lyricCache.size(); i++) {
            if (isAttachedBgLine(i)) continue;
            if (interlude.active() && !dotsPlaced && i == interlude.anchor() + 1) {
                updateInterludeAnimation(showDots ? baseY + offsetY : interludeY, showDots, s, safeDt, snap);
                offsetY += slotHeight;
                dotsPlaced = true;
            }

            CachedLyricLine line = lyricCache.get(i);
            LyricPresentation presentation = computeLyricPresentation(i, active);
            float targetY = baseY + offsetY;
            boolean dynamicMask = isActiveLine(i, active) && line.lineRef != null && line.lineRef.isDynamic();
            updateCachedLyricLine(line, targetY, presentation, params, safeDt, snap, computeAMLLGroupBlur(i), dynamicMask);

            int bgIndex = attachedBgIndex(i);
            float groupH = getLineContentHeight(i, active, s);
            if (bgIndex >= 0) {
                CachedLyricLine bgLine = lyricCache.get(bgIndex);
                boolean groupActive = isActiveLine(i, active) || isActiveLine(bgIndex, active);
                float bgH = getLineContentHeight(bgIndex, active, s);
                float bgProgress = groupActive ? 1f : 0f;
                float bgSlideY = -80f * (1f - bgProgress);
                float bgTargetY = targetY + groupH + bgH * (bgSlideY / 100f);
                LyricPresentation bgPresentation = new LyricPresentation(groupActive ? 0.4f : 0.0001f, groupActive ? 1f : 0.8f);
                boolean bgDynamicMask = isActiveLine(bgIndex, active) && bgLine.lineRef != null && bgLine.lineRef.isDynamic();
                updateCachedLyricLine(bgLine, bgTargetY, bgPresentation, params, safeDt, snap, computeAMLLGroupBlur(bgIndex), bgDynamicMask);
                if (groupActive) groupH += bgH;
            }

            			offsetY += groupH;
            			if (interlude.active() && !dotsPlaced && i == interlude.anchor()) {
                updateInterludeAnimation(showDots ? baseY + offsetY : interludeY, showDots, s, safeDt, snap);
                offsetY += slotHeight;
                dotsPlaced = true;
            }
        }
        if (showDots) {
            interludeAnchor = interlude.anchor();
            interludeStartTime = interlude.start();
            interludeEndTime = interlude.end();
            interludeNextDuet = interlude.nextDuet();
            interludeCurrentTime = currentSeconds;
            if (!dotsPlaced) updateInterludeAnimation(baseY + offsetY, true, s, safeDt, snap);
        } else if (!dotsPlaced || interludeOpacity > 0.001f) {
            updateInterludeAnimation(interludeY, false, s, safeDt, snap);
        }
    }

    private void updateCachedLyricLine(CachedLyricLine line, float targetY, LyricPresentation presentation, SpringParams params, float safeDt, boolean snap, float targetBlur, boolean dynamicMask) {
        if (!line.initialized || snap) {
            line.currentY = targetY;
            line.velocityY = 0f;
            line.currentScale = presentation.scale();
            line.velocityScale = 0f;
            line.currentOpacity = presentation.opacity();
            line.velocityOpacity = 0f;
            line.currentBlurVisual = targetBlur;
            line.velocityBlur = 0f;
            updateAMLLMaskAlpha(line, safeDt, true, dynamicMask);
            line.initialized = true;
            return;
        }

        float dy = targetY - line.currentY;
        line.velocityY += dy * params.stiffness() * safeDt;
        line.velocityY *= (float) Math.exp(-params.damping() * safeDt);
        line.currentY += line.velocityY * safeDt;

        float scaleStiffness = params.stiffness() * 1.08f;
        float scaleDamping = (float) Math.sqrt(scaleStiffness) * 2.15f;
        float ds = presentation.scale() - line.currentScale;
        line.velocityScale += ds * scaleStiffness * safeDt;
        line.velocityScale *= (float) Math.exp(-scaleDamping * safeDt);
        line.currentScale += line.velocityScale * safeDt;

        float opacityStiffness = params.stiffness() * 1.35f;
        float opacityDamping = (float) Math.sqrt(opacityStiffness) * 2.1f;
        float da = presentation.opacity() - line.currentOpacity;
        line.velocityOpacity += da * opacityStiffness * safeDt;
        line.velocityOpacity *= (float) Math.exp(-opacityDamping * safeDt);
        line.currentOpacity += line.velocityOpacity * safeDt;

        float blurStiffness = params.stiffness() * 0.82f;
        float blurDamping = (float) Math.sqrt(blurStiffness) * 2.0f;
        float db = targetBlur - line.currentBlurVisual;
        line.velocityBlur += db * blurStiffness * safeDt;
        line.velocityBlur *= (float) Math.exp(-blurDamping * safeDt);
        line.currentBlurVisual += line.velocityBlur * safeDt;

        if (Math.abs(dy) < 0.20f && Math.abs(line.velocityY) < 0.30f) {
            line.currentY = targetY;
            line.velocityY = 0f;
        }
        if (Math.abs(ds) < 0.002f && Math.abs(line.velocityScale) < 0.002f) {
            line.currentScale = presentation.scale();
            line.velocityScale = 0f;
        }
        if (Math.abs(da) < 0.002f && Math.abs(line.velocityOpacity) < 0.002f) {
            line.currentOpacity = presentation.opacity();
            line.velocityOpacity = 0f;
        }
        if (Math.abs(db) < 0.01f && Math.abs(line.velocityBlur) < 0.01f) {
            line.currentBlurVisual = targetBlur;
            line.velocityBlur = 0f;
        }
        updateAMLLMaskAlpha(line, safeDt, false, dynamicMask);
    }

    private void updateAMLLMaskAlpha(CachedLyricLine line, float dt, boolean snap, boolean dynamicMask) {
        float factor = clamp01((line.currentScale - 0.97f) / 0.03f);
        float dynamicDarkAlpha = factor * 0.2f + 0.2f;
        float dynamicBrightAlpha = factor * 0.8f + 0.2f;
        line.targetBrightAlpha = dynamicMask ? dynamicBrightAlpha : dynamicDarkAlpha;
        line.targetDarkAlpha = dynamicDarkAlpha;
        if (snap) {
            line.currentBrightAlpha = line.targetBrightAlpha;
            line.currentDarkAlpha = line.targetDarkAlpha;
            return;
        }
        float safeDt = dt <= 0f ? 0.016f : dt;
        float brightSpeed = line.targetBrightAlpha > line.currentBrightAlpha ? 50f : 7f;
        float brightFactor = 1f - (float) Math.exp(-brightSpeed * safeDt);
        if (Math.abs(line.targetBrightAlpha - line.currentBrightAlpha) < 0.001f) line.currentBrightAlpha = line.targetBrightAlpha;
        else line.currentBrightAlpha += (line.targetBrightAlpha - line.currentBrightAlpha) * brightFactor;
        float darkSpeed = line.targetDarkAlpha > line.currentDarkAlpha ? 50f : 7f;
        float darkFactor = 1f - (float) Math.exp(-darkSpeed * safeDt);
        if (Math.abs(line.targetDarkAlpha - line.currentDarkAlpha) < 0.001f) line.currentDarkAlpha = line.targetDarkAlpha;
        else line.currentDarkAlpha += (line.targetDarkAlpha - line.currentDarkAlpha) * darkFactor;
    }

    private InterludeState computeInterlude(int active, float currentSeconds) {
        if (lyricLines.isEmpty()) return new InterludeState(false, Integer.MIN_VALUE, 0f, 0f, false);

        float currentTimeMs = currentSeconds * 1000f + 20f;
        int checkActive = active;

        for (int k = checkActive - 1; k <= checkActive + 1; k++) {
            if (k < -1 || k >= lyricLines.size() - 1) continue;
            float gapStartMs;
            if (k == -1) {
                gapStartMs = 0f;
            } else {
                gapStartMs = lyricLines.get(k).endTime() * 1000f;
            }
            float gapEndMs = Math.max(gapStartMs, lyricLines.get(k + 1).startTime() * 1000f - 250f);

            if (gapEndMs - gapStartMs < 4000f) continue;

            if (gapEndMs > currentTimeMs && gapStartMs < currentTimeMs) {
                boolean nextDuet = lyricLines.get(k + 1).isDuet();
                return new InterludeState(true, k, gapStartMs / 1000f, gapEndMs / 1000f, nextDuet);
            }
        }

        return closingInterludeState(checkActive);
    }

    private InterludeState closingInterludeState(int fallbackAnchor) {
        if (interludeOpacity > 0.001f && interludeAnchor != Integer.MIN_VALUE) return new InterludeState(false, interludeAnchor, interludeStartTime, interludeEndTime, false);
        return new InterludeState(false, fallbackAnchor, 0f, 0f, false);
    }



    private float interludeSlotHeight(float s) {
        return amllInterludeDotSize() + amllInterludeDotMargin() * 2f;
    }

    private void updateInterludeAnimation(float targetY, boolean visible, float s, float dt, boolean snap) {
        float targetOpacity = visible ? 1f : 0f;
        if (snap) {
            interludeY = targetY;
            interludeVelocityY = 0f;
            interludeOpacity = targetOpacity;
            return;
        }

        float safeDt = clamp(dt, 0f, 0.05f);
        float stiffness = 90f;
        float damping = 15f;
        float dy = targetY - interludeY;
        interludeVelocityY += dy * stiffness * safeDt;
        interludeVelocityY *= (float) Math.exp(-damping * safeDt);
        interludeY += interludeVelocityY * safeDt;

        float opacityDuration = 0.25f;
        float opacityStep = safeDt / Math.max(0.001f, opacityDuration);
        if (interludeOpacity < targetOpacity) interludeOpacity = Math.min(targetOpacity, interludeOpacity + opacityStep);
        else if (interludeOpacity > targetOpacity) interludeOpacity = Math.max(targetOpacity, interludeOpacity - opacityStep);

        if (Math.abs(dy) < 0.20f && Math.abs(interludeVelocityY) < 0.30f) {
            interludeY = targetY;
            interludeVelocityY = 0f;
        }
    }

    private LyricPresentation computeLyricPresentation(int index, int active) {
        int groupIndex = groupMainIndex(index);
        boolean hasBuffered = amllBufferedGroups.contains(groupIndex);
        boolean playing = currentUiTrack != null && MusicLoader.isPlaying(currentUiTrack);
        boolean isBG = lyricLines.get(index).isBG();
        boolean nonDynamic = isNonDynamicLyrics();

        boolean groupActive = isAMLLGroupActive(groupIndex);

        if (isBG) {
            return new LyricPresentation(groupActive ? 0.4f : 0.0001f, groupActive ? 1f : 0.75f);
        }

        float opacity = hasBuffered ? 0.85f : (nonDynamic ? 0.2f : 1f);
        float scale = (!groupActive && playing) ? 0.97f : 1f;
        return new LyricPresentation(opacity, scale);
    }

    private boolean isAMLLGroupActive(int groupIndex) {
        if (amllBufferedGroups.contains(groupIndex)) return true;
        int latest = maxSet(amllBufferedGroups, amllScrollToIndex);
        return groupIndex >= amllScrollToIndex && groupIndex < latest;
    }

    private boolean isNonDynamicLyrics() {
        for (LyricLine line : lyricLines) {
            if (line.words() != null && line.words().size() > 1) return false;
        }
        return true;
    }

    private boolean isActiveLine(int index, int active) {
        if (index == active) return true;
        if (active >= 0 && active < lyricLines.size() && index >= 0 && index < lyricLines.size()) {
            return Math.abs(lyricLines.get(index).startTime() - lyricLines.get(active).startTime()) < 0.001f;
        }
        return false;
    }

    private void startLyricViewAnimation(long now) {
        lyricLayoutAnimStart = lyricLayoutVisual;
        lyricOpacityAnimStart = lyricOpacityVisual;
        lyricViewAnimStartedAt = now;
    }

    private boolean updateLyricViewAnimation(long now) {
        float target = lyricsVisible ? 1f : 0f;
        float oldLayout = lyricLayoutVisual;
        float oldOpacity = lyricOpacityVisual;
        float elapsed = Math.max(0f, now - lyricViewAnimStartedAt);
        float layoutT = clamp(elapsed / LYRIC_LAYOUT_ANIM_MS, 0f, 1f);
        lyricLayoutVisual = lyricLayoutAnimStart + (target - lyricLayoutAnimStart) * amllLayoutEase(layoutT);
        if (layoutT >= 1f) lyricLayoutVisual = target;
        if (lyricsVisible) {
            if (elapsed < LYRIC_SHOW_DELAY_MS) {
                lyricOpacityVisual = lyricOpacityAnimStart;
            } else {
                float opacityT = clamp((elapsed - LYRIC_SHOW_DELAY_MS) / LYRIC_SHOW_ANIM_MS, 0f, 1f);
                lyricOpacityVisual = lyricOpacityAnimStart + (target - lyricOpacityAnimStart) * amllLayoutEase(opacityT);
                if (opacityT >= 1f) lyricOpacityVisual = target;
            }
        } else {
            float opacityT = clamp(elapsed / LYRIC_HIDE_ANIM_MS, 0f, 1f);
            lyricOpacityVisual = lyricOpacityAnimStart + (target - lyricOpacityAnimStart) * amllLayoutEase(opacityT);
            if (opacityT >= 1f) lyricOpacityVisual = target;
        }
        lyricLayoutVisual = clamp(lyricLayoutVisual, 0f, 1f);
        lyricOpacityVisual = clamp(lyricOpacityVisual, 0f, 1f);
        return Math.abs(oldLayout - lyricLayoutVisual) > 0.001f || Math.abs(oldOpacity - lyricOpacityVisual) > 0.001f || Math.abs(lyricLayoutVisual - target) > 0.001f || Math.abs(lyricOpacityVisual - target) > 0.001f;
    }

    private float amllLayoutEase(float t) {
        return cubicBezier(clamp(t, 0f, 1f), 0.5f, 0f, 0.5f, 1f);
    }

    private float cubicBezier(float t, float x1, float y1, float x2, float y2) {
        float lo = 0f;
        float hi = 1f;
        float p = t;
        for (int i = 0; i < 10; i++) {
            p = (lo + hi) * 0.5f;
            if (cubicBezierValue(p, x1, x2) < t) lo = p;
            else hi = p;
        }
        return cubicBezierValue(p, y1, y2);
    }

    private float cubicBezierValue(float t, float a, float b) {
        float u = 1f - t;
        return 3f * u * u * t * a + 3f * u * t * t * b + t * t * t;
    }

    private float leftVisualOffset() {
        float leftW = amllHorizontalLayoutMaxWidth();
        float infoColumnW = amllHorizontalInfoColumnWidth();
        float baseLeftX = Math.max(0f, (infoColumnW - leftW) * 0.5f);
        float hiddenLeftX = Math.max(0f, (width - leftW) * 0.5f);
        return (hiddenLeftX - baseLeftX) * (1f - clamp(lyricLayoutVisual, 0f, 1f));
    }

    private float leftMovingRegionWidth() {
        float leftW = amllHorizontalLayoutMaxWidth();
        float infoColumnW = amllHorizontalInfoColumnWidth();
        float baseLeftX = Math.max(0f, (infoColumnW - leftW) * 0.5f);
        return clamp(Math.max(infoColumnW, baseLeftX + leftW), 1f, width);
    }

    private boolean updateControlAnimations(float dt, float s) {
        float safeDt = clamp(dt, 0f, 0.05f);
        float hoverStep = 1f - (float) Math.exp(-safeDt * 13.5f);
        float pressStep = safeDt / 0.70f;
        boolean animating = false;
        for (int i = 0; i < controlHover.length; i++) {
            float target = hoveredControl == i ? 1f : 0f;
            float oldHover = controlHover[i];
            float oldPress = controlPress[i];
            controlHover[i] += (target - controlHover[i]) * hoverStep;
            if (controlPress[i] > 0f) {
                controlPress[i] += pressStep;
                if (controlPress[i] >= 1f) controlPress[i] = 0f;
            }
            if (Math.abs(controlHover[i] - target) < 0.002f) controlHover[i] = target;
            if (Math.abs(oldHover - controlHover[i]) > 0.001f || Math.abs(oldPress - controlPress[i]) > 0.001f) animating = true;
        }
        float sliderStep = 1f - (float) Math.exp(-safeDt * 10.5f);
        float oldProgressVisual = progressVisual;
        float oldVolumeVisual = volumeVisual;
        float oldProgressBounce = progressBounceX;
        float oldVolumeBounce = volumeBounceX;
        progressVisual += (((hoveredProgress || draggingProgress) ? 1f : 0f) - progressVisual) * sliderStep;
        volumeVisual += (((hoveredVolume || draggingVolume) ? 1f : 0f) - volumeVisual) * sliderStep;
        if (Math.abs(progressVisual) < 0.002f && !hoveredProgress && !draggingProgress) progressVisual = 0f;
        if (Math.abs(volumeVisual) < 0.002f && !hoveredVolume && !draggingVolume) volumeVisual = 0f;
        if (!draggingProgress) progressBounceX += (0f - progressBounceX) * sliderStep;
        if (!draggingVolume) volumeBounceX += (0f - volumeBounceX) * sliderStep;
        if (Math.abs(progressBounceX) < 0.02f) progressBounceX = 0f;
        if (Math.abs(volumeBounceX) < 0.02f) volumeBounceX = 0f;
        if (Math.abs(oldProgressVisual - progressVisual) > 0.001f || Math.abs(oldVolumeVisual - volumeVisual) > 0.001f || Math.abs(oldProgressBounce - progressBounceX) > 0.01f || Math.abs(oldVolumeBounce - volumeBounceX) > 0.01f) animating = true;
        return animating;
    }

    private float controlScale(int index) {
        float t = clamp(controlPress[index], 0f, 1f);
        float press = 1f;
        if (t > 0f && t < 0.20f) press = 1f - 0.15f * (t / 0.20f);
        else if (t >= 0.20f && t < 0.50f) press = 0.85f + 0.25f * ((t - 0.20f) / 0.30f);
        else if (t >= 0.50f && t < 1f) press = 1.10f - 0.10f * ((t - 0.50f) / 0.50f);
        return press;
    }

    private float controlHoverRadius(int index) {
        return controlHover[index];
    }

    private void startControlPress(int index) {
        if (index >= 0 && index < controlPress.length) {
            controlPress[index] = 0.0001f;
            controlsLayerDirty = true;
        }
    }

    private void drawPlayPause(float cx, float cy, float size, boolean showPlay, boolean hover, float hoverR) {
        drawButtonHover(cx, cy, hoverR, hover);
        drawIcon(new String[]{showPlay ? ICON_PLAY : ICON_PAUSE}, 38f, cx, cy, size, WHITE);
    }

    private void drawPrevNext(float cx, float cy, float size, boolean next, boolean hover, float hoverR) {
        drawButtonHover(cx, cy, hoverR, hover);
        drawIcon(next ? new String[]{ICON_FORWARD_LEFT, ICON_FORWARD_RIGHT} : new String[]{ICON_REWIND_RIGHT, ICON_REWIND_LEFT}, 134f, cx, cy, size, WHITE);
    }

    private void drawShuffleButton(float cx, float cy, float size, boolean active, boolean hover, float hoverR) {
        drawToggleIcon(new String[]{ICON_SHUFFLE}, cx, cy, size, active, hover, hoverR);
    }

    private void drawRepeatButton(float cx, float cy, float size, boolean active, boolean hover, float hoverR) {
        drawToggleIcon(new String[]{ICON_REPEAT}, cx, cy, size, active, hover, hoverR);
    }

    private void drawToggleIcon(String[] paths, float cx, float cy, float size, boolean active, boolean hover, float hoverR) {
        if (active) {
            renderer.drawSquircle(cx - size * 0.5f, cy - size * 0.5f, size, size, size * 0.28f, AMLL_ACTIVE_BG);
            drawIcon(paths, 56f, cx, cy, size, AMLL_ACTIVE_ICON);
        } else {
            drawButtonHover(cx, cy, hoverR, hover);
            drawIcon(paths, 56f, cx, cy, size, WHITE);
        }
    }

    private void drawButtonHover(float cx, float cy, float radius, boolean hover) {
        float a = clamp(radius / Math.max(1f, btnR), 0f, 1f);
        if (hover && a > 0.01f) renderer.drawCircle(cx, cy, btnR, withAlpha(AMLL_BUTTON_HOVER, alphaOf(AMLL_BUTTON_HOVER) * a));
    }

    private void drawSpeaker(float cx, float cy, float r, int color) {
        drawIconRect(new String[]{ICON_SPEAKER}, 32f, 40f, cx, cy, r * 2.0f, color);
    }

    private void drawSpeakerWithWaves(float cx, float cy, float r, int color) {
        drawIconRect(new String[]{ICON_SPEAKER3_BODY, ICON_SPEAKER3_W1, ICON_SPEAKER3_W2, ICON_SPEAKER3_W3}, 43f, 40f, cx, cy, r * 2.0f, color);
    }

    private void drawIcon(String[] paths, float viewBox, float cx, float cy, float size, int color) {
        drawIconRect(paths, viewBox, viewBox, cx, cy, size, color);
    }

    private void drawIconRect(String[] paths, float viewBoxW, float viewBoxH, float cx, float cy, float size, int color) {
        Canvas c = renderer.canvas();
        c.save();
        float scale = size / Math.max(viewBoxW, viewBoxH);
        float drawW = viewBoxW * scale;
        float drawH = viewBoxH * scale;
        c.translate(cx - drawW * 0.5f, cy - drawH * 0.5f);
        c.scale(scale, scale);
        try (Paint paint = new Paint()) {
            paint.setColor(color);
            paint.setAntiAlias(true);
            for (String data : paths) {
                Path path = iconPath(data);
                if (path != null) c.drawPath(path, paint);
            }
        }
        c.restore();
    }

    private Path iconPath(String data) {
        if (data == null || data.isBlank()) return null;
        Path cached = iconPathCache.get(data);
        if (cached != null) return cached;
        Path path = Path.makeFromSVGString(data);
        if (path != null) {
            path.setFillMode(io.github.humbleui.skija.PathFillMode.EVEN_ODD);
            iconPathCache.put(data, path);
        }
        return path;
    }


    private void handleMouse(int mx, int my) {
        long window = Minecraft.getInstance().getWindow().handle();
        long now = System.currentTimeMillis();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean leftClick = leftDown && !wasLeftDown;
        boolean wasDraggingProgress = draggingProgress;
        boolean wasDraggingVolume = draggingVolume;
        if (leftClick) handleControlClick(mx, my);
        if (!leftDown) {
            if (wasDraggingProgress) finishProgressDrag();
            else progressDragSeconds = -1f;
            draggingVolume = false;
            if (wasDraggingVolume) controlsLayerDirty = true;
        }
        float controlsOffset = leftVisualOffset();
        if (draggingProgress && currentUiTrack != null && progW > 0) {
            float rel = (mx - (progX + controlsOffset)) / progW;
            float p = clamp(rel, 0f, 1f);
            progressBounceX = rel < 0f ? (float) Math.tanh(rel * 2f) * amllCssPx(12f) : rel > 1f ? (float) Math.tanh((rel - 1f) * 2f) * amllCssPx(12f) : 0f;
            progressDragSeconds = currentUiTrack.duration() * p;
            if (now - lastDragRender >= DRAG_RENDER_INTERVAL_MS) {
                controlsLayerDirty = true;
                lyricsLayerDirty = true;
                lastDragRender = now;
            }
        }
        if (draggingVolume && volW > 0) {
            float rel = (mx - (volX + controlsOffset)) / volW;
            volumeBounceX = rel < 0f ? (float) Math.tanh(rel * 2f) * amllCssPx(12f) : rel > 1f ? (float) Math.tanh((rel - 1f) * 2f) * amllCssPx(12f) : 0f;
            volume = clamp(rel, 0f, 1f);
            MusicLoader.setVolume(volume);
            if (now - lastDragRender >= DRAG_RENDER_INTERVAL_MS) {
                controlsLayerDirty = true;
                lastDragRender = now;
            }
        }
        updateControlHover(mx, my);
        updateSliderHover(mx, my);
        updateLyricsHover(mx, my);
        wasLeftDown = leftDown;
    }

    private void updateControlHover(float mx, float my) {
        int hover = -1;
        float ox = leftVisualOffset();
        if (inside(mx, my, modeX + ox - btnR, modeY - btnR, btnR * 2f, btnR * 2f)) hover = 0;
        else if (inside(mx, my, prevX + ox - btnR, prevY - btnR, btnR * 2f, btnR * 2f)) hover = 1;
        else if (inside(mx, my, playX + ox - btnR, playY - btnR, btnR * 2f, btnR * 2f)) hover = 2;
        else if (inside(mx, my, nextX + ox - btnR, nextY - btnR, btnR * 2f, btnR * 2f)) hover = 3;
        else if (inside(mx, my, repeatX + ox - btnR, repeatY - btnR, btnR * 2f, btnR * 2f)) hover = 4;
        if (hoveredControl != hover) {
            hoveredControl = hover;
            controlsLayerDirty = true;
        }
    }

    private void updateSliderHover(float mx, float my) {
        float ox = leftVisualOffset();
        boolean progress = inside(mx, my, progX + ox, progY, progW, progH);
        boolean volumeHover = inside(mx, my, volX + ox, volY, volW, volH);
        if (hoveredProgress != progress || hoveredVolume != volumeHover) {
            hoveredProgress = progress;
            hoveredVolume = volumeHover;
            controlsLayerDirty = true;
        }
    }

    private void updateLyricsHover(float mx, float my) {
        float padX = amllLyricLinePaddingX(amllLyricFontSize());
        boolean hover = lyricsVisible && inside(mx, my, layoutRightX - padX, layoutLyricTop, layoutRightW + padX * 2f, layoutLyricH);
        if (hoveredLyrics != hover) {
            hoveredLyrics = hover;
            if (!useLineLyricTextures()) lyricsLayerDirty = true;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleControlClick((float) event.x(), (float) event.y())) {
            wasLeftDown = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingProgress) {
            if (currentUiTrack != null && progW > 0f) progressDragSeconds = currentUiTrack.duration() * clamp(((float) event.x() - progX) / progW, 0f, 1f);
            finishProgressDrag();
            wasLeftDown = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean handleControlClick(float mx, float my) {
        float ox = leftVisualOffset();
        if (inside(mx, my, thumbX + ox, thumbY, thumbW, thumbH)) {
            startReturnTransition();
            return true;
        }
        if (inside(mx, my, menuX + ox, menuY, menuW, menuH)) {
            playlistOpen = !playlistOpen;
            dirty = true;
            return true;
        }
        if (inside(mx, my, lyricToggleX, lyricToggleY, lyricToggleW, lyricToggleH)) {
            lyricsVisible = !lyricsVisible;
            startLyricViewAnimation(System.currentTimeMillis());
            hoveredLyrics = false;
            playlistOpen = false;
            dirty = true;
            controlsLayerDirty = true;
            lyricsLayerDirty = true;
            return true;
        }
        if (playlistOpen) {
            if (playlistContains(mx, my)) {
                int row = playlistRowAt(mx, my);
                if (row >= 0 && row < MusicLoader.getTracks().size()) {
                    trackIndex = row;
                    refreshTrackCache();
                    playSelectedTrackFromStart();
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
        int clickedLyric = lyricLineAt(mx, my);
        if (clickedLyric >= 0 && clickedLyric < lyricLines.size()) {
            playDisplayedTrackFrom(lyricLines.get(clickedLyric).startTime());
            lyricSnapOnNextRender = true;
            controlsLayerDirty = true;
            return true;
        }
        if (inside(mx, my, playX + ox - btnR, playY - btnR, btnR * 2f, btnR * 2f)) {
            startControlPress(2);
            if (currentUiTrack != null) {
                MusicLoader.toggle(currentUiTrack);
                if (MusicLoader.isPlaying(currentUiTrack)) resetPlaybackUiState();
            }
            controlsLayerDirty = true;
            return true;
        }
        if (inside(mx, my, prevX + ox - btnR, prevY - btnR, btnR * 2f, btnR * 2f)) {
            startControlPress(1);
            previousTrack();
            return true;
        }
        if (inside(mx, my, nextX + ox - btnR, nextY - btnR, btnR * 2f, btnR * 2f)) {
            startControlPress(3);
            nextTrack();
            return true;
        }
        if (inside(mx, my, modeX + ox - btnR, modeY - btnR, btnR * 2f, btnR * 2f)) {
            startControlPress(0);
            shuffleMode = !shuffleMode;
            controlsLayerDirty = true;
            return true;
        }
        if (inside(mx, my, repeatX + ox - btnR, repeatY - btnR, btnR * 2f, btnR * 2f)) {
            startControlPress(4);
            repeatMode = !repeatMode;
            controlsLayerDirty = true;
            return true;
        }
        if (inside(mx, my, listX, listY, listW, listH)) {
            playlistOpen = !playlistOpen;
            dirty = true;
            return true;
        }
        if (inside(mx, my, progX + ox, progY, progW, progH)) {
            draggingProgress = true;
            progressDragSeconds = currentUiTrack != null ? currentUiTrack.duration() * clamp((mx - (progX + ox)) / progW, 0f, 1f) : -1f;
            if (currentUiTrack != null && progressDragSeconds >= 0f) playDisplayedTrackFrom(progressDragSeconds);
            resetPlaybackUiState();
            controlsLayerDirty = true;
            lyricsLayerDirty = true;
            lastDragRender = System.currentTimeMillis();
            return true;
        }
        if (inside(mx, my, volX + ox, volY, volW, volH)) {
            draggingVolume = true;
            volume = clamp((mx - (volX + ox)) / volW, 0f, 1f);
            MusicLoader.setVolume(volume);
            controlsLayerDirty = true;
            lastDragRender = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private int lyricLineAt(float mx, float my) {
        if (!lyricsVisible || !useLineLyricTextures() || lyricCache.size() != lyricLines.size()) return -1;
        float s = pageScale();
        if (!inside(mx, my, layoutRightX - 24f * s, layoutLyricTop, layoutRightW + 48f * s, layoutLyricH)) return -1;
        for (int i = 0; i < lyricCache.size(); i++) {
            CachedLyricLine line = lyricCache.get(i);
            if (!line.initialized) continue;
            float h = getLineContentHeight(i, lastActiveLyric, s);
            float y = line.currentY - 7f * s;
            if (my >= y && my <= y + h + 14f * s) return i;
        }
        return -1;
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
        float h = Math.min(260f * s, height * 0.48f);
        float y = height - h - 88f * s;
        if (!playlistContains(mx, my)) return -1;
        return (int) ((my - y - 34f * s) / (26f * s));
    }

    private void nextTrack() {
        int count = MusicLoader.getTracks().size();
        if (count <= 0) return;
        trackIndex = nextTrackIndex(count);
        refreshTrackCache();
        playSelectedTrackFromStart();
        dirty = true;
    }

    private void previousTrack() {
        int count = MusicLoader.getTracks().size();
        if (count <= 0) return;
        trackIndex = shuffleMode ? randomDifferentTrackIndex(count) : (trackIndex - 1 + count) % count;
        refreshTrackCache();
        playSelectedTrackFromStart();
        dirty = true;
    }

    private void handlePlaybackEnded() {
        int count = MusicLoader.getTracks().size();
        if (count <= 0) return;
        if (repeatMode) {
            playSelectedTrackFromStart();
        } else if (count > 1) {
            trackIndex = nextTrackIndex(count);
            refreshTrackCache();
            playSelectedTrackFromStart();
            dirty = true;
        } else {
            controlsLayerDirty = true;
            lyricsLayerDirty = true;
        }
    }

    private int nextTrackIndex(int count) {
        if (count <= 1) return trackIndex;
        return shuffleMode ? randomDifferentTrackIndex(count) : (trackIndex + 1) % count;
    }

    private int randomDifferentTrackIndex(int count) {
        if (count <= 1) return trackIndex;
        int offset = 1 + Math.floorMod((int) (System.nanoTime() ^ (System.currentTimeMillis() * 31L)), count - 1);
        return (trackIndex + offset) % count;
    }

    private void playSelectedTrackFromStart() {
        List<MusicLoader.MusicTrack> tracks = MusicLoader.getTracks();
        if (tracks.isEmpty()) return;
        clampTrackIndex();
        MusicLoader.play(tracks.get(trackIndex));
        resetPlaybackUiState();
    }

    private void playDisplayedTrackFrom(float seconds) {
        if (currentUiTrack == null) return;
        MusicLoader.playFrom(currentUiTrack, clamp(seconds, 0f, Math.max(0, currentUiTrack.duration())));
        resetPlaybackUiState();
    }

    private void finishProgressDrag() {
        if (currentUiTrack != null && progressDragSeconds >= 0f) playDisplayedTrackFrom(progressDragSeconds);
        progressDragSeconds = -1f;
        draggingProgress = false;
        controlsLayerDirty = true;
        lyricsLayerDirty = true;
    }

    private void resetPlaybackUiState() {
        lastRenderedSecond = -1;
        lastActiveLyric = -1;
        resetAMLLTimeline();
        lyricSnapOnNextRender = true;
        controlsLayerDirty = true;
        lyricsLayerDirty = true;
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
            coverImage = Image.makeDeferredFromEncodedBytes(coverBytes);
            derivePaletteFromCover(coverBytes);
            rebuildFluidBackground(coverBytes, track);
        } catch (Exception e) {
            derivePaletteFromText(track.title() + track.artist());
            rebuildFluidBackground(null, track);
            SkijaTestClient.LOGGER.info("[Musicpage] Failed to decode cover art for {}", track.title(), e);
        }
    }

    private void rebuildFluidBackground(byte[] coverBytes, MusicLoader.MusicTrack track) {
        String seed = track.title() + track.artist();
        if (gpuBackground != null) {
            gpuBackground.rebuild(coverBytes, seed);
            lastBgRender = 0L;
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
            SkijaTestClient.LOGGER.info("[Musicpage] Failed to derive cover palette", e);
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
        resetAMLLTimeline();
        clearLyricCache();
        if (coverImage != null) {
            coverImage.close();
            coverImage = null;
        }
    }

    private static float parseTTMLTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 0f;
        String[] parts = timeStr.split(":");
        try {
            if (parts.length == 3) {
                return Float.parseFloat(parts[0]) * 3600 + Float.parseFloat(parts[1]) * 60 + Float.parseFloat(parts[2]);
            } else if (parts.length == 2) {
                return Float.parseFloat(parts[0]) * 60 + Float.parseFloat(parts[1]);
            } else {
                return Float.parseFloat(parts[0]);
            }
        } catch (Exception e) {
            return 0f;
        }
    }

    private java.util.List<RubyText> parseRubyTexts(org.w3c.dom.Element wordEl) {
        ArrayList<RubyText> result = new ArrayList<>();
        String rubyAttr = wordEl.getAttribute("tts:ruby");
        if (rubyAttr.isEmpty()) rubyAttr = wordEl.getAttributeNS("http://www.w3.org/ns/ttml#styling", "ruby");
        if (!"container".equals(rubyAttr)) return result;
        org.w3c.dom.NodeList descendants = wordEl.getElementsByTagName("span");
        for (int i = 0; i < descendants.getLength(); i++) {
            org.w3c.dom.Element el = (org.w3c.dom.Element) descendants.item(i);
            String role = el.getAttribute("tts:ruby");
            if (role.isEmpty()) role = el.getAttributeNS("http://www.w3.org/ns/ttml#styling", "ruby");
            if (!"text".equals(role)) continue;
            float begin = parseTTMLTime(el.getAttribute("begin"));
            float end = parseTTMLTime(el.getAttribute("end"));
            String text = el.getTextContent();
            if (text != null && !text.isBlank()) result.add(new RubyText(begin, end, text));
        }
        return result;
    }

    private String parseRubyBaseText(org.w3c.dom.Element wordEl) {
        org.w3c.dom.NodeList descendants = wordEl.getElementsByTagName("span");
        for (int i = 0; i < descendants.getLength(); i++) {
            org.w3c.dom.Element el = (org.w3c.dom.Element) descendants.item(i);
            String role = el.getAttribute("tts:ruby");
            if (role.isEmpty()) role = el.getAttributeNS("http://www.w3.org/ns/ttml#styling", "ruby");
            if ("base".equals(role)) return el.getTextContent();
        }
        return wordEl.getTextContent();
    }

    private java.util.List<LyricWord> attachRomanWords(java.util.List<LyricWord> words, java.util.List<LyricWord> romanWords) {
        if (words == null || words.isEmpty() || romanWords == null || romanWords.isEmpty()) return words;
        ArrayList<LyricWord> result = new ArrayList<>(words.size());
        for (int i = 0; i < words.size(); i++) {
            LyricWord word = words.get(i);
            LyricWord best = null;
            float bestOverlap = 0f;
            for (LyricWord roman : romanWords) {
                float overlap = Math.min(word.endTime(), roman.endTime()) - Math.max(word.startTime(), roman.startTime());
                if (overlap > bestOverlap) {
                    bestOverlap = overlap;
                    best = roman;
                }
            }
            if (best == null && i < romanWords.size()) best = romanWords.get(i);
            result.add(word.withRomanWord(best == null ? null : best.word()));
        }
        return result;
    }

    private java.util.List<LyricWord> splitAMLLWords(java.util.List<LyricWord> words) {
        if (words == null || words.isEmpty()) return words;
        ArrayList<LyricWord> result = new ArrayList<>();
        ArrayList<LyricWord> currentGroup = new ArrayList<>();

        java.util.function.Consumer<LyricWord> flushAtom = atom -> {
            boolean isSpace = atom.word() == null || atom.word().trim().isEmpty();
            boolean isCjk = atom.word() != null && containsCjk(atom.word());
            boolean mergeable = !isSpace && !isCjk;
            if (mergeable) {
                currentGroup.add(atom);
            } else {
                if (!currentGroup.isEmpty()) {
                    result.add(mergeAMLLWordGroup(currentGroup));
                    currentGroup.clear();
                }
                result.add(atom);
            }
        };

        for (LyricWord word : words) {
            String text = word.word() == null ? "" : word.word();
            if (word.ruby() != null && !word.ruby().isEmpty()) {
                flushAtom.accept(word);
                continue;
            }
            if (text.trim().isEmpty()) {
                flushAtom.accept(word);
                continue;
            }
            java.util.regex.Matcher matcher = Pattern.compile("\\S+|\\s+").matcher(text);
            int nonSpaceUnits = Math.max(1, text.replaceAll("\\s", "").length());
            float timePerUnit = (word.endTime() - word.startTime()) / nonSpaceUnits;
            int currentOffset = 0;
            while (matcher.find()) {
                String part = matcher.group();
                if (part.trim().isEmpty()) {
                    float t = word.startTime() + currentOffset * timePerUnit;
                    flushAtom.accept(new LyricWord(t, t, part, word.romanWord(), word.ruby()));
                    continue;
                }
                if (containsCjk(part) && part.codePointCount(0, part.length()) > 1) {
                    for (int offset = 0; offset < part.length();) {
                        int cp = part.codePointAt(offset);
                        int len = Character.charCount(cp);
                        String unit = part.substring(offset, offset + len);
                        float t = word.startTime() + currentOffset * timePerUnit;
                        flushAtom.accept(new LyricWord(t, t + timePerUnit, unit, word.romanWord(), word.ruby()));
                        currentOffset += 1;
                        offset += len;
                    }
                } else {
                    int partUnits = Math.max(1, part.replaceAll("\\s", "").length());
                    float t = word.startTime() + currentOffset * timePerUnit;
                    flushAtom.accept(new LyricWord(t, t + partUnits * timePerUnit, part, word.romanWord(), word.ruby()));
                    currentOffset += partUnits;
                }
            }
        }
        if (!currentGroup.isEmpty()) result.add(mergeAMLLWordGroup(currentGroup));
        return result;
    }

    private LyricWord mergeAMLLWordGroup(java.util.List<LyricWord> words) {
        if (words.size() == 1) return words.get(0);
        float start = Float.POSITIVE_INFINITY;
        float end = Float.NEGATIVE_INFINITY;
        StringBuilder text = new StringBuilder();
        StringBuilder roman = new StringBuilder();
        ArrayList<RubyText> ruby = new ArrayList<>();
        for (LyricWord word : words) {
            start = Math.min(start, word.startTime());
            end = Math.max(end, word.endTime());
            text.append(word.word() == null ? "" : word.word());
            if (word.romanWord() != null) roman.append(word.romanWord());
            if (word.ruby() != null) ruby.addAll(word.ruby());
        }
        return new LyricWord(start, end, text.toString(), roman.toString().isBlank() ? null : roman.toString(), ruby);
    }

    private List<LyricLine> parseTTML(String xmlStr, String title, String artist) {
        List<LyricLine> result = new ArrayList<>();
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlStr.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

            java.util.Map<String, String> agentTypes = new java.util.HashMap<>();
            org.w3c.dom.NodeList agentNodes = doc.getElementsByTagNameNS("http://www.w3.org/ns/ttml#metadata", "agent");
            if (agentNodes.getLength() == 0) {
                agentNodes = doc.getElementsByTagName("ttm:agent");
            }
            for (int i = 0; i < agentNodes.getLength(); i++) {
                org.w3c.dom.Element agentEl = (org.w3c.dom.Element) agentNodes.item(i);
                String id = agentEl.getAttribute("xml:id");
                String type = agentEl.getAttribute("type");
                if (!id.isEmpty() && !type.isEmpty()) {
                    agentTypes.put(id, type);
                }
            }

            java.util.Map<String, String> translations = new java.util.HashMap<>();
            org.w3c.dom.NodeList transNodes = doc.getElementsByTagName("translation");
            if (transNodes.getLength() > 0) {
                org.w3c.dom.Element transEl = (org.w3c.dom.Element) transNodes.item(0);
                org.w3c.dom.NodeList textNodes = transEl.getElementsByTagName("text");
                for (int i = 0; i < textNodes.getLength(); i++) {
                    org.w3c.dom.Element textEl = (org.w3c.dom.Element) textNodes.item(i);
                    String forKey = textEl.getAttribute("for");
                    String text = textEl.getTextContent();
                    if (!forKey.isEmpty() && text != null && !text.isEmpty()) {
                        translations.put(forKey, text);
                    }
                }
            }

            org.w3c.dom.NodeList pNodes = doc.getElementsByTagName("p");
            String lastPersonAgentId = null;
            boolean lastPersonIsDuet = false;

            for (int i = 0; i < pNodes.getLength(); i++) {
                org.w3c.dom.Element pEl = (org.w3c.dom.Element) pNodes.item(i);
                float lineBegin = parseTTMLTime(pEl.getAttribute("begin"));
                float lineEnd = parseTTMLTime(pEl.getAttribute("end"));
                String lineKey = pEl.getAttribute("itunes:key");
                if (lineKey.isEmpty()) lineKey = pEl.getAttributeNS("http://itunes.apple.com/lyric-ttml-extensions", "key");
                String agentId = pEl.getAttribute("ttm:agent");
                if (agentId.isEmpty()) agentId = pEl.getAttributeNS("http://www.w3.org/ns/ttml#metadata", "agent");

                String agentType = agentTypes.getOrDefault(agentId, "person");
                boolean isDuet = false;
                if ("group".equals(agentType)) {
                    isDuet = false;
                } else {
                    if (lastPersonAgentId == null) {
                        isDuet = "other".equals(agentType);
                        lastPersonAgentId = agentId;
                        lastPersonIsDuet = isDuet;
                    } else if (lastPersonAgentId.equals(agentId)) {
                        isDuet = lastPersonIsDuet;
                    } else {
                        isDuet = !lastPersonIsDuet;
                        lastPersonAgentId = agentId;
                        lastPersonIsDuet = isDuet;
                    }
                }

                java.util.List<LyricWord> mainWords = new ArrayList<>();
                java.util.List<LyricWord> bgWords = new ArrayList<>();
                java.util.List<LyricWord> romanWords = new ArrayList<>();
                StringBuilder mainTextBuilder = new StringBuilder();
                StringBuilder bgTextBuilder = new StringBuilder();
                StringBuilder romanTextBuilder = new StringBuilder();

                org.w3c.dom.NodeList childNodes = pEl.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    org.w3c.dom.Node child = childNodes.item(j);
                    if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && "span".equals(child.getLocalName())) {
                        org.w3c.dom.Element spanEl = (org.w3c.dom.Element) child;
                        String role = spanEl.getAttribute("ttm:role");
                        if (role.isEmpty()) role = spanEl.getAttributeNS("http://www.w3.org/ns/ttml#metadata", "role");

                        if ("x-roman".equals(role)) {
                            org.w3c.dom.NodeList romanChildNodes = spanEl.getChildNodes();
                            for (int k = 0; k < romanChildNodes.getLength(); k++) {
                                org.w3c.dom.Node romanChild = romanChildNodes.item(k);
                                if (romanChild.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && "span".equals(romanChild.getLocalName())) {
                                    org.w3c.dom.Element romanSpanEl = (org.w3c.dom.Element) romanChild;
                                    float rBegin = parseTTMLTime(romanSpanEl.getAttribute("begin"));
                                    float rEnd = parseTTMLTime(romanSpanEl.getAttribute("end"));
                                    String text = romanSpanEl.getTextContent();
                                    romanWords.add(new LyricWord(rBegin, rEnd, text));
                                    romanTextBuilder.append(text);
                                } else if (romanChild.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                                    romanTextBuilder.append(romanChild.getTextContent());
                                }
                            }
                            if (romanChildNodes.getLength() == 0) romanTextBuilder.append(spanEl.getTextContent());
                            continue;
                        }

                        if ("x-bg".equals(role)) {
                            org.w3c.dom.NodeList bgChildNodes = spanEl.getChildNodes();
                            for (int k = 0; k < bgChildNodes.getLength(); k++) {
                                org.w3c.dom.Node bgChild = bgChildNodes.item(k);
                                if (bgChild.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && "span".equals(bgChild.getLocalName())) {
                                    org.w3c.dom.Element bgSpanEl = (org.w3c.dom.Element) bgChild;
                                    float wBegin = parseTTMLTime(bgSpanEl.getAttribute("begin"));
                                    float wEnd = parseTTMLTime(bgSpanEl.getAttribute("end"));
                                    String text;
                                    java.util.List<RubyText> ruby = parseRubyTexts(bgSpanEl);
                                    if (!ruby.isEmpty()) text = parseRubyBaseText(bgSpanEl);
                                    else text = bgSpanEl.getTextContent();
                                    bgWords.add(new LyricWord(wBegin, wEnd, text, null, ruby));
                                    bgTextBuilder.append(text);
                                } else if (bgChild.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                                    bgTextBuilder.append(bgChild.getTextContent());
                                }
                            }
                        } else {
                            float wBegin = parseTTMLTime(spanEl.getAttribute("begin"));
                            float wEnd = parseTTMLTime(spanEl.getAttribute("end"));
                            String text;
                            java.util.List<RubyText> ruby = parseRubyTexts(spanEl);
                            if (!ruby.isEmpty()) text = parseRubyBaseText(spanEl);
                            else text = spanEl.getTextContent();
                            mainWords.add(new LyricWord(wBegin, wEnd, text, null, ruby));
                            mainTextBuilder.append(text);
                        }
                    } else if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                        mainTextBuilder.append(child.getTextContent());
                    }
                }

                String mainText = mainTextBuilder.toString().trim();
                String bgText = bgTextBuilder.toString().trim();
                String romanText = romanTextBuilder.toString().trim();
                String translation = translations.get(lineKey);

                if (!mainText.isEmpty() || !mainWords.isEmpty()) {
                    if (mainWords.isEmpty()) {
                        mainWords.add(new LyricWord(lineBegin, lineEnd, mainText));
                    }
                    result.add(new LyricLine(lineBegin, lineEnd, mainText, false, attachRomanWords(splitAMLLWords(mainWords), splitAMLLWords(romanWords)), translation, romanText, isDuet));
                }

                if (!bgText.isEmpty() || !bgWords.isEmpty()) {
                    if (bgWords.isEmpty()) {
                        bgWords.add(new LyricWord(lineBegin, lineEnd, bgText));
                    }
                    bgText = bgText.replaceAll("^[（(]+|[)）]+$", "").trim();
                    bgText = "（" + bgText + "）";
                    result.add(new LyricLine(lineBegin, lineEnd, bgText, true, splitAMLLWords(bgWords), null, isDuet));
                }
            }
        } catch (Exception e) {
            SkijaTestClient.LOGGER.info("[Musicpage] Failed to parse TTML lyrics", e);
        }

        if (result.isEmpty()) {
            result.add(new LyricLine(0f, 4f, title, false));
            result.add(new LyricLine(4f, 8f, artist, false));
            result.add(new LyricLine(8f, 12f, "The loaded TTML has no valid lines", false));
        }

        result.sort(java.util.Comparator.comparingDouble(LyricLine::startTime));

        for (int i = 0; i < result.size() - 1; i++) {
            LyricLine curr = result.get(i);
            LyricLine next = result.get(i + 1);
            if (curr.endTime() <= 0.001f) {
                result.set(i, new LyricLine(curr.startTime(), next.startTime(), curr.text(), curr.isBG(), curr.words(), curr.translation(), curr.isDuet()));
            }
        }
        if (!result.isEmpty()) {
            int lastIndex = result.size() - 1;
            LyricLine last = result.get(lastIndex);
            if (last.endTime() <= 0.001f) {
                result.set(lastIndex, new LyricLine(last.startTime(), last.startTime() + 10f, last.text(), last.isBG(), last.words(), last.translation(), last.isDuet()));
            }
        }

        return result;
    }



    private List<LyricLine> parseLyrics(String rawLyrics, String title, String artist) {
        if (rawLyrics != null && rawLyrics.trim().startsWith("<tt")) {
            return parseTTML(rawLyrics, title, artist);
        }
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
                    float time = min * 60f + sec + ms / 1000f;
                    String text = m.group(4).trim();
                    Matcher inlineBg = Pattern.compile("^(.*?)\\s*[(（](.+)[)）]$").matcher(text);
                    if (inlineBg.matches() && !inlineBg.group(1).isBlank()) {
                        String mainText = inlineBg.group(1).trim();
                        String bgText = "（" + inlineBg.group(2).trim() + "）";
                        result.add(new LyricLine(time, 0f, mainText, false));
                        result.add(new LyricLine(time, 0f, bgText, true));
                    } else {
                        boolean isBG = text.matches("^[(（].+[)）]$");
                        result.add(new LyricLine(time, 0f, text, isBG));
                    }
                } else if (!line.isBlank()) {
                    String text = line.trim();
                    float time = plainIndex++ * 4.0f;
                    Matcher inlineBg = Pattern.compile("^(.*?)\\s*[(（](.+)[)）]$").matcher(text);
                    if (inlineBg.matches() && !inlineBg.group(1).isBlank()) {
                        String mainText = inlineBg.group(1).trim();
                        String bgText = "（" + inlineBg.group(2).trim() + "）";
                        result.add(new LyricLine(time, 0f, mainText, false));
                        result.add(new LyricLine(time, 0f, bgText, true));
                    } else {
                        boolean isBG = text.matches("^[(（].+[)）]$");
                        result.add(new LyricLine(time, 0f, text, isBG));
                    }
                }
            }
        }
        if (result.isEmpty()) {
            result.add(new LyricLine(0f, 4f, title, false));
            result.add(new LyricLine(4f, 8f, artist, false));
            result.add(new LyricLine(8f, 12f, "把带歌词标签的 MP3 / FLAC 放到 lazychara/music", false));
            result.add(new LyricLine(12f, 16f, "之后接入真实播放进度即可同步歌词", false));
        }
        result.sort(java.util.Comparator.comparingDouble(LyricLine::startTime));

        int consecutiveBgCount = 0;
        for (int i = 0; i < result.size(); i++) {
            LyricLine line = result.get(i);
            if (line.isBG()) {
                consecutiveBgCount++;
                if (consecutiveBgCount > 1) {
                    result.set(i, new LyricLine(line.startTime(), line.endTime(), line.text(), false));
                }
            } else {
                consecutiveBgCount = 0;
            }
        }

        for (int i = result.size() - 1; i >= 0; i--) {
            LyricLine line = result.get(i);
            if (line.isBG()) continue;
            if (i + 1 < result.size()) {
                LyricLine nextLine = result.get(i + 1);
                if (nextLine.isBG()) {
                    float minStart = Math.min(line.startTime(), nextLine.startTime());
                    result.set(i, new LyricLine(minStart, line.endTime(), line.text(), line.isBG()));
                    result.set(i + 1, new LyricLine(minStart, nextLine.endTime(), nextLine.text(), nextLine.isBG()));
                }
            }
        }

        for (int i = 0; i < result.size() - 1; i++) {
            LyricLine curr = result.get(i);
            LyricLine next = result.get(i + 1);
            if (curr.endTime() <= 0.001f) {
                result.set(i, new LyricLine(curr.startTime(), next.startTime(), curr.text(), curr.isBG()));
            }
        }

        if (!result.isEmpty()) {
            int lastIndex = result.size() - 1;
            LyricLine last = result.get(lastIndex);
            if (last.endTime() <= 0.001f) {
                result.set(lastIndex, new LyricLine(last.startTime(), last.startTime() + 10f, last.text(), last.isBG()));
            }
        }

        result.removeIf(line -> line.text().isBlank());

        return result;
    }

    private int activeLyricIndex(float elapsed) {
        updateAMLLTimeline(elapsed);
        return Math.max(0, Math.min(amllScrollToIndex, Math.max(0, lyricLines.size() - 1)));
    }

    private void resetAMLLTimeline() {
        amllHotGroups.clear();
        amllBufferedGroups.clear();
        amllScrollToIndex = 0;
        amllLastTimelineSeconds = -1f;
    }

    private void updateAMLLTimeline(float elapsed) {
        if (lyricLines.isEmpty()) {
            resetAMLLTimeline();
            return;
        }
        if (Math.abs(elapsed - amllLastTimelineSeconds) < 0.001f) return;
        amllLastTimelineSeconds = elapsed;

        Set<Integer> nextHotGroups = new HashSet<>(amllHotGroups);
        Set<Integer> addedIds = new HashSet<>();
        Set<Integer> removedHotIds = new HashSet<>();
        Set<Integer> removedBufferedIds = new HashSet<>();

        for (Integer hotId : new HashSet<>(amllHotGroups)) {
            if (!isGroupActiveAt(hotId, elapsed)) {
                nextHotGroups.remove(hotId);
                removedHotIds.add(hotId);
            }
        }

        for (int i = 0; i < lyricLines.size(); i++) {
            if (isAttachedBgLine(i)) continue;
            if (isGroupActiveAt(i, elapsed) && !nextHotGroups.contains(i)) {
                nextHotGroups.add(i);
                addedIds.add(i);
            }
        }

        for (Integer id : new HashSet<>(amllBufferedGroups)) {
            if (!nextHotGroups.contains(id)) removedBufferedIds.add(id);
        }

        amllHotGroups.clear();
        amllHotGroups.addAll(nextHotGroups);

        if (addedIds.size() > 0) {
            amllBufferedGroups.addAll(addedIds);
            for (Integer id : removedBufferedIds) amllBufferedGroups.remove(id);
            if (!amllBufferedGroups.isEmpty()) amllScrollToIndex = minSet(amllBufferedGroups);
        } else if (!removedBufferedIds.isEmpty() && removedBufferedIds.containsAll(amllBufferedGroups) && amllBufferedGroups.containsAll(removedBufferedIds)) {
            for (Integer id : removedBufferedIds) {
                if (!amllHotGroups.contains(id)) amllBufferedGroups.remove(id);
            }
        }

        if (amllBufferedGroups.isEmpty()) {
            int lastMain = lastMainLineIndex();
            if (lastMain >= 0 && elapsed >= lyricLines.get(lastMain).endTime()) {
                amllScrollToIndex = lastMain;
            } else if (amllScrollToIndex < 0 || amllScrollToIndex >= lyricLines.size()) {
                amllScrollToIndex = firstGroupAtOrAfter(elapsed);
            }
        }
    }

    private boolean isGroupActiveAt(int mainIndex, float elapsed) {
        if (mainIndex < 0 || mainIndex >= lyricLines.size()) return false;
        LyricLine main = lyricLines.get(mainIndex);
        return main.startTime() <= elapsed && main.endTime() > elapsed;
    }

    private int minSet(Set<Integer> values) {
        int min = Integer.MAX_VALUE;
        for (Integer value : values) if (value != null && value < min) min = value;
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private int maxSet(Set<Integer> values, int fallback) {
        int max = Integer.MIN_VALUE;
        for (Integer value : values) if (value != null && value > max) max = value;
        return max == Integer.MIN_VALUE ? fallback : max;
    }

    private int lastMainLineIndex() {
        for (int i = lyricLines.size() - 1; i >= 0; i--) {
            if (!isAttachedBgLine(i)) return i;
        }
        return -1;
    }

    private int firstGroupAtOrAfter(float elapsed) {
        for (int i = 0; i < lyricLines.size(); i++) {
            if (!isAttachedBgLine(i) && lyricLines.get(i).startTime() >= elapsed) return i;
        }
        int lastMain = lastMainLineIndex();
        return lastMain >= 0 ? lastMain : 0;
    }

    private float elapsedSeconds(MusicLoader.MusicTrack track, long now) {
        if (MusicLoader.getCurrentTrack() == track) {
            return MusicLoader.getCurrentSeconds();
        }
        return 0f;
    }

    private float displayedElapsedSeconds(MusicLoader.MusicTrack track, long now) {
        if (draggingProgress && track == currentUiTrack && progressDragSeconds >= 0f) {
            return clamp(progressDragSeconds, 0f, Math.max(0, track.duration()));
        }
        return elapsedSeconds(track, now);
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
        blitStaticLayer(g);
        blitRenderer(g, controlsRenderer, controlsLayerX + leftVisualOffset(), controlsLayerY);
        float lyricAlpha = clamp(lyricOpacityVisual, 0f, 1f);
        if (currentUiTrack != null && lyricAlpha > 0.01f && !blitLyricLines(g, lyricAlpha)) blitLyricsRenderer(g, lyricAlpha);
        pose.popMatrix();
        float entryA = 220f * (1f - easeInOut(entryT));
        float returnA = 225f * returnE;
        float closeA = 205f * closeE;
        int a = Math.max(0, Math.min(240, Math.round(Math.max(entryA, Math.max(returnA, closeA)))));
        if (a > 0) g.fill(0, 0, width, height, a << 24);
    }

    private void blitStaticLayer(GuiGraphicsExtractor g) {
        if (staticRenderer == null) return;
        float offset = leftVisualOffset();
        if (Math.abs(offset) <= 0.01f) {
            blitRenderer(g, staticRenderer, 0f, 0f);
            return;
        }
        float movingW = leftMovingRegionWidth();
        blitRendererRegion(g, staticRenderer, offset, 0f, movingW, height, 0f, 0f, movingW, height);
        if (movingW < width - 0.5f) blitRendererRegion(g, staticRenderer, movingW, 0f, width - movingW, height, movingW, 0f, width - movingW, height);
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

    private void blitBackgroundRenderer(GuiGraphicsExtractor g) {
        if (gpuBackground != null && gpuBackground.draw(g, width, height, backgroundRenderTime, backgroundLowFreqPulse)) {
            drawAMLLBackgroundOverlay(g);
            return;
        }
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
        drawAMLLBackgroundOverlay(g);
    }

    private void drawAMLLBackgroundOverlay(GuiGraphicsExtractor g) {
        int startY = Math.max(0, Math.min(height, Math.round(height * 0.60f)));
        int steps = 20;
        int span = Math.max(1, height - startY);
        for (int i = 0; i < steps; i++) {
            int y0 = startY + Math.round(span * (i / (float) steps));
            int y1 = startY + Math.round(span * ((i + 1f) / steps));
            int alpha = Math.max(0, Math.min(0x1A, Math.round(0x1A * ((i + 1f) / steps))));
            if (y1 > y0 && alpha > 0) g.fill(0, y0, width, y1, alpha << 24);
        }
    }



    private void updateActiveDynamicRenderer(CachedLyricLine cached, float currentSeconds, float s) {
        if (cached.whiteImage == null) return;
        SkijaRenderer renderer = cached.activeRenderer;
        renderer.clear(0x00000000);
        Canvas c = renderer.canvas();
        c.save();
        float drawScale = Math.max(1f, guiScale);
        c.scale(drawScale, drawScale);
        float w = cached.activeW;
        float h = cached.activeH;
        WordRange activeRange = amllCurrentWordRange(cached, currentSeconds);
        float contentH = Math.max(1f, h - cached.activePad * 2f);
        float fade = Math.max(0.0001f, (activeRange != null ? activeRange.h() : contentH) * 0.5f);
        float gradientX = activeRange == null ? cached.activeW : amllDynamicMaskX(cached, currentSeconds, fade);
        try (Paint basePaint = new Paint()) {
            basePaint.setAntiAlias(true);
            basePaint.setColor(0xB8FFFFFF);
            c.drawImageRect(cached.activeImage, Rect.makeXYWH(0, 0, w, h), basePaint);

            c.saveLayer(null, null);
            try (Paint maskPaint = new Paint()) {
                maskPaint.setAntiAlias(true);
                maskPaint.setColor(0xFFFFFFFF);
                c.drawImageRect(cached.whiteImage, Rect.makeXYWH(0, 0, w, h), maskPaint);
            }
            try (Paint gradientPaint = new Paint(); io.github.humbleui.skija.Shader shader = io.github.humbleui.skija.Shader.makeLinearGradient(
                    gradientX - fade, 0f, gradientX + fade, 0f,
                    new int[]{withAlpha(WHITE, cached.currentBrightAlpha), withAlpha(WHITE, cached.currentDarkAlpha)})) {
                gradientPaint.setAntiAlias(true);
                gradientPaint.setBlendMode(io.github.humbleui.skija.BlendMode.SRC_IN);
                gradientPaint.setShader(shader);
                if (activeRange == null) {
                    gradientPaint.setColor(withAlpha(WHITE, cached.currentBrightAlpha));
                    c.drawRect(Rect.makeXYWH(0, 0, w, h), gradientPaint);
                } else {
                    drawAMLLDynamicMaskSegments(c, cached, activeRange, currentSeconds, fade, w);
                    drawAMLLRomanMaskSegments(c, cached, activeRange, currentSeconds, w);
                }
            }
            if (activeRange != null && activeRange.emphasize()) {
                drawAMLLWordEmphasis(c, cached, activeRange, currentSeconds);
            }
            c.restore();
        }
        c.restore();
        renderer.upload();
    }

    private void drawAMLLDynamicMaskSegments(Canvas c, CachedLyricLine cached, WordRange activeRange, float currentSeconds, float fade, float w) {
        if (cached.dynamicWordRanges == null || cached.dynamicWordRanges.isEmpty()) return;
        for (WordRange range : cached.dynamicWordRanges) {
            if (currentSeconds < range.startTime()) break;
            float x = Math.max(0f, range.startX() - fade);
            float rectW = Math.max(1f, range.endX() - range.startX() + fade * 2f);
            if (currentSeconds > range.endTime()) {
                try (Paint paint = new Paint()) {
                    paint.setAntiAlias(true);
                    paint.setBlendMode(io.github.humbleui.skija.BlendMode.SRC_IN);
                    paint.setColor(withAlpha(WHITE, cached.currentBrightAlpha));
                    c.drawRect(Rect.makeXYWH(x, range.y(), rectW, range.h()), paint);
                }
            } else {
                float gradientX = amllDynamicMaskX(cached, currentSeconds, fade);
                try (Paint paint = new Paint(); io.github.humbleui.skija.Shader shader = io.github.humbleui.skija.Shader.makeLinearGradient(
                        gradientX - fade, 0f, gradientX + fade, 0f,
                        new int[]{withAlpha(WHITE, cached.currentBrightAlpha), withAlpha(WHITE, cached.currentDarkAlpha)})) {
                    paint.setAntiAlias(true);
                    paint.setBlendMode(io.github.humbleui.skija.BlendMode.SRC_IN);
                    paint.setShader(shader);
                    c.drawRect(Rect.makeXYWH(x, range.y(), rectW, range.h()), paint);
                }
                break;
            }
        }
    }

    private void drawAMLLRomanMaskSegments(Canvas c, CachedLyricLine cached, WordRange activeRange, float currentSeconds, float w) {
        if (activeRange.romanH() <= 0.5f || cached.dynamicWordRanges == null || cached.dynamicWordRanges.isEmpty()) return;
        boolean hasWordBox = activeRange.romanWordH() > 0.5f;
        if (!hasWordBox) {
            float romanFade = Math.max(0.0001f, activeRange.romanH() * 0.5f);
            float romanX = amllRomanMaskX(cached, activeRange, currentSeconds, romanFade);
            try (Paint paint = new Paint(); io.github.humbleui.skija.Shader shader = io.github.humbleui.skija.Shader.makeLinearGradient(
                    romanX - romanFade, 0f, romanX + romanFade, 0f,
                    new int[]{withAlpha(WHITE, cached.currentBrightAlpha), withAlpha(WHITE, cached.currentDarkAlpha)})) {
                paint.setAntiAlias(true);
                paint.setBlendMode(io.github.humbleui.skija.BlendMode.SRC_IN);
                paint.setShader(shader);
                c.drawRect(Rect.makeXYWH(0, activeRange.romanY(), w, activeRange.romanH()), paint);
            }
            return;
        }
        for (WordRange range : cached.dynamicWordRanges) {
            if (range.romanWordH() <= 0.5f || range.romanWordEndX() <= range.romanWordStartX()) continue;
            if (currentSeconds < range.startTime()) break;
            float romanFade = Math.max(0.0001f, range.romanWordH() * 0.5f);
            float x = Math.max(0f, range.romanWordStartX() - romanFade);
            float rectW = Math.max(1f, range.romanWordEndX() - range.romanWordStartX() + romanFade * 2f);
            if (currentSeconds > range.endTime()) {
                try (Paint paint = new Paint()) {
                    paint.setAntiAlias(true);
                    paint.setBlendMode(io.github.humbleui.skija.BlendMode.SRC_IN);
                    paint.setColor(withAlpha(WHITE, cached.currentBrightAlpha));
                    c.drawRect(Rect.makeXYWH(x, range.romanWordY(), rectW, range.romanWordH()), paint);
                }
            } else {
                float romanX = amllRomanMaskX(cached, range, currentSeconds, romanFade);
                try (Paint paint = new Paint(); io.github.humbleui.skija.Shader shader = io.github.humbleui.skija.Shader.makeLinearGradient(
                        romanX - romanFade, 0f, romanX + romanFade, 0f,
                        new int[]{withAlpha(WHITE, cached.currentBrightAlpha), withAlpha(WHITE, cached.currentDarkAlpha)})) {
                    paint.setAntiAlias(true);
                    paint.setBlendMode(io.github.humbleui.skija.BlendMode.SRC_IN);
                    paint.setShader(shader);
                    c.drawRect(Rect.makeXYWH(x, range.romanWordY(), rectW, range.romanWordH()), paint);
                }
                break;
            }
        }
    }

    private void drawAMLLWordEmphasis(Canvas c, CachedLyricLine cached, WordRange range, float currentSeconds) {
        float duration = Math.max(1f, range.endTime() - range.startTime());
        float du = duration;
        float amount = du / 2f;
        amount = amount > 1f ? (float) Math.sqrt(amount) : amount * amount * amount;
        float blur = du / 3f;
        blur = blur > 1f ? (float) Math.sqrt(blur) : blur * blur * blur;
        amount *= 0.6f;
        blur *= 0.5f;
        if (range.wordIndex() == range.wordCount() - 1) {
            amount *= 1.6f;
            blur *= 1.5f;
            du *= 1.2f;
        }
        amount = Math.min(1.2f, amount);
        blur = Math.min(0.8f, blur);
        int visualCharCount = Math.max(1, range.emphasisSlices() == null || range.emphasisSlices().isEmpty() ? emphasisCharacterCount(range.wordText()) : range.emphasisSlices().size());
        int anchorCharCount = Math.max(1, emphasisAnchorCharacterCount(range));
        float animateDu = Math.max(1f, du);
        for (int i = 0; i < visualCharCount; i++) {
            EmphasisSlice slice = emphasisSliceAt(range, i, visualCharCount);
            float wordDe = duration / 2.5f / anchorCharCount * i;
            float glowT = clamp((currentSeconds - range.startTime() - wordDe) / animateDu, 0f, 1f);
            float floatT = clamp((currentSeconds - range.startTime() - wordDe + 0.4f) / (animateDu * 1.4f), 0f, 1f);
            float transX = empEasing(glowT);
            float glowLevel = transX * blur;
            float floatY = (float) Math.sin(floatT * Math.PI);
            float scale = 1f + transX * 0.10f * amount;
            float offsetX = -transX * 0.03f * amount * (visualCharCount / 2f - i) * range.baseH();
            float offsetY = -transX * 0.025f * amount * range.baseH() - floatY * 0.05f * range.baseH();
            float sliceX = slice.startX() + cached.activePad;
            float sliceW = Math.max(1f, slice.endX() - slice.startX());
            float clipX = Math.max(0f, sliceX - range.h() * 0.22f);
            float clipY = Math.max(0f, range.y() + cached.activePad - range.h() * 0.38f);
            float clipW = Math.max(1f, sliceW + range.h() * 0.44f);
            float clipH = Math.max(1f, range.h() * 1.28f);
            float centerX = sliceX + sliceW * 0.5f;
            float centerY = range.y() + cached.activePad + range.h() * 0.5f;
            drawAMLLCharacterEmphasisSlice(c, cached, clipX, clipY, clipW, clipH, centerX, centerY, scale, offsetX, offsetY, glowLevel, blur, transX);
        }
    }

    private EmphasisSlice emphasisSliceAt(WordRange range, int index, int count) {
        if (range.emphasisSlices() != null && index >= 0 && index < range.emphasisSlices().size()) return range.emphasisSlices().get(index);
        float wordW = Math.max(1f, range.endX() - range.startX());
        float charW = wordW / Math.max(1, count);
        float start = range.startX() + index * charW;
        float end = index == count - 1 ? range.endX() : start + charW;
        return new EmphasisSlice(start, end);
    }

    private void drawAMLLCharacterEmphasisSlice(Canvas c, CachedLyricLine cached, float clipX, float clipY, float clipW, float clipH, float centerX, float centerY, float scale, float offsetX, float offsetY, float glowLevel, float blur, float transX) {
        c.save();
        c.clipRect(Rect.makeXYWH(clipX, clipY, clipW, clipH));
        float drawX = centerX - centerX * scale + offsetX;
        float drawY = centerY - centerY * scale + offsetY;
        float scaledW = cached.activeW * scale;
        float scaledH = cached.activeH * scale;
        try (Paint glowPaint = new Paint(); MaskFilter glow = MaskFilter.makeBlur(FilterBlurMode.NORMAL, Math.max(0.5f, Math.min(0.3f, blur * 0.3f) * 48f), false)) {
            glowPaint.setAntiAlias(true);
            glowPaint.setColor(withAlpha(WHITE, clamp(0.10f, glowLevel, 0.65f)));
            glowPaint.setMaskFilter(glow);
            c.drawImageRect(cached.whiteImage, Rect.makeXYWH(drawX, drawY, scaledW, scaledH), glowPaint);
        }
        try (Paint sharpPaint = new Paint()) {
            sharpPaint.setAntiAlias(true);
            sharpPaint.setColor(withAlpha(WHITE, 0.25f + 0.45f * transX));
            c.drawImageRect(cached.whiteImage, Rect.makeXYWH(drawX, drawY, scaledW, scaledH), sharpPaint);
        }
        c.restore();
    }

    private int emphasisCharacterCount(String text) {
        if (text == null || text.isBlank()) return 1;
        int count = 0;
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            if (!Character.isWhitespace(cp)) count++;
            i += Character.charCount(cp);
        }
        return Math.max(1, count);
    }

    private int emphasisAnchorCharacterCount(WordRange range) {
        if (range.ruby() != null && !range.ruby().isEmpty()) {
            int count = 0;
            for (RubyText ruby : range.ruby()) {
                count += emphasisCharacterCount(ruby.text());
            }
            if (count > 0) return count;
        }
        return emphasisCharacterCount(range.wordText());
    }

    private float empEasing(float x) {
        x = clamp01(x);
        return x < 0.5f ? cubicBezier(x / 0.5f, 0.2f, 0.4f, 0.58f, 1.0f) : 1f - cubicBezier((x - 0.5f) / 0.5f, 0.3f, 0.0f, 0.58f, 1.0f);
    }

    private WordRange amllCurrentWordRange(CachedLyricLine cached, float currentSeconds) {
        if (cached.dynamicWordRanges == null || cached.dynamicWordRanges.isEmpty()) return null;
        WordRange first = cached.dynamicWordRanges.get(0);
        if (currentSeconds <= first.startTime()) return first;
        WordRange previous = first;
        for (int i = 0; i < cached.dynamicWordRanges.size(); i++) {
            WordRange range = cached.dynamicWordRanges.get(i);
            if (currentSeconds >= range.startTime() && currentSeconds <= range.endTime()) return range;
            if (currentSeconds < range.startTime()) return previous;
            previous = range;
        }
        return null;
    }

    private float amllDynamicMaskX(CachedLyricLine cached, float currentSeconds, float fadeWidth) {
        WordRange range = amllCurrentWordRange(cached, currentSeconds);
        if (range == null) return cached.activeW;
        if (currentSeconds < range.startTime()) return range.startX() - fadeWidth;
        if (currentSeconds > range.endTime()) return range.endX() + fadeWidth;
        return range.startX() + amllRangeProgressWidth(range, currentSeconds, fadeWidth, false) + fadeWidth * 0.5f;
    }

    private float amllRomanMaskX(CachedLyricLine cached, WordRange activeRange, float currentSeconds, float fadeWidth) {
        WordRange range = amllCurrentWordRange(cached, currentSeconds);
        if (range == null) range = activeRange;
        if (range.romanWordH() > 0.5f && range.romanWordEndX() > range.romanWordStartX()) {
            if (currentSeconds < range.startTime()) return range.romanWordStartX() - fadeWidth;
            if (currentSeconds > range.endTime()) return range.romanWordEndX() + fadeWidth;
            return range.romanWordStartX() + amllRangeProgressWidth(range, currentSeconds, fadeWidth, true) + fadeWidth * 0.5f;
        }
        if (range.romanH() <= 0.5f) return range.romanStartX() - fadeWidth;
        if (currentSeconds < range.startTime()) return range.romanStartX() - fadeWidth;
        if (currentSeconds > range.endTime()) return range.romanEndX() + fadeWidth;
        float progress = clamp((currentSeconds - range.startTime()) / Math.max(0.001f, range.endTime() - range.startTime()), 0f, 1f);
        return range.romanStartX() + (range.romanEndX() - range.romanStartX()) * progress + fadeWidth * 0.5f;
    }

    private float amllRangeProgressWidth(WordRange range, float currentSeconds, float fadeWidth, boolean roman) {
        float wordWidth = roman ? Math.max(0f, range.romanWordEndX() - range.romanWordStartX()) : Math.max(0f, range.endX() - range.startX());
        if (wordWidth <= 0f) return 0f;
        if (!roman && range.ruby() != null && !range.ruby().isEmpty()) return amllRubySegmentProgressWidth(range, currentSeconds, fadeWidth, wordWidth);
        return amllSingleSegmentProgressWidth(range, currentSeconds, fadeWidth, wordWidth);
    }

    private float amllSingleSegmentProgressWidth(WordRange range, float currentSeconds, float fadeWidth, float wordWidth) {
        float duration = clampPositive(range.endTime() - range.startTime());
        if (duration <= 0f) return currentSeconds >= range.endTime() ? amllApplyEdgeFade(range, wordWidth, fadeWidth, 1f, true, true) : 0f;
        float progress = clamp((currentSeconds - range.startTime()) / duration, 0f, 1f);
        float width = wordWidth * progress;
        return amllApplyEdgeFade(range, width, fadeWidth, progress, progress > 0f, progress >= 1f);
    }

    private float amllRubySegmentProgressWidth(WordRange range, float currentSeconds, float fadeWidth, float wordWidth) {
        int rubyCharCount = 0;
        for (RubyText ruby : range.ruby()) {
            if (ruby.text() != null) rubyCharCount += Math.max(0, ruby.text().length());
        }
        if (rubyCharCount <= 0) return amllSingleSegmentProgressWidth(range, currentSeconds, fadeWidth, wordWidth);
        float widthPerChar = wordWidth / rubyCharCount;
        float width = 0f;
        int charIndex = 0;
        float lastTimeStamp = range.startTime();
        for (RubyText ruby : range.ruby()) {
            String text = ruby.text() == null ? "" : ruby.text();
            int charCount = text.length();
            if (charCount <= 0) continue;
            float rubyStartTime = Float.isFinite(ruby.startTime()) ? ruby.startTime() : range.startTime();
            float rubyEndTime = Float.isFinite(ruby.endTime()) ? ruby.endTime() : range.endTime();
            float rubyStart = Math.max(rubyStartTime, range.startTime());
            float rubyEnd = Math.min(Math.max(rubyEndTime, rubyStart), range.endTime());
            if (currentSeconds < rubyStart) return width;
            lastTimeStamp = rubyStart;
            float rubyDuration = clampPositive(rubyEnd - rubyStart);
            if (rubyDuration <= 0f) {
                for (int i = 0; i < charCount; i++) {
                    width += widthPerChar;
                    width = amllApplyEdgeFade(range, width, fadeWidth, 1f, charIndex == 0, charIndex == rubyCharCount - 1);
                    charIndex++;
                }
                lastTimeStamp = rubyEnd;
                continue;
            }
            float perCharDuration = rubyDuration / charCount;
            for (int i = 0; i < charCount; i++) {
                float charStart = rubyStart + perCharDuration * i;
                float charEnd = charStart + perCharDuration;
                if (currentSeconds >= charEnd) {
                    width += widthPerChar;
                    width = amllApplyEdgeFade(range, width, fadeWidth, 1f, charIndex == 0, charIndex == rubyCharCount - 1);
                } else if (currentSeconds > charStart) {
                    float p = clamp((currentSeconds - charStart) / Math.max(0.001f, perCharDuration), 0f, 1f);
                    width += widthPerChar * p;
                    width = amllApplyEdgeFade(range, width, fadeWidth, p, charIndex == 0, charIndex == rubyCharCount - 1);
                    return width;
                } else {
                    return width;
                }
                lastTimeStamp = charEnd;
                charIndex++;
            }
            if (currentSeconds < rubyEnd) return width;
            lastTimeStamp = rubyEnd;
        }
        if (currentSeconds >= Math.max(range.endTime(), lastTimeStamp)) return width;
        return width;
    }

    private float amllApplyEdgeFade(WordRange range, float width, float fadeWidth, float progress, boolean firstSegment, boolean lastSegment) {
        float result = width;
        if (range.wordIndex() == 0 && firstSegment && progress > 0f) result += fadeWidth * 1.5f * clamp01(progress);
        if (range.wordIndex() == range.wordCount() - 1 && lastSegment && progress >= 1f) result += fadeWidth * 0.5f;
        return result;
    }

    private boolean blitLyricLines(GuiGraphicsExtractor g, float globalAlpha) {
        if (!useLineLyricTextures()) return false;
        SkijaTestScreen.ensureFontLoaded();
        Typeface tf = SkijaTestScreen.curTf;
        float s = pageScale();
        float activePreferred = amllLyricFontSize();
        float inactivePreferred = amllInactiveLyricFontSize(activePreferred);
        int active = lastActiveLyric >= 0 ? lastActiveLyric : activeLyricIndex(elapsedSeconds(currentUiTrack, System.currentTimeMillis()));
        if (ensureLyricCache(tf, layoutRightW, activePreferred, inactivePreferred, s)) {
            float currentSeconds = currentUiTrack == null ? 0f : displayedElapsedSeconds(currentUiTrack, System.currentTimeMillis());
            InterludeState interlude = computeInterlude(active, currentSeconds);
            updateLyricLineAnimations(active, currentSeconds, s, 0f, true, interlude);
        }
        if (lyricCache.size() != lyricLines.size()) return false;
        if (lyricCache.isEmpty()) return true;

        float currentSecondsDyn = currentUiTrack == null ? 0f : displayedElapsedSeconds(currentUiTrack, System.currentTimeMillis());
        if (active >= 0 && active < lyricCache.size()) {
            for (int i = 0; i < lyricCache.size(); i++) {
                if (!isActiveLine(i, active)) continue;
                CachedLyricLine activeLine = lyricCache.get(i);
                if (activeLine.lineRef != null && activeLine.lineRef.isDynamic()) {
                    updateActiveDynamicRenderer(activeLine, currentSecondsDyn, s);
                }
            }
        }

        float clipX = layoutRightX - amllLyricLinePaddingX(activePreferred);
        float clipY = layoutLyricTop;
        float clipW = layoutRightW + amllLyricLinePaddingX(activePreferred) * 2f;
        float clipH = layoutLyricH;
        float overscan = amllCssPx(300f);
        float overscanTop = clipY - overscan;
        float overscanBottom = clipY + clipH + overscan;
        boolean allowNewBlurBake = !lyricLayerAnimating();
        bakedBlurBudgetThisFrame = 0;
        for (int i = 0; i < lyricCache.size(); i++) {
            CachedLyricLine line = lyricCache.get(i);
            boolean isActive = isActiveLine(i, active);
            float blurLevel = line.currentBlurVisual;
            boolean useBlur = shouldUseAMLLLineBlur(isActive, blurLevel);
            SkijaRenderer lineRenderer = isActive ? line.activeRenderer : line.inactiveRenderer;
            Image lineImage = isActive ? line.activeImage : line.inactiveImage;
            if (lineRenderer == null || !line.initialized) continue;
            float alpha = clamp(line.currentOpacity * globalAlpha, 0f, 1f);
            if (alpha <= 0.01f) continue;
            float scale = clamp(line.currentScale, 0.86f, 1.08f);
            float texW = isActive ? line.activeW : line.inactiveW;
            float texH = isActive ? line.activeH : line.inactiveH;
            float pad = isActive ? line.activePad : line.inactivePad;
            float contentH = Math.max(1f, texH - pad * 2f);
            float rawX = layoutRightX - pad;
            if (line.lineRef != null && line.lineRef.isDuet()) {
                rawX = layoutRightX + layoutRightW - texW + pad;
            }
            float rawY = line.currentY - pad;
            float originX = layoutRightX;
            float originY = line.currentY + contentH * 0.5f;
            float drawX = originX + (rawX - originX) * scale;
            float drawY = originY + (rawY - originY) * scale;
            float drawW = texW * scale;
            float drawH = texH * scale;
            if (drawY + drawH < overscanTop || drawY > overscanBottom) continue;
            alpha *= amllLyricEdgeMaskAlpha(drawY, drawH, clipY, clipH);
            if (alpha <= 0.01f) continue;
            if (useBlur) {
                int targetStep = amllBlurStep(blurLevel);
                boolean alreadyBaked = line.bakedBlurImage != null && line.bakedBlurRenderer != null && line.bakedBlurStep == targetStep;
                boolean canBake = alreadyBaked || (allowNewBlurBake && bakedBlurBudgetThisFrame < 1);
                if (canBake && ensureBakedBlur(line, lineImage, texW, texH, blurLevel)) {
                    if (!alreadyBaked) bakedBlurBudgetThisFrame++;
                    float bakedPad = line.bakedBlurPad * scale;
                    blitRendererClippedAlpha(g, line.bakedBlurRenderer, drawX - bakedPad, drawY - bakedPad, line.bakedBlurW * scale, line.bakedBlurH * scale, alpha, clipX, clipY, clipW, clipH);
                    continue;
                }
            }
            blitRendererClippedAlpha(g, lineRenderer, drawX, drawY, drawW, drawH, alpha, clipX, clipY, clipW, clipH);
        }
        blitInterludeDots(g, s, clipX, clipY, clipW, clipH, globalAlpha);
        return true;
    }

    private float computeAMLLGroupBlur(int index) {
        return computeAMLLLineBlur(groupMainIndex(index));
    }

    private float amllLyricEdgeMaskAlpha(float y, float h, float clipY, float clipH) {
        float fade = Math.max(1f, clipH * 0.10f);
        float center = y + h * 0.5f;
        float top = clamp((center - clipY) / fade, 0f, 1f);
        float bottom = clamp((clipY + clipH - center) / fade, 0f, 1f);
        return Math.min(top, bottom);
    }

    private float computeAMLLLineBlur(int index) {
        int groupIndex = groupMainIndex(index);
        if (isAMLLGroupActive(groupIndex)) return 0f;
        int latest = maxSet(amllBufferedGroups, amllScrollToIndex);
        float blur = 1f;
        if (groupIndex < amllScrollToIndex) blur += Math.abs(amllScrollToIndex - groupIndex) + 1f;
        else blur += Math.abs(groupIndex - Math.max(amllScrollToIndex, latest));
        if (amllViewportWidthPx() <= 1024f) blur *= 0.8f;
        return blur;
    }

    private boolean shouldUseAMLLLineBlur(boolean isActive, float blurLevel) {
        return !hoveredLyrics && !isActive && amllBlurRadius(blurLevel) > 0.05f;
    }

    private float amllBlurRadius(float blurLevel) {
        return amllCssPx(clamp(blurLevel, 0f, 5f));
    }

    private int amllBlurStep(float blurLevel) {
        float cssRadius = clamp(blurLevel, 0f, 5f);
        if (cssRadius <= 0.05f) return 0;
        return Math.max(1, Math.round(cssRadius));
    }

    private float amllBlurRadiusForStep(int step) {
        return amllCssPx(clamp(step, 0, 5));
    }

    private void blitAMLLSoftBlurredLine(GuiGraphicsExtractor g, SkijaRenderer r, float x, float y, float w, float h, float alpha, float clipX, float clipY, float clipW, float clipH, float blurLevel) {
        float radius = amllBlurRadius(blurLevel);
        if (radius <= 0.05f) {
            blitRendererClippedAlpha(g, r, x, y, w, h, alpha, clipX, clipY, clipW, clipH);
            return;
        }
        float t = clamp01(radius / Math.max(0.001f, amllCssPx(5f)));
        float innerAxis = radius * (0.85f + 0.15f * t);
        float innerDiagonal = innerAxis * 0.7071f;
        float outerAxis = radius * (1.65f + 0.25f * t);
        float outerDiagonal = outerAxis * 0.7071f;
        float centerAlpha = alpha * (0.22f - 0.12f * t);
        float innerAxisAlpha = alpha * (0.095f - 0.015f * t);
        float innerDiagonalAlpha = alpha * (0.055f - 0.010f * t);
        float outerAxisAlpha = alpha * (0.050f + 0.010f * t);
        float outerDiagonalAlpha = Math.max(0f, (alpha - centerAlpha - innerAxisAlpha * 4f - innerDiagonalAlpha * 4f - outerAxisAlpha * 4f) * 0.25f);
        blitRendererClippedAlpha(g, r, x, y, w, h, centerAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x - innerAxis, y, w, h, innerAxisAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x + innerAxis, y, w, h, innerAxisAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x, y - innerAxis, w, h, innerAxisAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x, y + innerAxis, w, h, innerAxisAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x - innerDiagonal, y - innerDiagonal, w, h, innerDiagonalAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x + innerDiagonal, y - innerDiagonal, w, h, innerDiagonalAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x - innerDiagonal, y + innerDiagonal, w, h, innerDiagonalAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x + innerDiagonal, y + innerDiagonal, w, h, innerDiagonalAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x - outerAxis, y, w, h, outerAxisAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x + outerAxis, y, w, h, outerAxisAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x, y - outerAxis, w, h, outerAxisAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x, y + outerAxis, w, h, outerAxisAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x - outerDiagonal, y - outerDiagonal, w, h, outerDiagonalAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x + outerDiagonal, y - outerDiagonal, w, h, outerDiagonalAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x - outerDiagonal, y + outerDiagonal, w, h, outerDiagonalAlpha, clipX, clipY, clipW, clipH);
        blitRendererClippedAlpha(g, r, x + outerDiagonal, y + outerDiagonal, w, h, outerDiagonalAlpha, clipX, clipY, clipW, clipH);
    }

    private void blitInterludeDots(GuiGraphicsExtractor g, float s, float clipX, float clipY, float clipW, float clipH, float globalAlpha) {
        if (interludeOpacity <= 0.01f) return;
        if (!ensureInterludeDotRenderer(s)) return;
        float duration = Math.max(0.001f, interludeEndTime - interludeStartTime);
        float current = clamp(interludeCurrentTime - interludeStartTime, 0f, duration);
        float interludeDuration = duration * 1000f;
        float currentDuration = current * 1000f;
        float breatheDuration = interludeDuration / Math.max(1f, (float) Math.ceil(interludeDuration / 1500f));
        float scale = 1f;

        float globalOpacity = 1f;
        scale *= (float) Math.sin(1.5f * Math.PI - currentDuration / breatheDuration * 2f) / 20f + 1f;
        if (currentDuration < 2000f) scale *= easeOutExpo(currentDuration / 2000f);
        if (currentDuration < 500f) globalOpacity = 0f;
        else if (currentDuration < 1000f) globalOpacity *= (currentDuration - 500f) / 500f;
        if (interludeDuration - currentDuration < 750f) scale *= 1f - easeInOutBack((750f - (interludeDuration - currentDuration)) / 750f / 2f);
        if (interludeDuration - currentDuration < 375f) globalOpacity *= clamp01((interludeDuration - currentDuration) / 375f);
        float dotsDuration = clampPositive(interludeDuration - 750f);
        float groupScale = clampPositive(scale) * 0.7f;

        float dot = amllInterludeDotSize();
        float gap = amllInterludeDotGap();
        float contentW = dot * 3f + gap * 2f;
        float baseX = interludeNextDuet
                ? layoutRightX + layoutRightW - contentW * groupScale - amllInterludeDotPaddingX()
                : layoutRightX + amllInterludeDotPaddingX();
        float baseY = interludeY + interludeSlotHeight(s) * 0.5f;

        float dot0Opacity = clamp(0.25f, currentDuration * 3f / dotsDuration * 0.75f, 1f);
        float dot1Opacity = clamp(0.25f, (currentDuration - dotsDuration / 3f) * 3f / dotsDuration * 0.75f, 1f);
        float dot2Opacity = clamp(0.25f, (currentDuration - dotsDuration / 3f * 2f) * 3f / dotsDuration * 0.75f, 1f);

        for (int i = 0; i < 3; i++) {
            float dotOpacity = i == 0 ? dot0Opacity : i == 1 ? dot1Opacity : dot2Opacity;
            float size = dot * groupScale;
            float x = baseX + (i * (dot + gap)) * groupScale;
            float y = baseY - size * 0.5f;
            blitRendererClippedAlpha(g, interludeDotRenderer, x, y, size, size, clamp01(interludeOpacity * globalOpacity * dotOpacity * globalAlpha), clipX, clipY, clipW, clipH);
        }
    }

    private void blitInterludeDotsToLayer(Canvas c, float s, float clipX, float clipY, float clipW, float clipH) {
        if (interludeOpacity <= 0.01f) return;
        float duration = Math.max(0.001f, interludeEndTime - interludeStartTime);
        float current = clamp(interludeCurrentTime - interludeStartTime, 0f, duration);
        float interludeDuration = duration * 1000f;
        float currentDuration = current * 1000f;
        float breatheDuration = interludeDuration / Math.max(1f, (float) Math.ceil(interludeDuration / 1500f));
        float scale = 1f;

        float globalOpacity = 1f;
        scale *= (float) Math.sin(1.5f * Math.PI - currentDuration / breatheDuration * 2f) / 20f + 1f;
        if (currentDuration < 2000f) scale *= easeOutExpo(currentDuration / 2000f);
        if (currentDuration < 500f) globalOpacity = 0f;
        else if (currentDuration < 1000f) globalOpacity *= (currentDuration - 500f) / 500f;
        if (interludeDuration - currentDuration < 750f) scale *= 1f - easeInOutBack((750f - (interludeDuration - currentDuration)) / 750f / 2f);
        if (interludeDuration - currentDuration < 375f) globalOpacity *= clamp01((interludeDuration - currentDuration) / 375f);
        float dotsDuration = clampPositive(interludeDuration - 750f);
        float groupScale = clampPositive(scale) * 0.7f;

        float dot = amllInterludeDotSize();
        float gap = amllInterludeDotGap();
        float contentW = dot * 3f + gap * 2f;
        float baseX = interludeNextDuet
                ? layoutRightX + layoutRightW - contentW * groupScale - amllInterludeDotPaddingX()
                : layoutRightX + amllInterludeDotPaddingX();
        float baseY = interludeY + interludeSlotHeight(s) * 0.5f;

        float dot0Opacity = clamp(0.25f, currentDuration * 3f / dotsDuration * 0.75f, 1f);
        float dot1Opacity = clamp(0.25f, (currentDuration - dotsDuration / 3f) * 3f / dotsDuration * 0.75f, 1f);
        float dot2Opacity = clamp(0.25f, (currentDuration - dotsDuration / 3f * 2f) * 3f / dotsDuration * 0.75f, 1f);

        try (Paint paint = new Paint()) {
            paint.setAntiAlias(true);
            for (int i = 0; i < 3; i++) {
                float dotOpacity = i == 0 ? dot0Opacity : i == 1 ? dot1Opacity : dot2Opacity;
                float size = dot * groupScale;
                float x = baseX + (i * (dot + gap)) * groupScale;
                float y = baseY - size * 0.5f;
                if (x + size < clipX || x > clipX + clipW || y + size < clipY || y > clipY + clipH) continue;
                float alpha = clamp01(interludeOpacity * globalOpacity * dotOpacity);
                if (alpha <= 0.01f) continue;
                paint.setColor(withAlpha(WHITE, alpha));
                c.drawCircle(x + size * 0.5f, y + size * 0.5f, size * 0.5f, paint);
            }
        }
    }

    private boolean ensureInterludeDotRenderer(float s) {
        float dot = amllInterludeDotSize();
        if (interludeDotRenderer != null && Math.abs(interludeDotScale - dot) < 0.001f) return true;
        if (interludeDotRenderer != null) interludeDotRenderer.close();
        interludeDotScale = dot;
        int size = Math.max(2, Math.round(dot * guiScale));
        interludeDotRenderer = new SkijaRenderer("music_page_interlude_dot", size, size);
        interludeDotRenderer.clear(0x00000000);
        Canvas c = interludeDotRenderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        try (Paint paint = new Paint()) {
            paint.setAntiAlias(true);
            paint.setColor(WHITE);
            c.drawCircle(dot * 0.5f, dot * 0.5f, dot * 0.5f, paint);
        }
        c.restore();
        interludeDotRenderer.upload();
        return true;
    }

    private void blitRendererClippedAlpha(GuiGraphicsExtractor g, SkijaRenderer r, float dstX, float dstY, float dstW, float dstH, float alpha, float clipX, float clipY, float clipW, float clipH) {
        if (r == null || dstW <= 0.5f || dstH <= 0.5f || clipW <= 0.5f || clipH <= 0.5f) return;
        float left = Math.max(dstX, clipX);
        float top = Math.max(dstY, clipY);
        float right = Math.min(dstX + dstW, clipX + clipW);
        float bottom = Math.min(dstY + dstH, clipY + clipH);
        if (right <= left + 0.5f || bottom <= top + 0.5f) return;
        float u0 = (left - dstX) / dstW;
        float v0 = (top - dstY) / dstH;
        float u1 = (right - dstX) / dstW;
        float v1 = (bottom - dstY) / dstH;
        blitRendererAlpha(g, r, left, top, right - left, bottom - top, u0, v0, u1, v1, alpha);
    }

    private void blitRendererAlpha(GuiGraphicsExtractor g, SkijaRenderer r, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float alpha) {
        if (r == null || w <= 0.5f || h <= 0.5f) return;
        AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(r.textureId());
        if (abstractTexture == null || abstractTexture.getTextureView() == null || abstractTexture.getSampler() == null) return;
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle scissor = g.scissorStack.peek();
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bw = Math.max(1, (int) Math.ceil(x + w) - bx);
        int bh = Math.max(1, (int) Math.ceil(y + h) - by);
        ScreenRectangle bounds = new ScreenRectangle(bx, by, bw, bh).transformMaxBounds(pose);
        TextureSetup textureSetup = TextureSetup.singleTexture(abstractTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        int a = Math.max(0, Math.min(255, Math.round(clamp(alpha, 0f, 1f) * 255f)));
        g.guiRenderState.addGuiElement(new TexturedQuadRenderState(RenderPipelines.GUI_TEXTURED, textureSetup, pose, x, y, w, h, u0, v0, u1, v1, (a << 24) | 0x00FFFFFF, scissor, bounds));
    }

    private void blitLyricsRenderer(GuiGraphicsExtractor g, float alpha) {
        if (lyricsRenderer == null) return;
        float dstX = lyricsLayerX;
        float dstY = layoutLyricTop;
        float dstW = lyricsLayerW;
        float dstH = layoutLyricH;
        float srcX = 0f;
        float srcY = layoutLyricTop - lyricsLayerY - lyricLayerBlitOffsetY;
        float logicalH = lyricsRenderer.getHeight() / (float) guiScale;
        if (srcY < 0f) {
            float cut = -srcY;
            srcY = 0f;
            dstY += cut;
            dstH -= cut;
        }
        if (srcY + dstH > logicalH) dstH = logicalH - srcY;
        if (dstH <= 0.5f || dstW <= 0.5f) return;
        if (alpha >= 0.999f) blitRendererRegion(g, lyricsRenderer, dstX, dstY, dstW, dstH, srcX, srcY, dstW, dstH);
        else blitRendererRegionAlpha(g, lyricsRenderer, dstX, dstY, dstW, dstH, srcX, srcY, dstW, dstH, alpha);
    }

    private void blitRendererRegionAlpha(GuiGraphicsExtractor g, SkijaRenderer r, float dstX, float dstY, float dstW, float dstH, float srcX, float srcY, float srcW, float srcH, float alpha) {
        if (r == null) return;
        int srcPxX = Math.max(0, Math.round(srcX * guiScale));
        int srcPxY = Math.max(0, Math.round(srcY * guiScale));
        int srcPxW = Math.max(1, Math.min(r.getWidth() - srcPxX, Math.round(srcW * guiScale)));
        int srcPxH = Math.max(1, Math.min(r.getHeight() - srcPxY, Math.round(srcH * guiScale)));
        float u0 = srcPxX / (float) r.getWidth();
        float v0 = srcPxY / (float) r.getHeight();
        float u1 = (srcPxX + srcPxW) / (float) r.getWidth();
        float v1 = (srcPxY + srcPxH) / (float) r.getHeight();
        blitRendererAlpha(g, r, dstX, dstY, dstW, dstH, u0, v0, u1, v1, alpha);
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

    private void blitRendererRegion(GuiGraphicsExtractor g, SkijaRenderer r, float dstX, float dstY, float dstW, float dstH, float srcX, float srcY, float srcW, float srcH) {
        if (r == null) return;
        int srcPxX = Math.max(0, Math.round(srcX * guiScale));
        int srcPxY = Math.max(0, Math.round(srcY * guiScale));
        int srcPxW = Math.max(1, Math.min(r.getWidth() - srcPxX, Math.round(srcW * guiScale)));
        int srcPxH = Math.max(1, Math.min(r.getHeight() - srcPxY, Math.round(srcH * guiScale)));
        int dstPxW = Math.max(1, Math.round(dstW * guiScale));
        int dstPxH = Math.max(1, Math.round(dstH * guiScale));
        float inv = 1f / guiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(dstX, dstY);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, r.textureId(), 0, 0, (float) srcPxX, (float) srcPxY, dstPxW, dstPxH, srcPxW, srcPxH, r.getWidth(), r.getHeight());
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
        if (gpuBackground != null) {
            gpuBackground.close();
            gpuBackground = null;
        }
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
        if (interludeDotRenderer != null) {
            interludeDotRenderer.close();
            interludeDotRenderer = null;
        }
        interludeDotScale = -1f;
        renderer = null;
        if (coverImage != null) {
            coverImage.close();
            coverImage = null;
        }
        clearLyricCache();
        clearIconPathCache();
    }

    private void clearIconPathCache() {
        for (Path path : iconPathCache.values()) {
            if (path != null) path.close();
        }
        iconPathCache.clear();
    }

    private void syncTrackIndexToPlaying() {
        MusicLoader.MusicTrack playing = MusicLoader.getCurrentTrack();
        if (playing == null) return;
        List<MusicLoader.MusicTrack> tracks = MusicLoader.getTracks();
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i) == playing) {
                trackIndex = i;
                return;
            }
        }
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
        return clamp(Math.max(width / 1422f, height / 800f), 0.72f, 1.80f);
    }

    private float amllCssPx(float px) {
        return px / Math.max(1f, guiScale);
    }

    private float amllViewportWidthPx() {
        return width * Math.max(1f, guiScale);
    }

    private float amllViewportHeightPx() {
        return height * Math.max(1f, guiScale);
    }

    private float amllHorizontalLayoutMaxWidth() {
        float cssHeight = amllViewportHeightPx();
        float cssWidth = amllViewportWidthPx();
        float heightFactor = cssHeight <= 1000f ? 0.45f : 0.50f;
        return Math.max(1f, amllCssPx(Math.min(cssHeight * heightFactor, cssWidth * 0.38f)));
    }

    private float amllHorizontalColumnGap() {
        return amllHorizontalGap();
    }

    private float amllHorizontalGridColumnsWidth() {
        return Math.max(0f, width - amllHorizontalColumnGap() * 2f);
    }

    private float amllHorizontalInfoColumnWidth() {
        return amllHorizontalGridColumnsWidth() * 0.45f;
    }

    private float amllHorizontalPlayerColumnWidth() {
        return amllHorizontalGridColumnsWidth() * 0.55f;
    }

    private float amllHorizontalFontScale() {
        return amllViewportHeightPx() <= 768f ? 0.8f : 1f;
    }

    private float amllHorizontalEm() {
        return amllCssPx(16f * amllHorizontalFontScale());
    }

    private float amllHorizontalControlsMarginTop() {
        return Math.max(0f, amllHorizontalEm() * 1.75f - amllCssPx(8f));
    }

    private float amllHorizontalThumbRowHeight() {
        return height * 0.04f;
    }

    private float amllControlThumbWidth() {
        return amllCssPx(50f);
    }

    private float amllControlThumbHeight() {
        return amllCssPx(8f);
    }

    private float amllHorizontalGap() {
        return amllCssPx(amllViewportHeightPx() <= 768f ? 2f : 8f);
    }

    private float amllHorizontalLyricPaddingRightRatio() {
        return amllViewportWidthPx() <= 1600f || amllViewportHeightPx() <= 1000f ? 0.08f : 0.15f;
    }

    private float amllHorizontalMusicInfoPaddingTop() {
        return amllViewportHeightPx() >= 1000f ? height * 0.02f : 0f;
    }

    private float amllHorizontalMusicInfoPaddingBottom() {
        return amllViewportHeightPx() >= 1000f ? height * 0.01f : 0f;
    }

    private HorizontalGrid amllHorizontalGrid(float coverSize) {
        float gap = amllHorizontalGap();
        float thumbH = amllHorizontalThumbRowHeight();
        float tailFr = amllViewportHeightPx() <= 768f ? 0.2f : 0.3f;
        float fixed = thumbH + coverSize + gap * 5f;
        float free = Math.max(0f, height - fixed);
        float totalFr = 0.45f + 3f + tailFr;
        float dragH = free * 0.45f / totalFr;
        float controlsH = free * 3f / totalFr;
        float minDrag = amllCssPx(30f);
        if (dragH < minDrag) {
            float flexFree = Math.max(0f, height - fixed - minDrag);
            float flexFr = 3f + tailFr;
            dragH = minDrag;
            controlsH = flexFree * 3f / flexFr;
        }
        float coverY = dragH + gap + thumbH + gap;
        float controlsY = coverY + coverSize + gap + amllHorizontalControlsMarginTop();
        float controlsAvailableH = Math.max(0f, controlsH - amllHorizontalControlsMarginTop());
        float lyricTop = dragH + gap;
        float lyricH = thumbH + gap + coverSize + gap + controlsH;
        return new HorizontalGrid(coverY, controlsY, controlsAvailableH, lyricTop, lyricH);
    }

    private float amllMusicInfoBlockHeight(float infoSize) {
        return amllHorizontalMusicInfoPaddingTop() + amllMusicInfoLineHeight(infoSize) * 2f + amllHorizontalMusicInfoPaddingBottom();
    }

    private float amllMusicInfoLineHeight(float infoSize) {
        return infoSize * 1.25f;
    }

    private float amllMenuButtonSize() {
        if (amllViewportWidthPx() <= 1000f) return Math.max(amllHorizontalEm() * 2f, Math.min(width * 0.05f, height * 0.04f));
        return height * 0.035f;
    }

    private float amllMenuButtonMarginLeft() {
        return amllCssPx(16f);
    }

    private float amllSliderContainerHeight() {
        return amllCssPx(24f);
    }

    private float amllSliderInnerMinHeight() {
        return amllCssPx(8f);
    }

    private float amllSliderInnerMaxHeight() {
        return amllCssPx(20f);
    }

    private float amllProgressLabelMarginTop() {
        return amllViewportHeightPx() <= 768f ? 0f : amllCssPx(4f);
    }

    private float amllProgressBlockHeight(String qualityLabel) {
        return amllSliderContainerHeight() + amllProgressLabelMarginTop() + amllProgressLabelRowHeight(qualityLabel);
    }

    private float amllQualityTagScale() {
        float cssH = amllViewportHeightPx();
        float scale = 1.4f;
        if (cssH <= 768f) scale = 0.5f;
        if (cssH <= 1080f) scale = 1f;
        return scale;
    }

    private float amllQualityBadgeHeight() {
        return amllCssPx(20f) * amllQualityTagScale();
    }

    private float amllMusicInfoFontSize() {
        return Math.max(height * 0.02f, amllHorizontalEm());
    }

    private float amllProgressLabelFontSize() {
        return Math.max(height * 0.012f, amllHorizontalEm() * 0.8f);
    }

    private float amllProgressLabelLineHeight() {
        return amllProgressLabelFontSize() * 1.2f;
    }

    private float amllProgressLabelRowHeight(String qualityLabel) {
        float labelH = amllProgressLabelLineHeight();
        if (qualityLabel != null && !qualityLabel.isBlank()) labelH = Math.max(labelH, amllQualityBadgeHeight());
        return labelH;
    }

    private float amllProgressBarY(float leftW) {
        return layoutProgressY;
    }

    private float amllMediaButtonSize(float controlsWidth) {
        return controlsWidth * 0.18f;
    }

    private float amllMediaIconCssScale(boolean play) {
        float cssW = amllViewportWidthPx();
        float cssH = amllViewportHeightPx();
        float result = play ? 2f : 3f;
        if (cssH <= 1080f) result = play ? 1.1f : 2f;
        if (cssH <= 768f) result = play ? 0.8f : 1.5f;
        if (cssH <= 512f) result = play ? 0.5f : 1f;
        if (cssW <= 480f) result = 0.5f;
        return result;
    }

    private float amllMediaIconSize(float baseCssPx, boolean play) {
        return amllCssPx(baseCssPx * amllMediaIconCssScale(play));
    }

    private float amllMediaToggleIconSize() {
        return amllCssPx(16f * amllHorizontalFontScale() * 1.3f * amllMediaIconCssScale(false));
    }

    private float amllVolumeControlMarginLeft() {
        return amllCssPx(-12f);
    }

    private float amllVolumeControlMarginRight() {
        return amllCssPx(-8f);
    }

    private float amllVolumeControlHeight() {
        return amllCssPx(40f);
    }

    private float amllVolumeMinIconWidth() {
        return amllCssPx(32f);
    }

    private float amllVolumeMaxIconWidth() {
        return amllCssPx(43f);
    }

    private float amllVolumeIconHeight() {
        return amllCssPx(40f);
    }

    private float amllBottomToggleSize() {
        boolean compact = amllViewportWidthPx() <= 1600f || amllViewportHeightPx() <= 1000f;
        return amllHorizontalEm() * (compact ? 3f : 4f);
    }

    private float amllBottomControlsGap() {
        boolean compact = amllViewportWidthPx() <= 1600f || amllViewportHeightPx() <= 1000f;
        return amllHorizontalEm() * (compact ? 2f : 4f);
    }

    private float amllBottomControlsPadding() {
        boolean compact = amllViewportWidthPx() <= 1600f || amllViewportHeightPx() <= 1000f;
        return amllHorizontalEm() * (compact ? 2f : 4f);
    }

    private float amllLyricFontSize() {
        return Math.max(Math.max(height * 0.05f, width * 0.025f), amllCssPx(14f));
    }

    private float amllInactiveLyricFontSize(float activeSize) {
        return activeSize;
    }

    private float amllLyricLinePaddingX(float fontSize) {
        return amllViewportWidthPx() <= 500f ? amllCssPx(20f) : fontSize;
    }

    private float amllLyricLinePaddingY(float fontSize) {
        return fontSize * 0.4f;
    }

    private float amllLyricTextMaxWidth(float lineWidth, float fontSize) {
        return Math.max(1f, lineWidth - amllLyricLinePaddingX(fontSize) * 2f);
    }

    private float amllLyricLineGap(float s) {
        return amllLyricFontSize() * 0.3f;
    }

    private float amllInterludeDotSize() {
        float fontSize = amllLyricFontSize();
        return clamp(height * 0.01f, fontSize * 0.5f, fontSize * 3f);
    }

    private float amllInterludeDotGap() {
        return amllLyricFontSize() * 0.25f + amllCssPx(4f);
    }

    private float amllInterludeDotMargin() {
        return amllLyricFontSize() * 0.4f;
    }

    private float amllInterludeDotPaddingX() {
        return amllLyricFontSize() * 0.75f;
    }

    private float amllInterludeDotPaddingY() {
        return Math.max(0f, layoutRightW * 0.025f);
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


}

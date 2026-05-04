package com.lazychara.skijatest.client.esp;
import com.lazychara.skijatest.client.SkijaRenderer;
import com.lazychara.skijatest.client.SkijaTestScreen;
import com.lazychara.skijatest.module.render.ESP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public final class EspRender2D {
    private static final List<Esp2DBox> BOXES = new ArrayList<>();
    private static final Map<String, LabelTexture> LABEL_CACHE = new LinkedHashMap<>();
    private static final int MAX_LABEL_CACHE = 128;
    private static final int LABEL_H = 14;
    private static final float LABEL_FONT_SIZE = 10f;
    private EspRender2D() {
    }
    public static void clear() {
        BOXES.clear();
    }
    public static void collect(Minecraft mc, ESP esp, List<EspTarget> targets, Vec3 cameraPos, Matrix4f view, Matrix4f projection) {
        BOXES.clear();
        if (!esp.render2D.value) return;
        for (EspTarget target : targets) {
            addBox(mc, esp, target, cameraPos, view, projection);
        }
    }
    public static void renderHud(GuiGraphicsExtractor g, ESP esp, Minecraft mc) {
        if (!esp.render2D.value) return;
        for (Esp2DBox box : BOXES) {
            int x = Math.round(box.minX());
            int y = Math.round(box.minY());
            int w = Math.max(1, Math.round(box.maxX() - box.minX()));
            int h = Math.max(1, Math.round(box.maxY() - box.minY()));
            g.outline(x, y, w, h, 0xCC000000);
            g.outline(x + 1, y + 1, Math.max(1, w - 2), Math.max(1, h - 2), box.color());
            if (esp.health.value && box.maxHealth() > 0f) {
                float ratio = Math.max(0f, Math.min(1f, box.health() / box.maxHealth()));
                int barH = Math.max(1, Math.round(h * ratio));
                int barColor = EspRenderUtil.healthColor(ratio);
                g.fill(x - 5, y, x - 3, y + h, 0xAA000000);
                g.fill(x - 5, y + h - barH, x - 3, y + h, barColor);
            }
            if (esp.names.value) {
                LabelTexture label = labelTexture(box.label(), mc);
                int lx = x + w / 2 - label.guiW() / 2;
                int ly = y - LABEL_H - 2;
                g.fill(lx - 3, ly - 1, lx + label.guiW() + 3, ly + LABEL_H, 0x88000000);
                blitLabel(g, label, lx, ly);
            }
        }
    }
    private static LabelTexture labelTexture(String text, Minecraft mc) {
        int guiScale = Math.max(1, mc.getWindow().getGuiScale());
        SkijaTestScreen.ensureFontLoaded();
        String fontKey = SkijaTestScreen.selectedFontName == null ? "" : SkijaTestScreen.selectedFontName;
        String key = guiScale + "|" + fontKey + "|" + text;
        LabelTexture cached = LABEL_CACHE.get(key);
        if (cached != null) return cached;
        if (LABEL_CACHE.size() >= MAX_LABEL_CACHE) {
            clearLabelCache();
        }
        int guiW = Math.max(24, Math.min(260, text.length() * 10 + 16));
        SkijaRenderer renderer = new SkijaRenderer("esp_label", guiW * guiScale, LABEL_H * guiScale);
        renderer.clear(0x00000000);
        var c = renderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        renderer.drawTextCentered(text, guiW / 2f, 2f, SkijaTestScreen.curTf, LABEL_FONT_SIZE, 0xFFFFFFFF);
        c.restore();
        renderer.upload();
        LabelTexture label = new LabelTexture(renderer, guiW, LABEL_H, guiScale);
        LABEL_CACHE.put(key, label);
        return label;
    }
    private static void blitLabel(GuiGraphicsExtractor g, LabelTexture label, int x, int y) {
        float inv = 1f / label.guiScale();
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, label.renderer().textureId(), 0, 0, 0f, 0f,
                label.renderer().getWidth(), label.renderer().getHeight(),
                label.renderer().getWidth(), label.renderer().getHeight());
        pose.popMatrix();
    }
    private static void clearLabelCache() {
        for (LabelTexture label : LABEL_CACHE.values()) {
            label.renderer().close();
        }
        LABEL_CACHE.clear();
    }
    private static void addBox(Minecraft mc, ESP esp, EspTarget target, Vec3 cameraPos, Matrix4f view, Matrix4f projection) {
        AABB box = target.box();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        int visible = 0;
        double[] xs = { box.minX, box.maxX };
        double[] ys = { box.minY, box.maxY };
        double[] zs = { box.minZ, box.maxZ };
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    ScreenPoint point = project(mc, x - cameraPos.x, y - cameraPos.y, z - cameraPos.z, view, projection);
                    if (point == null) continue;
                    visible++;
                    minX = Math.min(minX, point.x());
                    minY = Math.min(minY, point.y());
                    maxX = Math.max(maxX, point.x());
                    maxY = Math.max(maxY, point.y());
                }
            }
        }
        if (visible == 0 || maxX <= minX || maxY <= minY) return;
        String label = target.entity().getName().getString();
        if (esp.distance.value && mc.player != null) {
            label += " " + Math.round(mc.player.distanceTo(target.entity())) + "m";
        }
        float health = -1f;
        float maxHealth = -1f;
        if (target.entity() instanceof LivingEntity living) {
            health = living.getHealth();
            maxHealth = living.getMaxHealth();
        }
        BOXES.add(new Esp2DBox(minX, minY, maxX, maxY, target.color(), label, health, maxHealth));
    }
    private static ScreenPoint project(Minecraft mc, double relX, double relY, double relZ, Matrix4f view, Matrix4f projection) {
        Vector4f clip = new Vector4f((float) relX, (float) relY, (float) relZ, 1f);
        clip.mul(view);
        clip.mul(projection);
        if (clip.w() <= 0.05f) return null;
        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        if (ndcX < -2f || ndcX > 2f || ndcY < -2f || ndcY > 2f) return null;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        float sx = (ndcX * 0.5f + 0.5f) * screenW;
        float sy = (0.5f - ndcY * 0.5f) * screenH;
        return new ScreenPoint(sx, sy);
    }
    private record ScreenPoint(float x, float y) {}
    private record LabelTexture(SkijaRenderer renderer, int guiW, int guiH, int guiScale) {}
    private record Esp2DBox(float minX, float minY, float maxX, float maxY, int color, String label, float health, float maxHealth) {}
}

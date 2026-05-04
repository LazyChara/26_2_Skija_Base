package com.lazychara.skijatest.client.esp;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
public final class EspRender3D {
    private static final float LINE_WIDTH = 2.0f;
    private EspRender3D() {
    }
    public static void submit(List<EspTarget> targets, net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        if (targets.isEmpty()) return;
        DrawableGizmoPrimitives gizmos = new DrawableGizmoPrimitives();
        for (EspTarget target : targets) {
            addBoxLines(gizmos, target.box(), target.color());
        }
        gizmos.submit(context.submitNodeCollector(), context.levelState().cameraRenderState, true);
    }
    private static void addBoxLines(DrawableGizmoPrimitives gizmos, AABB box, int color) {
        Vec3 p000 = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 p001 = new Vec3(box.minX, box.minY, box.maxZ);
        Vec3 p010 = new Vec3(box.minX, box.maxY, box.minZ);
        Vec3 p011 = new Vec3(box.minX, box.maxY, box.maxZ);
        Vec3 p100 = new Vec3(box.maxX, box.minY, box.minZ);
        Vec3 p101 = new Vec3(box.maxX, box.minY, box.maxZ);
        Vec3 p110 = new Vec3(box.maxX, box.maxY, box.minZ);
        Vec3 p111 = new Vec3(box.maxX, box.maxY, box.maxZ);
        line(gizmos, p000, p001, color); line(gizmos, p001, p101, color); line(gizmos, p101, p100, color); line(gizmos, p100, p000, color);
        line(gizmos, p010, p011, color); line(gizmos, p011, p111, color); line(gizmos, p111, p110, color); line(gizmos, p110, p010, color);
        line(gizmos, p000, p010, color); line(gizmos, p001, p011, color); line(gizmos, p100, p110, color); line(gizmos, p101, p111, color);
    }
    private static void line(DrawableGizmoPrimitives gizmos, Vec3 from, Vec3 to, int color) {
        gizmos.addLine(from, to, color, LINE_WIDTH);
    }
}

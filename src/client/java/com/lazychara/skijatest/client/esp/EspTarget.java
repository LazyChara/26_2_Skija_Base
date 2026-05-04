package com.lazychara.skijatest.client.esp;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
public record EspTarget(net.minecraft.world.entity.Entity entity, AABB box, int color) {
    public Vec3 center() {
        return box.getCenter();
    }
}

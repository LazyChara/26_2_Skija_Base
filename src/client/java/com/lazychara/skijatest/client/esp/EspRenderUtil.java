package com.lazychara.skijatest.client.esp;
import com.lazychara.skijatest.module.ModuleManager;
import com.lazychara.skijatest.module.render.ESP;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
public final class EspRenderUtil {
    private static final double MAX_DISTANCE = 128.0;
    private EspRenderUtil() {
    }
    public static ESP getEspModule() {
        return (ESP) ModuleManager.modules.stream()
                .filter(m -> m instanceof ESP)
                .findFirst()
                .orElse(null);
    }
    public static boolean shouldRenderEntity(ESP esp, Minecraft mc, Entity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive() || entity.isSpectator()) return false;
        if (mc.player == null || mc.player.distanceToSqr(entity) > MAX_DISTANCE * MAX_DISTANCE) return false;
        if (entity instanceof Player) return esp.players.value;
        if (entity instanceof Monster) return esp.mobs.value;
        if (entity instanceof Animal) return esp.animals.value;
        if (entity instanceof ItemEntity) return esp.items.value;
        return false;
    }
    public static int entityColor(Entity entity) {
        if (entity instanceof Player) return 0xFF55AAFF;
        if (entity instanceof Monster) return 0xFFFF5555;
        if (entity instanceof Animal) return 0xFF55FF88;
        if (entity instanceof ItemEntity) return 0xFFFFDD55;
        return 0xFFFFFFFF;
    }
    public static int healthColor(float ratio) {
        int r = Math.round(255f * (1f - ratio));
        int g = Math.round(255f * ratio);
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}

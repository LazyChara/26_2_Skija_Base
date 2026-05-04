package com.lazychara.skijatest.module.movement;
import com.lazychara.skijatest.module.Category;
import com.lazychara.skijatest.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
public class Sprint extends Module {
    public Sprint() {
        super("Sprint", Category.MOVEMENT);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.enabled && client.player != null) {
                if (client.options.keyUp.isDown() && !client.player.isSprinting() && !client.player.isCrouching()) {
                    client.player.setSprinting(true);
                }
            }
        });
    }
}

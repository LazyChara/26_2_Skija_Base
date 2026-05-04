package com.lazychara.skijatest.module.player;
import com.lazychara.skijatest.module.BooleanSetting;
import com.lazychara.skijatest.module.Category;
import com.lazychara.skijatest.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
public class Eagle extends Module {
    public final BooleanSetting onlyBlocks = new BooleanSetting("Block-in-hand only", true);
    public Eagle() {
        super("Eagle", Category.PLAYER);
        settings.add(onlyBlocks);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.enabled && client.player != null && client.level != null) {
                if (onlyBlocks.value) {
                    boolean hasBlock = client.player.getMainHandItem().getItem() instanceof BlockItem ||
                            client.player.getOffhandItem().getItem() instanceof BlockItem;
                    if (!hasBlock)
                        return;
                }
                BlockState stateBelow = client.level.getBlockState(client.player.blockPosition().below());
                boolean isAir = stateBelow.isAir();
                if (isAir && client.player.onGround()) {
                    client.options.keyShift.setDown(true);
                } else if (!isAir) {
                    client.options.keyShift.setDown(false);
                }
            }
        });
    }
}

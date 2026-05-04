package com.lazychara.skijatest.mixin;
import com.lazychara.skijatest.client.HudManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public class HudMixin {
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void skijaTest$hideVanillaEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HudManager.shouldHideVanillaEffects()) {
            ci.cancel();
        }
    }
}

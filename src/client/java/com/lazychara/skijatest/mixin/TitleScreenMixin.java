package com.lazychara.skijatest.mixin;

import com.lazychara.skijatest.client.MainMenuRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void skijaTest$layoutMainMenu(CallbackInfo ci) {
        MainMenuRenderer.layoutVanillaButtons((TitleScreen) (Object) this);
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void skijaTest$cancelVanillaBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void skijaTest$renderCustomMainMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        MainMenuRenderer.layoutVanillaButtons((TitleScreen) (Object) this);
        MainMenuRenderer.render((TitleScreen) (Object) this, graphics, mouseX, mouseY, partialTick);
        ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void skijaTest$handleBgSelectorClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        TitleScreen screen = (TitleScreen) (Object) this;
        if (MainMenuRenderer.mouseClicked(screen, event, doubleClick)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void skijaTest$closeMainMenuRenderer(CallbackInfo ci) {
        MainMenuRenderer.close();
    }
}

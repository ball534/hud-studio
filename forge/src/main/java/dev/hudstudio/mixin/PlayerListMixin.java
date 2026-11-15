package dev.hudstudio.mixin;

import dev.hudstudio.client.NativeHud;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class PlayerListMixin {
   @Inject(method = "renderPlayerlist", at = @At("HEAD"), cancellable = true)
   private void studio$tab(CallbackInfo ci) {
      if (NativeHud.hidden("player_tab")) {
         ci.cancel();
      } else {
         NativeHud.begin("player_tab");
      }
   }

   @Inject(method = "renderPlayerlist", at = @At("RETURN"))
   private void studio$tabEnd(CallbackInfo ci) {
      NativeHud.end();
   }
}

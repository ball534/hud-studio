package dev.hudstudio.mixin;

import dev.hudstudio.client.NativeHud;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class HudMixin {
   @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
   private void studio$scoreboard(CallbackInfo ci) {
      if (NativeHud.hidden("scoreboard")) {
         ci.cancel();
      } else {
         NativeHud.begin("scoreboard");
      }
   }

   @Inject(method = "renderScoreboard", at = @At("RETURN"))
   private void studio$scoreboardEnd(CallbackInfo ci) {
      NativeHud.end();
   }

   @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
   private void studio$bossbar(CallbackInfo ci) {
      if (NativeHud.hidden("bossbar")) {
         ci.cancel();
      } else {
         NativeHud.begin("bossbar");
      }
   }

   @Inject(method = "renderBossHealth", at = @At("RETURN"))
   private void studio$bossbarEnd(CallbackInfo ci) {
      NativeHud.end();
   }
}

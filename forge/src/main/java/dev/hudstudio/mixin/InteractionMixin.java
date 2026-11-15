package dev.hudstudio.mixin;

import dev.hudstudio.client.ClientTweaks;
import dev.hudstudio.client.ComboMeter;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class InteractionMixin {
   @Inject(method = {"clickMouse", "rightClickMouse", "sendClickBlockToController"}, at = @At("HEAD"), cancellable = true)
   private void studio$blockInteraction(CallbackInfo ci) {
      if (ClientTweaks.freecam()) {
         ci.cancel();
      }
   }

   @Inject(
      method = "clickMouse",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;attackEntity(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)V"
      )
   )
   private void studio$attack(CallbackInfo ci) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.objectMouseOver != null) {
         ComboMeter.attack(mc.objectMouseOver.entityHit);
      }
   }
}

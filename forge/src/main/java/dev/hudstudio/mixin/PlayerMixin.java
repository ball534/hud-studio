package dev.hudstudio.mixin;

import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PlayerMixin {
   @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
   private void studio$turn(float yaw, float pitch, CallbackInfo ci) {
      if ((Object)this == Minecraft.getMinecraft().thePlayer && ClientTweaks.detached()) {
         ClientTweaks.turn(yaw, pitch);
         ci.cancel();
      }
   }
}

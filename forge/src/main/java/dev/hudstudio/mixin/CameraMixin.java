package dev.hudstudio.mixin;

import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class CameraMixin {
   @Unique
   private float studio$yaw;
   @Unique
   private float studio$pitch;
   @Unique
   private float studio$previousYaw;
   @Unique
   private float studio$previousPitch;

   @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
   private void studio$zoom(CallbackInfoReturnable<Float> cir) {
      cir.setReturnValue((Float)cir.getReturnValue() / ClientTweaks.zoomFactor());
   }

   @Inject(method = {"hurtCameraEffect", "setupViewBobbing"}, at = @At("HEAD"), cancellable = true)
   private void studio$shake(CallbackInfo ci) {
      if (ClientTweaks.enabled("no_shake")) {
         ci.cancel();
      }
   }

   @Redirect(
      method = {"updateCameraAndRender", "updateRenderer"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;mouseSensitivity:F")
   )
   private float studio$sensitivity(GameSettings settings) {
      return ClientTweaks.sensitivity(settings.mouseSensitivity);
   }

   @Redirect(
      method = {"updateCameraAndRender", "updateRenderer"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;smoothCamera:Z")
   )
   private boolean studio$smooth(GameSettings settings) {
      return settings.smoothCamera || ClientTweaks.cinematicZoom();
   }

   @Inject(method = "orientCamera", at = @At("HEAD"))
   private void studio$orbit(CallbackInfo ci) {
      if (ClientTweaks.freelook()) {
         Entity player = Minecraft.getMinecraft().thePlayer;
         this.studio$yaw = player.rotationYaw;
         this.studio$pitch = player.rotationPitch;
         this.studio$previousYaw = player.prevRotationYaw;
         this.studio$previousPitch = player.prevRotationPitch;
         player.rotationYaw = player.prevRotationYaw = ClientTweaks.yaw();
         player.rotationPitch = player.prevRotationPitch = ClientTweaks.pitch();
      }
   }

   @Inject(method = "orientCamera", at = @At("RETURN"))
   private void studio$restoreAim(CallbackInfo ci) {
      if (ClientTweaks.freelook()) {
         Entity player = Minecraft.getMinecraft().thePlayer;
         player.rotationYaw = this.studio$yaw;
         player.rotationPitch = this.studio$pitch;
         player.prevRotationYaw = this.studio$previousYaw;
         player.prevRotationPitch = this.studio$previousPitch;
      }
   }

   @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
   private void studio$hideHand(CallbackInfo ci) {
      if (ClientTweaks.freecam()) {
         ci.cancel();
      }
   }
}

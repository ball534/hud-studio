package dev.hudstudio.mixin;

import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.EnumParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EffectRenderer.class)
public abstract class ParticleMixin {
   @Inject(method = "spawnEffectParticle", at = @At("HEAD"), cancellable = true)
   private void studio$explosions(int id, double x, double y, double z, double vx, double vy, double vz, int[] parameters, CallbackInfoReturnable<EntityFX> cir) {
      if (ClientTweaks.enabled("no_explosions")
         && (
            id == EnumParticleTypes.EXPLOSION_NORMAL.getParticleID()
               || id == EnumParticleTypes.EXPLOSION_LARGE.getParticleID()
               || id == EnumParticleTypes.EXPLOSION_HUGE.getParticleID()
         )) {
         cir.setReturnValue(null);
      }
   }
}

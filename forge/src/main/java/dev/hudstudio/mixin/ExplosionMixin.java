package dev.hudstudio.mixin;

import dev.hudstudio.client.ClientTweaks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
   @Redirect(
      method = "doExplosionB",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V")
   )
   private void studio$debris(World world, EnumParticleTypes particle, double x, double y, double z, double vx, double vy, double vz, int[] args) {
      if (!world.isRemote || !ClientTweaks.enabled("no_explosions")) {
         world.spawnParticle(particle, x, y, z, vx, vy, vz, args);
      }
   }
}

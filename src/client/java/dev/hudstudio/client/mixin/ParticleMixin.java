package dev.hudstudio.client.mixin;
import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ParticleEngine.class)
public class ParticleMixin {
    @Inject(method="createParticle",at=@At("HEAD"),cancellable=true)
    private void studio_particles(ParticleOptions options,double x,double y,double z,double dx,double dy,double dz,CallbackInfoReturnable<Particle> ci) {
        if(ClientTweaks.enabled("no_explosions") && (options.getType()==ParticleTypes.EXPLOSION || options.getType()==ParticleTypes.EXPLOSION_EMITTER))ci.setReturnValue(null);
    }
}

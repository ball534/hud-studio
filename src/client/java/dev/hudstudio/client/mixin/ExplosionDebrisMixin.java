package dev.hudstudio.client.mixin;
import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class)
public class ExplosionDebrisMixin {
    @Inject(method="trackExplosionEffects",at=@At("HEAD"),cancellable=true)
    private void studio_noDebris(CallbackInfo ci){if(ClientTweaks.enabled("no_explosions"))ci.cancel();}
}

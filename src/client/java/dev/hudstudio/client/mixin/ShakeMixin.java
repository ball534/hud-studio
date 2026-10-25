package dev.hudstudio.client.mixin;
import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class ShakeMixin {
    @Inject(method={"bobHurt","bobView"},at=@At("HEAD"),cancellable=true)
    private void studio_noShake(CallbackInfo ci) {if(ClientTweaks.enabled("no_shake") || ClientTweaks.freecam())ci.cancel();}
}

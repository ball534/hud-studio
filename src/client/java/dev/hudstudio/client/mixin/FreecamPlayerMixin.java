package dev.hudstudio.client.mixin;

import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class FreecamPlayerMixin {
    // Keep normal player simulation and movement packets while a separate entity owns the view.
    @Inject(method="isControlledCamera",at=@At("HEAD"),cancellable=true)
    private void studio_playerSimulation(CallbackInfoReturnable<Boolean> ci) {
        if(ClientTweaks.freecam())ci.setReturnValue(true);
    }
}


package dev.hudstudio.client.mixin;
import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public class FreecamInteractionMixin {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void studio_noAttack(CallbackInfoReturnable<Boolean> ci){if(ClientTweaks.freecam())ci.setReturnValue(false);}
    @Inject(method={"continueAttack","startUseItem"},at=@At("HEAD"),cancellable=true)
    private void studio_noUse(CallbackInfo ci){if(ClientTweaks.freecam())ci.cancel();}
}

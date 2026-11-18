package dev.hudstudio.client.mixin;
import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public class FreecamInteractionMixin {
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
        method="handleKeybinds",
        slice=@Slice(from=@At(value="FIELD",target="Lnet/minecraft/client/Options;keySwapOffhand:Lnet/minecraft/client/KeyMapping;")),
        at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"))
    private boolean studio_spectatorActions(boolean spectator) {return spectator || ClientTweaks.freecam();}
    @Inject(method="pick",at=@At("HEAD"),cancellable=true)
    private void studio_noTarget(float partial,CallbackInfo ci) {
        if(!ClientTweaks.freecam())return;
        var mc=Minecraft.getInstance();mc.crosshairPickEntity=null;
        mc.hitResult=net.minecraft.world.phys.BlockHitResult.miss(ClientTweaks.position(1),net.minecraft.core.Direction.UP,net.minecraft.core.BlockPos.containing(ClientTweaks.position(1)));
        ci.cancel();
    }
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void studio_noAttack(CallbackInfoReturnable<Boolean> ci){if(ClientTweaks.freecam())ci.setReturnValue(false);}
    @Inject(method={"continueAttack","startUseItem","pickBlockOrEntity"},at=@At("HEAD"),cancellable=true)
    private void studio_noUse(CallbackInfo ci){if(ClientTweaks.freecam())ci.cancel();}
}

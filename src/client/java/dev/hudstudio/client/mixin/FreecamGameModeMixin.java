package dev.hudstudio.client.mixin;

import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class FreecamGameModeMixin {
    @Inject(method={"dropItem","handleCreativeModeItemDrop","handleContainerInput","handleInventoryButtonClick","handlePickItemFromBlock","handlePickItemFromEntity","attack","piercingAttack","handleSlotStateChanged"},at=@At("HEAD"),cancellable=true)
    private void studio_cameraOnly(CallbackInfo ci) {if(ClientTweaks.freecam())ci.cancel();}
    @Inject(method={"destroyBlock","startDestroyBlock","continueDestroyBlock"},at=@At("HEAD"),cancellable=true)
    private void studio_noBreak(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {if(ClientTweaks.freecam())ci.setReturnValue(false);}
    @Inject(method={"useItemOn","useItem","interact"},at=@At("HEAD"),cancellable=true)
    private void studio_noInteraction(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.InteractionResult> ci) {if(ClientTweaks.freecam())ci.setReturnValue(net.minecraft.world.InteractionResult.PASS);}
}

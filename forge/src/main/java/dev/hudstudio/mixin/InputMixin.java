package dev.hudstudio.mixin;

import dev.hudstudio.client.ClientTweaks;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public abstract class InputMixin extends MovementInput {
   @Inject(method = "updatePlayerMoveState", at = @At("RETURN"))
   private void studio$freezeInput(CallbackInfo ci) {
      if (ClientTweaks.freecam()) {
         this.moveForward = this.moveStrafe = 0.0F;
         this.jump = this.sneak = false;
      }
   }
}

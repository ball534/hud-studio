package dev.hudstudio.mixin;

import dev.hudstudio.client.ClientTweaks;
import dev.hudstudio.client.HudStudioClient;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.init.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActiveRenderInfo.class)
public abstract class FogMixin {
   @Inject(method = "getBlockAtEntityViewpoint", at = @At("HEAD"), cancellable = true)
   private static void studio$fog(CallbackInfoReturnable<Block> cir) {
      if (ClientTweaks.freecam() && !HudStudioClient.config.freecamShowSubmersion) {
         cir.setReturnValue(Blocks.air);
      }
   }
}

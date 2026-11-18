package dev.hudstudio.client.mixin;

import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Supply full ambient light to the shared vanilla/Sodium lightmap. */
@Mixin(LightmapRenderStateExtractor.class)
public class FullBrightMixin {
    @Shadow private boolean needsUpdate;
    @Unique private boolean studio_wasFullBright;

    @Inject(method="extract",at=@At("HEAD"))
    private void studio_refreshToggle(LightmapRenderState state,float partial,CallbackInfo ci) {
        boolean enabled=ClientTweaks.enabled("full_bright");
        // BadOptimizations can skip tick() when vanilla lighting is unchanged.
        // Invalidate on both transitions so enabling AND restoration reach the GPU.
        if(enabled!=studio_wasFullBright)needsUpdate=true;
        studio_wasFullBright=enabled;
    }

    @Inject(method="extract",at=@At("TAIL"))
    private void studio_fullBright(LightmapRenderState state,float partial,CallbackInfo ci) {
        if(!state.needsUpdate || !ClientTweaks.enabled("full_bright"))return;
        // Gamma cannot illuminate black in 26.3's shader. White ambient light
        // covers every light level without changing options or potion effects.
        state.ambientColor=LightmapRenderStateExtractor.WHITE;
        state.darknessEffectScale=0;
        state.bossOverlayWorldDarkening=0;
    }
}

package dev.hudstudio.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/** Override only the lightmap's gamma input, leaving options.txt untouched. */
@Mixin(LightmapRenderStateExtractor.class)
public class FullBrightMixin {
    @ModifyExpressionValue(method="extract", at=@At(value="INVOKE", target="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"),
        slice=@Slice(from=@At(value="INVOKE",target="Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;"),
            to=@At(value="INVOKE",target="Lnet/minecraft/client/Options;darknessEffectScale()Lnet/minecraft/client/OptionInstance;")))
    private Object studio_gamma(Object gamma) {
        return ClientTweaks.enabled("full_bright") ? 100.0d : gamma;
    }
}

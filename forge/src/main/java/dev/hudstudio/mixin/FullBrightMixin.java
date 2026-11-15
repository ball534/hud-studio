package dev.hudstudio.mixin;

import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supply maximum gamma only to the lightmap; never modify the saved setting. */
@Mixin(EntityRenderer.class)
public class FullBrightMixin {
    @Redirect(method="updateLightmap",at=@At(value="FIELD",target="Lnet/minecraft/client/settings/GameSettings;gammaSetting:F"))
    private float studio$gamma(GameSettings settings) {
        return ClientTweaks.enabled("full_bright") ? 100.0f : settings.gammaSetting;
    }
}

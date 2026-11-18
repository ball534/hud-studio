package dev.hudstudio.client.mixin;

import dev.hudstudio.client.navigation.WaypointRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class WaypointRenderMixin {
    @Inject(method="extractGizmos",at=@At("HEAD"))
    private void studio_waypoints(CallbackInfo ci) {WaypointRenderer.extract();}
}

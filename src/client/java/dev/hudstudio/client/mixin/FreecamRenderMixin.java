package dev.hudstudio.client.mixin;

import dev.hudstudio.client.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelExtractor.class)
public class FreecamRenderMixin {
    // Vanilla skips LocalPlayer when a different entity owns the camera. Bypass only that guard.
    @WrapOperation(method="extractVisibleEntities",at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;entity()Lnet/minecraft/world/entity/Entity;",ordinal=3))
    private Entity studio_showBody(Camera camera,Operation<Entity> original) {
        if(ClientTweaks.freecam() && HudStudioClient.config.freecamShowPlayer)return Minecraft.getInstance().player;
        return original.call(camera);
    }
}

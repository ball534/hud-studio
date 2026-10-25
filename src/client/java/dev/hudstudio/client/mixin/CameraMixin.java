package dev.hudstudio.client.mixin;
import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow protected abstract void move(float forward,float up,float right);
    @Shadow private boolean detached;
    @Shadow private float getMaxZoom(float distance){throw new AssertionError();}
    @Inject(method="alignWithEntity",at=@At("HEAD"),cancellable=true)
    private void studio_camera(float partial,CallbackInfo ci) {
        if(!ClientTweaks.detached())return;
        var player=Minecraft.getInstance().player;if(player==null)return;
        setRotation(ClientTweaks.yaw(),ClientTweaks.pitch());detached=true;
        if(ClientTweaks.freecam())setPosition(ClientTweaks.position(partial));
        else {setPosition(player.getEyePosition(partial));move(-getMaxZoom(4*player.getScale()),0,0);}
        ci.cancel();
    }
    @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
    private void studio_zoom(float partial,CallbackInfoReturnable<Float> ci) {ci.setReturnValue(ci.getReturnValue()/ClientTweaks.zoomFactor());}
}

package dev.hudstudio.client.mixin;
import dev.hudstudio.client.HudWidgets;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.player.LocalPlayer;
@Mixin(MouseHandler.class)
public class MouseMixin {
    @WrapOperation(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void studio_cameraTurn(LocalPlayer player,double x,double y,Operation<Void> original) {
        if(ClientTweaks.detached())ClientTweaks.turn(x,y);
        else original.call(player,x,y);
    }
    // Scale the sensitivity before vanilla's cubic curve, matching WI Zoom without changing saved options.
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",ordinal=0))
    private Object studio_zoomSensitivity(Object value) {return (Double)value/ClientTweaks.zoomFactor();}
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="turnPlayer",at=@At(value="FIELD",target="Lnet/minecraft/client/Options;smoothCamera:Z"))
    private boolean studio_cinematicZoom(boolean original) {return original || ClientTweaks.cinematicZoom();}
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void studio_zoomScroll(long handle,double horizontal,double vertical,CallbackInfo ci) {
        if(handle==Minecraft.getInstance().getWindow().handle() && ClientTweaks.scrollZoom(vertical))ci.cancel();
    }
    @Inject(method="onButton",at=@At("HEAD"))
    private void studio_click(long handle,MouseButtonInfo button,int action,CallbackInfo ci) {
        if(action==1 && handle==Minecraft.getInstance().getWindow().handle()) HudWidgets.click(button.button());
    }
}

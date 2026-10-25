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
        else original.call(player,x/ClientTweaks.zoomFactor(),y/ClientTweaks.zoomFactor());
    }
    @Inject(method="onButton",at=@At("HEAD"))
    private void studio_click(long handle,MouseButtonInfo button,int action,CallbackInfo ci) {
        if(action==1 && handle==Minecraft.getInstance().getWindow().handle()) HudWidgets.click(button.button());
    }
}

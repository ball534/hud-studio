package dev.hudstudio.client.mixin;
import dev.hudstudio.client.ClientTweaks;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public class FreecamInputMixin extends ClientInput {
    @Inject(method="tick",at=@At("TAIL"))
    private void studio_freezeInput(CallbackInfo ci) {if(ClientTweaks.freecam()){keyPresses=Input.EMPTY;moveVector=Vec2.ZERO;}}
}

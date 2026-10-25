package dev.hudstudio.client.mixin;
import dev.hudstudio.client.ComboMeter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public class DamageMixin {
    @Inject(method="handleDamageEvent",at=@At("TAIL"))
    private void studio_damage(ClientboundDamageEventPacket packet,CallbackInfo ci){ComboMeter.damage(packet);}
}

package dev.hudstudio.mixin;

import dev.hudstudio.client.ComboMeter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class DamageMixin {
   @Inject(method = "handleEntityStatus", at = @At("RETURN"))
   private void studio$damage(S19PacketEntityStatus packet, CallbackInfo ci) {
      if (packet.getOpCode() == 2 && Minecraft.getMinecraft().theWorld != null) {
         ComboMeter.hurt(packet.getEntity(Minecraft.getMinecraft().theWorld));
      }
   }
}

package dev.hudstudio.client.mixin;
import dev.hudstudio.client.ComboMeter;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MultiPlayerGameMode.class)
public class AttackMixin {
    @Inject(method="attack",at=@At("HEAD"))
    private void studio_attack(Player player,Entity target,CallbackInfo ci){ComboMeter.attack(player,target);}
    @Inject(method="piercingAttack",at=@At("HEAD"))
    private void studio_piercing(CallbackInfo ci){ComboMeter.piercingAttack();}
}

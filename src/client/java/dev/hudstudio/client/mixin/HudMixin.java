package dev.hudstudio.client.mixin;
import dev.hudstudio.client.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(Hud.class)
public class HudMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void studio_hideHud(GuiGraphicsExtractor g,DeltaTracker d,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if(ClientTweaks.hideHud())ci.cancel();
    }
    @WrapMethod(method="extractScoreboardSidebar")
    private void studio_scoreboard(GuiGraphicsExtractor g,DeltaTracker d,Operation<Void> original) {
        var e=HudStudioClient.config.elements.get("scoreboard");
        if(!e.visible || Minecraft.getInstance().gui.screen() instanceof HudEditorScreen) return;
        HudLayout.begin(g,e,true);
        try {original.call(g,d);} finally {HudLayout.end(g);}
    }
    @WrapMethod(method="extractBossOverlay")
    private void studio_bossbar(GuiGraphicsExtractor g,DeltaTracker d,Operation<Void> original) {
        var e=HudStudioClient.config.elements.get("bossbar");
        if(!e.visible || Minecraft.getInstance().gui.screen() instanceof HudEditorScreen) return;
        HudLayout.begin(g,e,true);
        try {original.call(g,d);} finally {HudLayout.end(g);}
    }
    @WrapMethod(method="extractTabList")
    private void studio_player_tab(GuiGraphicsExtractor g,DeltaTracker d,Operation<Void> original) {
        var e=HudStudioClient.config.elements.get("player_tab");
        if(!e.visible || Minecraft.getInstance().gui.screen() instanceof HudEditorScreen) return;
        HudLayout.begin(g,e,true);
        try {original.call(g,d);} finally {HudLayout.end(g);}
    }
    @WrapMethod(method="extractEffects")
    private void studio_effects(GuiGraphicsExtractor g,DeltaTracker d,Operation<Void> original) {
        var e=HudStudioClient.config.elements.get("effects");
        if(!e.visible || Minecraft.getInstance().gui.screen() instanceof HudEditorScreen) return;
        HudLayout.begin(g,e,true);
        try {original.call(g,d);} finally {HudLayout.end(g);}
    }
    @ModifyExpressionValue(method="extractItemHotbar",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack studio_offhand(ItemStack original) {
        return !(Minecraft.getInstance().gui.screen() instanceof HudEditorScreen) && HudWidgets.vanillaOffhand()?original:ItemStack.EMPTY;
    }
}

package dev.hudstudio.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public final class HudStudioClient implements ClientModInitializer {
    public static HudConfig config;
    @Override public void onInitializeClient() {
        config = HudConfig.load();
        ClientTweaks.init();
        dev.hudstudio.client.navigation.Navigation.init();
        HudElementRegistry.attachElementAfter(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.HOTBAR,Identifier.fromNamespaceAndPath("hud-studio","widgets"),(g,d) -> {
            if(!ClientTweaks.hideHud()) {
                HudWidgets.draw(g,false);
            }
        });
    }
}

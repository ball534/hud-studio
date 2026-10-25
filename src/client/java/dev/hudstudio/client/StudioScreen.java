package dev.hudstudio.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public abstract class StudioScreen extends Screen {
    protected final Screen parent;
    protected StudioScreen(String title,Screen parent) {super(Component.literal(title));this.parent=parent;}
    @Override public boolean isPauseScreen() {return false;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float a) {
        g.fill(0,0,width,height,StudioUi.BG);
    }
    @Override public void onClose() {HudStudioClient.config.save();minecraft.gui.setScreen(parent);}
    @Override public void removed() {HudStudioClient.config.save();}
}

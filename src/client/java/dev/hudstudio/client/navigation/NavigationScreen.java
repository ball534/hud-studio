package dev.hudstudio.client.navigation;

import dev.hudstudio.client.StudioUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class NavigationScreen extends Screen {
    protected final Screen parent;
    protected final WaypointStore world;
    NavigationScreen(String title,Screen parent) {super(Component.literal(title));this.parent=parent;world=Navigation.store();}
    @Override public boolean isPauseScreen() {return true;}
    @Override public void tick() {if(world!=Navigation.store() || minecraft.level==null)minecraft.gui.setScreen(null);}
    @Override public void onClose() {minecraft.gui.setScreen(world==Navigation.store()?parent:null);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial) {g.fill(0,0,width,height,StudioUi.BG);}
}

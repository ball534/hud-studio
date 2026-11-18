package dev.hudstudio.client.navigation;

import dev.hudstudio.client.HudSettingsScreen;
import net.minecraft.client.gui.screens.Screen;

/** U opens the same management category that appears inside Studio. */
public final class WaypointListScreen extends HudSettingsScreen {
    private final WaypointStore world=Navigation.store();
    public WaypointListScreen(Screen parent){super(parent,4);}
    public double scrollOffset(){return waypointPanel()==null?0:waypointPanel().scrollOffset();}
    public int filteredCount(){return waypointPanel()==null?0:waypointPanel().filteredCount();}
    @Override public void tick(){if(world!=Navigation.store() || minecraft.level==null){minecraft.gui.setScreen(null);return;}super.tick();}
}
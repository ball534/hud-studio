package dev.hudstudio.client.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.hudstudio.client.HudStudioClient;

public final class WaypointRenderer {
    public static int labelColor(int color) {return 0xff000000|(color&0xffffff);}
    public static void extract() {
        var mc=Minecraft.getInstance();var store=Navigation.store();
        var config=HudStudioClient.config;
        if(store==null || mc.level==null || mc.player==null || !config.waypointsEnabled)return;
        try(var ignored=mc.levelExtractor.collectPerFrameMainThreadGizmos()) {
            for(var waypoint:store.visible(Navigation.dimension())) {
                var pos=new BlockPos(waypoint.x(),waypoint.y()-1,waypoint.z());
                double distance=mc.player.position().distanceTo(Vec3.atCenterOf(pos));
                // Avoid extremely distant geometry; the map still shows every marker.
                if(distance>4096)continue;
                int rgb=waypoint.color()&0xffffff;
                Gizmos.cuboid(new AABB(pos).inflate(.003),GizmoStyle.strokeAndFill(0xff000000|rgb,2,0x30000000|rgb)).setAlwaysOnTop();
            }
        }
    }
    private static final java.util.Map<String,net.minecraft.world.entity.Display.TextDisplay> labels=new java.util.HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel labelLevel;
    private static int nextLabelId=-1_000_000;
    public static void clearLabels() {
        labels.values().forEach(net.minecraft.world.entity.Entity::discard);labels.clear();labelLevel=null;
    }
    /** Local text displays use vanilla billboarding, layout, background and perspective rendering. */
    public static void updateLabels(Minecraft mc) {
        if(labelLevel!=mc.level){clearLabels();labelLevel=mc.level;}
        var config=HudStudioClient.config;var store=Navigation.store();
        java.util.Set<String> active=new java.util.HashSet<>();
        if(mc.level!=null && mc.player!=null && store!=null && config.waypointsEnabled) {
            for(var waypoint:store.visible(Navigation.dimension())) {
                double distance=mc.player.position().distanceTo(new Vec3(waypoint.x()+.5,waypoint.y()+.5,waypoint.z()+.5));
                String text=(config.waypointShowName?waypoint.name():"")+(config.waypointShowDistance?(config.waypointShowName?"  ":"")+Math.round(distance)+"m":"");
                if(text.isEmpty() || distance>4096)continue;
                active.add(waypoint.id());
                var display=labels.get(waypoint.id());
                boolean created=display==null || display.isRemoved();
                if(created) {
                    display=new net.minecraft.world.entity.Display.TextDisplay(net.minecraft.world.entity.EntityTypes.TEXT_DISPLAY,mc.level);
                    display.setId(nextLabelId--);
                    display.setBillboardConstraints(net.minecraft.world.entity.Display.BillboardConstraints.CENTER);
                    display.setBrightnessOverride(new net.minecraft.util.Brightness(15,15));
                    display.setViewRange(64);display.setLineWidth(1000);
                    display.setFlags(net.minecraft.world.entity.Display.TextDisplay.FLAG_SEE_THROUGH);
                    labels.put(waypoint.id(),display);
                }
                display.setPos(waypoint.x()+.5,waypoint.y()+.15,waypoint.z()+.5);
                display.setOldPosAndRot();
                var component=net.minecraft.network.chat.Component.literal(text).withColor(labelColor(waypoint.color())&0xffffff);
                if(!component.equals(display.getText()))display.setText(component);
                if(created){display.tick();mc.level.addEntity(display);}
            }
        }
        labels.entrySet().removeIf(entry->{if(active.contains(entry.getKey()))return false;entry.getValue().discard();return true;});
    }
}

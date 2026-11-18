package dev.hudstudio.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hudstudio.client.navigation.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.*;
import java.nio.file.*;
import java.util.*;

public final class NavigationSmokeTest {
    private static void check(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
    private static KeyEvent key(int key) {return new KeyEvent(key,0,0);}
    private static MouseButtonEvent mouse(double x,double y,int button) {return new MouseButtonEvent(x,y,new MouseButtonInfo(button,0));}
    private static List<EditBox> fields(Minecraft mc) {return mc.gui.screen().children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();}
    private static StudioUi.Action button(Minecraft mc,String label) {
        return mc.gui.screen().children().stream().filter(StudioUi.Action.class::isInstance).map(StudioUi.Action.class::cast)
            .filter(b->b.getMessage().getString().equals(label)).findFirst().orElseThrow(()->new AssertionError("Missing button: "+label));
    }
    private static void click(ClientGameTestContext c,String label) {c.runOnClient(mc->{var b=button(mc,label);check(b.active,"Inactive button: "+label);b.onPress(new MouseButtonInfo(1,0));});c.waitTicks(2);}
    public static void run(ClientGameTestContext c,TestServerContext server) {
        server.runCommand("execute at @p run fill ~-12 ~-1 ~-12 ~-5 ~-1 ~-5 minecraft:water");
        server.runCommand("execute at @p run fill ~5 ~-1 ~-12 ~12 ~-1 ~-5 minecraft:sand");
        server.runCommand("execute at @p run fill ~-12 ~-1 ~5 ~12 ~-1 ~7 minecraft:stone_bricks");
        server.runCommand("execute at @p run fill ~-9 ~ ~9 ~-5 ~3 ~13 minecraft:oak_log hollow");
        c.waitTicks(220);
        c.runOnClient(mc->mc.gui.setScreen(new HudSettingsScreen(null)));
        c.waitTicks(2);c.takeScreenshot("studio-navigation");click(c,"Waypoints");c.waitForScreen(WaypointListScreen.class);
        click(c,"Back");click(c,"World Map");c.waitForScreen(WorldMapScreen.class);click(c,"Back");click(c,"Back");
        c.runOnClient(mc->{
            check(Navigation.store()!=null,"Navigation has no world store");
            check(Navigation.createKey.matches(key(InputConstants.KEY_N)) && Navigation.listKey.matches(key(InputConstants.KEY_U))
                && Navigation.mapKey.matches(key(InputConstants.KEY_M)),"Wrong navigation defaults");
            // Simple Voice Chat binds N/M too and opens onboarding on either key.
            // Isolate navigation input without changing the user's installed options.
            for(String id:List.of("key.disable_voice_chat","key.mute_microphone")) {
                var mapping=KeyMapping.get(id);if(mapping!=null)mapping.setKey(InputConstants.UNKNOWN);
            }
            KeyMapping.resetMapping();
            var old=mc.player.position();mc.player.setPos(-12.6,70.49,25.5);
            var draft=Navigation.newWaypoint();mc.player.setPos(old);
            check(draft.name().equals("Waypoint") && draft.x()==-13 && draft.y()==70 && draft.z()==26,"Default name or rounding incorrect");
            check(Navigation.COLORS.stream().anyMatch(color->(0xff000000|color.getValue())==draft.color()),"Random colour is not a vanilla colour");
            check(mc.gui.screen()==null,"A screen remained open before N: "+mc.gui.screen());
        });
        c.getInput().pressKey(Navigation.createKey);
        c.waitTicks(3);c.takeScreenshot("waypoint-key-N");
        c.runOnClient(mc->check(mc.gui.screen() instanceof WaypointEditorScreen,"N opened "+mc.gui.screen()));
        c.waitForScreen(WaypointEditorScreen.class);c.waitTicks(2);c.takeScreenshot("waypoint-new");
        c.runOnClient(mc->{
            var boxes=fields(mc);check(boxes.getFirst().getValue().equals("Waypoint"),"N did not create a default draft");
            boxes.get(1).setValue("12.5");check(!button(mc,"Save").active,"Decimal coordinates accepted");
            boxes.get(1).setValue("30000001");check(!button(mc,"Save").active,"Out-of-range coordinates accepted");
            boxes.get(0).setValue("Camp");boxes.get(1).setValue("-13");boxes.get(2).setValue("64");boxes.get(3).setValue("27");
            button(mc,"gold").onPress(new MouseButtonInfo(1,0));
        });
        click(c,"Save");
        c.runOnClient(mc->{
            var saved=Navigation.store().all().getFirst();check(saved.name().equals("Camp") && saved.x()==-13 && saved.y()==64 && saved.z()==27,"Waypoint fields were not saved");
            check(saved.color()==0xffffaa00,"Selected vanilla gold was not saved");
            check(new WaypointStore(Navigation.store().directory,"Test").all().equals(Navigation.store().all()),"Waypoints did not persist");
        });
        c.getInput().pressKey(Navigation.listKey);
        c.waitForScreen(WaypointListScreen.class);c.waitTicks(2);c.takeScreenshot("waypoint-list");
        click(c,"Edit");
        c.runOnClient(mc->{check(fields(mc).getFirst().getValue().equals("Camp"),"Edit did not load saved waypoint");fields(mc).getFirst().setValue("Discarded edit");});
        click(c,"Cancel");
        c.runOnClient(mc->check(Navigation.store().all().getFirst().name().equals("Camp"),"Cancel changed the saved waypoint"));
        click(c,"Edit");
        c.runOnClient(mc->{
            var boxes=fields(mc);boxes.get(0).setValue("Base camp");
            boxes.get(1).setValue(""+mc.player.getBlockX());boxes.get(2).setValue(""+mc.player.getBlockY());boxes.get(3).setValue(""+(mc.player.getBlockZ()+4));
        });
        click(c,"Save");
        c.runOnClient(mc->{mc.player.setYRot(0);mc.player.setXRot(0);});
        click(c,"Hide");
        c.runOnClient(mc->{check(Navigation.store().visible(Navigation.dimension()).isEmpty(),"Hide retained the world marker");
            check(!new WaypointStore(Navigation.store().directory,"Test").all().getFirst().visible(),"Hidden state did not persist");});
        click(c,"Back");c.waitTicks(3);var hidden=c.takeScreenshot("waypoint-hidden-block");
        c.getInput().pressKey(Navigation.listKey);c.waitForScreen(WaypointListScreen.class);
        click(c,"Show");click(c,"Back");
        c.waitTicks(3);var visible=c.takeScreenshot("waypoint-highlight-block");
        check(goldPixels(visible)>goldPixels(hidden)+50,"Waypoint cube did not render, or Hide did not remove it");
        c.runOnClient(mc->{
            check(Navigation.store().visible("minecraft:the_nether").isEmpty(),"Waypoint leaked into another dimension");
            var nether=Navigation.draft(0,70,0,"minecraft:the_nether");check(Navigation.store().put(nether),"Cannot save other-dimension waypoint");
        });
        c.getInput().pressKey(Navigation.mapKey);
        c.waitForScreen(WorldMapScreen.class);c.waitTicks(3);c.takeScreenshot("world-map");
        click(c,"overworld");c.waitTicks(2);c.takeScreenshot("world-map-nether");click(c,"Find player");
        c.runOnClient(mc->{
            var map=(WorldMapScreen)mc.gui.screen();double x=map.blockX(180),z=map.blockZ(100),zoom=map.zoom();
            map.mouseScrolled(180,100,0,2);check(map.zoom()>zoom,"Map did not zoom");
            check(Math.abs(map.blockX(180)-x)<.0001 && Math.abs(map.blockZ(100)-z)<.0001,"Zoom did not preserve cursor position");
            double before=map.blockX(180);map.mouseClicked(mouse(180,100,1),false);map.mouseDragged(mouse(200,110,1),20,10);map.mouseReleased(mouse(200,110,1));
            check(map.blockX(180)<before,"Map drag did not pan");
        });
        c.waitTicks(2);c.takeScreenshot("world-map-zoomed");
        click(c,"Grid OFF");click(c,"Find player");
        c.runOnClient(mc->mc.gui.screen().mouseClicked(mouse(200,110,3),false));
        c.waitForScreen(WaypointEditorScreen.class);click(c,"Cancel");
        c.runOnClient(mc->mc.gui.screen().keyPressed(key(InputConstants.KEY_M)));c.waitTicks(2);
        c.runOnClient(mc->{
            var atlas=Navigation.atlas();String dim=Navigation.dimension();int x=mc.player.getBlockX(),z=mc.player.getBlockZ();
            var height=atlas.height(dim,x,z);check(height!=null,"Map did not sample loaded terrain");
            atlas.flush();MapAtlas.finishWrites();
            try(var reloaded=new MapAtlas(Navigation.store().directory)){check(height.equals(reloaded.height(dim,x,z)),"Explored map did not persist");}
            testStorage();
            // Search and paginate without dropping records or losing search focus.
            for(int i=0;i<6;i++)check(Navigation.store().put(new Waypoint("page-test-"+i,"Page test "+i,0xff55ffff,i,64,i,dim,false)),"Cannot add pagination fixture");
            mc.gui.setScreen(new WaypointListScreen(null));
            fields(mc).getFirst().setValue("Page test");
            check(button(mc,">").active,"Additional waypoints did not create another page");
            button(mc,">").onPress(new MouseButtonInfo(1,0));
            check(fields(mc).getFirst().getValue().equals("Page test"),"Pagination lost the search text");
            fields(mc).getFirst().setValue("No such marker");check(!button(mc,">").active,"Empty search retained extra pages");
            for(int i=0;i<6;i++)Navigation.store().delete("page-test-"+i);
            mc.gui.setScreen(null);
        });
        c.getInput().pressKey(Navigation.listKey);
        c.waitForScreen(WaypointListScreen.class);click(c,"Delete");c.waitForScreen(ConfirmScreen.class);
        c.runOnClient(mc->{
            // Delete uses an explicit confirmation and leaves the other dimension intact.
            var screen=(ConfirmScreen)mc.gui.screen();
            for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("Yes")){b.onPress(new MouseButtonInfo(1,0));break;}
        });c.waitTicks(2);
        c.runOnClient(mc->{check(Navigation.store().all().size()==1,"Delete did not remove exactly one waypoint");mc.gui.setScreen(null);});
    }
    private static void testStorage() {
        try {
            Path root=Files.createTempDirectory(Path.of("."),"waypoint-storage-test-");
            var a=new WaypointStore(root.resolve("world-a"),"A");var b=new WaypointStore(root.resolve("world-b"),"B");
            var waypoint=Navigation.draft(-17,64,-129,"minecraft:overworld");
            check(a.put(waypoint) && b.all().isEmpty(),"Waypoints leaked between world stores");
            check(new WaypointStore(a.directory,"A").all().equals(List.of(waypoint)),"Negative coordinates failed persistence");
            check(a.delete(waypoint.id()) && new WaypointStore(a.directory,"A").all().isEmpty(),"Delete did not persist");
            Files.writeString(a.directory.resolve("waypoints.json"),"broken JSON");
            var broken=new WaypointStore(a.directory,"A");check(!broken.readable() && !broken.put(waypoint),"Unreadable storage was overwritten");
            check(Files.readString(a.directory.resolve("waypoints.json")).equals("broken JSON"),"Invalid file was not preserved");
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static int goldPixels(Path path) {
        try {
            var image=javax.imageio.ImageIO.read(path.toFile());int count=0;
            for(int y=image.getHeight()*2/5;y<image.getHeight()*4/5;y++)
                for(int x=image.getWidth()*2/5;x<image.getWidth()*3/5;x++) {
                    int rgb=image.getRGB(x,y);if((rgb>>16&255)>200 && (rgb>>8&255)>140 && (rgb>>8&255)<220 && (rgb&255)<80)count++;
                }
            return count;
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
}

package dev.hudstudio.client;

import dev.hudstudio.client.navigation.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.*;
import net.minecraft.world.level.material.MapColor;
import java.util.*;

public final class RevisionSmokeTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static MouseButtonEvent mouse(double x,double y,int b){return new MouseButtonEvent(x,y,new MouseButtonInfo(b,0));}
    private static List<EditBox> fields(Minecraft mc){return mc.gui.screen().children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();}
    private static StudioUi.Action button(Minecraft mc,String label){return mc.gui.screen().children().stream().filter(StudioUi.Action.class::isInstance).map(StudioUi.Action.class::cast).filter(b->b.getMessage().getString().equals(label)).findFirst().orElseThrow(()->new AssertionError("Missing "+label));}
    private static void click(ClientGameTestContext c,String label){c.runOnClient(mc->button(mc,label).onPress(new MouseButtonInfo(1,0)));c.waitTicks(2);}
    private static void query(ClientGameTestContext c,String value){c.runOnClient(mc->fields(mc).getFirst().setValue(value));c.waitTicks(2);}
    private static void pointer(Minecraft mc,double x,double y) {
        var w=mc.getWindow();
        for(int i=0;i<2;i++)mc.mouseHandler.onMove(w.handle(),x*w.getScreenWidth()/w.getGuiScaledWidth(),y*w.getScreenHeight()/w.getGuiScaledHeight(),0,0);
        mc.mouseHandler.handleAccumulatedMovement();
    }
    public static void run(ClientGameTestContext c) {
        try(var world=c.worldBuilder().adjustSettings(s->s.setAllowCommands(true)).create()) {
            c.waitFor(mc->mc.player!=null && mc.gui.screen()==null && Navigation.store()!=null);
            c.runOnClient(dev.hudstudio.client.navigation.MapRasterSmokeTest::run);
            menuDimming(c,world.getServer());
            c.runOnClient(mc->{HudStudioClient.config=HudConfig.defaults();mc.gui.setScreen(new HudSettingsScreen(null));});
            c.runOnClient(mc->{
                for(int[] size:List.of(new int[]{320,240},new int[]{640,360},new int[]{854,480})) {
                    var screen=mc.gui.screen();screen.resize(size[0],size[1]);
                    int fixedLeft=button(mc,"Appearance").getX();int fixedBack=button(mc,"Back").getX();
                    int standardWidth=0,standardHeight=0;
                    for(String category:List.of("HUD","Inventory","Camera","Visuals")) {
                        button(mc,category).onPress(new MouseButtonInfo(1,0));
                        check(button(mc,"Appearance").getX()==fixedLeft && button(mc,"Back").getX()==fixedBack,"Sidebar moved between categories at "+size[0]);
                        var cards=screen.children().stream().filter(ModuleCard.class::isInstance).map(ModuleCard.class::cast).toList();
                        if(standardWidth==0){standardWidth=cards.getFirst().getWidth();standardHeight=cards.getFirst().getHeight();}
                        for(var card:cards)check(card.getWidth()==standardWidth && card.getHeight()==standardHeight,"Module sizes changed between categories");
                        if(category.equals("HUD")) {
                            int right=cards.stream().mapToInt(w->w.getX()+w.getWidth()).max().orElseThrow();
                            check(Math.abs(right-(screen.width-fixedLeft))<=3,"HUD grid does not fill Waypoints template");
                        }
                    }
                    button(mc,"Waypoints").onPress(new MouseButtonInfo(1,0));
                    check(button(mc,"Appearance").getX()==fixedLeft && button(mc,"Back").getX()==fixedBack,"Waypoints frame differs from module sections");
                    var end=button(mc,"Show Distances");var dimension=button(mc,"Dimension");
                    int panelLeft=fields(mc).getFirst().getX()-7;
                    check(Math.abs(button(mc,"New Waypoint").getX()+end.getX()+end.getWidth()-panelLeft-dimension.getX()-dimension.getWidth())<=1,"Waypoint footer not centred at "+size[0]);
                    button(mc,"HUD").onPress(new MouseButtonInfo(1,0));
                }
                mc.gui.setScreen(new HudSettingsScreen(null));
                check(HudLayout.name(HudStudioClient.config.elements.get("bossbar")).equals("Boss Bars"),"Module title case missing");
                check(HudLayout.name(HudStudioClient.config.elements.get("player_tab")).equals("Player Tab"),"Module title case missing");
            });
            c.waitTicks(3);c.takeScreenshot("revision-hud-category");
            c.runOnClient(mc->{
                for(String label:List.of("HUD","Inventory","Camera","Visuals","Waypoints"))button(mc,label);
                check(mc.gui.screen().children().stream().filter(StudioUi.Action.class::isInstance).map(StudioUi.Action.class::cast).noneMatch(b->List.of("Stats","Keystrokes","Game HUD").contains(b.getMessage().getString())),"Old HUD categories remain");
            });
            var cardSize=c.computeOnClient(mc->{
                var cards=mc.gui.screen().children().stream().filter(ModuleCard.class::isInstance).map(ModuleCard.class::cast).toList();
                check(cards.size()==16,"All HUD modules must fit without scrolling");return new int[]{cards.getFirst().getWidth(),cards.getFirst().getHeight()};
            });
            for(String category:List.of("HUD","Inventory","Camera","Visuals")) {
                click(c,category);
                c.runOnClient(mc->{check(fields(mc).isEmpty(),"Category search remains");
                    var back=button(mc,"Back");var appearance=button(mc,"Appearance");
                    check(back.getX()==appearance.getX() && back.getWidth()==appearance.getWidth() && back.getHeight()==appearance.getHeight(),"Back not aligned with Appearance");
                    for(var child:mc.gui.screen().children())if(child instanceof ModuleCard card){
                        check(card.getWidth()==cardSize[0] && card.getHeight()==cardSize[1],"Module sizes differ");
                        check(card.getY()>=32 && card.getY()+card.getHeight()<=back.getY()-6,"Module clipped");
                        int y=card.getY();mc.gui.screen().mouseScrolled(card.getX()+4,y+4,0,-8);check(card.getY()==y,"Modules still scroll");
                    }
                });
            }
            click(c,"Waypoints");c.waitTicks(3);c.takeScreenshot("revision-waypoint-manager-category");
            c.runOnClient(mc->{
                check(((HudSettingsScreen)mc.gui.screen()).waypointPanel()!=null,"Waypoints category is not the manager");
                for(String label:List.of("New Waypoint","Enabled","Show Names","Show Distances"))check(button(mc,label).getY()==button(mc,"Back").getY(),"Footer alignment: "+label);
                check(!fields(mc).getFirst().isBordered(),"Waypoint search does not use Studio style");
                check(mc.gui.screen().children().stream().noneMatch(AbstractSliderButton.class::isInstance),"Distance sliders remain");
                check(mc.gui.screen().children().stream().filter(StudioUi.Action.class::isInstance).map(StudioUi.Action.class::cast).noneMatch(b->List.of("Manage","World Map").contains(b.getMessage().getString())),"Studio shortcuts remain");
            });
            click(c,"Show Names");c.runOnClient(mc->check(!HudStudioClient.config.waypointShowName && !HudConfig.load().waypointShowName,"Name toggle not persisted"));
            click(c,"Show Distances");c.runOnClient(mc->check(!HudStudioClient.config.waypointShowDistance,"Distance toggle failed"));
            click(c,"Enabled");c.runOnClient(mc->check(!HudStudioClient.config.waypointsEnabled,"Enabled toggle failed"));
            c.runOnClient(mc->{HudStudioClient.config=HudConfig.defaults();mc.gui.setScreen(null);});
            c.waitTicks(160);
            c.runOnClient(mc->{
                for(int cz=-3;cz<=3;cz++)for(int cx=-3;cx<=3;cx++) {
                    if(!mc.level.hasChunk(cx,cz))continue;
                    var r=Navigation.atlas().region(Navigation.dimension(),Math.floorDiv(cx,8),Math.floorDiv(cz,8),false);check(r!=null,"Missing loaded map region");
                    for(int z=0;z<16;z++)for(int x=0;x<16;x++)check(r.colors[Math.floorMod(cz*16+z,128)*128+Math.floorMod(cx*16+x,128)]!=0,"Map hole in loaded chunk "+cx+", "+cz);
                }
                check(MapAtlas.vanillaColor(MapColor.GRASS,64,64,0,0,0)==MapColor.GRASS.calculateARGBColor(MapColor.Brightness.NORMAL),"Vanilla grass shade mismatch");
                check(MapAtlas.vanillaColor(MapColor.WATER,64,64,1,0,0)==MapColor.WATER.calculateARGBColor(MapColor.Brightness.HIGH),"Vanilla shallow water mismatch");
                check(MapAtlas.vanillaColor(MapColor.WATER,64,64,20,0,0)==MapColor.WATER.calculateARGBColor(MapColor.Brightness.LOW),"Vanilla deep water mismatch");
                mc.gui.setScreen(new WaypointEditorScreen(null,Navigation.newWaypoint(),false));
            });c.waitTicks(3);c.takeScreenshot("revision-compact-editor");
            c.runOnClient(mc->{
                var f=fields(mc);check(f.getFirst().getWidth()==160,"Editor is not inventory sized");f.getFirst().setValue("Camp");f.get(1).setValue("0");f.get(2).setValue("-60");f.get(3).setValue("4");
                button(mc,"gold").onPress(new MouseButtonInfo(1,0));
            });click(c,"Save");
            c.runOnClient(mc->{
                for(int i=0;i<16;i++)Navigation.store().put(new Waypoint("scroll-"+i,"Camp "+i,0xff55ffff,i,64,i,Navigation.dimension(),true));
                Navigation.store().put(Navigation.draft(0,64,0,"minecraft:the_nether"));
                mc.gui.setScreen(new WaypointListScreen(null));
            });c.waitTicks(3);c.takeScreenshot("revision-scroll-list");
            c.runOnClient(mc->{var b=button(mc,"Delete");pointer(mc,b.getX()+10,b.getY()+8);});c.waitTicks(2);c.takeScreenshot("revision-manager-delete-hover");
            c.runOnClient(mc->pointer(mc,1,1));
            managerSorting(c);
            query(c,"Camp 1");c.runOnClient(mc->check(((WaypointListScreen)mc.gui.screen()).filteredCount()==7,"Waypoint search failed"));query(c,"");
            c.runOnClient(mc->{
                var screen=(WaypointListScreen)mc.gui.screen();check(screen.filteredCount()==17,"Manager does not default to current dimension");
                check(screen.children().stream().filter(StudioUi.Action.class::isInstance).map(StudioUi.Action.class::cast).noneMatch(b->List.of("World Map","<",">").contains(b.getMessage().getString())),"Old manager controls remain");
                screen.mouseScrolled(screen.width-100,100,0,-10);check(screen.scrollOffset()>0,"Manager did not scroll");
            });c.waitTicks(2);c.takeScreenshot("revision-scroll-list-bottom");
            var count=c.computeOnClient(mc->Navigation.store().all().size());click(c,"Delete");
            c.runOnClient(mc->{check(mc.gui.screen() instanceof WaypointListScreen,"Delete opened a confirmation screen");check(Navigation.store().all().size()==count-1,"Delete was not immediate");});
            click(c,"Back");labels(c,world.getServer());
            c.runOnClient(mc->mc.gui.setScreen(new WorldMapScreen(null)));c.takeScreenshot("revision-map-opening");
            c.waitTicks(8);c.takeScreenshot("revision-minimal-map");
            c.runOnClient(mc->pointer(mc,mc.gui.screen().width/2,mc.gui.screen().height/2));c.waitTicks(8);
            var hoverOn=c.takeScreenshot("revision-label-hover-on");
            c.runOnClient(mc->pointer(mc,1,1));c.waitTicks(8);
            var hoverOff=c.takeScreenshot("revision-label-hover-off");
            check(goldPixels(hoverOn)>goldPixels(hoverOff)+30,"Map hover label did not fade away");
            for(int step=0;step<3;step++) {
                c.runOnClient(mc->{var map=(WorldMapScreen)mc.gui.screen();map.mouseScrolled(map.width/2,map.height/2,0,-1);});
                c.waitTicks(2);c.takeScreenshot("revision-fractional-fog-"+step);
            }
            c.runOnClient(mc->{var b=button(mc,"x");pointer(mc,b.getX()+7,b.getY()+7);});c.waitTicks(2);c.takeScreenshot("revision-map-close-active");
            c.runOnClient(mc->pointer(mc,1,1));
            c.runOnClient(mc->{
                var map=(WorldMapScreen)mc.gui.screen();check(map.children().size()==1 && button(mc,"x")!=null,"Map is not minimal");
                double x=map.blockX(100),z=map.blockZ(100);map.mouseScrolled(100,100,0,1);
                check(Math.abs(map.blockX(100)-x)<.001 && Math.abs(map.blockZ(100)-z)<.001,"Map zoom lost cursor anchor");
                map.mouseClicked(mouse(160,80,1),false);map.mouseDragged(mouse(180,90,1),20,10);map.mouseReleased(mouse(180,90,1));
            });
            mapMenus(c);
            click(c,"x");
            world.getServer().runCommand("fill -16 24 -16 16 48 16 minecraft:stone");
            world.getServer().runCommand("fill -5 34 -5 5 43 5 minecraft:stone hollow");
            world.getServer().runCommand("tp @p 0 35 0");c.waitTicks(240);
            c.runOnClient(mc->{check(Navigation.atlas().height(Navigation.dimension(),0,0)==48,"Underground surveying did not retain the surface roof");mc.gui.setScreen(new WorldMapScreen(null));});
            c.waitTicks(3);c.takeScreenshot("revision-surface-map-underground");click(c,"x");
            world.getServer().runCommand("tp @p 0 49 0");c.waitTicks(25);
            c.runOnClient(mc->{Navigation.atlas().flush();MapAtlas.finishWrites();
                try {
                    var oldLayer=Base64.getUrlEncoder().withoutPadding().encodeToString((Navigation.dimension()+"@cave/35").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    java.nio.file.Files.createDirectories(Navigation.store().directory.resolve("maps").resolve(oldLayer));
                    try(var reload=new MapAtlas(Navigation.store().directory)) {
                        check(reload.height(Navigation.dimension(),0,0)==48,"Surface map did not persist from underground");
                        check(reload.dimensions().stream().noneMatch(d->d.contains("@")),"Obsolete altitude layers entered dimension index");
                    }
                    java.nio.file.Files.writeString(java.nio.file.Path.of("revision-result.txt"),"PASS: shared category layout, visibility/name sorting in manager, vanilla client text displays and world-only toggles, independent map markers and labels, map hover fade, context menus, fractional zoom fog, vanilla palette, surface-only mapping, persistence and obsolete layer filtering");
                }catch(Exception e){throw new RuntimeException(e);}
            });
            if(Boolean.getBoolean("hudstudio.freecamRevision"))freecam(c,world.getServer());
        }
    }
    private static void managerSorting(ClientGameTestContext c) {
        c.runOnClient(mc->{
            String[] names={"B Fourth Sort","a Third Sort","9 Second Sort","0 First Sort","0 Hidden Sort","a Hidden Sort"};
            for(int i=0;i<names.length;i++)Navigation.store().put(new Waypoint("sorting-fixture-"+i,names[i],0xff55ffff,i,64,i,Navigation.dimension(),i<4));
        });query(c,"Sort");c.takeScreenshot("revision-manager-sorted-hidden");
        c.runOnClient(mc->check(button(mc,"Show").getWidth()>=mc.font.width("Show")+8,"Show button truncates its label"));
        click(c,"Edit");c.runOnClient(mc->check(fields(mc).getFirst().getValue().equals("0 First Sort"),"Manager did not sort shown numeric names before letters/hidden names"));click(c,"Cancel");query(c,"Sort");
        click(c,"Hide");click(c,"Edit");c.runOnClient(mc->check(fields(mc).getFirst().getValue().equals("9 Second Sort"),"Hidden waypoint did not move below shown waypoints"));click(c,"Cancel");query(c,"Sort");
        click(c,"Show");click(c,"Edit");c.runOnClient(mc->check(fields(mc).getFirst().getValue().equals("0 First Sort"),"Shown waypoint did not return to sorted position"));click(c,"Cancel");query(c,"Sort");
        c.runOnClient(mc->{for(var w:Navigation.store().all())if(w.id().startsWith("sorting-fixture-"))Navigation.store().delete(w.id());});query(c,"");
    }
    private static void mapMenus(ClientGameTestContext c) {
        c.runOnClient(mc->{
            var map=(WorldMapScreen)mc.gui.screen();
            for(var w:Navigation.store().all())Navigation.store().delete(w.id());
            int x=(int)Math.floor(map.blockX(map.width/2)),z=(int)Math.floor(map.blockZ(map.height/2));
            Navigation.store().put(new Waypoint("map-pin","Home",0xff55ff55,x,64,z,Navigation.dimension(),true));
            map.mouseClicked(mouse(map.width/2,map.height/2-10,1),false);
            check(mc.gui.screen()==map && map.children().size()==1,"Left click waypoint must do nothing");
            double old=map.blockX(100);map.mouseDragged(mouse(200,130,1),20,20);check(map.blockX(100)==old,"Left click waypoint started dragging");
            map.mouseClicked(mouse(map.width/2,map.height/2-10,3),false);
            for(String label:List.of("Edit","Copy Coords","Teleport","Delete","Close"))button(mc,label);
            check(!button(mc,"Edit").isFocused(),"Popup first row was focused automatically");pointer(mc,1,1);
        });c.waitTicks(2);c.takeScreenshot("revision-map-waypoint-popup");
        c.runOnClient(mc->{var b=button(mc,"Delete");pointer(mc,b.getX()+10,b.getY()+8);});c.waitTicks(2);c.takeScreenshot("revision-map-delete-hover");
        click(c,"Copy Coords");c.takeScreenshot("revision-coords-copied");c.runOnClient(mc->{var w=Navigation.store().all().getFirst();check(mc.keyboardHandler.getClipboard().equals(w.x()+" "+w.y()+" "+w.z()),"Waypoint clipboard coordinates differ");
            var map=(WorldMapScreen)mc.gui.screen();map.mouseClicked(mouse(map.width/2,map.height/2-10,3),false);
        });click(c,"Edit");c.runOnClient(mc->check(mc.gui.screen() instanceof WaypointEditorScreen,"Edit menu failed"));click(c,"Cancel");
        c.runOnClient(mc->{var map=(WorldMapScreen)mc.gui.screen();map.mouseClicked(mouse(map.width/2,map.height/2-10,3),false);});click(c,"Delete");
        c.runOnClient(mc->{check(Navigation.store().all().isEmpty(),"Map delete was not immediate");var map=(WorldMapScreen)mc.gui.screen();map.mouseClicked(mouse(120,100,3),false);
            for(String label:List.of("New Waypoint","Copy Coords","Teleport","Close"))button(mc,label);
        });c.waitTicks(2);c.takeScreenshot("revision-map-location-popup");click(c,"New Waypoint");
        c.runOnClient(mc->check(mc.gui.screen() instanceof WaypointEditorScreen,"Create menu failed"));click(c,"Cancel");
        c.runOnClient(mc->{var map=(WorldMapScreen)mc.gui.screen();map.mouseClicked(mouse(120,100,3),false);});click(c,"Close");
        c.runOnClient(mc->check(mc.gui.screen().children().size()==1,"Close menu failed"));
        var target=c.computeOnClient(mc->{var map=(WorldMapScreen)mc.gui.screen();
            int x=(int)Math.floor(map.blockX(map.width/2))+3,z=(int)Math.floor(map.blockZ(map.height/2));
            Navigation.store().put(new Waypoint("teleport-pin","Destination",0xffffaa00,x,-60,z,Navigation.dimension(),true));
            map.mouseClicked(mouse(map.width/2+3*map.zoom(),map.height/2-10,3),false);button(mc,"Teleport");return new int[]{x,z};
        });click(c,"Teleport");c.waitTicks(10);
        c.runOnClient(mc->{check(mc.gui.screen()==null && Math.abs(mc.player.getX()-(target[0]+.5))<1 && Math.abs(mc.player.getZ()-(target[1]+.5))<1,"Map teleport failed");
            mc.gui.setScreen(new WorldMapScreen(null));
        });
    }
    private static java.util.List<net.minecraft.world.entity.Display.TextDisplay> displays(Minecraft mc) {
        var result=new java.util.ArrayList<net.minecraft.world.entity.Display.TextDisplay>();
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.Display.TextDisplay d && d.getId()<=-1_000_000 && !d.isRemoved())result.add(d);
        return result;
    }
    private static void labels(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
        c.runOnClient(mc->{for(var w:Navigation.store().all())Navigation.store().delete(w.id());Navigation.store().put(new Waypoint("label-size","Camp",0xffffaa00,0,-60,8,Navigation.dimension(),true));});
        server.runCommand("tp @p 0 -60 0 0 0");c.waitTicks(10);
        c.runOnClient(mc->{
            var d=displays(mc).getFirst();check(displays(mc).size()==1,"Native text display missing");
            check(d.getText().getString().startsWith("Camp  "),"Native display text differs");
            check(d.getBillboardConstraints()==net.minecraft.world.entity.Display.BillboardConstraints.CENTER,"Text display does not face camera");
            check(d.getBackgroundColor()==net.minecraft.world.entity.Display.TextDisplay.INITIAL_BACKGROUND,"Non-vanilla text background");
            check(Math.abs(d.getY()-(-60+.15))<.001,"World label not lowered above the highlighted block");
            check(d.getText().getStyle().getColor().getValue()==0xffaa00,"Label colour does not match waypoint");
        });c.takeScreenshot("waypoint-native-label-near");
        c.runOnClient(mc->{var w=Navigation.store().all().getFirst();Navigation.store().put(new Waypoint(w.id(),w.name(),0xff0000aa,w.x(),w.y(),w.z(),w.dimension(),true));});c.waitTicks(3);
        c.runOnClient(mc->{check(displays(mc).getFirst().getText().getStyle().getColor().getValue()==0x0000aa,"Dark waypoint text was replaced with white");var w=Navigation.store().all().getFirst();Navigation.store().put(new Waypoint(w.id(),w.name(),0xffffaa00,w.x(),w.y(),w.z(),w.dimension(),true));});c.waitTicks(3);
        server.runCommand("tp @p 0 -60 -24 0 0");c.waitTicks(10);c.takeScreenshot("waypoint-native-label-far");
        c.runOnClient(mc->HudStudioClient.config.waypointShowName=false);c.waitTicks(3);
        c.runOnClient(mc->{check(!displays(mc).getFirst().getText().getString().contains("Camp"),"Name toggle ignored by world display");HudStudioClient.config.waypointShowDistance=false;});c.waitTicks(3);
        c.runOnClient(mc->{check(displays(mc).isEmpty(),"Empty world labels remained");HudStudioClient.config.waypointShowName=true;});c.waitTicks(3);
        c.runOnClient(mc->{check(displays(mc).getFirst().getText().getString().equals("Camp"),"Distance toggle ignored by world display");HudStudioClient.config.waypointsEnabled=false;});c.waitTicks(3);
        c.runOnClient(mc->{check(displays(mc).isEmpty(),"Disabling world waypoints left displays behind");HudStudioClient.config.waypointShowName=false;});
        server.runCommand("tp @p 0 -60 0 0 0");c.waitTicks(5);
    }
    private static void menuDimming(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
        server.runCommand("item replace entity @p hotbar.0 with minecraft:diamond_chestplate");c.waitTicks(3);
        var dimensions=c.computeOnClient(mc->{
            HudStudioClient.config=HudConfig.defaults();var slot=new HudConfig.Element("slot-dim-test");slot.id="slot_dim_test";slot.slot=0;slot.hideEmpty=false;
            HudStudioClient.config.slots.add(slot);
            int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
            HudLayout.move(slot,8,h-24,w,h);return new int[]{w,h};
        });c.waitTicks(3);var before=c.takeScreenshot("revision-menu-hud-before");
        c.runOnClient(mc->mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));c.waitTicks(8);
        var dimmed=c.takeScreenshot("revision-menu-hud-dimmed");
        double custom=dimRatio(before,dimmed,dimensions,8,dimensions[1]-24);
        double vanilla=dimRatio(before,dimmed,dimensions,dimensions[0]/2-91,dimensions[1]-22);
        check(custom<.8 && Math.abs(custom-vanilla)<.12,"Custom slot does not dim with hotbar: "+custom+" vs "+vanilla);
        c.runOnClient(mc->{mc.gui.setScreen(null);HudStudioClient.config=HudConfig.defaults();});c.waitTicks(2);
    }
    private static double dimRatio(java.nio.file.Path before,java.nio.file.Path after,int[] gui,int x,int y) {
        try {var a=javax.imageio.ImageIO.read(before.toFile());var b=javax.imageio.ImageIO.read(after.toFile());
            double scale=a.getHeight()/(double)gui[1],lit=0,dim=0;int count=0;
            for(int py=(int)((y+3)*scale);py<(int)((y+19)*scale);py++)for(int px=(int)((x+3)*scale);px<(int)((x+19)*scale);px++) {
                int rgb=a.getRGB(px,py),r=rgb>>16&255,g=rgb>>8&255,blue=rgb&255;
                if(g>100 && blue>100 && g>r*1.3){lit+=g+blue;int dark=b.getRGB(px,py);dim+=(dark>>8&255)+(dark&255);count++;}
            }
            check(count>10,"No diamond pixels in HUD comparison");return dim/lit;
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static int goldPixels(java.nio.file.Path path) {
        try {var image=javax.imageio.ImageIO.read(path.toFile());int count=0;
            for(int y=image.getHeight()/4;y<image.getHeight()*3/4;y++)for(int x=image.getWidth()/4;x<image.getWidth()*3/4;x++) {
                int rgb=image.getRGB(x,y);if((rgb>>16&255)>220 && (rgb>>8&255)>140 && (rgb>>8&255)<210 && (rgb&255)<70)count++;
            }return count;
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static void freecam(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
        server.runCommand("item replace entity @p hotbar.0 with minecraft:diamond_pickaxe");
        server.runCommand("item replace entity @p hotbar.2 with minecraft:diamond_sword");
        server.runCommand("item replace entity @p hotbar.6 with minecraft:torch 64");
        server.runCommand("item replace entity @p weapon.offhand with minecraft:shield");c.waitTicks(5);
        c.runOnClient(mc->{HudStudioClient.config.waypointsEnabled=false;mc.player.getInventory().setSelectedSlot(2);});
        c.waitTicks(3);c.takeScreenshot("revision-real-hotbar-before-freecam");
        var body=c.computeOnClient(mc->mc.player.position());
        c.runOnClient(mc->{
            HudStudioClient.config.tweaks.get("freecam").visible=true;ClientTweaks.toggleFreecam(mc);
            try {
                var getPlayer=net.minecraft.client.gui.Hud.class.getDeclaredMethod("getCameraPlayer");getPlayer.setAccessible(true);
                check(getPlayer.invoke(mc.gui.hud)==mc.player,"Freecam HUD does not use the real player inventory");
                var target=new net.minecraft.core.BlockPos(0,48,0);
                mc.hitResult=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(target),net.minecraft.core.Direction.UP,target,false);
                mc.crosshairPickEntity=mc.player;
                var pick=Minecraft.class.getDeclaredMethod("pick",float.class);pick.setAccessible(true);pick.invoke(mc,1f);
                check(mc.hitResult.getType()==net.minecraft.world.phys.HitResult.Type.MISS && mc.crosshairPickEntity==null,"Freecam still targets blocks/entities");
                check(!mc.gameMode.destroyBlock(target) && !mc.gameMode.startDestroyBlock(target,net.minecraft.core.Direction.UP),"Freecam can break blocks");
                check(mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(target),net.minecraft.core.Direction.UP,target,false))==net.minecraft.world.InteractionResult.PASS,"Freecam can use blocks");
                mc.gameMode.dropItem(mc.player,true);
            }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        });c.waitTicks(4);c.takeScreenshot("revision-freecam-real-hotbar");
        c.getInput().pressKey(mc->mc.keyInventory);c.waitTicks(2);
        c.runOnClient(mc->check(mc.gui.screen()==null,"Freecam opened an inventory"));
        c.getInput().pressKey(mc->mc.keySwapOffhand);c.getInput().pressKey(mc->mc.keyDrop);c.waitTicks(4);
        c.runOnClient(mc->{
            check(mc.player.getInventory().getSelectedSlot()==2 && mc.player.getInventory().getItem(2).is(net.minecraft.world.item.Items.DIAMOND_SWORD),"Freecam dropped or changed the real hotbar");
            check(mc.player.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD),"Freecam swapped hands");
            check(mc.hitResult.getType()==net.minecraft.world.phys.HitResult.Type.MISS,"Freecam resumed block targeting");
            mc.options.keyUp.setDown(true);
        });c.waitTicks(5);
        c.runOnClient(mc->{mc.options.keyUp.setDown(false);check(ClientTweaks.position(1).distanceTo(body)>1,"Freecam cannot move");
            check(mc.player.position().distanceTo(body)<.02,"Freecam moved the player");ClientTweaks.stop(mc);check(mc.getCameraEntity()==mc.player,"Freecam did not restore the player camera");
            try{java.nio.file.Files.writeString(java.nio.file.Path.of("freecam-revision-result.txt"),"PASS: real hotbar and selection, no targeting, block use/break/drop/swap/inventory blocked, independent camera movement and restoration");}catch(Exception e){throw new RuntimeException(e);}
        });
    }
}

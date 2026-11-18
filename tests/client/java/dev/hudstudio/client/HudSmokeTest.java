package dev.hudstudio.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import com.terraformersmc.modmenu.ModMenu;
import java.util.*;

public final class HudSmokeTest implements FabricClientGameTest {
    private static MouseButtonEvent mouse(double x,double y,int button) {return new MouseButtonEvent(x,y,new MouseButtonInfo(button,0));}
    private static void check(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
    private static boolean playerRendered(Minecraft mc) {
        try {
            var state=new net.minecraft.client.renderer.state.level.LevelRenderState();
            var method=net.minecraft.client.renderer.extract.LevelExtractor.class.getDeclaredMethod("extractVisibleEntities",net.minecraft.client.Camera.class,net.minecraft.client.renderer.culling.Frustum.class,net.minecraft.client.DeltaTracker.class,state.getClass());
            method.setAccessible(true);
            var camera=mc.gameRenderer.mainCamera();
            method.invoke(mc.levelExtractor,camera,camera.getCullFrustum(),mc.getDeltaTracker(),state);
            return state.entityRenderStates.stream().anyMatch(e->e.entityType==net.minecraft.world.entity.EntityTypes.PLAYER);
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    private static EditBox search(Minecraft mc) {
        for(var child:mc.gui.screen().children())if(child instanceof EditBox box)return box;
        throw new AssertionError("No search bar");
    }
    private static List<String> modules(Minecraft mc) {
        List<String> names=new ArrayList<>();
        for(var child:mc.gui.screen().children())if(child instanceof ModuleCard card)names.add(card.getMessage().getString());
        return names;
    }
    private static void click(ClientGameTestContext c,String label) {
        for(int attempt=0;attempt<20;attempt++) {
            boolean found=c.computeOnClient(mc -> {
                var screen=mc.gui.screen();
                for(var child:screen.children()) {
                    if(child instanceof StudioUi.Action a && a.getMessage().getString().equals(label)) {a.onPress(new MouseButtonInfo(1,0));return true;}
                    if(child instanceof ModuleCard card && card.getMessage().getString().equals(label)
                        && card.getY()>=58 && card.getY()+68<=screen.height-30) {card.onPress(new MouseButtonInfo(1,0));return true;}
                }
                if(screen instanceof HudSettingsScreen)screen.mouseScrolled(screen.width-30,100,0,-1);
                return false;
            });
            if(found){c.waitTicks(2);return;}
        }
        throw new AssertionError("Missing control: "+label);
    }
    public void runTest(ClientGameTestContext c) {
        if(Boolean.getBoolean("hudstudio.visualRevision")){RevisionSmokeTest.run(c);return;}
        c.runOnClient(mc -> {
            HudStudioClient.config=HudConfig.defaults();
            for(String[] ids:new String[][]{HudConfig.INPUTS,HudConfig.STATS})for(String id:ids)
                check(!HudStudioClient.config.elements.get(id).visible,"Non-vanilla module enabled by default: "+id);
            check(HudStudioClient.config.slots.isEmpty() && HudWidgets.vanillaOffhand(),"Defaults do not preserve vanilla offhand");
            check(HudWidgets.statistic("left_cps",true).equals("0 CPS") && HudWidgets.statistic("right_cps",true).equals("0 CPS"),"CPS still includes a side label");
            for(String id:HudConfig.STATS)if(!id.equals("combo"))check(HudLayout.base(id,427,240).w()==64 && HudLayout.base(id,427,240).h()==20,"Stat dimensions differ from keystrokes");
            for(String id:new String[]{"mouse","space","sneak","sprint"})check(HudLayout.base(id,427,240).w()==64 && HudLayout.base(id,427,240).h()==20,"Keystroke dimensions inconsistent");
            mc.gui.setScreen(ModMenu.getConfigScreen("hud-studio",mc.gui.screen()));
        });
        c.waitForScreen(HudSettingsScreen.class);c.waitTicks(3);click(c,"Keystrokes");
        c.runOnClient(mc -> {
            check(modules(mc).equals(List.of("WASD","LMB / RMB","Left CPS","Right CPS","SPACE","SNEAK","SPRINT")),"Keystrokes category incorrect");
            int appearance=999,keystrokes=0;
            for(var child:mc.gui.screen().children())if(child instanceof StudioUi.Action a){if(a.getMessage().getString().equals("Appearance"))appearance=a.getY();if(a.getMessage().getString().equals("Keystrokes"))keystrokes=a.getY();}
            check(appearance<keystrokes,"Appearance not above categories");
        });
        c.takeScreenshot("keystrokes");
        click(c,"Keystrokes");
        EditBox original=c.computeOnClient(mc -> {var box=search(mc);mc.gui.screen().setFocused(box);box.setFocused(true);return box;});
        c.getInput().typeChars("cps");c.waitTicks(3);
        c.runOnClient(mc -> {
            check(search(mc)==original && search(mc).isFocused() && search(mc).getCursorPosition()==3,"Live search lost focus or caret");
            check(modules(mc).equals(List.of("Left CPS","Right CPS")),"Search results incorrect");
        });
        c.takeScreenshot("search");
        c.runOnClient(mc -> search(mc).setValue("not-a-module"));
        c.runOnClient(mc -> check(modules(mc).isEmpty(),"Unmatched search did not clear modules"));
        c.runOnClient(mc -> search(mc).setValue(""));
        click(c,"Stats");
        c.runOnClient(mc -> check(modules(mc).equals(List.of("FPS","Speed","Clock","Memory")),"Stats category contains CPS or lacks combo"));
        click(c,"Camera");
        c.runOnClient(mc -> check(modules(mc).equals(List.of("Zoom","Freelook","Freecam")),"Client tweaks missing"));
        c.takeScreenshot("client-tweaks");click(c,"Zoom");
        c.waitForScreen(TweakSettingsScreen.class);c.takeScreenshot("zoom-settings");
        click(c,"Show HUD: ON");click(c,"Cinematic Zoom: OFF");
        c.runOnClient(mc -> {
            var loaded=HudConfig.load();
            check(!loaded.zoomShowHud && loaded.zoomCinematic,"Zoom options not saved");
            check(ClientTweaks.zoomKey.getDefaultKey().getValue()==com.mojang.blaze3d.platform.InputConstants.KEY_Z,"Default zoom key is not Z");
        });
        click(c,"Show HUD: OFF");click(c,"Cinematic Zoom: ON");
        c.runOnClient(mc -> {
            String label="Key: "+ClientTweaks.zoomKey.getTranslatedKeyMessage().getString();
            for(var child:List.copyOf(mc.gui.screen().children()))if(child instanceof StudioUi.Action a && a.getMessage().getString().equals(label)){a.onPress(new MouseButtonInfo(1,0));break;}
            mc.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_Z,0,0));
            check(ClientTweaks.zoomKey.matches(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_Z,0,0)),"Key rebinding failed");
            ClientTweaks.zoomKey.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD.getOrCreate(com.mojang.blaze3d.platform.InputConstants.KEY_Z));net.minecraft.client.KeyMapping.resetMapping();mc.options.save();
        });
        click(c,"Back");
        click(c,"Freecam");click(c,"> Controls");click(c,"Activation: Toggle");click(c,"Exit on damage: ON");
        click(c,"Movement");click(c,"Flight: Horizontal");click(c,"Block collision: OFF");c.takeScreenshot("freecam-movement-settings");
        click(c,"Display");click(c,"Show Player: ON");click(c,"Show HUD: ON");click(c,"Submersion Fog: OFF");c.takeScreenshot("freecam-display-settings");
        c.runOnClient(mc -> {
            var loaded=HudConfig.load();
            check(loaded.freecamHold && loaded.freecamLookFlight && loaded.freecamCollision && !loaded.freecamDisableOnDamage
                && !loaded.freecamShowPlayer && !loaded.freecamShowHud && loaded.freecamShowSubmersion,"Freecam options not saved");
        });
        click(c,"Back");click(c,"Visuals");
        c.runOnClient(mc -> check(modules(mc).equals(List.of("No Camera Shake","No Explosions","Full Bright")),"Visual effects category incorrect"));
        click(c,"Full Bright");click(c,"Enabled: OFF");
        c.runOnClient(mc -> check(HudConfig.load().tweaks.get("full_bright").visible,"Full Bright toggle not persisted"));
        click(c,"Enabled: ON");click(c,"Back");
        click(c,"Game HUD");
        c.runOnClient(mc -> check(modules(mc).equals(List.of("Scoreboard","Boss Bars","Status Effects","Player Tab","Combo Meter")),"Vanilla HUD category incorrect"));
        c.runOnClient(mc -> {
            var screen=mc.gui.screen();
            for(var child:screen.children())if(child instanceof ModuleCard card && card.getMessage().getString().equals("Status Effects")) {
                screen.mouseClicked(mouse(card.getX()+10,card.getY()+10,org.lwjgl.sdl.SDLMouse.SDL_BUTTON_RIGHT),false);break;
            }
        });
        click(c,"Time Remaining: OFF");
        c.runOnClient(mc -> check(HudConfig.load().effectsShowTime,"Effect timer option not persisted"));
        click(c,"Keystrokes");click(c,"WASD");c.waitForScreen(HudEditorScreen.class);
        c.runOnClient(mc -> {
            var screen=mc.gui.screen();var config=HudStudioClient.config;
            check(screen.children().size()==2,"Arranger controls changed");
            var fps=config.elements.get("fps");fps.visible=true;HudLayout.move(fps,230,150,screen.width,screen.height);
            float oldX=fps.x,oldY=fps.y,oldScale=fps.scale;
            var b=HudLayout.bounds(fps,screen.width,screen.height);
            check(!screen.mouseClicked(mouse(b.x()+8,b.y()+8,1),false),"Another module can be selected");
            check(!screen.mouseDragged(mouse(b.x()+30,b.y()+30,1),22,22),"Another module can be dragged");
            check(!screen.mouseClicked(mouse(b.x()+b.w(),b.y()+b.h(),1),false),"Another module can be resized");
            check(fps.x==oldX && fps.y==oldY && fps.scale==oldScale,"Locked module changed");
            var e=config.elements.get("wasd");
            check(screen.mouseClicked(mouse(12,12,1),false),"Selected hidden module cannot be edited");
            screen.mouseDragged(mouse(124,84,1),112,72);screen.mouseReleased(mouse(0,0,1));
            b=HudLayout.bounds(e,screen.width,screen.height);
            screen.mouseClicked(mouse(b.x()+b.w(),b.y()+b.h(),1),false);
            screen.mouseDragged(mouse(b.x()+b.w()+32,b.y()+b.h()+21,1),32,21);
            check(Math.abs(e.scale-1.5f)<.01f,"Selected corner resize failed");screen.mouseReleased(mouse(0,0,1));
        });
        c.waitTicks(3);c.takeScreenshot("locked-arranger");click(c,"Reset");
        c.runOnClient(mc -> {
            check(HudStudioClient.config.elements.get("wasd").scale==1 && !HudStudioClient.config.elements.get("wasd").moved,"Reset affected wrong module");
            check(HudStudioClient.config.elements.get("fps").moved,"Reset changed an unselected module");
        });
        click(c,"Back");click(c,"Appearance");c.waitForScreen(AppearanceScreen.class);
        c.runOnClient(mc -> {
            var screen=mc.gui.screen();int left=(screen.width-Math.min(360,screen.width-24))/2;
            screen.mouseClicked(mouse(left+76,193,1),false);screen.mouseReleased(mouse(0,0,1));
            check(HudStudioClient.config.themeColor==0xff669cdd,"Colour preset failed");
            for(var child:screen.children())if(child instanceof AbstractSliderButton slider){
                slider.onClick(mouse(slider.getX()+slider.getWidth()/2.,slider.getY()+18,1),false);slider.onRelease(mouse(0,0,1));break;
            }
            check(Math.abs(HudStudioClient.config.opacity-.5)<.02,"Opacity slider failed");
            screen.mouseClicked(mouse(left+35,50,1),false);screen.mouseDragged(mouse(left+50,50,1),15,0);screen.mouseReleased(mouse(0,0,1));
            screen.mouseClicked(mouse(left+85,95,1),false);screen.mouseDragged(mouse(left+80,100,1),-5,5);screen.mouseReleased(mouse(0,0,1));
            check(HudStudioClient.config.themeColor!=0xff669cdd,"HSV colour selection failed");
            var loaded=HudConfig.load();check(loaded.themeColor==HudStudioClient.config.themeColor && Math.abs(loaded.opacity-.5)<.02,"Global appearance was not saved");
        });
        c.waitTicks(3);c.takeScreenshot("appearance");click(c,"Reset Appearance");
        c.runOnClient(mc -> check(HudWidgets.vanillaOffhand(),"Appearance reset did not restore vanilla offhand"));
        click(c,"Back");c.runOnClient(mc -> mc.gui.screen().onClose());
        try(var world=c.worldBuilder().create()){
            c.waitFor(mc -> mc.player!=null && mc.gui.screen()==null);
            c.runOnClient(mc -> {
                HudStudioClient.config=HudConfig.defaults();
                mc.player.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
                mc.player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.DIAMOND_HELMET));
            });
            c.waitTicks(5);c.takeScreenshot("vanilla-defaults");
            c.runOnClient(mc -> mc.gui.setScreen(ModMenu.getConfigScreen("hud-studio",null)));
            click(c,"Inventory");click(c,"Add Slot");click(c,"Helmet");click(c,"Add Display");
            c.waitForScreen(HudEditorScreen.class);
            c.runOnClient(mc -> {
                var e=HudStudioClient.config.slots.getFirst();var screen=mc.gui.screen();
                check(HudLayout.base(e.id,screen.width,screen.height).w()==22 && HudLayout.base(e.id,screen.width,screen.height).h()==22,"Hotbar frame bounds incorrect");
                HudLayout.move(e,120,180,screen.width,screen.height);HudStudioClient.config.save();
                check(!screen.mouseClicked(mouse(screen.width/2f-110,screen.height-10,1),false),"Offhand is interactive while editing custom slot");
            });
            c.waitTicks(3);c.takeScreenshot("framed-slot");
            c.runOnClient(mc -> {
                var e=HudStudioClient.config.slots.getFirst();var screen=mc.gui.screen();
                HudLayout.move(e,100,60,screen.width,screen.height);
                screen.mouseClicked(new MouseButtonEvent(108,68,new MouseButtonInfo(1,1)),false);
                screen.mouseDragged(mouse(111.25,70.5,1),3.25,2.5);screen.mouseReleased(mouse(0,0,1));
                var b=HudLayout.bounds(e,screen.width,screen.height);
                check(Math.abs(b.x()-103.25)<.001 && Math.abs(b.y()-62.5)<.001,"Shift did not disable move snapping");
                HudLayout.move(e,100,60,screen.width,screen.height);
                screen.mouseClicked(mouse(108,68,1),false);screen.mouseDragged(mouse(111.25,70.5,1),3.25,2.5);screen.mouseReleased(mouse(0,0,1));
                b=HudLayout.bounds(e,screen.width,screen.height);
                check(Math.abs(b.x()-104)<.001 && Math.abs(b.y()-62)<.001,"Two-pixel grid snapping failed");
                e.scale=1;HudLayout.move(e,100,60,screen.width,screen.height);
                screen.mouseClicked(new MouseButtonEvent(122,82,new MouseButtonInfo(1,1)),false);
                screen.mouseDragged(mouse(123.606,83.606,1),1.606,1.606);
                check(Math.abs(e.scale-1.05)<.001,"Shift resize did not snap to five percent");
                screen.keyReleased(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT,0,0));
                screen.mouseDragged(mouse(123.606,83.606,1),0,0);screen.mouseReleased(mouse(0,0,1));
                check(Math.abs(e.scale-1.073)<.001,"Releasing Shift did not restore continuous resize");
                e.scale=1.37f;
            });
            c.waitTicks(3);c.takeScreenshot("fractional-slot-outline");
            click(c,"Back");
            c.runOnClient(mc -> mc.gui.setScreen(new SlotPickerScreen(mc.gui.screen(),HudStudioClient.config.slots.getFirst())));
            c.runOnClient(mc -> {
                var screen=mc.gui.screen();boolean found=false;
                for(var child:screen.children())if(child instanceof AbstractSliderButton slider){
                    slider.onClick(mouse(slider.getX()+4+(slider.getWidth()-8)*.4,slider.getY()+18,1),false);slider.onRelease(mouse(0,0,1));found=true;break;
                }
                check(found,"Individual slot opacity slider missing");
                check(HudStudioClient.config.slots.getFirst().opacity==1,"Slot draft changed saved config prematurely");
            });
            c.waitTicks(3);c.takeScreenshot("slot-picker");click(c,"Use This Slot");
            c.runOnClient(mc -> {
                var e=HudStudioClient.config.slots.getFirst();
                check(Math.abs(e.opacity-.4)<.01,"Slot opacity was not applied");
                check(Math.abs(HudConfig.load().slots.getFirst().opacity-.4)<.01,"Slot opacity was not persisted");
                e.opacity=1;e.scale=1;e.slot=9;HudLayout.move(e,10,60,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
                for(int i=1;i<4;i++){
                    var slot=new HudConfig.Element("slot_test_"+i);slot.slot=9+i;
                    HudLayout.move(slot,10+30*i,60,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());HudStudioClient.config.slots.add(slot);
                }
                mc.gui.setScreen(null);
            });
            c.waitTicks(3);var plain=c.takeScreenshot("normal-hotbar-slots");
            c.runOnClient(mc -> {HudStudioClient.config.themeColor=0xffff0000;HudStudioClient.config.opacity=.05f;});
            c.waitTicks(3);var themed=c.takeScreenshot("theme-independent-slots");
            compareSlotBorders(c,plain,themed);
            c.runOnClient(mc -> check(HudWidgets.vanillaOffhand(),"Global appearance affected native offhand"));
            c.runOnClient(mc -> mc.gui.setScreen(new HudEditorScreen(new HudSettingsScreen(null),HudStudioClient.config.slots.getFirst())));
            c.waitTicks(3);var arranged=c.takeScreenshot("unclipped-slot-borders");
            compareSlotBorders(c,themed,arranged);
            c.runOnClient(mc -> {
                var config=HudStudioClient.config;int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
                config.themeColor=0xff101010;config.opacity=.85f;
                for(int i=0;i<4;i++)HudLayout.move(config.slots.get(i),10+i*20,60,w,h);
                check(!HudLayout.slotNeighbours(config.slots.get(0),w,h,false).left(),"Left cap missing");
                check(HudLayout.slotNeighbours(config.slots.get(0),w,h,false).right(),"First slot did not join");
                check(HudLayout.slotNeighbours(config.slots.get(1),w,h,false).left() && HudLayout.slotNeighbours(config.slots.get(1),w,h,false).right(),"Middle slot disconnected");
                check(!HudLayout.slotNeighbours(config.slots.get(3),w,h,false).right(),"Right cap missing");
                var extra=new HudConfig.Element("slot_extra");config.slots.add(extra);
                for(float scale:new float[]{1,1.37f,2}) {
                    extra.scale=scale;config.slots.get(3).scale=scale;
                    HudLayout.move(config.slots.get(3),100,100,w,h);
                    var snapped=HudLayout.snap(extra,100+20*scale+2,101,w,h);
                    check(Math.abs(snapped.x()-(100+20*scale))<.001 && Math.abs(snapped.y()-100)<.001,"Scaled slot join failed");
                }
                config.slots.get(3).scale=1;extra.scale=1;
                for(int i=0;i<4;i++)HudLayout.move(config.slots.get(i),10+i*20,60,w,h);
                HudLayout.move(extra,130,60,w,h);
                config.slots.get(0).slot=39;config.slots.get(3).slot=36;
                mc.player.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));
                mc.gui.setScreen(null);
            });
            c.waitTicks(3);var joined=c.takeScreenshot("mini-hotbar-and-single-slot");
            checkEndBorders(c,joined,10,60,82);checkEndBorders(c,joined,130,60,22);
            c.runOnClient(mc -> {
                int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
                HudLayout.move(HudStudioClient.config.slots.get(1),200,90,w,h);
                check(!HudLayout.slotNeighbours(HudStudioClient.config.slots.get(0),w,h,false).right(),"Removing neighbour did not close the frame");
                check(!HudLayout.slotNeighbours(HudStudioClient.config.slots.get(2),w,h,false).left(),"Split hotbar did not get a left cap");
                var offhand=HudStudioClient.config.elements.get("offhand");HudLayout.move(offhand,270,60,w,h);
            });
            c.waitTicks(3);var split=c.takeScreenshot("split-mini-hotbar");
            checkEndBorders(c,split,10,60,22);checkEndBorders(c,split,50,60,42);
            c.runOnClient(mc -> {HudStudioClient.config.themeColor=0xffff00ff;HudStudioClient.config.opacity=0;});
            c.waitTicks(3);var offhandThemed=c.takeScreenshot("independent-offhand");
            compareRegion(c,split,offhandThemed,270,60,22,22);
            c.runOnClient(mc -> mc.gui.setScreen(new HudSettingsScreen(null)));
            click(c,"Appearance");
            c.runOnClient(mc -> {HudStudioClient.config.themeColor=0xff669cdd;HudStudioClient.config.opacity=.6f;HudStudioClient.config.save();});
            click(c,"Back");
            c.runOnClient(mc -> {
                for(String id:new String[]{"wasd","left_cps","right_cps","fps"})HudStudioClient.config.elements.get(id).visible=true;
                mc.gui.setScreen(null);
            });
            c.waitTicks(3);c.takeScreenshot("themed-hud");
            c.runOnClient(mc -> {
                var config=HudConfig.defaults();HudStudioClient.config=config;
                for(var e:config.elements.values())e.visible=false;
                var wasd=config.elements.get("wasd");var fps=config.elements.get("fps");wasd.visible=true;fps.visible=true;
                int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
                for(float scale:new float[]{1,1.37f,2}) {
                    wasd.scale=scale;fps.scale=scale;
                    HudLayout.move(wasd,8,8,w,h);
                    float targetY=8+22*scale,targetX=8+64*scale+6;
                    var snapped=HudLayout.snap(fps,targetX,targetY-2,w,h);
                    check(Math.abs(snapped.y()-targetY)<.001,"Stat did not align to scaled ASD row");
                    HudLayout.move(fps,targetX,targetY,w,h);
                    snapped=HudLayout.snap(wasd,8,10,w,h);
                    check(Math.abs(snapped.y()-8)<.001,"Moving WASD did not align its ASD row to stat");
                }
                wasd.scale=1;fps.scale=1;HudLayout.move(wasd,8,8,w,h);HudLayout.move(fps,76,28,w,h);
                var mouse=config.elements.get("mouse");mouse.visible=true;HudLayout.move(mouse,8,52,w,h);
                var cps=config.elements.get("left_cps");cps.visible=true;HudLayout.move(cps,76,52,w,h);
                mc.gui.setScreen(new HudEditorScreen(null,fps));
                mc.gui.screen().mouseClicked(mouse(84,36,1),false);
                mc.gui.screen().mouseDragged(mouse(84,36,1),0,0);mc.gui.screen().mouseReleased(mouse(0,0,1));
                check(Math.abs(HudLayout.bounds(fps,w,h).y()-30)<.001,"Editor did not snap FPS to ASD row");
                mc.gui.setScreen(null);
            });
            c.waitTicks(3);c.takeScreenshot("aligned-wasd-and-stats");
            testNewFeatures(c,world.getServer());
            NavigationSmokeTest.run(c,world.getServer());
            c.runOnClient(mc -> {
                HudStudioClient.config=HudConfig.defaults();HudStudioClient.config.save();
                try{java.nio.file.Files.writeString(java.nio.file.Path.of("hud-result.txt"),"PASS: HUD, inventory, camera, Full Bright rendered OFF/ON/restoration, effects and combo regressions; N/U/M waypoint creation/list/map, defaults and rounded coordinates, editor validation/save/cancel, vanilla palette, rendered hide/show, confirmed delete, search/pagination, persistence and world/dimension isolation, corrupt-file preservation, terrain cache reload, map markers/dimensions, anchored zoom and panning, Studio navigation buttons");}
                catch(java.io.IOException e){throw new RuntimeException(e);}
            });
        }
    }
    private static void testNewFeatures(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext serverContext) {
        c.runOnClient(mc -> {
            var config=HudConfig.defaults();HudStudioClient.config=config;
            check(!config.effectsShowTime && !ClientTweaks.enabled("full_bright"),"1.8 features enabled by default");
            check(EffectTimers.format(-1).equals("\u221e") && EffectTimers.format(0).equals("0:00")
                && EffectTimers.format(1).equals("0:01") && EffectTimers.format(1200).equals("1:00")
                && EffectTimers.format(72000).equals("60:00"),"Effect duration formatting incorrect");
            var extractor=new net.minecraft.client.renderer.LightmapRenderStateExtractor(mc.gameRenderer,mc);
            var state=new net.minecraft.client.renderer.state.LightmapRenderState();
            double gamma=mc.options.gamma().get();
            extractor.tick();extractor.extract(state,0);
            float normal=state.brightness;
            var normalAmbient=new org.joml.Vector3f(state.ambientColor);
            config.tweaks.get("full_bright").visible=true;
            extractor.tick();extractor.extract(state,0);
            check(state.ambientColor.equals(new org.joml.Vector3f(1,1,1)),"Full Bright did not supply full ambient light");
            check(state.darknessEffectScale==0 && state.bossOverlayWorldDarkening==0,"Full Bright retained lightmap darkening");
            check(mc.options.gamma().get()==gamma,"Full Bright changed saved brightness");
            config.tweaks.get("full_bright").visible=false;
            extractor.tick();extractor.extract(state,0);
            check(Math.abs(state.brightness-normal)<.001,"Full Bright did not restore lighting");
            check(state.ambientColor.equals(normalAmbient),"Full Bright did not restore ambient lighting");
        });
        testDarkRoom(c,serverContext);
        serverContext.runCommand("effect give @p minecraft:speed 150 0 false");
        serverContext.runCommand("effect give @p minecraft:weakness 45 0 false");
        serverContext.runCommand("effect give @p minecraft:night_vision infinite 0 false");
        c.waitTicks(4);
        c.runOnClient(mc -> {
            check(mc.player.getActiveEffects().stream().filter(e->e.showIcon()).count()==3,"Missing visible timer test effects");
            HudStudioClient.config.effectsShowTime=true;mc.gui.setScreen(null);
        });
        c.waitTicks(3);
        c.takeScreenshot("effects-time-remaining");
        c.runOnClient(mc -> {
            var effects=HudStudioClient.config.elements.get("effects");effects.scale=2;
            HudLayout.move(effects,130,55,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
        });
        c.waitTicks(3);
        c.takeScreenshot("effects-time-remaining-scaled");
        serverContext.runCommand("effect clear @p");
        c.runOnClient(mc -> {
            var config=HudConfig.defaults();HudStudioClient.config=config;
            for(int i=0;i<4;i++) {
                var slot=new HudConfig.Element("slot_vertical_"+i);slot.slot=i==0?39:i==3?36:9+i;
                HudLayout.move(slot,20,20+i*20,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());config.slots.add(slot);
            }
            var neighbours=HudLayout.slotNeighbours(config.slots.get(1),427,240,false);
            check(neighbours.up() && neighbours.down() && !neighbours.left() && !neighbours.right(),"Vertical neighbours incorrect");
            var extra=new HudConfig.Element("slot_candidate");
            var snap=HudLayout.snap(extra,21,101,427,240);
            check(Math.abs(snap.x()-20)<.001 && Math.abs(snap.y()-100)<.001,"Vertical end did not snap");
            config.tweaks.values().forEach(e->check(!e.visible,"Client tweak enabled by default"));
        });
        c.waitTicks(3);c.takeScreenshot("vertical-hotbar");
        c.runOnClient(mc -> mc.gui.setScreen(new HudSettingsScreen(null)));
        c.waitTicks(3);
        c.runOnClient(mc -> check(mc.isPaused(),"Module menu did not pause single player"));
        c.runOnClient(mc -> mc.gui.screen().onClose());c.waitTicks(3);
        c.runOnClient(mc -> check(!mc.isPaused(),"Closing module menu did not resume game"));
        c.runOnClient(mc -> {
            HudStudioClient.config.tweaks.get("no_explosions").visible=true;
            check(mc.particleEngine.createParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION,0,0,0,0,0,0)==null,"Explosion particle not suppressed");
            try {
                var camera=new net.minecraft.client.renderer.state.level.CameraRenderState();
                camera.entityRenderState.isLiving=true;camera.entityRenderState.hurtTime=5;camera.entityRenderState.hurtDuration=10;camera.entityRenderState.hurtDir=35;
                camera.entityRenderState.isPlayer=true;camera.entityRenderState.bob=.8f;camera.entityRenderState.backwardsInterpolatedWalkDistance=.5f;
                HudStudioClient.config.tweaks.get("no_shake").visible=true;
                for(String name:new String[]{"bobHurt","bobView"}) {
                    var method=net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethod(name,camera.getClass(),com.mojang.blaze3d.vertex.PoseStack.class);method.setAccessible(true);
                    var pose=new com.mojang.blaze3d.vertex.PoseStack();var beforePose=new org.joml.Matrix4f(pose.last().pose());
                    method.invoke(mc.gameRenderer,camera,pose);
                    check(beforePose.equals(pose.last().pose()),"Camera shake was not disabled: "+name);
                }
                HudStudioClient.config.tweaks.get("no_shake").visible=false;
            }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
            HudStudioClient.config.tweaks.get("freecam").visible=true;
            ClientTweaks.toggleFreecam(mc);
            check(ClientTweaks.freecam(),"Freecam did not enable");
            check(mc.getCameraEntity()!=mc.player,"Freecam did not detach rendering while retaining player simulation");
            int cameraId=mc.getCameraEntity().getId();
            check(cameraId<0 && mc.level.getEntity(cameraId)==null,"Freecam camera needs a private client ID");
            // Essential asks the player renderer to extract the camera's equipment.
            mc.getEntityRenderDispatcher().getRenderer(mc.getCameraEntity()).createRenderState(mc.getCameraEntity(),0);
            ClientTweaks.turn(0,500);
            mc.options.keyUp.setDown(true);
        });
        var before=c.computeOnClient(mc->new double[]{mc.player.getX(),mc.player.getZ(),mc.player.getYRot(),mc.player.getXRot()});
        var cameraBefore=c.computeOnClient(mc->ClientTweaks.position(1));
        c.waitTicks(5);
        c.runOnClient(mc -> {
            mc.options.keyUp.setDown(false);
            check(ClientTweaks.position(1).distanceTo(cameraBefore)>1,"Freecam camera did not move");
            check(Math.abs(ClientTweaks.position(1).y-cameraBefore.y)<.001,"Looking down changed freecam flight height");
            check(Math.abs(mc.player.getX()-before[0])<.01 && Math.abs(mc.player.getZ()-before[1])<.01,"Freecam moved player body");
            ClientTweaks.turn(100,40);
            check(mc.player.getYRot()==before[2] && mc.player.getXRot()==before[3],"Detached camera changed player aim");
        });
        c.waitTicks(3);c.takeScreenshot("freecam");
        c.runOnClient(mc -> {
            ClientTweaks.turn(-ClientTweaks.yaw()/.15,-ClientTweaks.pitch()/.15);
            mc.options.keyRight.setDown(true);
        });
        var strafeStart=c.computeOnClient(mc->ClientTweaks.position(1));c.waitTicks(3);
        c.runOnClient(mc -> {
            mc.options.keyRight.setDown(false);
            check(ClientTweaks.position(1).x<strafeStart.x-1,"D moves left on screen at yaw zero");
            mc.options.keyLeft.setDown(true);
        });
        var rightEnd=c.computeOnClient(mc->ClientTweaks.position(1));c.waitTicks(3);
        c.runOnClient(mc -> {
            mc.options.keyLeft.setDown(false);
            check(ClientTweaks.position(1).x>rightEnd.x+1,"A moves right on screen at yaw zero");
            ClientTweaks.stop(mc);ClientTweaks.toggleFreecam(mc);
            ClientTweaks.turn(0,-ClientTweaks.pitch()/.15);
            mc.options.keyDown.setDown(true);
        });c.waitTicks(7);
        c.runOnClient(mc -> mc.options.keyDown.setDown(false));c.waitTicks(3);
        c.takeScreenshot("freecam-visible-body");
        c.runOnClient(mc -> {
            check(playerRendered(mc),"Local player body missing from freecam render state");
        });
        c.runOnClient(mc -> HudStudioClient.config.freecamShowPlayer=false);c.waitTicks(3);
        c.runOnClient(mc -> check(!playerRendered(mc),"Show player OFF did not hide player"));
        c.runOnClient(mc -> {
            HudStudioClient.config.freecamShowPlayer=true;
            HudStudioClient.config.freecamShowHud=false;
            check(ClientTweaks.hideHud(),"Freecam HUD toggle ignored");
        });c.waitTicks(3);c.takeScreenshot("freecam-hidden-hud");
        c.runOnClient(mc -> HudStudioClient.config.freecamShowHud=true);
        c.runOnClient(mc -> {ClientTweaks.stop(mc);check(mc.options.getCameraType()==net.minecraft.client.CameraType.FIRST_PERSON && mc.getCameraEntity()==mc.player,"Freecam view not restored");
            ClientTweaks.toggleFreecam(mc);
        });
        serverContext.runCommand("damage @p 1 minecraft:generic");
        c.waitTicks(5);
        c.runOnClient(mc -> {
            check(!ClientTweaks.freecam() && mc.getCameraEntity()==mc.player,"Damage did not return freecam to player");
            HudStudioClient.config.freecamHold=true;ClientTweaks.freecamKey.setDown(true);
        });c.waitTicks(2);
        c.runOnClient(mc -> {check(ClientTweaks.freecam(),"Hold freecam did not start");ClientTweaks.freecamKey.setDown(false);});c.waitTicks(2);
        c.runOnClient(mc -> {
            check(!ClientTweaks.freecam(),"Hold freecam did not stop on release");HudStudioClient.config.freecamHold=false;
            HudStudioClient.config.tweaks.get("freelook").visible=true;ClientTweaks.freelookKey.setDown(true);
        });
        c.waitTicks(2);
        c.runOnClient(mc -> {check(ClientTweaks.freelook(),"Freelook did not start");ClientTweaks.turn(160,30);});
        c.waitTicks(2);c.takeScreenshot("freelook");
        c.runOnClient(mc -> {ClientTweaks.freelookKey.setDown(false);});c.waitTicks(2);
        c.runOnClient(mc -> {
            check(!ClientTweaks.detached() && mc.options.getCameraType()==net.minecraft.client.CameraType.FIRST_PERSON,"Freelook release did not restore view");
            check(mc.player.getYRot()==before[2] && mc.player.getXRot()==before[3],"Freelook changed player orientation");
            HudStudioClient.config.tweaks.get("zoom").visible=true;ClientTweaks.zoomKey.setDown(true);
        });
        c.waitTicks(3);
        c.runOnClient(mc -> check(mc.gameRenderer.mainCamera().getFov()<30,"Zoom did not narrow camera FOV"));
        c.takeScreenshot("zoom");
        c.runOnClient(mc -> {
            float start=ClientTweaks.zoomFactor();
            HudStudioClient.config.zoomShowHud=false;HudStudioClient.config.zoomCinematic=true;
            check(ClientTweaks.hideHud() && ClientTweaks.cinematicZoom(),"Zoom HUD/cinematic options ignored");
            check(!mc.options.smoothCamera,"Cinematic zoom changed saved camera option");
            int selected=mc.player.getInventory().getSelectedSlot();
            mc.mouseHandler.onScroll(mc.getWindow().handle(),0,1);
            check(Math.abs(ClientTweaks.zoomFactor()-start*1.1f)<.001,"Zoom scroll did not increase magnification by 10 percent");
            check(mc.player.getInventory().getSelectedSlot()==selected,"Zoom scroll changed hotbar selection");
            for(int i=0;i<100;i++)mc.mouseHandler.onScroll(mc.getWindow().handle(),0,1);
            check(ClientTweaks.zoomFactor()==50,"Zoom maximum incorrect");
            for(int i=0;i<100;i++)mc.mouseHandler.onScroll(mc.getWindow().handle(),0,-1);
            check(ClientTweaks.zoomFactor()==1,"Zoom minimum incorrect");
            ClientTweaks.zoomKey.setDown(false);check(ClientTweaks.zoomFactor()==1,"Zoom remained active on release");
            check(!ClientTweaks.hideHud() && !ClientTweaks.cinematicZoom(),"Zoom display options remained active on release");
            HudStudioClient.config.zoomShowHud=true;HudStudioClient.config.zoomCinematic=false;
            ClientTweaks.zoomKey.setDown(true);
            check(ClientTweaks.zoomFactor()==HudStudioClient.config.zoomFactor,"Zoom did not reset on next hold");
        });
        c.runOnClient(mc -> {
            ClientTweaks.zoomKey.setDown(false);
            HudStudioClient.config.elements.get("combo").visible=true;
        });
        serverContext.runCommand("execute at @p run summon minecraft:cow ~2 ~ ~ {NoAI:1b,PersistenceRequired:1b}");
        serverContext.runCommand("item replace entity @p weapon.mainhand with minecraft:diamond_sword");
        c.waitTicks(5);
        for(int i=0;i<2;i++) {
            c.runOnClient(mc -> {
                net.minecraft.world.entity.Entity cow=null;
                for(var e:mc.level.entitiesForRendering())if(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("cow"))cow=e;
                check(cow!=null,"Test cow not spawned");mc.gameMode.attack(mc.player,cow);
            });
            c.waitTicks(22);
        }
        c.runOnClient(mc -> check(ComboMeter.state.hits==2 && ComboMeter.state.rank==1,"Two confirmed melee hits did not start Good: "+ComboMeter.state.hits));
        c.runOnClient(mc -> check(ComboMeter.state.damage>2 && ComboMeter.state.lastGain>1.5,"Sword hits still display fist damage"));
        c.takeScreenshot("confirmed-combo");
        c.runOnClient(mc -> {
            var measured=new ComboState();
            measured.hitDamage(8,false,0);measured.hitDamage(11,true,1);
            check(measured.damage==19 && measured.lastGain==11 && measured.lastMultiplier>1,"Measured critical damage was multiplied twice");
            var state=new ComboState();
            check(!state.hit(7,1,0) && !state.active() && state.hits==1 && state.damage==7,"First hit should only prime the combo");
            check(state.hit(7,1.5,1) && state.rank==1 && state.damage==17.5 && state.lastGain==10.5,"Crit damage or per-hit progression incorrect");
            check(state.duration()==80,"Good timer was not halved");
            int previous=state.duration();
            for(int rank=2;rank<=8;rank++) {
                check(state.hit(100,1,rank) && state.rank==rank,"Style rank missing");
                check(state.duration()<previous,"Higher combo rank did not shorten timer");previous=state.duration();
            }
            check(state.duration()==38,"Legendary timer was not halved");
            check(!state.hit(2,1,9),"Legendary should remain the maximum tier");
            check(state.rank==8 && state.hits==10 && state.damage==719.5,"Legendary did not keep accumulating hits and damage");
            check(state.expire(state.deadline) && !state.active(),"Combo did not time out");
            check(state.brokenHits==10 && state.brokenDamage==719.5,"End summary lost totals");
            state.hit(7,1,500);state.hit(7,1,501);check(state.breakCombo(502) && !state.active(),"Damage interruption failed");
            for(int rank=1;rank<=8;rank++)check(mc.getResourceManager().getResource(net.minecraft.resources.Identifier.fromNamespaceAndPath("hud-studio","sounds/combo_"+rank+".ogg")).isPresent(),"Combo sound missing");
            ComboMeter.state.clear();ComboMeter.state.hit(7,1,ComboMeter.ticks());ComboMeter.state.hit(7,1.5,ComboMeter.ticks());
            for(int rank=2;rank<=8;rank++)ComboMeter.state.hit(100,1.5,ComboMeter.ticks());
        });
        c.waitTicks(2);c.takeScreenshot("legendary-combo");
        serverContext.runCommand("damage @p 1 minecraft:generic");
        c.waitTicks(4);
        c.runOnClient(mc -> check(!ComboMeter.state.active(),"Incoming server damage did not interrupt combo"));
        c.takeScreenshot("broken-combo");
        testDamageKinds(c,serverContext);
    }
    private static void testDarkRoom(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
        var origin=c.computeOnClient(mc->mc.player.position());
        server.runCommand("fill -4 98 -4 4 105 4 minecraft:stone hollow");
        server.runCommand("tp @p 0 99 0 0 0");
        c.waitTicks(40);
        var dark=c.takeScreenshot("fullbright-dark-room-off");
        c.runOnClient(mc->HudStudioClient.config.tweaks.get("full_bright").visible=true);
        c.waitTicks(5);
        var bright=c.takeScreenshot("fullbright-dark-room-on");
        c.runOnClient(mc->HudStudioClient.config.tweaks.get("full_bright").visible=false);
        c.waitTicks(5);
        var restored=c.takeScreenshot("fullbright-dark-room-restored");
        double off=roomBrightness(dark),on=roomBrightness(bright),after=roomBrightness(restored);
        check(on>off+40,"Full Bright did not brighten rendered darkness: "+off+" -> "+on);
        check(Math.abs(after-off)<5,"Disabling Full Bright did not restore rendered darkness: "+off+" -> "+after);
        server.runCommand("tp @p "+origin.x+" "+origin.y+" "+origin.z);
        server.runCommand("fill -4 98 -4 4 105 4 minecraft:air");
        c.waitTicks(10);
    }
    private static double roomBrightness(java.nio.file.Path path) {
        try {
            var image=javax.imageio.ImageIO.read(path.toFile());double sum=0;int count=0;
            // Sample the wall above the crosshair, clear of the hand and HUD.
            for(int y=image.getHeight()/3;y<image.getHeight()*5/12;y++)
                for(int x=image.getWidth()/3;x<image.getWidth()*2/3;x++) {
                    int rgb=image.getRGB(x,y);sum+=((rgb>>16&255)+(rgb>>8&255)+(rgb&255))/3.0;count++;
                }
            return sum/count;
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static void testDamageKinds(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
        String[][] projectiles={{"arrow","arrow"},{"spectral_arrow","arrow"},{"firework_rocket","fireworks"},
            {"trident","trident"},{"snowball","thrown"},{"ender_pearl","thrown"}};
        for(var projectile:projectiles) {
            c.runOnClient(mc->ComboMeter.state.clear());
            server.runCommand("kill @e[tag=combo_target]");
            server.runCommand("kill @e[tag=combo_projectile]");
            server.runCommand("execute at @p run summon minecraft:cow ~2 ~ ~ {NoAI:1b,Tags:[\"combo_target\"]}");
            server.runCommand("execute at @p run summon minecraft:"+projectile[0]+" ~ ~8 ~ {NoGravity:1b,LifeTime:1000,Tags:[\"combo_projectile\"]}");
            c.waitTicks(4);
            for(int i=0;i<2;i++) {
                server.runCommand("damage @e[tag=combo_target,limit=1] 4 minecraft:"+projectile[1]+" by @e[tag=combo_projectile,limit=1] from @p");
                c.waitTicks(12);
            }
            c.runOnClient(mc->{
                var state=ComboMeter.state;
                if(projectile[0].equals("ender_pearl"))check(state.hits==0,"Ender pearl was credited");
                else check(state.hits==2 && state.damage==8 && state.feedback==ComboState.Feedback.PROJECTILE,
                    "Projectile damage/feedback incorrect for "+projectile[0]+": "+state.hits+" hits, "+state.damage+" damage");
            });
        }
        server.runCommand("kill @e[tag=combo_projectile]");
        server.runCommand("kill @e[tag=combo_target]");
        server.runCommand("execute at @p run summon minecraft:cow ~2 ~ ~ {NoAI:1b,Tags:[\"combo_target\"]}");
        server.runCommand("item replace entity @p weapon.mainhand with minecraft:diamond_spear");
        c.waitTicks(4);
        c.runOnClient(mc->{ComboMeter.state.clear();mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);});
        c.waitTicks(3);
        server.runCommand("damage @e[tag=combo_target,limit=1] 3 minecraft:spear by @p");
        c.waitTicks(12);
        c.runOnClient(mc->{check(ComboMeter.state.feedback==ComboState.Feedback.SPEARED && ComboMeter.state.lastGain==3,"Held spear feedback incorrect: "+ComboMeter.state.feedback+", damage "+ComboMeter.state.lastGain);mc.options.keyUse.setDown(false);mc.player.stopUsingItem();ComboMeter.piercingAttack();});
        server.runCommand("damage @e[tag=combo_target,limit=1] 3 minecraft:spear by @p");
        c.waitTicks(12);
        c.runOnClient(mc->check(ComboMeter.state.feedback==ComboState.Feedback.DAMAGE && ComboMeter.state.hits==2,"Left-click spear should display Damage"));
        server.runCommand("damage @e[tag=combo_target,limit=1] 2 minecraft:mace_smash by @p");
        c.waitTicks(3);
        c.runOnClient(mc->check(ComboMeter.state.feedback==ComboState.Feedback.CRUSHED && ComboMeter.state.lastGain==2,"Mace smash feedback incorrect"));
        server.runCommand("kill @e[tag=combo_target]");
    }
    private static void compareSlotBorders(ClientGameTestContext c,java.nio.file.Path before,java.nio.file.Path after) {
        try {
            var a=javax.imageio.ImageIO.read(before.toFile());var b=javax.imageio.ImageIO.read(after.toFile());
            int guiWidth=c.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());float scale=a.getWidth()/(float)guiWidth;
            for(int slot=0;slot<4;slot++)for(int x=2;x<18;x++)for(int y:new int[]{0,1,20,21}){
                int px=(int)((10+slot*30+x+.5f)*scale),py=(int)((60+y+.5f)*scale);
                check(a.getRGB(px,py)==b.getRGB(px,py),"Slot border changed at "+slot+":"+x+","+y);
            }
        } catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static void checkEndBorders(ClientGameTestContext c,java.nio.file.Path path,int x,int y,int width) {
        try {
            var image=javax.imageio.ImageIO.read(path.toFile());
            float scale=image.getWidth()/(float)c.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
            for(int yy=0;yy<22;yy++)for(int xx:new int[]{0,width-1})
                check((image.getRGB((int)((x+xx+.5f)*scale),(int)((y+yy+.5f)*scale))&0xffffff)==0,"Missing black end border at "+xx+","+yy);
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static void compareRegion(ClientGameTestContext c,java.nio.file.Path first,java.nio.file.Path second,int x,int y,int w,int h) {
        try {
            var a=javax.imageio.ImageIO.read(first.toFile());var b=javax.imageio.ImageIO.read(second.toFile());
            var texture=c.computeOnClient(mc -> {
                var id=net.minecraft.resources.Identifier.withDefaultNamespace("textures/gui/sprites/hud/hotbar_offhand_left.png");
                try(var stream=mc.getResourceManager().getResourceOrThrow(id).open()) {
                    return javax.imageio.ImageIO.read(stream);
                }
            });
            float scale=a.getWidth()/(float)c.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
            for(int yy=0;yy<h;yy++)for(int xx=0;xx<w;xx++) {
                // Transparent corners show the world, whose sky can change between frames.
                if((texture.getRGB(xx,yy+1)>>>24)!=255) continue;
                int px=(int)((x+xx+.5f)*scale),py=(int)((y+yy+.5f)*scale);
                check(a.getRGB(px,py)==b.getRGB(px,py),"Offhand changed under global appearance at "+xx+","+yy);
            }
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
}

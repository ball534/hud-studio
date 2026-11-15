package dev.hudstudio.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import dev.hudstudio.client.ui.AbstractButton;
import dev.hudstudio.client.ui.GuiEventListener;
import dev.hudstudio.client.ui.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Isolated real-client checks, excluded from ordinary production builds. */
@Mod(modid="hud-studio-test",name="HUD Studio Test",version="1",clientSideOnly=true)
public class FeatureSmokeTest {
    private int stage,ticks;
    @EventHandler public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
    private static void click(StudioScreen screen,String label) {
        for(GuiEventListener child:screen.children())if(child instanceof AbstractButton) {
            AbstractButton button=(AbstractButton)child;
            if(button.getMessage().getString().equals(label)) {button.onPress(new MouseButtonEvent(0,0,0));return;}
        }
        throw new AssertionError("Missing control: "+label);
    }
    private static int[] lightmap(Minecraft mc) throws Exception {
        Field dirty=EntityRenderer.class.getDeclaredField("lightmapUpdateNeeded");dirty.setAccessible(true);dirty.setBoolean(mc.entityRenderer,true);
        Method method=EntityRenderer.class.getDeclaredMethod("updateLightmap",float.class);method.setAccessible(true);
        method.invoke(mc.entityRenderer,0f);
        Field colors=EntityRenderer.class.getDeclaredField("lightmapColors");colors.setAccessible(true);
        return ((int[])colors.get(mc.entityRenderer)).clone();
    }
    private static void screenshot(Minecraft mc,String name) {
        ScreenShotHelper.saveScreenshot(mc.mcDataDir,name+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        try {
            if(stage==0 && mc.currentScreen instanceof net.minecraft.client.gui.GuiMainMenu) {
                Files.deleteIfExists(Paths.get("hud-result.txt"));
                mc.gameSettings.pauseOnLostFocus=false;
                HudStudioClient.config=HudConfig.defaults();
                check(!ClientTweaks.enabled("full_bright") && !HudStudioClient.config.effectsShowTime,"New defaults changed");
                mc.launchIntegratedServer("hud-studio-test","HUD Studio Test",new WorldSettings(1,WorldSettings.GameType.CREATIVE,false,false,WorldType.FLAT));
                stage=1;ticks=0;
            } else if(stage==1 && mc.thePlayer!=null && ++ticks>40) {
                mc.displayGuiScreen(null);
                double gamma=mc.gameSettings.gammaSetting;
                int[] normal=lightmap(mc);
                HudStudioClient.config.tweaks.get("full_bright").visible=true;
                int[] bright=lightmap(mc);
                check((bright[0]&0xffffff)==0xffffff,"Darkest lightmap entry did not reach full brightness");
                check(!Arrays.equals(normal,bright),"Full Bright did not change actual lightmap");
                check(mc.gameSettings.gammaSetting==gamma,"Full Bright changed saved brightness");
                HudStudioClient.config.tweaks.get("full_bright").visible=false;
                check(Arrays.equals(normal,lightmap(mc)),"Full Bright did not restore lighting");
                HudSettingsScreen menu=new HudSettingsScreen(null);mc.displayGuiScreen(menu);
                click(menu,"Visuals");click(menu,"Full Bright");
                click((StudioScreen)mc.currentScreen,"Enabled: OFF");
                check(HudConfig.load().tweaks.get("full_bright").visible,"Full Bright did not persist");
                click((StudioScreen)mc.currentScreen,"Enabled: ON");click((StudioScreen)mc.currentScreen,"Back");
                click(menu,"Game HUD");
                for(GuiEventListener child:menu.children())if(child instanceof ModuleCard) {
                    ModuleCard card=(ModuleCard)child;
                    if(card.getMessage().getString().equals("Status effects")) {
                        menu.mouseClicked(new MouseButtonEvent(card.getX()+10,card.getY()+10,1),false);break;
                    }
                }
                click(menu,"Time remaining: OFF");
                check(HudConfig.load().effectsShowTime,"Effect timer toggle did not persist");
                // Forge's custom effects module is disabled by default to preserve vanilla 1.8.9.
                HudStudioClient.config.elements.get("effects").visible=true;
                mc.getIntegratedServer().addScheduledTask(() -> {
                    for(net.minecraft.entity.player.EntityPlayerMP player:mc.getIntegratedServer().getConfigurationManager().playerEntityList) {
                        player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id,3000));
                        player.addPotionEffect(new PotionEffect(Potion.weakness.id,900));
                    }
                });
                mc.displayGuiScreen(null);stage=2;ticks=0;
            } else if(stage==2 && ++ticks>10) {
                check(mc.thePlayer.getActivePotionEffects().size()==2,"Missing synchronized live effects");
                screenshot(mc,"effects-time-remaining");
                HudConfig.Element effects=HudStudioClient.config.elements.get("effects");effects.scale=2;
                HudLayout.move(effects,100,60,new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth(),new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight());
                stage=3;ticks=0;
            } else if(stage==3 && ++ticks>10) {
                screenshot(mc,"effects-time-remaining-scaled");
                HudStudioClient.config.effectsShowTime=false;stage=4;ticks=0;
            } else if(stage==4 && ++ticks>10) {
                screenshot(mc,"effects-no-time");
                Files.write(Paths.get("hud-result.txt"),"PASS: Full Bright lightmap saturation/restoration, unchanged saved gamma, Visuals controls, persisted Full Bright and effect timers, live effects and scaled HUD".getBytes(StandardCharsets.UTF_8));
                stage=5;mc.shutdown();
            }
        } catch(Throwable error) {
            try {Files.write(Paths.get("hud-result.txt"),("FAIL: "+error).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored) {}
            error.printStackTrace();stage=5;mc.shutdown();
        }
    }
}

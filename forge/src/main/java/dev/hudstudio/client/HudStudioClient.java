package dev.hudstudio.client;

import dev.hudstudio.client.ui.DrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public final class HudStudioClient {
   public static HudConfig config;
   public static KeyBinding editorKey;

   public static void init() {
      config = HudConfig.load();
      editorKey = new KeyBinding("key.hud-studio.editor", 54, "key.categories.hud-studio");
      ClientRegistry.registerKeyBinding(editorKey);
      ClientTweaks.init();
      HudStudioClient events = new HudStudioClient();
      MinecraftForge.EVENT_BUS.register(events);
      FMLCommonHandler.instance().bus().register(events);
   }

   @SubscribeEvent
   public void tick(ClientTickEvent event) {
      Minecraft mc = Minecraft.getMinecraft();
      if (event.phase == Phase.START) {
         ClientTweaks.tick();
      } else {
         while (editorKey.isPressed()) {
            if (mc.currentScreen == null && mc.thePlayer != null) {
               mc.displayGuiScreen(new HudSettingsScreen(null));
            }
         }

         ComboMeter.tick(mc);
      }
   }

   @SubscribeEvent
   public void mouse(MouseEvent event) {
      if (event.buttonstate) {
         HudWidgets.click(event.button);
      }

      if (event.dwheel != 0 && ClientTweaks.scrollZoom(event.dwheel)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void beforeHud(Pre event) {
      if (event.type == ElementType.ALL && ClientTweaks.hideHud()) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void renderHud(Post event) {
      if (event.type == ElementType.ALL && !ClientTweaks.hideHud()) {
         HudWidgets.draw(new DrawContext(), false);
      }
   }

   @SubscribeEvent
   public void renderPlayer(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
      if (ClientTweaks.freecam() && !config.freecamShowPlayer && event.entityPlayer == Minecraft.getMinecraft().thePlayer) {
         event.setCanceled(true);
      }
   }
}

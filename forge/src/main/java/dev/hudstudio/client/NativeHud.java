package dev.hudstudio.client;

import dev.hudstudio.client.ui.DrawContext;
import net.minecraft.client.Minecraft;

public final class NativeHud {
   public static boolean hidden(String id) {
      if (HudStudioClient.config == null) {
         return false;
      }

      HudConfig.Element e = HudStudioClient.config.elements.get(id);
      return e != null && (!e.visible || Minecraft.getMinecraft().currentScreen instanceof HudEditorScreen);
   }

   public static void begin(String id) {
      DrawContext g = new DrawContext();
      HudConfig.Element e = HudStudioClient.config == null ? null : HudStudioClient.config.elements.get(id);
      g.pose().pushMatrix();
      if (e != null && (e.moved || e.scale != 1.0F)) {
         HudLayout.Bounds base = HudLayout.base(id, g.guiWidth(), g.guiHeight());
         HudLayout.Bounds target = HudLayout.bounds(e, g.guiWidth(), g.guiHeight());
         g.pose().translate(target.x, target.y);
         g.pose().scale(e.scale, e.scale);
         g.pose().translate(-base.x, -base.y);
      }
   }

   public static void end() {
      new DrawContext().pose().popMatrix();
   }
}

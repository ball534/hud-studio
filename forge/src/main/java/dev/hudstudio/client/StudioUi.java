package dev.hudstudio.client;

import dev.hudstudio.client.ui.AbstractButton;
import dev.hudstudio.client.ui.Component;
import dev.hudstudio.client.ui.DrawContext;
import dev.hudstudio.client.ui.InputWithModifiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public final class StudioUi {
   public static final int BG = -16185079;
   public static final int PANEL = -15592942;
   public static final int CARD = -15132391;
   public static final int BORDER = -13290187;
   public static final int TEXT = -1118482;
   public static final int MUTED = -6710887;

   public static void box(DrawContext g, int x, int y, int w, int h, int color) {
      if (w > 0 && h > 0) {
         g.fill(x, y, x + w, y + h, color);
      }
   }

   public static void card(DrawContext g, int x, int y, int w, int h, int fill) {
      box(g, x, y, w, h, -13290187);
      box(g, x + 1, y + 1, w - 2, h - 2, fill);
   }

   public static void text(DrawContext g, String text, int x, int y, int color) {
      g.text(Minecraft.getMinecraft().fontRendererObj, text, x, y, color, false);
   }

   public static void centered(DrawContext g, String text, float cx, float y, int maxWidth, float scale, int color) {
      FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
      int max = (int)(maxWidth / scale);
      if (font.getStringWidth(text) > max) {
         text = font.trimStringToWidth(text, Math.max(0, max - 10)) + "...";
      }

      g.pose().pushMatrix();
      g.pose().translate(cx, y);
      g.pose().scale(scale, scale);
      g.pose().translate(-font.getStringWidth(text) / 2.0F, 0.0F);
      text(g, text, 0, 0, color);
      g.pose().popMatrix();
   }

   public static class Action extends AbstractButton {
      private final Runnable action;

      public Action(int x, int y, int w, int h, String label, Runnable action) {
         super(x, y, w, h, Component.literal(label));
         this.action = action;
      }

      @Override
      public void onPress(InputWithModifiers input) {
         this.action.run();
      }

      @Override
      protected void extractContents(DrawContext g, int mx, int my, float a) {
         StudioUi.card(g, this.getX(), this.getY(), this.width, this.height, this.isHoveredOrFocused() ? -14079703 : -15132391);
         StudioUi.centered(
            g,
            this.getMessage().getString(),
            this.getX() + this.width / 2.0F,
            this.getY() + (this.height - 7) / 2.0F,
            this.width - 8,
            0.8F,
            this.active ? -1118482 : -10066330
         );
      }
   }
}

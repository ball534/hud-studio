package dev.hudstudio.client;

import dev.hudstudio.client.ui.AbstractSliderButton;
import dev.hudstudio.client.ui.Component;
import dev.hudstudio.client.ui.DrawContext;
import dev.hudstudio.client.ui.MouseButtonEvent;
import java.util.Locale;
import net.minecraft.client.gui.GuiScreen;

public final class AppearanceScreen extends StudioScreen {
   private int left;
   private int hueY;
   private int planeY;
   private int size;
   private int previewX;
   private int previewWidth;
   private int drag;
   private float hue;
   private float saturation;
   private float value;
   private static final int[] PRESETS = new int[]{-15724528, -1118482, -1680035, -1197992, -9387636, -10052387, -6193958, -2128971};

   public AppearanceScreen(GuiScreen parent) {
      super("Global appearance", parent);
   }

   @Override
   protected void init() {
      this.clearWidgets();
      float[] hsv = StudioTheme.hsv(StudioTheme.color());
      this.hue = hsv[0];
      this.saturation = hsv[1];
      this.value = hsv[2];
      int w = Math.min(360, this.width - 24);
      this.left = (this.width - w) / 2;
      this.hueY = 46;
      this.planeY = 64;
      this.size = Math.min(116, this.height - 108);
      this.previewX = this.left + this.size + 20;
      this.previewWidth = this.left + w - this.previewX;
      this.addRenderableWidget(new AppearanceScreen.OpacitySlider(this.previewX, 142, this.previewWidth));
      this.addRenderableWidget(new StudioUi.Action(this.previewX, 181, this.previewWidth, 18, "Reset appearance", () -> {
         HudStudioClient.config.themeColor = -15724528;
         HudStudioClient.config.opacity = 0.85F;
         HudStudioClient.config.save();
         this.init();
      }));
      this.addRenderableWidget(new StudioUi.Action(this.width / 2 - 30, this.height - 24, 60, 18, "Back", this::onClose));
   }

   private void saveColor() {
      HudStudioClient.config.themeColor = StudioTheme.hsv(this.hue, this.saturation, this.value);
      HudStudioClient.config.save();
   }

   private void update(double mx, double my) {
      if (this.drag == 1) {
         this.hue = Numbers.clamp((float)(mx - this.left) / (this.size - 1), 0.0F, 0.9999F);
      }

      if (this.drag == 2) {
         this.saturation = Numbers.clamp((float)(mx - this.left) / (this.size - 1), 0.0F, 1.0F);
         this.value = 1.0F - Numbers.clamp((float)(my - this.planeY) / (this.size - 1), 0.0F, 1.0F);
      }

      this.saveColor();
   }

   @Override
   public void extractRenderState(DrawContext g, int mx, int my, float a) {
      StudioUi.centered(g, "Global appearance", this.width / 2.0F, 12.0F, this.width - 24, 1.0F, -1118482);
      StudioUi.centered(g, "Choose a colour and opacity for your HUD panels.", this.width / 2.0F, 28.0F, this.width - 24, 0.7F, -6710887);

      for (int x = 0; x < this.size; x += 2) {
         g.fill(this.left + x, this.hueY, this.left + Math.min(x + 2, this.size), this.hueY + 10, StudioTheme.hsv((float)x / this.size, 1.0F, 1.0F));
      }

      for (int y = 0; y < this.size; y += 4) {
         for (int x = 0; x < this.size; x += 4) {
            g.fill(
               this.left + x,
               this.planeY + y,
               this.left + Math.min(x + 4, this.size),
               this.planeY + Math.min(y + 4, this.size),
               StudioTheme.hsv(this.hue, (float)x / (this.size - 1), 1.0F - (float)y / (this.size - 1))
            );
         }
      }

      g.outline(this.left - 1, this.hueY - 1, this.size + 2, 12, -13290187);
      g.outline(this.left - 1, this.planeY - 1, this.size + 2, this.size + 2, -13290187);
      int hx = this.left + Math.round(this.hue * (this.size - 1));
      int sx = this.left + Math.round(this.saturation * (this.size - 1));
      int sy = this.planeY + Math.round((1.0F - this.value) * (this.size - 1));
      g.outline(hx - 2, this.hueY - 2, 5, 14, -15658735);
      g.outline(hx - 1, this.hueY - 1, 3, 12, -1);
      g.outline(sx - 3, sy - 3, 7, 7, -15658735);
      g.outline(sx - 2, sy - 2, 5, 5, -1);
      int swatch = Math.max(8, (this.size - 14) / 8);

      for (int i = 0; i < PRESETS.length; i++) {
         int x = this.left + i * (swatch + 2);
         int y = this.planeY + this.size + 8;
         g.fill(x, y, x + swatch, y + 12, PRESETS[i]);
         g.outline(x, y, swatch, 12, PRESETS[i] == StudioTheme.color() ? -1 : -11184811);
      }

      StudioUi.card(g, this.previewX, 46, this.previewWidth, 80, -15592942);
      StudioUi.centered(g, "Preview", this.previewX + this.previewWidth / 2.0F, 51.0F, this.previewWidth - 8, 0.65F, -6710887);
      g.pose().pushMatrix();
      g.pose().translate(this.previewX + (this.previewWidth - 51.2F) / 2.0F, 62.0F);
      g.pose().scale(0.8F, 0.8F);
      HudWidgets.drawLocal(g, HudStudioClient.config.elements.get("wasd"), true);
      g.pose().translate(0.0F, 48.0F);
      HudWidgets.drawLocal(g, HudStudioClient.config.elements.get("fps"), true);
      g.pose().popMatrix();
      StudioUi.centered(
         g,
         String.format(Locale.ROOT, "#%06X", StudioTheme.color() & 16777215),
         this.previewX + this.previewWidth / 2.0F,
         132.0F,
         this.previewWidth,
         0.75F,
         -6710887
      );
      super.extractRenderState(g, mx, my, a);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
      if (e.button() != 0) {
         return super.mouseClicked(e, twice);
      }

      if (e.x() >= this.left && e.x() < this.left + this.size) {
         if (e.y() >= this.hueY && e.y() < this.hueY + 10) {
            this.drag = 1;
         } else if (e.y() >= this.planeY && e.y() < this.planeY + this.size) {
            this.drag = 2;
         }

         if (this.drag != 0) {
            this.update(e.x(), e.y());
            return true;
         }

         int swatch = Math.max(8, (this.size - 14) / 8);
         if (e.y() >= this.planeY + this.size + 8 && e.y() < this.planeY + this.size + 20) {
            int index = (int)(e.x() - this.left) / (swatch + 2);
            if (index < PRESETS.length) {
               HudStudioClient.config.themeColor = PRESETS[index];
               HudStudioClient.config.save();
               float[] hsv = StudioTheme.hsv(PRESETS[index]);
               this.hue = hsv[0];
               this.saturation = hsv[1];
               this.value = hsv[2];
               return true;
            }
         }
      }

      return super.mouseClicked(e, twice);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
      if (this.drag != 0) {
         this.update(e.x(), e.y());
         return true;
      } else {
         return super.mouseDragged(e, dx, dy);
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent e) {
      this.drag = 0;
      return super.mouseReleased(e);
   }

   private static final class OpacitySlider extends AbstractSliderButton {
      OpacitySlider(int x, int y, int w) {
         super(x, y, w, 30, Component.empty(), HudStudioClient.config.opacity);
         this.updateMessage();
      }

      @Override
      protected void updateMessage() {
         this.setMessage(Component.literal("Opacity  " + Math.round(HudStudioClient.config.opacity * 100.0F) + "%"));
      }

      @Override
      protected void applyValue() {
         HudStudioClient.config.opacity = (float)this.value;
         HudStudioClient.config.save();
      }

      @Override
      public void extractWidgetRenderState(DrawContext g, int mx, int my, float a) {
         StudioUi.centered(g, this.getMessage().getString(), this.getX() + this.width / 2.0F, this.getY(), this.width, 0.8F, -1118482);
         int y = this.getY() + 18;
         StudioUi.box(g, this.getX(), y, this.width, 3, -12303292);
         StudioUi.box(g, this.getX(), y, (int)(this.value * this.width), 3, StudioTheme.accent());
         StudioUi.box(g, this.getX() + (int)(this.value * (this.width - 5)), y - 3, 5, 9, -1118482);
         this.handleCursor(g);
      }
   }
}

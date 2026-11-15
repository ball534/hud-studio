package dev.hudstudio.client;

import dev.hudstudio.client.ui.AbstractButton;
import dev.hudstudio.client.ui.Component;
import dev.hudstudio.client.ui.DrawContext;
import dev.hudstudio.client.ui.InputWithModifiers;
import dev.hudstudio.client.ui.KeyEvent;
import dev.hudstudio.client.ui.MouseButtonEvent;
import dev.hudstudio.client.ui.Tooltip;
import java.util.Arrays;
import java.util.function.BiConsumer;

public final class ModuleCard extends AbstractButton {
   public static final int SIZE = 68;
   private final HudConfig.Element element;
   private final String[] description;
   private final Runnable open;
   private final BiConsumer<Integer, Integer> menu;
   private final int clipLeft;
   private final int clipTop;
   private final int clipRight;
   private final int clipBottom;

   public ModuleCard(
      int x, int y, HudConfig.Element e, String[] description, Runnable open, BiConsumer<Integer, Integer> menu, int left, int top, int right, int bottom
   ) {
      super(x, y, 68, 68, Component.literal(e == null ? "Add slot" : HudLayout.name(e)));
      this.element = e;
      this.description = description;
      this.open = open;
      this.menu = menu;
      this.clipLeft = left;
      this.clipTop = top;
      this.clipRight = right;
      this.clipBottom = bottom;
      this.setTooltip(
         Tooltip.create(
            Component.literal(
               e == null
                  ? "Add an inventory or armour display"
                  : HudLayout.name(e)
                     + (
                        Arrays.asList(HudConfig.TWEAKS).contains(e.id)
                           ? " · Click to configure; Space toggles"
                           : " · Click to arrange; Space toggles visibility"
                     )
            )
         )
      );
   }

   private boolean hasMenu() {
      return this.element != null && (this.element.id.startsWith("slot_") || this.element.id.equals("offhand") || this.element.id.equals("effects"));
   }

   private boolean inToggle(double x, double y) {
      return this.element != null && x >= this.getX() + 14 && x < this.getX() + 68 - 14 && y >= this.getY() + 53 && y < this.getY() + 68 - 2;
   }

   private void toggle() {
      this.element.visible = !this.element.visible;
      HudStudioClient.config.save();
   }

   @Override
   public void onPress(InputWithModifiers input) {
      if (input instanceof MouseButtonEvent) {
         MouseButtonEvent e = (MouseButtonEvent)input;
         if (this.inToggle(e.x(), e.y())) {
            this.toggle();
            return;
         }

         if (this.hasMenu() && e.x() >= this.getX() + 68 - 13 && e.y() < this.getY() + 13) {
            this.menu.accept((int)e.x(), (int)e.y());
            return;
         }
      }

      this.open.run();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
      if (event.y() < this.clipTop || event.y() >= this.clipBottom) {
         return false;
      } else if (this.hasMenu() && event.button() == 1 && this.isMouseOver(event.x(), event.y())) {
         this.menu.accept((int)event.x(), (int)event.y());
         return true;
      } else {
         return super.mouseClicked(event, twice);
      }
   }

   @Override
   public boolean isMouseOver(double x, double y) {
      return y >= this.clipTop && y < this.clipBottom && super.isMouseOver(x, y);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.element != null && event.key() == 57) {
         this.toggle();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   private String shortName() {
      if (this.element == null) {
         return "Add slot";
      } else if (this.element.id.equals("effects")) {
         return "Effects";
      } else if (this.element.id.equals("no_shake")) {
         return "No shake";
      } else if (this.element.id.startsWith("slot_") && this.element.slot < 36) {
         return this.element.slot < 9 ? "Hotbar " + (this.element.slot + 1) : "Inventory " + (this.element.slot - 8);
      } else {
         return HudLayout.name(this.element);
      }
   }

   @Override
   protected void extractContents(DrawContext g, int mx, int my, float a) {
      g.enableScissor(this.clipLeft, this.clipTop, this.clipRight, this.clipBottom);
      int x = this.getX();
      int y = this.getY();
      boolean hovered = this.isMouseOver(mx, my);
      StudioUi.card(g, x, y, 68, 68, !hovered && !this.isFocused() ? -15132391 : -14342875);
      if (this.isFocused()) {
         g.outline(x, y, 68, 68, StudioTheme.accent());
      }

      StudioIcons.draw(g, this.element == null ? "add" : this.element.id, x + 26, y + 7);
      StudioUi.centered(g, this.shortName(), x + 34.0F, y + 27, 60, 0.8F, -1118482);

      for (int i = 0; i < Math.min(2, this.description.length); i++) {
         StudioUi.centered(g, this.description[i], x + 34.0F, y + 39 + i * 7, 60, 0.65F, -6710887);
      }

      if (this.element != null) {
         int tx = x + 20;
         int ty = y + 56;
         StudioUi.box(g, tx, ty, 28, 9, this.element.visible ? StudioTheme.accent() : -13224394);
         StudioUi.centered(
            g, this.element.visible ? "ON" : "OFF", x + 34.0F, ty + 2, 26, 0.65F, this.element.visible ? StudioTheme.textOn(StudioTheme.accent()) : -4473925
         );
      }

      if (this.hasMenu()) {
         for (int i = 0; i < 3; i++) {
            g.fill(x + 68 - 10 + i * 2, y + 5, x + 68 - 9 + i * 2, y + 6, -5592406);
         }
      }

      g.disableScissor();
   }
}

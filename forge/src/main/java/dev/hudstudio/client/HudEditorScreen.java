package dev.hudstudio.client;

import dev.hudstudio.client.ui.DrawContext;
import dev.hudstudio.client.ui.GuiEventListener;
import dev.hudstudio.client.ui.InputConstants;
import dev.hudstudio.client.ui.KeyEvent;
import dev.hudstudio.client.ui.MouseButtonEvent;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;

public final class HudEditorScreen extends StudioScreen {
   private final HudConfig.Element selected;
   private boolean moving;
   private boolean resizing;
   private boolean shiftHeld;
   private int resizeCorner;
   private float grabX;
   private float grabY;
   private float startMouseX;
   private float startMouseY;
   private float startScale;
   private float anchorX;
   private float anchorY;

   public HudEditorScreen(GuiScreen parent, HudConfig.Element selected) {
      super("Arrange HUD", parent);
      this.selected = selected;
   }

   @Override
   public void extractBackground(DrawContext g, int mx, int my, float a) {
      if (this.mc.thePlayer == null) {
         g.fill(0, 0, this.width, this.height, -15724528);
      }
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.addRenderableWidget(new StudioUi.Action(this.width - 100, this.height - 24, 44, 18, "Back", this::onClose));
      StudioUi.Action reset = this.addRenderableWidget(new StudioUi.Action(this.width - 52, this.height - 24, 44, 18, "Reset", () -> {
         if (this.selected != null) {
            this.selected.moved = false;
            this.selected.scale = 1.0F;
            HudStudioClient.config.save();
         }
      }));
      reset.active = this.selected != null;
   }

   private boolean editable(HudConfig.Element e) {
      return e.visible || e == this.selected;
   }

   private static int corner(HudLayout.Bounds b, double x, double y) {
      if (Math.abs(y - (b.y() + b.h())) > 5.0) {
         return 0;
      } else {
         double left = Math.abs(x - b.x());
         double right = Math.abs(x - b.x() - b.w());
         if (Math.min(left, right) > 5.0) {
            return 0;
         } else {
            return left < right ? -1 : 1;
         }
      }
   }

   private boolean overButton(double x, double y) {
      for (GuiEventListener child : this.children()) {
         if (child.isMouseOver(x, y)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void extractRenderState(DrawContext g, int mx, int my, float a) {
      if (this.moving) {
         g.verticalLine(this.width / 2, 0, this.height, 1433892727);
         g.horizontalLine(0, this.width, this.height / 2, 1433892727);
      }

      List<HudConfig.Element> drawOrder = HudLayout.all();
      drawOrder.remove(this.selected);
      if (this.selected != null) {
         drawOrder.add(this.selected);
      }

      for (HudConfig.Element e : drawOrder) {
         if (this.editable(e)) {
            HudLayout.Bounds b = HudLayout.bounds(e, this.width, this.height);
            boolean nativeElement = Arrays.asList(HudConfig.VANILLA).contains(e.id) && !e.id.equals("offhand");
            if (nativeElement && e != this.selected) {
               g.outline((int)b.x(), (int)b.y(), (int)b.w(), (int)b.h(), 1148680055);
            } else {
               HudLayout.begin(g, e, false);

               try {
                  HudWidgets.drawLocal(g, e, true);
               } finally {
                  HudLayout.end(g);
               }
            }

            if (e == this.selected) {
               outline(g, b, -2236963);
               handle(g, b.x() - 6.0F, b.y() + b.h() - 5.0F);
               handle(g, b.x() + b.w(), b.y() + b.h() - 5.0F);
               String label = HudLayout.name(e) + "  " + Math.round(e.scale * 100.0F) + "%";
               float cy = b.y() >= 13.0F ? b.y() - 12.0F : b.y() + 4.0F;
               float cx = Numbers.clamp(b.x() + b.w() / 2.0F, 50.0F, Math.max(50, this.width - 50));
               StudioUi.centered(g, label, cx + 1.0F, cy + 1.0F, this.width - 12, 0.8F, -16777216);
               StudioUi.centered(g, label, cx, cy, this.width - 12, 0.8F, -1118482);
            }
         }
      }

      super.extractRenderState(g, mx, my, a);
   }

   private static void rectangle(DrawContext g, float x, float y, float w, float h, int color) {
      g.pose().pushMatrix();
      g.pose().translate(x, y);
      g.pose().scale(w, h);
      g.fill(0, 0, 1, 1, color);
      g.pose().popMatrix();
   }

   private static void outline(DrawContext g, HudLayout.Bounds b, int color) {
      rectangle(g, b.x() - 1.0F, b.y() - 1.0F, b.w() + 2.0F, 1.0F, color);
      rectangle(g, b.x() - 1.0F, b.y() + b.h(), b.w() + 2.0F, 1.0F, color);
      rectangle(g, b.x() - 1.0F, b.y(), 1.0F, b.h(), color);
      rectangle(g, b.x() + b.w(), b.y(), 1.0F, b.h(), color);
   }

   private static void handle(DrawContext g, float x, float y) {
      rectangle(g, x, y, 6.0F, 6.0F, -15658735);
      rectangle(g, x + 1.0F, y + 1.0F, 4.0F, 4.0F, -1118482);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
      if (this.overButton(event.x(), event.y())) {
         return super.mouseClicked(event, twice);
      }

      if (this.selected != null && event.button() == 0) {
         this.shiftHeld = event.hasShiftDown() || physicalShift();
         HudLayout.Bounds b = HudLayout.bounds(this.selected, this.width, this.height);
         int c = corner(b, event.x(), event.y());
         if (c == 0 && !b.contains(event.x(), event.y())) {
            return false;
         }

         this.resizeCorner = c;
         this.resizing = c != 0;
         this.moving = !this.resizing;
         this.grabX = (float)event.x() - b.x();
         this.grabY = (float)event.y() - b.y();
         this.startMouseX = (float)event.x();
         this.startMouseY = (float)event.y();
         this.startScale = this.selected.scale;
         this.anchorX = c < 0 ? b.x() + b.w() : b.x();
         this.anchorY = b.y();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      if (this.selected != null && (this.moving || this.resizing)) {
         if (this.resizing) {
            HudLayout.Bounds base = HudLayout.base(this.selected.id, this.width, this.height);
            float mx = ((float)event.x() - this.startMouseX) * (this.resizeCorner < 0 ? -1 : 1);
            float my = (float)event.y() - this.startMouseY;
            float delta = (mx * base.w() + my * base.h()) / (base.w() * base.w() + base.h() * base.h());
            this.selected.scale = Numbers.clamp(this.startScale + delta, 0.5F, 3.0F);
            if (this.shiftHeld || physicalShift()) {
               this.selected.scale = Math.round(this.selected.scale * 20.0F) / 20.0F;
            }

            HudLayout.move(
               this.selected,
               this.resizeCorner < 0 ? this.anchorX - base.w() * this.selected.scale : this.anchorX,
               this.anchorY,
               this.width,
               this.height
            );
         } else {
            float x = (float)event.x() - this.grabX;
            float y = (float)event.y() - this.grabY;
            if (HudStudioClient.config.snap && !this.shiftHeld && !physicalShift()) {
               HudLayout.Position snapped = HudLayout.snap(this.selected, x, y, this.width, this.height);
               x = snapped.x();
               y = snapped.y();
            }

            HudLayout.move(this.selected, x, y, this.width, this.height);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      boolean handled = this.moving || this.resizing;
      this.moving = false;
      this.resizing = false;
      if (handled) {
         HudStudioClient.config.save();
      }

      return super.mouseReleased(event) || handled;
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.key() != 42 && event.key() != 54) {
         if (this.selected != null) {
            int dx = event.key() == 203 ? -1 : (event.key() == 205 ? 1 : 0);
            int dy = event.key() == 200 ? -1 : (event.key() == 208 ? 1 : 0);
            if (dx != 0 || dy != 0) {
               HudLayout.Bounds b = HudLayout.bounds(this.selected, this.width, this.height);
               HudLayout.move(this.selected, b.x() + dx, b.y() + dy, this.width, this.height);
               HudStudioClient.config.save();
               return true;
            }
         }

         return super.keyPressed(event);
      } else {
         this.shiftHeld = true;
         return true;
      }
   }

   private static boolean physicalShift() {
      return InputConstants.isKeyDown(42) || InputConstants.isKeyDown(54);
   }

   @Override
   public boolean keyReleased(KeyEvent event) {
      if (event.key() != 42 && event.key() != 54) {
         return super.keyReleased(event);
      }

      this.shiftHeld = InputConstants.isKeyDown(event.key() == 42 ? 54 : 42);
      return true;
   }
}

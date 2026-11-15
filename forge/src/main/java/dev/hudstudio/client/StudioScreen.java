package dev.hudstudio.client;

import dev.hudstudio.client.ui.AbstractWidget;
import dev.hudstudio.client.ui.DrawContext;
import dev.hudstudio.client.ui.EditBox;
import dev.hudstudio.client.ui.GuiEventListener;
import dev.hudstudio.client.ui.KeyEvent;
import dev.hudstudio.client.ui.MouseButtonEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public abstract class StudioScreen extends GuiScreen {
   protected final GuiScreen parent;
   protected FontRenderer font;
   private final List<GuiEventListener> widgets = new ArrayList<>();
   private AbstractWidget focused;
   private int lastX;
   private int lastY;

   protected StudioScreen(String title, GuiScreen parent) {
      this.parent = parent;
   }

   protected abstract void init();

   public final void initGui() {
      this.font = this.fontRendererObj;
      Keyboard.enableRepeatEvents(true);
      this.init();
   }

   protected void clearWidgets() {
      this.widgets.clear();
      this.focused = null;
   }

   protected <T extends AbstractWidget> T addRenderableWidget(T widget) {
      this.widgets.add(widget);
      return widget;
   }

   protected void removeWidget(GuiEventListener widget) {
      this.widgets.remove(widget);
      if (widget == this.focused) {
         this.focused = null;
      }
   }

   protected List<GuiEventListener> children() {
      return this.widgets;
   }

   public void extractBackground(DrawContext g, int mx, int my, float partial) {
      g.fill(0, 0, this.width, this.height, -16185079);
   }

   public void extractRenderState(DrawContext g, int mx, int my, float partial) {
      for (GuiEventListener widget : new ArrayList<>(this.widgets)) {
         ((AbstractWidget)widget).render(g, mx, my, partial);
      }
   }

   public final void drawScreen(int mx, int my, float partial) {
      DrawContext g = new DrawContext();
      this.extractBackground(g, mx, my, partial);
      this.extractRenderState(g, mx, my, partial);

      for (GuiEventListener listener : this.widgets) {
         AbstractWidget widget = (AbstractWidget)listener;
         if (widget.isMouseOver(mx, my) && widget.getTooltip() != null) {
            this.drawHoveringText(this.font.listFormattedStringToWidth(widget.getTooltip().text, 220), mx, my);
            break;
         }
      }
   }

   public boolean doesGuiPauseGame() {
      return true;
   }

   public void onClose() {
      HudStudioClient.config.save();
      this.mc.displayGuiScreen(this.parent);
   }

   public void onGuiClosed() {
      HudStudioClient.config.save();
      Keyboard.enableRepeatEvents(false);
   }

   protected final void mouseClicked(int x, int y, int button) {
      this.lastX = x;
      this.lastY = y;
      this.mouseClicked(new MouseButtonEvent(x, y, button), false);
   }

   public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
      for (GuiEventListener listener : new ArrayList<>(this.widgets)) {
         AbstractWidget widget = (AbstractWidget)listener;
         if (widget.isMouseOver(e.x(), e.y()) && widget.active) {
            if (this.focused != null) {
               this.focused.setFocused(false);
            }

            this.focused = widget;
            widget.setFocused(true);
            if (widget.mouseClicked(e, twice)) {
               return true;
            }
         } else if (widget instanceof EditBox) {
            widget.setFocused(false);
         }
      }

      return false;
   }

   protected final void mouseClickMove(int x, int y, int button, long elapsed) {
      this.mouseDragged(new MouseButtonEvent(x, y, button), x - this.lastX, y - this.lastY);
      this.lastX = x;
      this.lastY = y;
   }

   public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
      return this.focused != null && this.focused.mouseDragged(e, dx, dy);
   }

   protected final void mouseReleased(int x, int y, int button) {
      this.mouseReleased(new MouseButtonEvent(x, y, button));
   }

   public boolean mouseReleased(MouseButtonEvent e) {
      return this.focused != null && this.focused.mouseReleased(e);
   }

   protected final void keyTyped(char character, int key) {
      this.keyPressed(new KeyEvent(key, character));
   }

   public boolean keyPressed(KeyEvent event) {
      if (event.isEscape()) {
         this.onClose();
         return true;
      }

      if (event.key() == 15 && !this.widgets.isEmpty()) {
         int index = this.widgets.indexOf(this.focused);
         if (this.focused != null) {
            this.focused.setFocused(false);
         }

         int next = (index + (isShiftKeyDown() ? -1 : 1) + this.widgets.size()) % this.widgets.size();
         this.focused = (AbstractWidget)this.widgets.get(next);
         this.focused.setFocused(true);
         return true;
      } else {
         return this.focused != null && this.focused.keyPressed(event);
      }
   }

   public boolean keyReleased(KeyEvent event) {
      return this.focused != null && this.focused.keyReleased(event);
   }

   public void handleKeyboardInput() throws IOException {
      super.handleKeyboardInput();
      if (!Keyboard.getEventKeyState()) {
         this.keyReleased(new KeyEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter()));
      }
   }

   public void handleMouseInput() throws IOException {
      super.handleMouseInput();
      int wheel = Mouse.getEventDWheel();
      if (wheel != 0) {
         this.mouseScrolled(
            (double)(Mouse.getEventX() * this.width) / this.mc.displayWidth,
            this.height - (double)(Mouse.getEventY() * this.height) / this.mc.displayHeight - 1.0,
            0.0,
            wheel > 0 ? 1.0 : -1.0
         );
      }
   }

   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      return false;
   }

   public void updateScreen() {
      for (GuiEventListener widget : this.widgets) {
         if (widget instanceof EditBox) {
            ((EditBox)widget).tick();
         }
      }
   }
}

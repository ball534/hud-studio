package dev.hudstudio.client.ui;

import dev.hudstudio.client.Numbers;

public abstract class AbstractSliderButton extends AbstractWidget {
   protected double value;
   private boolean dragging;

   protected AbstractSliderButton(int x, int y, int w, int h, Component label, double value) {
      super(x, y, w, h, label);
      this.value = value;
   }

   protected abstract void updateMessage();

   protected abstract void applyValue();

   protected void handleCursor(DrawContext context) {
   }

   private void set(double value) {
      this.value = Numbers.clamp(value, 0.0, 1.0);
      this.applyValue();
      this.updateMessage();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
      if (this.active && e.button() == 0 && this.isMouseOver(e.x(), e.y())) {
         this.dragging = true;
         this.set((e.x() - this.x) / Math.max(1, this.width));
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
      if (!this.dragging) {
         return false;
      }

      this.set((e.x() - this.x) / Math.max(1, this.width));
      return true;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent e) {
      boolean old = this.dragging;
      this.dragging = false;
      return old;
   }

   @Override
   public boolean keyPressed(KeyEvent e) {
      if (this.active && this.isFocused() && (e.key() == 203 || e.key() == 205)) {
         this.set(this.value + (e.key() == 203 ? -0.02 : 0.02));
         return true;
      } else {
         return false;
      }
   }
}

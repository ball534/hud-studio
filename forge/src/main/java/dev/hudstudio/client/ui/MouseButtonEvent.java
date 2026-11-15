package dev.hudstudio.client.ui;

public final class MouseButtonEvent extends InputWithModifiers {
   private final double x;
   private final double y;
   private final int button;

   public MouseButtonEvent(double x, double y, int button) {
      this.x = x;
      this.y = y;
      this.button = button;
   }

   public double x() {
      return this.x;
   }

   public double y() {
      return this.y;
   }

   public int button() {
      return this.button;
   }
}

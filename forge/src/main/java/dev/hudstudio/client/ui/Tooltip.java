package dev.hudstudio.client.ui;

public final class Tooltip {
   public final String text;

   private Tooltip(String text) {
      this.text = text;
   }

   public static Tooltip create(Component text) {
      return new Tooltip(text.getString());
   }
}

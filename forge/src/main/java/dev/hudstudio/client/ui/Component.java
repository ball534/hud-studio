package dev.hudstudio.client.ui;

public final class Component {
   private final String text;

   private Component(String text) {
      this.text = text;
   }

   public static Component literal(String text) {
      return new Component(text);
   }

   public static Component empty() {
      return literal("");
   }

   public Component withStyle(int color) {
      return this;
   }

   public String getString() {
      return this.text;
   }
}

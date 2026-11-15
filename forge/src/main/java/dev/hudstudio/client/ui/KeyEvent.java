package dev.hudstudio.client.ui;

public final class KeyEvent extends InputWithModifiers {
   private final int key;
   private final char character;

   public KeyEvent(int key, char character) {
      this.key = key;
      this.character = character;
   }

   public int key() {
      return this.key;
   }

   public char character() {
      return this.character;
   }

   public boolean isEscape() {
      return this.key == 1;
   }
}

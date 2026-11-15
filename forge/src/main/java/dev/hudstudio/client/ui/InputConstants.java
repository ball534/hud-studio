package dev.hudstudio.client.ui;

import org.lwjgl.input.Keyboard;

public final class InputConstants {
   public static final int KEY_SPACE = 57;
   public static final int KEY_LSHIFT = 42;
   public static final int KEY_RSHIFT = 54;
   public static final int KEY_LEFT = 203;
   public static final int KEY_RIGHT = 205;
   public static final int KEY_UP = 200;
   public static final int KEY_DOWN = 208;

   public static boolean isKeyDown(int key) {
      return Keyboard.isCreated() && Keyboard.isKeyDown(key);
   }
}

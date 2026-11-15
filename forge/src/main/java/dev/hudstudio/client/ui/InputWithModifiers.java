package dev.hudstudio.client.ui;

import net.minecraft.client.gui.GuiScreen;

public class InputWithModifiers {
   public boolean hasShiftDown() {
      return GuiScreen.isShiftKeyDown();
   }
}

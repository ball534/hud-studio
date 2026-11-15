package dev.hudstudio.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

public abstract class AbstractButton extends AbstractWidget {
   protected AbstractButton(int x, int y, int w, int h, Component label) {
      super(x, y, w, h, label);
   }

   public abstract void onPress(InputWithModifiers var1);

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
      if (this.active && event.button() == 0 && this.isMouseOver(event.x(), event.y())) {
         Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
         this.onPress(event);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (!this.active || !this.isFocused() || event.key() != 28 && event.key() != 57) {
         return false;
      }

      this.onPress(event);
      return true;
   }
}

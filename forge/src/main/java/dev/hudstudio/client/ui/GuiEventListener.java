package dev.hudstudio.client.ui;

public interface GuiEventListener {
   boolean isMouseOver(double var1, double var3);

   boolean mouseClicked(MouseButtonEvent var1, boolean var2);

   boolean mouseDragged(MouseButtonEvent var1, double var2, double var4);

   boolean mouseReleased(MouseButtonEvent var1);

   boolean keyPressed(KeyEvent var1);

   boolean keyReleased(KeyEvent var1);
}

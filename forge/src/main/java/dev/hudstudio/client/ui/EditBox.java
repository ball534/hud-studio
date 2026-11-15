package dev.hudstudio.client.ui;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public final class EditBox extends AbstractWidget {
   private final GuiTextField field;
   private Component hint = Component.empty();
   private Consumer<String> responder;

   public EditBox(FontRenderer font, int x, int y, int w, int h, Component label) {
      super(x, y, w, h, label);
      this.field = new GuiTextField(0, font, x, y, w, h);
      this.field.setEnableBackgroundDrawing(false);
   }

   public void setBordered(boolean value) {
      this.field.setEnableBackgroundDrawing(value);
   }

   public void setTextColor(int color) {
      this.field.setTextColor(color);
   }

   public void setMaxLength(int length) {
      this.field.setMaxStringLength(length);
   }

   public void setValue(String text) {
      this.field.setText(text);
   }

   public void setHint(Component hint) {
      this.hint = hint;
   }

   public void setResponder(Consumer<String> responder) {
      this.responder = responder;
   }

   @Override
   public void setFocused(boolean focused) {
      super.setFocused(focused);
      this.field.setFocused(focused);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
      this.field.mouseClicked((int)event.x(), (int)event.y(), event.button());
      return this.isMouseOver(event.x(), event.y());
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      String old = this.field.getText();
      boolean handled = this.field.textboxKeyTyped(event.character(), event.key());
      if (!old.equals(this.field.getText()) && this.responder != null) {
         this.responder.accept(this.field.getText());
      }

      return handled;
   }

   public void tick() {
      this.field.updateCursorCounter();
   }

   @Override
   protected void extractContents(DrawContext g, int mx, int my, float a) {
      this.field.drawTextBox();
      if (this.field.getText().isEmpty() && !this.isFocused()) {
         g.text(Minecraft.getMinecraft().fontRendererObj, this.hint.getString(), this.x, this.y, -8947849, false);
      }
   }
}

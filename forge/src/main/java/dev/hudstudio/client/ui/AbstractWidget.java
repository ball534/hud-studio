package dev.hudstudio.client.ui;

public abstract class AbstractWidget implements GuiEventListener {
   protected int x;
   protected int y;
   public int width;
   public int height;
   public boolean active = true;
   private boolean focused;
   private boolean hovered;
   private Component message;
   private Tooltip tooltip;

   protected AbstractWidget(int x, int y, int width, int height, Component message) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.message = message;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public Component getMessage() {
      return this.message;
   }

   public void setMessage(Component message) {
      this.message = message;
   }

   public boolean isFocused() {
      return this.focused;
   }

   public void setFocused(boolean focused) {
      this.focused = focused;
   }

   public boolean isHoveredOrFocused() {
      return this.hovered || this.focused;
   }

   public void setTooltip(Tooltip tooltip) {
      this.tooltip = tooltip;
   }

   public Tooltip getTooltip() {
      return this.tooltip;
   }

   public final void render(DrawContext g, int mx, int my, float partial) {
      this.hovered = this.isMouseOver(mx, my);
      this.extractWidgetRenderState(g, mx, my, partial);
   }

   public void extractWidgetRenderState(DrawContext g, int mx, int my, float partial) {
      this.extractContents(g, mx, my, partial);
   }

   protected void extractContents(DrawContext g, int mx, int my, float partial) {
   }

   @Override
   public boolean isMouseOver(double mx, double my) {
      return mx >= this.x && my >= this.y && mx < this.x + this.width && my < this.y + this.height;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
      return false;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
      return false;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent e) {
      return false;
   }

   @Override
   public boolean keyPressed(KeyEvent e) {
      return false;
   }

   @Override
   public boolean keyReleased(KeyEvent e) {
      return false;
   }
}

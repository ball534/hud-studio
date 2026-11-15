package dev.hudstudio.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class DrawContext {
   private final Minecraft mc = Minecraft.getMinecraft();
   private final ScaledResolution resolution = new ScaledResolution(this.mc);
   private final DrawContext.Pose pose = new DrawContext.Pose();

   public int guiWidth() {
      return this.resolution.getScaledWidth();
   }

   public int guiHeight() {
      return this.resolution.getScaledHeight();
   }

   public DrawContext.Pose pose() {
      return this.pose;
   }

   public void fill(int x, int y, int right, int bottom, int color) {
      if (color >>> 24 != 0) {
         Gui.drawRect(x, y, right, bottom, color);
      }
   }

   public void outline(int x, int y, int w, int h, int color) {
      this.fill(x, y, x + w, y + 1, color);
      this.fill(x, y + h - 1, x + w, y + h, color);
      this.fill(x, y, x + 1, y + h, color);
      this.fill(x + w - 1, y, x + w, y + h, color);
   }

   public void verticalLine(int x, int top, int bottom, int color) {
      this.fill(x, top, x + 1, bottom, color);
   }

   public void horizontalLine(int left, int right, int y, int color) {
      this.fill(left, y, right, y + 1, color);
   }

   public void text(FontRenderer font, String text, int x, int y, int color, boolean shadow) {
      font.drawString(text, x, y, color, shadow);
   }

   public void centeredText(FontRenderer font, String text, int x, int y, int color) {
      font.drawStringWithShadow(text, x - font.getStringWidth(text) / 2.0F, y, color);
   }

   public void enableScissor(int left, int top, int right, int bottom) {
      int scale = this.resolution.getScaleFactor();
      GL11.glEnable(3089);
      GL11.glScissor(left * scale, this.mc.displayHeight - bottom * scale, Math.max(0, right - left) * scale, Math.max(0, bottom - top) * scale);
   }

   public void disableScissor() {
      GL11.glDisable(3089);
   }

   public void item(ItemStack stack, int x, int y) {
      if (stack != null) {
         GlStateManager.enableDepth();
         RenderHelper.enableGUIStandardItemLighting();
         this.mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
         RenderHelper.disableStandardItemLighting();
         GlStateManager.disableDepth();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public void itemDecorations(FontRenderer font, ItemStack stack, int x, int y) {
      if (stack != null) {
         this.mc.getRenderItem().renderItemOverlays(font, stack, x, y);
         GlStateManager.disableLighting();
         GlStateManager.disableDepth();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public void blit(ResourceLocation texture, int x, int y, float u, float v, int w, int h, int tw, int th) {
      this.blit(texture, x, y, u, v, w, h, tw, th, -1);
   }

   public void blit(ResourceLocation texture, int x, int y, float u, float v, int w, int h, int tw, int th, int color) {
      if (color >>> 24 != 0) {
         this.mc.getTextureManager().bindTexture(texture);
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
         Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, w, h, tw, th);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public static final class Pose {
      public void pushMatrix() {
         GlStateManager.pushMatrix();
      }

      public void popMatrix() {
         GlStateManager.popMatrix();
      }

      public void translate(float x, float y) {
         GlStateManager.translate(x, y, 0.0F);
      }

      public void scale(float x, float y) {
         GlStateManager.scale(x, y, 1.0F);
      }
   }
}

package dev.hudstudio.client;

public final class StudioTheme {
   public static int color() {
      return HudStudioClient.config == null ? -15724528 : HudStudioClient.config.themeColor;
   }

   public static int alpha(int rgb) {
      float opacity = HudStudioClient.config == null ? 0.85F : HudStudioClient.config.opacity;
      return alpha(rgb, opacity);
   }

   public static int alpha(int rgb, float opacity) {
      return Math.round(opacity * 255.0F) << 24 | rgb & 16777215;
   }

   public static int textOn(int rgb) {
      return brightness(rgb) > 0.53F ? -15658735 : -1118482;
   }

   private static float brightness(int rgb) {
      return ((rgb >> 16 & 0xFF) * 0.2126F + (rgb >> 8 & 0xFF) * 0.7152F + (rgb & 0xFF) * 0.0722F) / 255.0F;
   }

   public static int accent() {
      int rgb = color();
      return brightness(rgb) < 0.14F ? -3750202 : rgb;
   }

   public static int hsv(float h, float s, float v) {
      float c = v * s;
      float x = c * (1.0F - Math.abs(h * 6.0F % 2.0F - 1.0F));
      float m = v - c;
      float r = 0.0F;
      float g = 0.0F;
      float b = 0.0F;
      switch ((int)(h * 6.0F) % 6) {
         case 0:
            r = c;
            g = x;
            break;
         case 1:
            r = x;
            g = c;
            break;
         case 2:
            g = c;
            b = x;
            break;
         case 3:
            g = x;
            b = c;
            break;
         case 4:
            r = x;
            b = c;
            break;
         case 5:
            r = c;
            b = x;
      }

      return 0xFF000000 | Math.round((r + m) * 255.0F) << 16 | Math.round((g + m) * 255.0F) << 8 | Math.round((b + m) * 255.0F);
   }

   public static float[] hsv(int rgb) {
      float r = (rgb >> 16 & 0xFF) / 255.0F;
      float g = (rgb >> 8 & 0xFF) / 255.0F;
      float b = (rgb & 0xFF) / 255.0F;
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float d = max - min;
      float h = 0.0F;
      if (d != 0.0F) {
         if (max == r) {
            h = (g - b) / d % 6.0F;
         } else if (max == g) {
            h = (b - r) / d + 2.0F;
         } else {
            h = (r - g) / d + 4.0F;
         }

         h /= 6.0F;
         if (h < 0.0F) {
            h++;
         }
      }

      return new float[]{h, max == 0.0F ? 0.0F : d / max, max};
   }
}

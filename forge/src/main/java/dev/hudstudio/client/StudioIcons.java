package dev.hudstudio.client;

import dev.hudstudio.client.ui.DrawContext;

public final class StudioIcons {
   private static final int C = -2236963;
   private static final int D = -8947849;

   private static void r(DrawContext g, int x, int y, int w, int h, int c) {
      g.fill(x, y, x + w, y + h, c);
   }

   public static void draw(DrawContext g, String id, int x, int y) {
      g.pose().pushMatrix();
      g.pose().translate(x, y);
      switch (id) {
         case "scoreboard":
            for (int i = 0; i < 3; i++) {
               r(g, 0, i * 5, 2, 2, -2236963);
               r(g, 4, i * 5, 11, 2, -2236963);
            }
            break;
         case "bossbar":
            g.outline(0, 2, 16, 5, -2236963);
            r(g, 2, 4, 9, 1, -2236963);
            r(g, 3, 10, 10, 2, -8947849);
            break;
         case "player_tab":
            for (int i = 0; i < 3; i++) {
               r(g, 1, i * 5, 3, 3, -2236963);
               r(g, 6, i * 5 + 1, 9, 1, -2236963);
            }
            break;
         case "effects":
            r(g, 6, 0, 4, 2, -2236963);
            r(g, 6, 3, 4, 4, -8947849);
            g.outline(3, 6, 10, 9, -2236963);
            r(g, 5, 10, 6, 3, -8947849);
            break;
         case "offhand":
            g.outline(2, 1, 12, 9, -2236963);
            r(g, 4, 10, 8, 2, -2236963);
            r(g, 6, 12, 4, 2, -2236963);
            break;
         case "wasd":
            key(g, 6, 0);
            key(g, 0, 6);
            key(g, 6, 6);
            key(g, 12, 6);
            break;
         case "mouse":
         case "left_cps":
         case "right_cps":
            g.outline(3, 0, 10, 16, -2236963);
            r(g, 7, 1, 1, 7, -2236963);
            r(g, 4, 7, 8, 1, -2236963);
            if (id.equals("left_cps")) {
               r(g, 4, 2, 3, 4, -2236963);
            }

            if (id.equals("right_cps")) {
               r(g, 8, 2, 3, 4, -2236963);
            }
            break;
         case "space":
            g.outline(0, 5, 16, 7, -2236963);
            r(g, 3, 8, 10, 1, -2236963);
            break;
         case "sneak":
            r(g, 7, 1, 3, 3, -2236963);
            r(g, 5, 6, 5, 2, -2236963);
            r(g, 3, 7, 2, 6, -2236963);
            r(g, 5, 11, 7, 2, -2236963);
            break;
         case "sprint":
            r(g, 10, 0, 3, 3, -2236963);
            r(g, 7, 4, 5, 2, -2236963);
            r(g, 6, 6, 3, 4, -2236963);
            r(g, 3, 11, 5, 2, -2236963);
            r(g, 10, 9, 2, 6, -2236963);
            r(g, 0, 5, 4, 1, -8947849);
            break;
         case "fps":
            r(g, 1, 10, 3, 5, -2236963);
            r(g, 6, 6, 3, 9, -2236963);
            r(g, 11, 1, 3, 14, -2236963);
            break;
         case "speed":
            g.outline(0, 5, 16, 10, -8947849);
            r(g, 7, 8, 2, 5, -2236963);
            r(g, 9, 5, 2, 5, -2236963);
            r(g, 3, 3, 10, 1, -2236963);
            break;
         case "clock":
            g.outline(1, 1, 14, 14, -2236963);
            r(g, 7, 3, 1, 6, -2236963);
            r(g, 7, 8, 5, 1, -2236963);
            break;
         case "memory":
            g.outline(3, 3, 10, 10, -2236963);
            r(g, 6, 6, 4, 4, -8947849);

            for (int i = 4; i < 13; i += 4) {
               r(g, i, 0, 1, 3, -2236963);
               r(g, i, 13, 1, 3, -2236963);
               r(g, 0, i, 3, 1, -2236963);
               r(g, 13, i, 3, 1, -2236963);
            }
            break;
         case "add":
            r(g, 7, 1, 2, 14, -2236963);
            r(g, 1, 7, 14, 2, -2236963);
            break;
         case "combo":
            r(g, 2, 10, 3, 5, -2236963);
            r(g, 6, 6, 3, 9, -2236963);
            r(g, 10, 1, 4, 14, -2236963);
            r(g, 0, 1, 5, 2, -8947849);
            break;
         case "zoom":
            g.outline(1, 1, 10, 10, -2236963);
            r(g, 10, 10, 3, 3, -2236963);
            r(g, 13, 13, 3, 3, -2236963);
            r(g, 5, 3, 1, 6, -8947849);
            r(g, 3, 5, 6, 1, -8947849);
            break;
         case "no_shake":
            r(g, 7, 1, 2, 14, -2236963);
            r(g, 0, 7, 16, 2, -2236963);
            g.outline(4, 4, 8, 8, -8947849);
            break;
         case "full_bright":
            g.outline(4, 4, 8, 8, -2236963);
            r(g, 6, 6, 4, 4, -2236963);
            r(g, 7, 0, 2, 3, -2236963);
            r(g, 7, 13, 2, 3, -2236963);
            r(g, 0, 7, 3, 2, -2236963);
            r(g, 13, 7, 3, 2, -2236963);
            break;
         case "freecam":
         case "freelook":
            g.outline(1, 3, 14, 10, -2236963);
            r(g, 6, 6, 4, 4, -8947849);
            r(g, 3, 1, 5, 2, -2236963);
            break;
         case "no_explosions":
            r(g, 6, 3, 4, 10, -8947849);
            r(g, 3, 6, 10, 4, -8947849);

            for (int i = 0; i < 15; i++) {
               r(g, i, 14 - i, 2, 1, -2236963);
            }
            break;
         default:
            for (int row = 0; row < 3; row++) {
               for (int col = 0; col < 3; col++) {
                  g.outline(col * 5, row * 5, 4, 4, -2236963);
               }
            }
      }

      g.pose().popMatrix();
   }

   private static void key(DrawContext g, int x, int y) {
      g.outline(x, y, 4, 4, -2236963);
   }
}

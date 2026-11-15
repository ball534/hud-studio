package dev.hudstudio.client;

import dev.hudstudio.client.ui.DrawContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class HudLayout {
   public static HudLayout.Bounds base(String id, int w, int h) {
      switch (id) {
         case "scoreboard":
            return new HudLayout.Bounds(w - 126, h / 2.0F - 45.0F, 122.0F, 90.0F);
         case "bossbar":
            return new HudLayout.Bounds(w / 2.0F - 91.0F, 12.0F, 182.0F, 19.0F);
         case "player_tab":
            return new HudLayout.Bounds(w / 2.0F - 140.0F, 10.0F, 280.0F, 120.0F);
         case "effects":
            return new HudLayout.Bounds(w - 86, 1.0F, 86.0F, 26.0F);
         case "wasd":
            return new HudLayout.Bounds(8.0F, 8.0F, 64.0F, 42.0F);
         case "mouse":
            return new HudLayout.Bounds(8.0F, 52.0F, 64.0F, 20.0F);
         case "space":
            return new HudLayout.Bounds(8.0F, 74.0F, 64.0F, 20.0F);
         case "sneak":
            return new HudLayout.Bounds(8.0F, 96.0F, 64.0F, 20.0F);
         case "sprint":
            return new HudLayout.Bounds(8.0F, 118.0F, 64.0F, 20.0F);
         case "fps":
            return new HudLayout.Bounds(8.0F, 140.0F, 64.0F, 20.0F);
         case "left_cps":
            return new HudLayout.Bounds(8.0F, 162.0F, 64.0F, 20.0F);
         case "right_cps":
            return new HudLayout.Bounds(8.0F, 184.0F, 64.0F, 20.0F);
         case "speed":
            return new HudLayout.Bounds(8.0F, 206.0F, 64.0F, 20.0F);
         case "clock":
            return new HudLayout.Bounds(8.0F, 228.0F, 64.0F, 20.0F);
         case "memory":
            return new HudLayout.Bounds(8.0F, 250.0F, 64.0F, 20.0F);
         case "combo":
            return new HudLayout.Bounds(w - 158, 28.0F, 140.0F, 60.0F);
         default:
            return new HudLayout.Bounds(w / 2.0F - 144.0F, h - 22, 22.0F, 22.0F);
      }
   }

   public static HudLayout.Bounds bounds(HudConfig.Element e, int w, int h) {
      HudLayout.Bounds b = base(e.id, w, h);
      float bw = b.w * e.scale;
      float bh = b.h * e.scale;
      float defaultX = b.x;
      float defaultY = b.y;
      if (Arrays.asList(HudConfig.STATS).contains(e.id) && defaultY + bh > h - 26) {
         defaultX = 78.0F;
         defaultY = 8 + Arrays.asList(HudConfig.STATS).indexOf(e.id) * 22;
      }

      return new HudLayout.Bounds(
         e.moved ? e.x * Math.max(0.0F, w - bw) : Numbers.clamp(defaultX, 0.0F, Math.max(0.0F, w - bw)),
         e.moved ? e.y * Math.max(0.0F, h - bh) : Numbers.clamp(defaultY, 0.0F, Math.max(0.0F, h - bh)),
         bw,
         bh
      );
   }

   public static void move(HudConfig.Element e, float x, float y, int w, int h) {
      HudLayout.Bounds b = base(e.id, w, h);
      e.moved = true;
      e.x = Numbers.clamp(x / Math.max(1.0F, w - b.w * e.scale), 0.0F, 1.0F);
      e.y = Numbers.clamp(y / Math.max(1.0F, h - b.h * e.scale), 0.0F, 1.0F);
   }

   public static HudLayout.SlotNeighbours slotNeighbours(HudConfig.Element e, int w, int h, boolean preview) {
      HudLayout.Bounds b = bounds(e, w, h);
      boolean left = false;
      boolean right = false;
      boolean up = false;
      boolean down = false;

      for (HudConfig.Element other : HudStudioClient.config.slots) {
         if (other != e
            && other.visible
            && !(Math.abs(e.scale - other.scale) > 0.001F)
            && (preview || !other.hideEmpty || !HudWidgets.empty(HudWidgets.stack(other.slot)))) {
            HudLayout.Bounds target = bounds(other, w, h);
            float stride = 20.0F * e.scale;
            if (Math.abs(target.y - b.y) < 0.1F) {
               left |= Math.abs(target.x + stride - b.x) < 0.1F;
               right |= Math.abs(b.x + stride - target.x) < 0.1F;
            }

            if (Math.abs(target.x - b.x) < 0.1F) {
               up |= Math.abs(target.y + stride - b.y) < 0.1F;
               down |= Math.abs(b.y + stride - target.y) < 0.1F;
            }
         }
      }

      return new HudLayout.SlotNeighbours(left, right, up, down);
   }

   public static HudLayout.Position snap(HudConfig.Element selected, float x, float y, int w, int h) {
      if (selected.id.startsWith("slot_")) {
         HudLayout.Position joined = null;
         float best = Float.MAX_VALUE;

         for (HudConfig.Element other : HudStudioClient.config.slots) {
            if (other != selected && other.visible && !(Math.abs(selected.scale - other.scale) > 0.001F)) {
               HudLayout.Bounds target = bounds(other, w, h);
               float stride = 20.0F * selected.scale;
               float tolerance = 5.0F * selected.scale;

               for (int[] direction : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                  float candidate = target.x + direction[0] * stride;
                  float candidateY = target.y + direction[1] * stride;
                  float dx = Math.abs(x - candidate);
                  float dy = Math.abs(y - candidateY);
                  if (dx <= tolerance && dy <= tolerance && dx + dy < best) {
                     boolean occupied = false;

                     for (HudConfig.Element slot : HudStudioClient.config.slots) {
                        if (slot != selected && slot.visible) {
                           HudLayout.Bounds occupiedBounds = bounds(slot, w, h);
                           if (Math.abs(occupiedBounds.x - candidate) < 0.1F && Math.abs(occupiedBounds.y - candidateY) < 0.1F) {
                              occupied = true;
                              break;
                           }
                        }
                     }

                     if (!occupied) {
                        joined = new HudLayout.Position(candidate, candidateY);
                        best = dx + dy;
                     }
                  }
               }
            }
         }

         if (joined != null) {
            return joined;
         }
      }

      HudLayout.Bounds b = base(selected.id, w, h);
      float sw = b.w * selected.scale;
      float sh = b.h * selected.scale;
      float sx = Math.round(x / 2.0F) * 2;
      float sy = Math.round(y / 2.0F) * 2;
      float bestX = 3.0F;
      float bestY = 3.0F;
      if (Math.abs(x + sw / 2.0F - w / 2.0F) < bestX) {
         sx = w / 2.0F - sw / 2.0F;
         bestX = Math.abs(sx - x);
      }

      if (Math.abs(y + sh / 2.0F - h / 2.0F) < bestY) {
         sy = h / 2.0F - sh / 2.0F;
         bestY = Math.abs(sy - y);
      }

      for (HudConfig.Element other : all()) {
         if (other != selected && other.visible) {
            HudLayout.Bounds target = bounds(other, w, h);
            float gap = 2.0F * Math.max(selected.scale, other.scale);
            float reach = 24.0F * Math.max(selected.scale, other.scale);
            if (y <= target.y + target.h + reach && y + sh >= target.y - reach) {
               for (float candidate : new float[]{target.x, target.x + target.w - sw, target.x - sw - gap, target.x + target.w + gap}) {
                  float distance = Math.abs(candidate - x);
                  if (distance < bestX) {
                     bestX = distance;
                     sx = candidate;
                  }
               }
            }

            if (x <= target.x + target.w + reach && x + sw >= target.x - reach) {
               for (float sourceRow : rowOffsets(selected)) {
                  for (float targetRow : rowOffsets(other)) {
                     float sourceHeight = selected.id.equals("wasd") ? 20.0F * selected.scale : sh;
                     float targetHeight = other.id.equals("wasd") ? 20.0F * other.scale : target.h;
                     float top = target.y + targetRow;

                     for (float candidate : new float[]{
                        top - sourceRow,
                        top + targetHeight - sourceHeight - sourceRow,
                        top - sourceHeight - gap - sourceRow,
                        top + targetHeight + gap - sourceRow
                     }) {
                        float distance = Math.abs(candidate - y);
                        if (distance < bestY) {
                           bestY = distance;
                           sy = candidate;
                        }
                     }
                  }
               }
            }
         }
      }

      return new HudLayout.Position(sx, sy);
   }

   private static float[] rowOffsets(HudConfig.Element e) {
      return e.id.equals("wasd") ? new float[]{0.0F, 22.0F * e.scale} : new float[]{0.0F};
   }

   public static void begin(DrawContext g, HudConfig.Element e, boolean vanilla) {
      HudLayout.Bounds b = base(e.id, g.guiWidth(), g.guiHeight());
      HudLayout.Bounds t = bounds(e, g.guiWidth(), g.guiHeight());
      g.pose().pushMatrix();
      g.pose().translate(t.x, t.y);
      g.pose().scale(e.scale, e.scale);
      if (vanilla) {
         g.pose().translate(-b.x, -b.y);
      }
   }

   public static void end(DrawContext g) {
      g.pose().popMatrix();
   }

   public static String name(HudConfig.Element e) {
      if (e.id.startsWith("slot_")) {
         return HudWidgets.slotName(e.slot);
      }

      switch (e.id) {
         case "scoreboard":
            return "Scoreboard";
         case "bossbar":
            return "Boss bars";
         case "player_tab":
            return "Player tab";
         case "effects":
            return "Status effects";
         case "offhand":
            return "Offhand";
         case "wasd":
            return "WASD";
         case "mouse":
            return "LMB / RMB";
         case "space":
            return "SPACE";
         case "sneak":
            return "SNEAK";
         case "sprint":
            return "SPRINT";
         case "fps":
            return "FPS";
         case "left_cps":
            return "Left CPS";
         case "right_cps":
            return "Right CPS";
         case "speed":
            return "Speed";
         case "clock":
            return "Clock";
         case "memory":
            return "Memory";
         case "combo":
            return "Combo Meter";
         case "zoom":
            return "Zoom";
         case "no_shake":
            return "No camera shake";
         case "freecam":
            return "Freecam";
         case "no_explosions":
            return "No explosions";
         case "freelook":
            return "Freelook";
         case "full_bright":
            return "Full Bright";
         default:
            return e.id;
      }
   }

   public static List<HudConfig.Element> all() {
      List<HudConfig.Element> a = new ArrayList<>(HudStudioClient.config.elements.values());
      a.addAll(HudStudioClient.config.slots);
      return a;
   }

   public static final class Bounds {
      public final float x;
      public final float y;
      public final float w;
      public final float h;

      public Bounds(float x, float y, float w, float h) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      public float w() {
         return this.w;
      }

      public float h() {
         return this.h;
      }

      public boolean contains(double px, double py) {
         return px >= this.x && py >= this.y && px <= this.x + this.w && py <= this.y + this.h;
      }
   }

   public static final class Position {
      public final float x;
      public final float y;

      public Position(float x, float y) {
         this.x = x;
         this.y = y;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }
   }

   public static final class SlotNeighbours {
      public final boolean left;
      public final boolean right;
      public final boolean up;
      public final boolean down;

      public SlotNeighbours(boolean left, boolean right, boolean up, boolean down) {
         this.left = left;
         this.right = right;
         this.up = up;
         this.down = down;
      }

      public boolean left() {
         return this.left;
      }

      public boolean right() {
         return this.right;
      }

      public boolean up() {
         return this.up;
      }

      public boolean down() {
         return this.down;
      }
   }
}

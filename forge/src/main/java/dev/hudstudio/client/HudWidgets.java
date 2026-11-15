package dev.hudstudio.client;

import dev.hudstudio.client.ui.DrawContext;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

public final class HudWidgets {
   public static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
   private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
   private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
   private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();

   public static boolean empty(ItemStack item) {
      return item == null || item.stackSize <= 0;
   }

   public static void click(int button) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.thePlayer != null && mc.currentScreen == null) {
         ArrayDeque<Long> queue = button == 0 ? LEFT : (button == 1 ? RIGHT : null);
         if (queue != null) {
            cps(queue);
            queue.addLast(System.currentTimeMillis());
         }
      }
   }

   private static int cps(ArrayDeque<Long> queue) {
      long now = System.currentTimeMillis();

      while (!queue.isEmpty() && queue.peekFirst() < now - 1000L) {
         queue.removeFirst();
      }

      return queue.size();
   }

   public static String slotName(int slot) {
      switch (slot) {
         case 36:
            return "Boots";
         case 37:
            return "Leggings";
         case 38:
            return "Chestplate";
         case 39:
            return "Helmet";
         default:
            return slot < 9 ? "Hotbar " + (slot + 1) : "Inventory row " + ((slot - 9) / 9 + 1) + ", slot " + ((slot - 9) % 9 + 1);
      }
   }

   public static ItemStack stack(int slot) {
      Minecraft mc = Minecraft.getMinecraft();
      return mc.thePlayer != null && slot >= 0 && slot <= 39 ? mc.thePlayer.inventory.getStackInSlot(slot) : null;
   }

   public static String statistic(String id, boolean preview) {
      Minecraft mc = Minecraft.getMinecraft();
      switch (id) {
         case "fps":
            return (preview && mc.thePlayer == null ? 144 : Minecraft.getDebugFPS()) + " FPS";
         case "left_cps":
            return cps(LEFT) + " CPS";
         case "right_cps":
            return cps(RIGHT) + " CPS";
         case "speed":
            return String.format(
               Locale.ROOT,
               "%.1f m/s",
               mc.thePlayer == null
                  ? 0.0
                  : Math.hypot(mc.thePlayer.posX - mc.thePlayer.prevPosX, mc.thePlayer.posZ - mc.thePlayer.prevPosZ)
                     * 20.0
            );
         case "clock":
            return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
         case "memory":
            Runtime r = Runtime.getRuntime();
            return "Mem " + (r.totalMemory() - r.freeMemory()) * 100L / r.maxMemory() + "%";
         default:
            return "";
      }
   }

   public static void customSlot(DrawContext g, ItemStack item, float opacity) {
      customSlot(g, item, opacity, new HudLayout.SlotNeighbours(false, false, false, false));
   }

   private static void customSlot(DrawContext g, ItemStack item, float opacity, HudLayout.SlotNeighbours joins) {
      int[] offsets = new int[]{0, 2, 20};
      int[] sizes = new int[]{2, 18, 2};

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            if ((col != 0 || !joins.left) && (row != 0 || !joins.up)) {
               float u = col == 0 ? 0.0F : (col == 1 ? 22.0F : (joins.right ? 40.0F : 180.0F));
               float v = row == 0 ? 0.0F : (row == 1 ? 2.0F : (joins.down ? 19.0F : 20.0F));
               g.blit(WIDGETS, offsets[col], offsets[row], u, v, sizes[col], sizes[row], 256, 256, StudioTheme.alpha(-1, opacity));
            }
         }
      }

      if (!empty(item)) {
         g.item(item, 3, 3);
         g.itemDecorations(Minecraft.getMinecraft().fontRendererObj, item, 3, 3);
      }
   }

   public static void draw(DrawContext g, boolean preview) {
      Minecraft mc = Minecraft.getMinecraft();
      if (preview || mc.thePlayer != null && !(mc.currentScreen instanceof StudioScreen) && !mc.gameSettings.hideGUI) {
         for (HudConfig.Element e : HudLayout.all()) {
            if ((e.visible || preview)
               && (preview || !Arrays.asList(HudConfig.VANILLA).contains(e.id) || e.id.equals("effects"))
               && (!e.id.startsWith("slot_") || !e.hideEmpty || !empty(stack(e.slot)) || preview)) {
               HudLayout.begin(g, e, false);

               try {
                  drawLocal(g, e, preview);
               } finally {
                  HudLayout.end(g);
               }
            }
         }
      }
   }

   public static void drawLocal(DrawContext g, HudConfig.Element e, boolean preview) {
      Minecraft mc = Minecraft.getMinecraft();
      GameSettings o = mc.gameSettings;
      switch (e.id) {
         case "wasd":
            tile(g, 22, 0, 20, 20, key(o.keyBindForward), o.keyBindForward.isKeyDown());
            tile(g, 0, 22, 20, 20, key(o.keyBindLeft), o.keyBindLeft.isKeyDown());
            tile(g, 22, 22, 20, 20, key(o.keyBindBack), o.keyBindBack.isKeyDown());
            tile(g, 44, 22, 20, 20, key(o.keyBindRight), o.keyBindRight.isKeyDown());
            break;
         case "mouse":
            tile(g, 0, 0, 31, 20, "LMB", Mouse.isButtonDown(0));
            tile(g, 33, 0, 31, 20, "RMB", Mouse.isButtonDown(1));
            break;
         case "space":
            tile(g, 0, 0, 64, 20, "SPACE", o.keyBindJump.isKeyDown());
            break;
         case "sneak":
            tile(g, 0, 0, 64, 20, "SNEAK", o.keyBindSneak.isKeyDown());
            break;
         case "sprint":
            tile(g, 0, 0, 64, 20, "SPRINT", o.keyBindSprint.isKeyDown());
            break;
         case "fps":
         case "left_cps":
         case "right_cps":
         case "speed":
         case "clock":
         case "memory":
            tile(g, 0, 0, 64, 20, statistic(e.id, preview), false);
            break;
         case "combo":
            ComboMeter.draw(g, preview);
            break;
         case "scoreboard":
            g.fill(0, 0, 122, 90, -1728053248);
            g.centeredText(mc.fontRendererObj, "Scoreboard", 61, 6, -1);

            for (int i = 0; i < 5; i++) {
               StudioUi.text(g, "Player " + (i + 1), 6, 24 + i * 12, -2236963);
               StudioUi.text(g, "" + (10 - i), 104, 24 + i * 12, -5592406);
            }
            break;
         case "bossbar":
            g.centeredText(mc.fontRendererObj, "Boss bar", 91, 0, -1);
            g.fill(0, 13, 182, 19, -13421773);
            g.fill(1, 14, 138, 18, -2236963);
            break;
         case "player_tab":
            g.fill(0, 0, 280, 120, -1442840576);
            g.centeredText(mc.fontRendererObj, "Player tab", 140, 6, -1);

            for (int i = 0; i < 12; i++) {
               StudioUi.text(g, "Player " + (i + 1), 8 + i / 6 * 140, 24 + i % 6 * 15, -2236963);
            }
            break;
         case "effects":
            effects(g, preview);
            break;
         default:
            if (e.id.startsWith("slot_")) {
               customSlot(g, stack(e.slot), e.opacity, HudLayout.slotNeighbours(e, g.guiWidth(), g.guiHeight(), preview));
            }
      }
   }

   private static void effects(DrawContext g, boolean preview) {
      Minecraft mc = Minecraft.getMinecraft();
      int i = 0;
      if (mc.thePlayer != null) {
         for (PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion != null) {
               StudioUi.card(g, i * 30, 0, 26, 26, -870967786);
               if (potion.hasStatusIcon()) {
                  int icon = potion.getStatusIconIndex();
                  g.blit(INVENTORY, i * 30 + 4, 2, icon % 8 * 18, 198 + icon / 8 * 18, 18, 18, 256, 256);
               }

               if (HudStudioClient.config.effectsShowTime) {
                  String duration = effect.getIsPotionDurationMax() ? "\u221e" : Potion.getDurationString(effect);
                  StudioUi.centered(g, duration, i * 30 + 13, 20.0F, 24, 0.5F, -1118482);
               }
               i++;
            }
         }
      }

      if (preview && i == 0) {
         for (int var8 = 0; var8 < 3; var8++) {
            StudioUi.card(g, var8 * 30, 0, 26, 26, -870967786);
            g.blit(INVENTORY, var8 * 30 + 4, 4, var8 * 18, 198.0F, 18, 18, 256, 256);
            if (HudStudioClient.config.effectsShowTime) {
               StudioUi.centered(g, var8 == 2 ? "\u221e" : var8 == 0 ? "2:30" : "0:45", var8 * 30 + 13, 20.0F, 24, 0.5F, -1118482);
            }
         }
      }
   }

   private static String key(KeyBinding binding) {
      return GameSettings.getKeyDisplayString(binding.getKeyCode());
   }

   private static void tile(DrawContext g, int x, int y, int w, int h, String label, boolean pressed) {
      int color = pressed ? StudioTheme.accent() : StudioTheme.color();
      StudioUi.box(g, x, y, w, h, StudioTheme.alpha(color));
      StudioUi.centered(g, label, x + w / 2.0F, y + (h - 8) / 2.0F, w - 6, 1.0F, StudioTheme.textOn(color));
   }
}

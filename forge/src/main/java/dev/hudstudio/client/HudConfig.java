package dev.hudstudio.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hudstudio.HudStudio;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraftforge.fml.common.Loader;

public final class HudConfig {
   private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path FILE = Loader.instance().getConfigDir().toPath().resolve("hud-studio-forge.json");
   public int schema = 2;
   public Map<String, HudConfig.Element> elements = new LinkedHashMap<>();
   public List<HudConfig.Element> slots = new ArrayList<>();
   public Map<String, HudConfig.Element> tweaks = new LinkedHashMap<>();
   public float zoomFactor = 3.0F;
   public float freecamSpeed = 1.0F;
   public boolean zoomShowHud = true;
   public boolean zoomCinematic = false;
   public boolean freecamHold = false;
   public boolean freecamLookFlight = false;
   public boolean freecamDisableOnDamage = true;
   public boolean freecamShowPlayer = true;
   public boolean freecamShowHud = true;
   public boolean freecamShowSubmersion = false;
   public boolean freecamCollision = false;
   public float freecamVerticalSpeed = 1.0F;
   public float freecamSprintMultiplier = 1.5F;
   public boolean snap = true;
   public boolean effectsShowTime = false;
   public int themeColor = -15724528;
   public float opacity = 0.85F;
   public static final String[] VANILLA = new String[]{"scoreboard", "bossbar", "player_tab", "effects"};
   public static final String[] INPUTS = new String[]{"wasd", "mouse", "space", "sneak", "sprint", "left_cps", "right_cps"};
   public static final String[] STATS = new String[]{"fps", "speed", "clock", "memory", "combo"};
   public static final String[] TWEAKS = new String[]{"zoom", "no_shake", "freecam", "no_explosions", "freelook", "full_bright"};

   public static HudConfig defaults() {
      HudConfig c = new HudConfig();

      for (String[] ids : new String[][]{VANILLA, INPUTS, STATS}) {
         for (String id : ids) {
            HudConfig.Element e = new HudConfig.Element(id);
            e.visible = ids == VANILLA;
            c.elements.put(id, e);
         }
      }

      c.elements.get("effects").visible = false;

      for (String id : TWEAKS) {
         HudConfig.Element e = new HudConfig.Element(id);
         e.visible = false;
         c.tweaks.put(id, e);
      }

      return c;
   }

   public static HudConfig load() {
      try {
         if (!Files.exists(FILE)) {
            return defaults();
         }

         HudConfig c = (HudConfig)JSON.fromJson(new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8), HudConfig.class);
         if (c == null) {
            return defaults();
         }

         c.themeColor |= -16777216;
         c.opacity = Float.isFinite(c.opacity) ? Numbers.clamp(c.opacity, 0.0F, 1.0F) : 0.85F;
         c.zoomFactor = Float.isFinite(c.zoomFactor) ? Numbers.clamp(c.zoomFactor, 1.0F, 50.0F) : 3.0F;
         c.freecamSpeed = Float.isFinite(c.freecamSpeed) ? Numbers.clamp(c.freecamSpeed, 0.1F, 3.0F) : 1.0F;
         c.freecamVerticalSpeed = Float.isFinite(c.freecamVerticalSpeed) ? Numbers.clamp(c.freecamVerticalSpeed, 0.1F, 3.0F) : 1.0F;
         c.freecamSprintMultiplier = Float.isFinite(c.freecamSprintMultiplier) ? Numbers.clamp(c.freecamSprintMultiplier, 1.0F, 5.0F) : 1.5F;
         if (c.tweaks == null) {
            c.tweaks = new LinkedHashMap<>();
         }

         for (String id : TWEAKS) {
            HudConfig.Element e = c.tweaks.get(id);
            if (e == null) {
               e = new HudConfig.Element(id);
               e.visible = false;
               c.tweaks.put(id, e);
            }

            e.id = id;
            sanitize(e);
         }

         Map<String, HudConfig.Element> old = c.elements == null ? Collections.emptyMap() : c.elements;
         c.elements = new LinkedHashMap<>();
         defaults().elements.forEach((idx, fallback) -> {
            HudConfig.Element ex = old.getOrDefault(idx, fallback);
            if (ex == null) {
               ex = fallback;
            }

            ex.id = idx;
            sanitize(ex);
            c.elements.put(idx, ex);
         });

         for (String[] ids : new String[][]{INPUTS, STATS}) {
            HudConfig.Element oldPanel = old.get(ids == INPUTS ? "keystrokes" : "stats");
            if (oldPanel != null) {
               for (String id : ids) {
                  if (!old.containsKey(id)) {
                     HudConfig.Element e = c.elements.get(id);
                     e.visible = oldPanel.visible;
                     e.scale = oldPanel.scale;
                     sanitize(e);
                  }
               }
            }
         }

         if (c.slots == null) {
            c.slots = new ArrayList<>();
         }

         c.slots.removeIf(Objects::isNull);
         c.slots.removeIf(ex -> ex.slot > 39);

         for (HudConfig.Element e : c.slots) {
            if (e.id == null || !e.id.startsWith("slot_")) {
               e.id = "slot_" + UUID.randomUUID();
            }

            sanitize(e);
         }

         c.schema = 2;
         return c;
      } catch (Exception e) {
         HudStudio.LOGGER.warn("Cannot read HUD configuration; using defaults", e);
         return defaults();
      }
   }

   private static void sanitize(HudConfig.Element e) {
      e.scale = Float.isFinite(e.scale) ? Numbers.clamp(e.scale, 0.5F, 3.0F) : 1.0F;
      e.x = Float.isFinite(e.x) ? Numbers.clamp(e.x, 0.0F, 1.0F) : 0.0F;
      e.y = Float.isFinite(e.y) ? Numbers.clamp(e.y, 0.0F, 1.0F) : 0.0F;
      e.slot = Numbers.clamp(e.slot, 0, 39);
      e.opacity = Float.isFinite(e.opacity) ? Numbers.clamp(e.opacity, 0.0F, 1.0F) : 1.0F;
   }

   public boolean save() {
      try {
         Files.createDirectories(FILE.getParent());
         Path tmp = FILE.resolveSibling("hud-studio-forge.json.tmp");
         Files.write(tmp, JSON.toJson(this).getBytes(StandardCharsets.UTF_8));
         Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
         return true;
      } catch (Exception e) {
         HudStudio.LOGGER.error("Cannot save HUD configuration", e);
         return false;
      }
   }

   public static class Element {
      public String id;
      public boolean visible = true;
      public float x;
      public float y;
      public float scale = 1.0F;
      public boolean moved;
      public int slot;
      public boolean hideEmpty;
      public float opacity = 1.0F;

      private Element() {
      }

      public Element(String id) {
         this.id = id;
      }
   }
}

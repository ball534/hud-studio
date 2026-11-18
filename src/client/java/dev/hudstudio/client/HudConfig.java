package dev.hudstudio.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

public final class HudConfig {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hud-studio.json");
    public int schema = 2;
    public Map<String, Element> elements = new LinkedHashMap<>();
    public List<Element> slots = new ArrayList<>();
    public Map<String,Element> tweaks = new LinkedHashMap<>();
    public float zoomFactor=3,freecamSpeed=1;
    public boolean zoomShowHud=true,zoomCinematic=false;
    public boolean freecamHold=false,freecamLookFlight=false,freecamDisableOnDamage=true;
    public boolean freecamShowPlayer=true,freecamShowHud=true,freecamShowSubmersion=false,freecamCollision=false;
    public float freecamVerticalSpeed=1,freecamSprintMultiplier=1.5f;
    public boolean snap = true;
    public boolean effectsShowTime = false;
    public boolean waypointsEnabled=true,waypointShowName=true,waypointShowDistance=true;
    public int themeColor = 0xff101010;
    public float opacity = .85f;
    public static final String[] VANILLA = {"scoreboard", "bossbar", "player_tab", "effects", "offhand"};
    public static final String[] INPUTS = {"wasd", "mouse", "space", "sneak", "sprint", "left_cps", "right_cps"};
    public static final String[] STATS = {"fps", "speed", "clock", "memory", "combo"};
    public static final String[] TWEAKS = {"zoom", "no_shake", "freecam", "no_explosions", "freelook", "full_bright"};
    public static class Element {
        public String id;
        public boolean visible = true;
        public float x, y, scale = 1;
        public boolean moved;
        public int slot;
        public boolean hideEmpty;
        public float opacity = 1;
        private Element() {}
        public Element(String id) { this.id = id; }
    }
    public static HudConfig defaults() {
        HudConfig c = new HudConfig();
        for (String[] ids : new String[][]{VANILLA, INPUTS, STATS})
            for (String id : ids) {
                var e=new Element(id);e.visible=ids==VANILLA;c.elements.put(id,e);
            }
        c.elements.get("offhand").hideEmpty = true;
        for(String id:TWEAKS) {var e=new Element(id);e.visible=false;c.tweaks.put(id,e);}
        return c;
    }
    public static HudConfig load() {
        try {
            if (!Files.exists(FILE)) return defaults();
            HudConfig c = JSON.fromJson(Files.readString(FILE), HudConfig.class);
            if (c == null) return defaults();
            c.themeColor |= 0xff000000;
            c.opacity = Float.isFinite(c.opacity) ? Math.clamp(c.opacity,0,1) : .85f;
            c.zoomFactor=Float.isFinite(c.zoomFactor)?Math.clamp(c.zoomFactor,1,50):3;
            c.freecamSpeed=Float.isFinite(c.freecamSpeed)?Math.clamp(c.freecamSpeed,.1f,3):1;
            c.freecamVerticalSpeed=Float.isFinite(c.freecamVerticalSpeed)?Math.clamp(c.freecamVerticalSpeed,.1f,3):1;
            c.freecamSprintMultiplier=Float.isFinite(c.freecamSprintMultiplier)?Math.clamp(c.freecamSprintMultiplier,1,5):1.5f;
            if(c.tweaks==null)c.tweaks=new LinkedHashMap<>();
            for(String id:TWEAKS) {
                var e=c.tweaks.get(id);
                if(e==null) {e=new Element(id);e.visible=false;c.tweaks.put(id,e);}
                e.id=id;sanitize(e);
            }
            Map<String,Element> old = c.elements == null ? Map.of() : c.elements;
            c.elements = new LinkedHashMap<>();
            defaults().elements.forEach((id, fallback) -> {
                Element e = old.getOrDefault(id, fallback);
                if (e == null) e = fallback;
                e.id = id; sanitize(e); c.elements.put(id, e);
            });
            // Migrate visibility and size from the old combined panels, then discard them.
            for (String[] ids : new String[][]{INPUTS, STATS}) {
                Element oldPanel = old.get(ids == INPUTS ? "keystrokes" : "stats");
                if (oldPanel != null) for (String id : ids) {
                    if (!old.containsKey(id)) { var e=c.elements.get(id); e.visible=oldPanel.visible; e.scale=oldPanel.scale; sanitize(e); }
                }
            }
            if (c.slots == null) c.slots = new ArrayList<>();
            c.slots.removeIf(Objects::isNull);
            for (Element e : c.slots) {
                if(e.id == null || !e.id.startsWith("slot_")) e.id="slot_"+UUID.randomUUID();
                sanitize(e);
            }
            c.schema=2;
            return c;
        } catch (Exception e) { dev.hudstudio.HudStudio.LOGGER.warn("Cannot read HUD configuration; using defaults", e); return defaults(); }
    }
    private static void sanitize(Element e) {
        e.scale = Float.isFinite(e.scale) ? Math.clamp(e.scale, .5f, 3f) : 1;
        e.x = Float.isFinite(e.x) ? Math.clamp(e.x,0,1) : 0;
        e.y = Float.isFinite(e.y) ? Math.clamp(e.y,0,1) : 0;
        e.slot = Math.clamp(e.slot,0,40);
        e.opacity = Float.isFinite(e.opacity) ? Math.clamp(e.opacity,0,1) : 1;
    }
    public boolean save() {
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("hud-studio.json.tmp");
            Files.writeString(tmp, JSON.toJson(this));
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) { dev.hudstudio.HudStudio.LOGGER.error("Cannot save HUD configuration",e); return false; }
    }
}

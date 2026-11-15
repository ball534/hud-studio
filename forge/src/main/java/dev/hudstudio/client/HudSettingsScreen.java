package dev.hudstudio.client;

import dev.hudstudio.client.ui.AbstractWidget;
import dev.hudstudio.client.ui.Component;
import dev.hudstudio.client.ui.DrawContext;
import dev.hudstudio.client.ui.EditBox;
import dev.hudstudio.client.ui.GuiEventListener;
import dev.hudstudio.client.ui.KeyEvent;
import dev.hudstudio.client.ui.MouseButtonEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiScreen;

public final class HudSettingsScreen extends StudioScreen {
   private static final int GAP = 6;
   private static final int STEP = 74;
   private static final int SIDEBAR = 96;
   private static final String[] CATEGORIES = new String[]{"Game HUD", "Inventory", "Keystrokes", "Stats", "Camera", "Visuals"};
   private int category;
   private int scroll;
   private int sidebarX;
   private int left;
   private int gridWidth;
   private int gridTop;
   private int gridBottom;
   private int totalHeight;
   private int columns;
   private String query = "";
   private EditBox search;
   private final List<GuiEventListener> cards = new ArrayList<>();
   private HudConfig.Element context;
   private int menuX;
   private int menuY;
   private int menuHeight;

   public HudSettingsScreen(GuiScreen parent) {
      super("ball's hud studio", parent);
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.cards.clear();
      int available = Math.min(600, this.width - 24);
      this.columns = Math.max(1, (available - 96 - 12 + 6) / 74);
      this.gridWidth = this.columns * 74 - 6;
      int bodyWidth = 108 + this.gridWidth;
      this.sidebarX = (this.width - bodyWidth) / 2;
      this.left = this.sidebarX + 96 + 12;
      this.gridTop = 58;
      this.gridBottom = this.height - 30;

      for (int i = 0; i < CATEGORIES.length; i++) {
         final int index = i;
         this.addRenderableWidget(
            new StudioUi.Action(this.sidebarX, this.gridTop + i * 27, 96, 22, CATEGORIES[i], () -> {
               this.category = index;
               this.scroll = 0;
               this.context = null;
               this.updateHint();
               this.refreshCards();
            }) {
               @Override
               protected void extractContents(DrawContext g, int mx, int my, float a) {
                  StudioUi.card(
                     g,
                     this.getX(),
                     this.getY(),
                     this.getWidth(),
                     this.getHeight(),
                     HudSettingsScreen.this.category == index ? -14079703 : (this.isHoveredOrFocused() ? -14540254 : -15592942)
                  );
                  if (HudSettingsScreen.this.category == index) {
                     StudioUi.box(g, this.getX(), this.getY(), 2, this.getHeight(), StudioTheme.accent());
                  }

                  StudioUi.centered(
                     g,
                     this.getMessage().getString(),
                     this.getX() + this.getWidth() / 2.0F,
                     this.getY() + 7,
                     this.getWidth() - 10,
                     0.8F,
                     HudSettingsScreen.this.category == index ? -1118482 : -6710887
                  );
               }
            }
         );
      }

      this.addRenderableWidget(
         new StudioUi.Action(this.sidebarX, 32, 96, 20, "Appearance", () -> this.mc.displayGuiScreen(new AppearanceScreen(this)))
      );
      this.search = this.addRenderableWidget(new EditBox(this.font, this.left + 7, 38, this.gridWidth - 14, 12, Component.literal("Search modules")));
      this.search.setBordered(false);
      this.search.setTextColor(-1118482);
      this.search.setMaxLength(80);
      this.search.setValue(this.query);
      this.updateHint();
      this.search.setResponder(value -> {
         this.query = value;
         this.scroll = 0;
         this.context = null;
         this.refreshCards();
      });
      this.addRenderableWidget(new StudioUi.Action(this.width / 2 - 30, this.height - 23, 60, 18, "Back", this::onClose));
      this.refreshCards();
   }

   private void updateHint() {
      this.search.setHint(Component.literal("Search " + CATEGORIES[this.category] + "...").withStyle(-6710887));
   }

   private String[] description(HudConfig.Element e) {
      if (e.id.startsWith("slot_")) {
         return new String[]{e.slot >= 36 ? "Equipment" : "Inventory", "slot display"};
      }

      switch (e.id) {
         case "scoreboard":
            return new String[]{"Sidebar", "and scores"};
         case "bossbar":
            return new String[]{"Boss health", "and events"};
         case "player_tab":
            return new String[]{"Player list", "overlay"};
         case "effects":
            return new String[]{"Active status", "effects"};
         case "offhand":
            return new String[]{"Offhand", "item display"};
         case "wasd":
            return new String[]{"Movement", "keys"};
         case "mouse":
            return new String[]{"Mouse", "buttons"};
         case "space":
            return new String[]{"Jump", "input"};
         case "sneak":
            return new String[]{"Sneak", "input"};
         case "sprint":
            return new String[]{"Sprint", "input"};
         case "fps":
            return new String[]{"Frames per", "second"};
         case "left_cps":
            return new String[]{"Left clicks", "per second"};
         case "right_cps":
            return new String[]{"Right clicks", "per second"};
         case "speed":
            return new String[]{"Movement", "speed"};
         case "clock":
            return new String[]{"Local", "time"};
         case "memory":
            return new String[]{"Heap", "usage"};
         case "combo":
            return new String[]{"Style ranks", "and hit streaks"};
         case "zoom":
            return new String[]{"Hold to zoom", "Adjustable lens"};
         case "no_shake":
            return new String[]{"No view bob", "or damage tilt"};
         case "freecam":
            return new String[]{"Detached camera", "Explore nearby"};
         case "no_explosions":
            return new String[]{"Hide explosion", "particles"};
         case "freelook":
            return new String[]{"Hold to orbit", "Keep your aim"};
         case "full_bright":
            return new String[]{"Maximum gamma", "See in the dark"};
         default:
            return new String[]{"HUD", "display"};
      }
   }

   private boolean matches(String text) {
      String needle = this.query.trim().toLowerCase(Locale.ROOT);
      String haystack = text.toLowerCase(Locale.ROOT);
      return needle.isEmpty() || haystack.contains(needle);
   }

   private List<HudConfig.Element> entries() {
      List<HudConfig.Element> result = new ArrayList<>();
      if (this.category == 1 && this.matches("Add slot inventory armour")) {
         result.add(null);
      }

      String[] ids;
      switch (this.category) {
         case 0:
            ids = new String[]{"scoreboard", "bossbar", "effects", "player_tab", "combo"};
            break;
         case 1:
            ids = new String[0];
            break;
         case 2:
            ids = new String[]{"wasd", "mouse", "left_cps", "right_cps", "space", "sneak", "sprint"};
            break;
         case 3:
            ids = new String[]{"fps", "speed", "clock", "memory"};
            break;
         case 4:
            ids = new String[]{"zoom", "freelook", "freecam"};
            break;
         default:
            ids = new String[]{"no_shake", "no_explosions", "full_bright"};
      }

      List<HudConfig.Element> source = new ArrayList<>();

      for (String id : ids) {
         source.add((this.category >= 4 ? HudStudioClient.config.tweaks : HudStudioClient.config.elements).get(id));
      }

      if (this.category == 1) {
         source.addAll(HudStudioClient.config.slots);
      }

      for (HudConfig.Element e : source) {
         if (this.matches(HudLayout.name(e) + " " + e.id + " " + String.join(" ", this.description(e)))) {
            result.add(e);
         }
      }

      return result;
   }

   private <T extends AbstractWidget> void addCard(T widget) {
      this.cards.add(widget);
      this.addRenderableWidget(widget);
   }

   private void refreshCards() {
      for (GuiEventListener widget : this.cards) {
         this.removeWidget(widget);
      }

      this.cards.clear();
      List<HudConfig.Element> all = this.entries();
      int count = all.size();
      this.totalHeight = count == 0 ? 0 : (count + this.columns - 1) / this.columns * 74 - 6;
      this.scroll = Numbers.clamp(this.scroll, 0, Math.max(0, this.totalHeight - (this.gridBottom - this.gridTop)));

      for (int i = 0; i < count; i++) {
         HudConfig.Element e = all.get(i);
         int x = this.left + i % this.columns * 74;
         int y = this.gridTop + i / this.columns * 74 - this.scroll;
         if (y + 68 > this.gridTop && y < this.gridBottom) {
            Runnable open = e == null
               ? () -> this.mc.displayGuiScreen(new SlotPickerScreen(this, null))
               : (
                  this.category >= 4
                     ? () -> this.mc.displayGuiScreen(new TweakSettingsScreen(this, e))
                     : () -> this.mc.displayGuiScreen(new HudEditorScreen(this, e))
               );
            this.addCard(new ModuleCard(x, y, e, e == null ? new String[]{"Inventory or", "armour"} : this.description(e), open, (mx, my) -> {
               this.context = e;
               this.menuX = Numbers.clamp(mx, this.left, Math.max(this.left, this.left + this.gridWidth - 114));
               this.menuY = my;
               this.refreshCards();
            }, this.left, this.gridTop, this.left + this.gridWidth, this.gridBottom));
         }
      }

      if (this.context != null) {
         if (this.context.id.equals("effects")) {
            this.menuHeight = 20;
            this.menuY = Numbers.clamp(this.menuY, this.gridTop, Math.max(this.gridTop, this.gridBottom - this.menuHeight));
            this.addCard(new StudioUi.Action(this.menuX, this.menuY, 114, 20,
               "Time remaining: " + (HudStudioClient.config.effectsShowTime ? "ON" : "OFF"), () -> {
                  HudStudioClient.config.effectsShowTime = !HudStudioClient.config.effectsShowTime;
                  this.context = null;
                  HudStudioClient.config.save();
                  this.refreshCards();
               }));
            return;
         }
         boolean slot = this.context.id.startsWith("slot_");
         this.menuHeight = slot ? 60 : 20;
         this.menuY = Numbers.clamp(this.menuY, this.gridTop, Math.max(this.gridTop, this.gridBottom - this.menuHeight));
         int y = this.menuY;
         if (slot) {
            this.addCard(new StudioUi.Action(this.menuX, y, 114, 20, "Change slot", () -> {
               HudConfig.Element e = this.context;
               this.context = null;
               this.mc.displayGuiScreen(new SlotPickerScreen(this, e));
            }));
            y += 20;
         }

         this.addCard(new StudioUi.Action(this.menuX, y, 114, 20, this.context.hideEmpty ? "Show empty slot" : "Hide empty slot", () -> {
            this.context.hideEmpty = !this.context.hideEmpty;
            this.context = null;
            HudStudioClient.config.save();
            this.refreshCards();
         }));
         y += 20;
         if (slot) {
            this.addCard(new StudioUi.Action(this.menuX, y, 114, 20, "Remove display", () -> {
               HudStudioClient.config.slots.remove(this.context);
               this.context = null;
               HudStudioClient.config.save();
               this.refreshCards();
            }));
         }
      }
   }

   @Override
   public void extractRenderState(DrawContext g, int mx, int my, float a) {
      StudioUi.centered(g, "ball's hud studio", this.width / 2.0F, 12.0F, this.width - 20, 1.0F, -1118482);
      StudioUi.card(g, this.left, 32, this.gridWidth, 20, -15592942);
      if (this.search.isFocused()) {
         g.outline(this.left, 32, this.gridWidth, 20, StudioTheme.accent());
      }

      if (this.totalHeight == 0) {
         StudioUi.centered(g, "No matching modules", this.left + this.gridWidth / 2.0F, this.gridTop + 34, this.gridWidth - 12, 0.8F, -1118482);
         StudioUi.centered(g, "Try another search or category.", this.left + this.gridWidth / 2.0F, this.gridTop + 50, this.gridWidth - 12, 0.7F, -6710887);
      }

      super.extractRenderState(g, mx, my, a);
      if (this.totalHeight > this.gridBottom - this.gridTop) {
         int track = this.gridBottom - this.gridTop;
         int thumb = Math.max(12, track * track / this.totalHeight);
         int y = this.gridTop + (int)((track - thumb) * ((float)this.scroll / Math.max(1, this.totalHeight - track)));
         StudioUi.box(g, this.left + this.gridWidth + 5, this.gridTop, 2, track, -14540254);
         StudioUi.box(g, this.left + this.gridWidth + 5, y, 2, thumb, -8947849);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
      if (this.context != null) {
         if (e.x() < this.menuX || e.x() >= this.menuX + 114 || e.y() < this.menuY || e.y() >= this.menuY + this.menuHeight) {
            this.context = null;
            this.refreshCards();
            return true;
         }

         List<GuiEventListener> widgets = this.children();

         for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i) instanceof StudioUi.Action && widgets.get(i).isMouseOver(e.x(), e.y())) {
               return ((StudioUi.Action)widgets.get(i)).mouseClicked(e, twice);
            }
         }
      }

      return super.mouseClicked(e, twice);
   }

   @Override
   public boolean mouseScrolled(double x, double y, double sx, double sy) {
      if (x >= this.left && x <= this.left + this.gridWidth && y >= this.gridTop && y < this.gridBottom) {
         this.context = null;
         this.scroll -= (int)(sy * 26.0);
         this.refreshCards();
         return true;
      } else {
         return super.mouseScrolled(x, y, sx, sy);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent e) {
      if (this.context != null && e.isEscape()) {
         this.context = null;
         this.refreshCards();
         return true;
      } else {
         return super.keyPressed(e);
      }
   }
}

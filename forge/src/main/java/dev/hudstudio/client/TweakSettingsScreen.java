package dev.hudstudio.client;

import dev.hudstudio.client.ui.AbstractSliderButton;
import dev.hudstudio.client.ui.Component;
import dev.hudstudio.client.ui.DrawContext;
import dev.hudstudio.client.ui.KeyEvent;
import dev.hudstudio.client.ui.MouseButtonEvent;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public final class TweakSettingsScreen extends StudioScreen {
   private final HudConfig.Element setting;
   private boolean binding;
   private int page;
   private int row;
   private int x;

   public TweakSettingsScreen(GuiScreen parent, HudConfig.Element setting) {
      super(HudLayout.name(setting), parent);
      this.setting = setting;
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.x = this.width / 2 - 120;
      this.row = 40;
      HudConfig c = HudStudioClient.config;
      if (this.setting.id.equals("freecam")) {
         String[] pages = new String[]{"Controls", "Movement", "Display"};

         for (int i = 0; i < pages.length; i++) {
            int index = i;
            this.addRenderableWidget(new StudioUi.Action(this.x + i * 81, 32, 78, 20, (this.page == i ? "> " : "") + pages[i], () -> {
               this.page = index;
               this.binding = false;
               this.init();
            }));
         }

         this.row = 62;
      }

      if (!this.setting.id.equals("freecam") || this.page == 0) {
         this.toggle("Enabled", () -> this.setting.visible, v -> this.setting.visible = v);
         KeyBinding key = ClientTweaks.key(this.setting.id);
         if (key != null) {
            this.action(this.binding ? "Press a key or mouse button..." : "Key: " + GameSettings.getKeyDisplayString(key.getKeyCode()), () -> {
               this.binding = true;
               this.init();
            });
         }
      }

      switch (this.setting.id) {
         case "zoom":
            this.slider("Starting zoom", 1.0, 50.0, () -> c.zoomFactor, v -> c.zoomFactor = (float)v, "x");
            this.toggle("Show HUD", () -> c.zoomShowHud, v -> c.zoomShowHud = v);
            this.toggle("Cinematic zoom", () -> c.zoomCinematic, v -> c.zoomCinematic = v);
            break;
         case "freecam":
            if (this.page == 0) {
               this.action("Activation: " + (c.freecamHold ? "Hold" : "Toggle"), () -> {
                  c.freecamHold = !c.freecamHold;
                  this.save();
               });
               this.toggle("Exit on damage", () -> c.freecamDisableOnDamage, v -> c.freecamDisableOnDamage = v);
            } else if (this.page == 1) {
               this.action("Flight: " + (c.freecamLookFlight ? "Follow crosshair" : "Horizontal"), () -> {
                  c.freecamLookFlight = !c.freecamLookFlight;
                  this.save();
               });
               this.slider("Horizontal speed", 0.1, 3.0, () -> c.freecamSpeed, v -> c.freecamSpeed = (float)v, " blocks/tick");
               this.slider("Vertical speed", 0.1, 3.0, () -> c.freecamVerticalSpeed, v -> c.freecamVerticalSpeed = (float)v, " blocks/tick");
               this.slider("Sprint boost", 1.0, 5.0, () -> c.freecamSprintMultiplier, v -> c.freecamSprintMultiplier = (float)v, "x");
               this.toggle("Block collision", () -> c.freecamCollision, v -> c.freecamCollision = v);
            } else {
               this.toggle("Show player", () -> c.freecamShowPlayer, v -> c.freecamShowPlayer = v);
               this.toggle("Show HUD", () -> c.freecamShowHud, v -> c.freecamShowHud = v);
               this.toggle("Submersion fog", () -> c.freecamShowSubmersion, v -> c.freecamShowSubmersion = v);
            }
      }

      this.addRenderableWidget(new StudioUi.Action(this.width / 2 - 36, this.height - 25, 72, 18, "Back", this::onClose));
   }

   private void action(String label, Runnable action) {
      this.addRenderableWidget(new StudioUi.Action(this.x, this.row, 240, 22, label, action));
      this.row += 26;
   }

   private void save() {
      HudStudioClient.config.save();
      this.init();
   }

   private void toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
      this.action(label + ": " + (get.getAsBoolean() ? "ON" : "OFF"), () -> {
         set.accept(!get.getAsBoolean());
         this.save();
      });
   }

   private void slider(String label, double min, double max, DoubleSupplier get, DoubleConsumer set, String suffix) {
      this.addRenderableWidget(new TweakSettingsScreen.ValueSlider(this.x, this.row, label, min, max, get, set, suffix));
      this.row += 26;
   }

   private void bind(int key) {
      ClientTweaks.key(this.setting.id).setKeyCode(key);
      KeyBinding.resetKeyBindingArrayAndHash();
      this.mc.gameSettings.saveOptions();
      this.binding = false;
      this.init();
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.binding) {
         if (!event.isEscape()) {
            this.bind(event.key());
         } else {
            this.binding = false;
            this.init();
         }

         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
      if (this.binding) {
         this.bind(event.button() - 100);
         return true;
      } else {
         return super.mouseClicked(event, twice);
      }
   }

   @Override
   public void extractRenderState(DrawContext g, int mx, int my, float a) {
      StudioUi.centered(g, HudLayout.name(this.setting), this.width / 2.0F, 13.0F, this.width - 24, 1.0F, -1118482);
      if (!this.setting.id.equals("zoom") && !this.setting.id.equals("freecam")) {
         String hint;
         switch (this.setting.id) {
            case "freelook":
               hint = "Hold to orbit. Release to restore your view.";
               break;
            case "no_shake":
               hint = "Removes walking bob and damage tilt.";
               break;
             case "full_bright":
                hint = "Maximum gamma while enabled. Saved brightness is preserved.";
                break;
            default:
               hint = "Hides explosion particles; keeps sounds.";
         }

         StudioUi.centered(g, hint, this.width / 2.0F, this.row + 10, this.width - 28, 0.72F, -6710887);
      }

      super.extractRenderState(g, mx, my, a);
   }

   private final class ValueSlider extends AbstractSliderButton {
      private final String label;
      private final String suffix;
      private final double min;
      private final double max;
      private final DoubleSupplier get;
      private final DoubleConsumer set;

      ValueSlider(int x, int y, String label, double min, double max, DoubleSupplier get, DoubleConsumer set, String suffix) {
         super(x, y, 240, 22, Component.empty(), (get.getAsDouble() - min) / (max - min));
         this.label = label;
         this.min = min;
         this.max = max;
         this.get = get;
         this.set = set;
         this.suffix = suffix;
         this.updateMessage();
      }

      @Override
      protected void updateMessage() {
         this.setMessage(Component.literal(this.label + ": " + String.format(Locale.ROOT, "%.1f", this.get.getAsDouble()) + this.suffix));
      }

      @Override
      protected void applyValue() {
         this.set.accept(this.min + this.value * (this.max - this.min));
         HudStudioClient.config.save();
      }

      @Override
      public void extractWidgetRenderState(DrawContext g, int mx, int my, float a) {
         StudioUi.card(g, this.getX(), this.getY(), this.width, this.height, -15592942);
         StudioUi.box(g, this.getX(), this.getY() + this.height - 3, (int)(this.width * this.value), 3, StudioTheme.accent());
         StudioUi.centered(g, this.getMessage().getString(), this.getX() + this.width / 2.0F, this.getY() + 7, this.width - 10, 0.8F, -1118482);
         this.handleCursor(g);
      }
   }
}

package dev.hudstudio.client;

import dev.hudstudio.client.ui.AbstractSliderButton;
import dev.hudstudio.client.ui.Component;
import dev.hudstudio.client.ui.DrawContext;
import dev.hudstudio.client.ui.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemStack;

public final class SlotPickerScreen extends StudioScreen {
   private final HudConfig.Element editing;
   private final List<SlotPickerScreen.Cell> cells = new ArrayList<>();
   private int chosen;
   private int x;
   private int invX;
   private int invY;
   private int detailX;
   private int detailWidth;
   private float inventoryScale;
   private float slotOpacity;

   public SlotPickerScreen(GuiScreen parent, HudConfig.Element editing) {
      super(editing == null ? "Add inventory display" : "Change inventory slot", parent);
      this.editing = editing;
      this.chosen = editing == null ? 0 : editing.slot;
      this.slotOpacity = editing == null ? 1.0F : editing.opacity;
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.cells.clear();
      int w = Math.min(380, this.width - 28);
      this.x = (this.width - w) / 2;
      this.inventoryScale = Math.min(1.0F, Math.min((w - 150) / 176.0F, Math.max(0.55F, (this.height - 88) / 166.0F)));
      this.invX = this.x;
      this.invY = 46;
      this.detailX = this.invX + (int)(176.0F * this.inventoryScale) + 14;
      this.detailWidth = this.x + w - this.detailX;

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.cell(9 + row * 9 + col, 7 + col * 18, 83 + row * 18);
         }
      }

      for (int col = 0; col < 9; col++) {
         this.cell(col, 7 + col * 18, 141);
      }

      for (int i = 0; i < 4; i++) {
         this.cell(39 - i, 7, 7 + i * 18);
      }

      this.addRenderableWidget(
         new SlotPickerScreen.SlotOpacitySlider(this.detailX + 12, this.invY + (int)(166.0F * this.inventoryScale) - 35, this.detailWidth - 24)
      );
      this.addRenderableWidget(new StudioUi.Action(this.x, this.height - 24, 68, 18, "Cancel", this::onClose));
      this.addRenderableWidget(
         new StudioUi.Action(this.x + w - 94, this.height - 24, 94, 18, this.editing == null ? "Add display" : "Use this slot", this::confirm)
      );
   }

   private void cell(int slot, int sx, int sy) {
      this.cells.add(new SlotPickerScreen.Cell(slot, sx, sy));
      StudioUi.Action button = new StudioUi.Action(
         this.invX + (int)(sx * this.inventoryScale),
         this.invY + (int)(sy * this.inventoryScale),
         Math.max(1, (int)(18.0F * this.inventoryScale)),
         Math.max(1, (int)(18.0F * this.inventoryScale)),
         HudWidgets.slotName(slot),
         () -> this.chosen = slot
      ) {
         @Override
         protected void extractContents(DrawContext g, int mx, int my, float a) {
         }
      };
      button.setTooltip(Tooltip.create(Component.literal(HudWidgets.slotName(slot))));
      this.addRenderableWidget(button);
   }

   private void confirm() {
      HudConfig.Element created = null;
      if (this.editing != null) {
         this.editing.slot = this.chosen;
         this.editing.opacity = this.slotOpacity;
      } else {
         HudConfig.Element e = new HudConfig.Element("slot_" + UUID.randomUUID());
         e.slot = this.chosen;
         e.opacity = this.slotOpacity;
         int n = HudStudioClient.config.slots.size();
         int columns = Math.max(1, (this.width / 2 - 124) / 24);
         HudLayout.move(
            e, this.width / 2.0F - 144.0F - n % columns * 24, this.height - 22 - n / columns * 24, this.width, this.height
         );
         HudStudioClient.config.slots.add(e);
         created = e;
      }

      HudStudioClient.config.save();
      this.mc.displayGuiScreen(created == null ? this.parent : new HudEditorScreen(this.parent, created));
   }

   @Override
   public void extractRenderState(DrawContext g, int mx, int my, float a) {
      StudioUi.centered(
         g,
         this.editing == null ? "Add inventory display" : "Change inventory slot",
         this.width / 2.0F,
         12.0F,
         this.width - 20,
         1.0F,
         -1118482
      );
      StudioUi.centered(g, "Select the inventory slot to display.", this.width / 2.0F, 28.0F, this.width - 24, 0.75F, -6710887);
      g.pose().pushMatrix();
      g.pose().translate(this.invX, this.invY);
      g.pose().scale(this.inventoryScale, this.inventoryScale);
      g.blit(HudWidgets.INVENTORY, 0, 0, 0.0F, 0.0F, 176, 166, 256, 256);
      StudioUi.text(g, "Crafting", 97, 6, -12566464);

      for (SlotPickerScreen.Cell c : this.cells) {
         ItemStack item = HudWidgets.stack(c.slot);
         if (!HudWidgets.empty(item)) {
            g.item(item, c.x + 1, c.y + 1);
            g.itemDecorations(this.font, item, c.x + 1, c.y + 1);
         }

         boolean hover = mx >= this.invX + c.x * this.inventoryScale
            && mx < this.invX + (c.x + 18) * this.inventoryScale
            && my >= this.invY + c.y * this.inventoryScale
            && my < this.invY + (c.y + 18) * this.inventoryScale;
         if (hover) {
            g.fill(c.x + 1, c.y + 1, c.x + 17, c.y + 17, 1442840575);
         }

         if (c.slot == this.chosen) {
            g.outline(c.x - 1, c.y - 1, 20, 20, -15724528);
            g.outline(c.x, c.y, 18, 18, -1);
         }
      }

      g.pose().popMatrix();
      if (this.mc.thePlayer != null) {
         GuiInventory.drawEntityOnScreen(
            this.invX + (int)(51.0F * this.inventoryScale),
            this.invY + (int)(75.0F * this.inventoryScale),
            Math.max(1, (int)(30.0F * this.inventoryScale)),
            this.invX + 51.0F * this.inventoryScale - mx,
            this.invY + 25.0F * this.inventoryScale - my,
            this.mc.thePlayer
         );
      }

      StudioUi.card(g, this.detailX, this.invY, this.detailWidth, (int)(166.0F * this.inventoryScale), -15592942);
      float center = this.detailX + this.detailWidth / 2.0F;
      StudioUi.centered(g, "SELECTED SLOT", center, this.invY + 12, this.detailWidth - 16, 0.75F, -6710887);
      StudioUi.centered(g, HudWidgets.slotName(this.chosen), center, this.invY + 28, this.detailWidth - 16, 0.8F, -1118482);
      g.pose().pushMatrix();
      g.pose().translate(center - 16.5F, this.invY + 46);
      g.pose().scale(1.5F, 1.5F);
      HudWidgets.customSlot(g, HudWidgets.stack(this.chosen), this.slotOpacity);
      g.pose().popMatrix();
      StudioUi.centered(
         g,
         HudWidgets.empty(HudWidgets.stack(this.chosen)) ? "Empty slot" : HudWidgets.stack(this.chosen).getDisplayName(),
         center,
         this.invY + 88,
         this.detailWidth - 16,
         0.75F,
         -6710887
      );
      if (this.mc.thePlayer == null) {
         StudioUi.centered(g, "Preview items in a world", center, this.invY + 104, this.detailWidth - 16, 0.65F, -6710887);
      }

      super.extractRenderState(g, mx, my, a);
   }

   private static final class Cell {
      final int slot;
      final int x;
      final int y;

      Cell(int slot, int x, int y) {
         this.slot = slot;
         this.x = x;
         this.y = y;
      }
   }

   private final class SlotOpacitySlider extends AbstractSliderButton {
      SlotOpacitySlider(int x, int y, int w) {
         super(x, y, w, 27, Component.empty(), SlotPickerScreen.this.slotOpacity);
         this.updateMessage();
      }

      @Override
      protected void updateMessage() {
         this.setMessage(Component.literal("Slot opacity  " + Math.round(SlotPickerScreen.this.slotOpacity * 100.0F) + "%"));
      }

      @Override
      protected void applyValue() {
         SlotPickerScreen.this.slotOpacity = (float)this.value;
      }

      @Override
      public void extractWidgetRenderState(DrawContext g, int mx, int my, float a) {
         StudioUi.centered(g, this.getMessage().getString(), this.getX() + this.width / 2.0F, this.getY(), this.width, 0.75F, -1118482);
         int y = this.getY() + 17;
         StudioUi.box(g, this.getX(), y, this.width, 3, -12303292);
         StudioUi.box(g, this.getX(), y, (int)(this.value * this.width), 3, -4473925);
         StudioUi.box(g, this.getX() + (int)(this.value * (this.width - 5)), y - 3, 5, 9, -1118482);
         this.handleCursor(g);
      }
   }
}

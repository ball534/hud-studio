package dev.hudstudio.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class SlotPickerScreen extends StudioScreen {
    private record Cell(int slot,int x,int y) {}
    private final HudConfig.Element editing;
    private final List<Cell> cells=new ArrayList<>();
    private int chosen,x,invX,invY,detailX,detailWidth;
    private float inventoryScale;
    private float slotOpacity;
    public SlotPickerScreen(Screen parent,HudConfig.Element editing) {
        super(editing==null?"Add inventory display":"Change inventory slot",parent);this.editing=editing;chosen=editing==null?0:editing.slot;
        slotOpacity=editing==null?1:editing.opacity;
    }
    @Override protected void init() {
        clearWidgets();cells.clear();
        int w=Math.min(380,width-28);x=(width-w)/2;
        inventoryScale=Math.min(1f,Math.min((w-150)/176f,Math.max(.55f,(height-88)/166f)));
        invX=x;invY=46;detailX=invX+(int)(176*inventoryScale)+14;detailWidth=x+w-detailX;
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) cell(9+row*9+col,7+col*18,83+row*18);
        for(int col=0;col<9;col++) cell(col,7+col*18,141);
        for(int i=0;i<4;i++) cell(39-i,7,7+i*18);
        cell(40,76,61);
        addRenderableWidget(new SlotOpacitySlider(detailX+12,invY+(int)(166*inventoryScale)-35,detailWidth-24));
        addRenderableWidget(new StudioUi.Action(x,height-24,68,18,"Cancel",this::onClose));
        addRenderableWidget(new StudioUi.Action(x+w-94,height-24,94,18,editing==null?"Add display":"Use this slot",this::confirm));
    }
    private void cell(int slot,int sx,int sy) {
        cells.add(new Cell(slot,sx,sy));
        var button=new StudioUi.Action(invX+(int)(sx*inventoryScale),invY+(int)(sy*inventoryScale),
                Math.max(1,(int)(18*inventoryScale)),Math.max(1,(int)(18*inventoryScale)),HudWidgets.slotName(slot),()->chosen=slot) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float a) {}
        };
        button.setTooltip(Tooltip.create(Component.literal(HudWidgets.slotName(slot))));
        addRenderableWidget(button);
    }
    private void confirm() {
        HudConfig.Element created=null;
        if(editing!=null) {editing.slot=chosen;editing.opacity=slotOpacity;}
        else {
            var e=new HudConfig.Element("slot_"+UUID.randomUUID());e.slot=chosen;
            e.opacity=slotOpacity;
            int n=HudStudioClient.config.slots.size();
            int columns=Math.max(1,(width/2-124)/24);
            HudLayout.move(e,width/2f-144-n%columns*24,height-22-n/columns*24,width,height);
            HudStudioClient.config.slots.add(e);
            created=e;
        }
        HudStudioClient.config.save();minecraft.gui.setScreen(created==null?parent:new HudEditorScreen(parent,created));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        StudioUi.centered(g,editing==null?"Add inventory display":"Change inventory slot",width/2f,12,width-20,1f,StudioUi.TEXT);
        StudioUi.centered(g,"Select the inventory slot to display.",width/2f,28,width-24,.75f,StudioUi.MUTED);
        g.pose().pushMatrix();g.pose().translate(invX,invY);g.pose().scale(inventoryScale,inventoryScale);
        g.blit(RenderPipelines.GUI_TEXTURED,HudWidgets.INVENTORY,0,0,0f,0f,176,166,256,256);
        StudioUi.text(g,"Crafting",97,6,0xff404040);
        for(var c:cells) {
            var item=HudWidgets.stack(c.slot);
            if(!item.isEmpty()) {g.item(item,c.x+1,c.y+1);g.itemDecorations(font,item,c.x+1,c.y+1);}
            else if(c.slot>=36) {
                String sprite=switch(c.slot) {case 39->"helmet";case 38->"chestplate";case 37->"leggings";case 36->"boots";default->"shield";};
                g.blitSprite(RenderPipelines.GUI_TEXTURED,net.minecraft.resources.Identifier.withDefaultNamespace("container/slot/"+sprite),c.x+1,c.y+1,16,16);
            }
            boolean hover=mx>=invX+c.x*inventoryScale && mx<invX+(c.x+18)*inventoryScale && my>=invY+c.y*inventoryScale && my<invY+(c.y+18)*inventoryScale;
            if(hover) g.fill(c.x+1,c.y+1,c.x+17,c.y+17,0x55ffffff);
            if(c.slot==chosen) {g.outline(c.x-1,c.y-1,20,20,0xff101010);g.outline(c.x,c.y,18,18,0xffffffff);}
        }
        g.pose().popMatrix();
        // Entity previews use screen coordinates; unlike items, they do not use the GUI pose.
        if(minecraft.player!=null) InventoryScreen.extractEntityInInventoryFollowsMouse(g,
            invX+(int)(26*inventoryScale),invY+(int)(8*inventoryScale),
            invX+(int)(75*inventoryScale),invY+(int)(78*inventoryScale),
            Math.max(1,(int)(30*inventoryScale)),.0625f,mx,my,minecraft.player);
        StudioUi.card(g,detailX,invY,detailWidth,(int)(166*inventoryScale),StudioUi.PANEL);
        float center=detailX+detailWidth/2f;
        StudioUi.centered(g,"SELECTED SLOT",center,invY+12,detailWidth-16,.75f,StudioUi.MUTED);
        StudioUi.centered(g,HudWidgets.slotName(chosen),center,invY+28,detailWidth-16,.8f,StudioUi.TEXT);
        g.pose().pushMatrix();g.pose().translate(center-16.5f,invY+46);g.pose().scale(1.5f,1.5f);
        HudWidgets.customSlot(g,HudWidgets.stack(chosen),slotOpacity);g.pose().popMatrix();
        StudioUi.centered(g,HudWidgets.stack(chosen).isEmpty()?"Empty slot":HudWidgets.stack(chosen).getHoverName().getString(),center,invY+88,detailWidth-16,.75f,StudioUi.MUTED);
        if(minecraft.player==null) StudioUi.centered(g,"Preview items in a world",center,invY+104,detailWidth-16,.65f,StudioUi.MUTED);
        super.extractRenderState(g,mx,my,a);
    }
    private final class SlotOpacitySlider extends AbstractSliderButton {
        SlotOpacitySlider(int x,int y,int w) {super(x,y,w,27,Component.empty(),slotOpacity);updateMessage();}
        @Override protected void updateMessage() {setMessage(Component.literal("Slot opacity  "+Math.round(slotOpacity*100)+"%"));}
        @Override protected void applyValue() {slotOpacity=(float)value;}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
            StudioUi.centered(g,getMessage().getString(),getX()+width/2f,getY(),width,.75f,StudioUi.TEXT);
            int y=getY()+17;
            StudioUi.box(g,getX(),y,width,3,0xff444444);
            StudioUi.box(g,getX(),y,(int)(value*width),3,0xffbbbbbb);
            StudioUi.box(g,getX()+(int)(value*(width-5)),y-3,5,9,0xffeeeeee);handleCursor(g);
        }
    }
}

package dev.hudstudio.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

public final class ModuleCard extends AbstractButton {
    private final HudConfig.Element element;
    private final Runnable open;
    private final java.util.function.BiConsumer<Integer,Integer> menu;
    private final int clipLeft,clipTop,clipRight,clipBottom;
    public ModuleCard(int x,int y,int width,int height,HudConfig.Element e,String[] description,Runnable open,
                      java.util.function.BiConsumer<Integer,Integer> menu,int left,int top,int right,int bottom) {
        super(x,y,width,height,Component.literal(e==null?"Add Slot":HudLayout.name(e)));
        this.element=e;this.open=open;this.menu=menu;
        clipLeft=left;clipTop=top;clipRight=right;clipBottom=bottom;
        setTooltip(Tooltip.create(Component.literal(e==null?"Add an inventory or armour display":String.join(" ",description))));
    }
    private boolean hasMenu() {return element!=null && (element.id.startsWith("slot_") || element.id.equals("offhand") || element.id.equals("effects"));}
    private int toggleWidth() {return Math.min(64,width-6);}
    private int toggleHeight() {return height>=30?18:13;}
    private int toggleX() {return getX()+(width-toggleWidth())/2;}
    private int toggleY() {return getY()+height-toggleHeight()-3;}
    private boolean inToggle(double x,double y) {
        return element!=null && x>=toggleX() && x<toggleX()+toggleWidth()
            && y>=toggleY() && y<toggleY()+toggleHeight();
    }
    private void toggle() {element.visible=!element.visible;HudStudioClient.config.save();}
    @Override public void onPress(InputWithModifiers input) {
        if(input instanceof MouseButtonEvent e) {
            if(inToggle(e.x(),e.y())) {toggle();return;}
            if(hasMenu() && e.x()>=getX()+width-13 && e.y()<getY()+13) {menu.accept((int)e.x(),(int)e.y());return;}
        }
        open.run();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(event.y()<clipTop || event.y()>=clipBottom) return false;
        if(hasMenu() && event.button()==org.lwjgl.sdl.SDLMouse.SDL_BUTTON_RIGHT && isMouseOver(event.x(),event.y())) {
            menu.accept((int)event.x(),(int)event.y());return true;
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean isMouseOver(double x,double y) {return y>=clipTop && y<clipBottom && super.isMouseOver(x,y);}
    @Override public boolean keyPressed(KeyEvent event) {
        if(element!=null && event.key()==InputConstants.KEY_SPACE) {toggle();return true;}
        return super.keyPressed(event);
    }
    private String shortName() {
        if(element==null) return "Add Slot";
        if(element.id.equals("effects")) return "Effects";
        if(element.id.equals("no_shake")) return "No Shake";
        if(element.id.startsWith("slot_") && element.slot<36)
            return element.slot<9?"Hotbar "+(element.slot+1):"Inventory "+(element.slot-8);
        return HudLayout.name(element);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float a) {
        g.enableScissor(clipLeft,clipTop,clipRight,clipBottom);
        int x=getX(),y=getY();boolean hovered=isMouseOver(mx,my);
        StudioUi.card(g,x,y,width,height,hovered || isFocused()?0xff929292:StudioUi.CARD);
        if(isFocused()) g.outline(x,y,width,height,StudioTheme.accent());
        String icon=element==null?"add":element.id;
        if(height>=54)StudioIcons.draw(g,icon,x+(width-16)/2,y+5);
        int titleY=height>=54?24:element==null?7:Math.max(1,Math.min(7,(toggleY()-y-8)/2));
        boolean inlineIcon=height>=40 && height<54 && width>=64;
        if(inlineIcon)StudioIcons.draw(g,icon,x+4,y+titleY-4);
        StudioUi.centered(g,shortName(),x+width/2f+(inlineIcon?8:0),y+titleY,width-(inlineIcon?24:8),1,0xff202020);
        if(element!=null) {
            StudioUi.button(g,toggleX(),toggleY(),toggleWidth(),toggleHeight(),element.visible?StudioTheme.accent():StudioUi.BUTTON_SELECTED,hovered && inToggle(mx,my));
            StudioUi.centered(g,element.visible?"ON":"OFF",x+width/2f,toggleY()+(toggleHeight()-7)/2f,toggleWidth()-2,1,element.visible?StudioTheme.textOn(StudioTheme.accent()):0xffbbbbbb);
        }
        if(hasMenu()) {
            for(int i=0;i<3;i++) g.fill(x+width-10+i*2,y+3,x+width-9+i*2,y+4,0xffaaaaaa);
        }
        g.disableScissor();
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
}

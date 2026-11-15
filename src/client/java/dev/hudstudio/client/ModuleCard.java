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
    public static final int SIZE=68;
    private final HudConfig.Element element;
    private final String[] description;
    private final Runnable open;
    private final java.util.function.BiConsumer<Integer,Integer> menu;
    private final int clipLeft,clipTop,clipRight,clipBottom;
    public ModuleCard(int x,int y,HudConfig.Element e,String[] description,Runnable open,
                      java.util.function.BiConsumer<Integer,Integer> menu,int left,int top,int right,int bottom) {
        super(x,y,SIZE,SIZE,Component.literal(e==null?"Add slot":HudLayout.name(e)));
        this.element=e;this.description=description;this.open=open;this.menu=menu;
        clipLeft=left;clipTop=top;clipRight=right;clipBottom=bottom;
        setTooltip(Tooltip.create(Component.literal(e==null?"Add an inventory or armour display":HudLayout.name(e)+
            (java.util.Arrays.asList(HudConfig.TWEAKS).contains(e.id)?" · Click to configure; Space toggles":" · Click to arrange; Space toggles visibility"))));
    }
    private boolean hasMenu() {return element!=null && (element.id.startsWith("slot_") || element.id.equals("offhand") || element.id.equals("effects"));}
    private boolean inToggle(double x,double y) {
        return element!=null && x>=getX()+14 && x<getX()+SIZE-14 && y>=getY()+53 && y<getY()+SIZE-2;
    }
    private void toggle() {element.visible=!element.visible;HudStudioClient.config.save();}
    @Override public void onPress(InputWithModifiers input) {
        if(input instanceof MouseButtonEvent e) {
            if(inToggle(e.x(),e.y())) {toggle();return;}
            if(hasMenu() && e.x()>=getX()+SIZE-13 && e.y()<getY()+13) {menu.accept((int)e.x(),(int)e.y());return;}
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
        if(element==null) return "Add slot";
        if(element.id.equals("effects")) return "Effects";
        if(element.id.equals("no_shake")) return "No shake";
        if(element.id.startsWith("slot_") && element.slot<36)
            return element.slot<9?"Hotbar "+(element.slot+1):"Inventory "+(element.slot-8);
        return HudLayout.name(element);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float a) {
        g.enableScissor(clipLeft,clipTop,clipRight,clipBottom);
        int x=getX(),y=getY();boolean hovered=isMouseOver(mx,my);
        StudioUi.card(g,x,y,SIZE,SIZE,hovered || isFocused()?0xff252525:StudioUi.CARD);
        if(isFocused()) g.outline(x,y,SIZE,SIZE,StudioTheme.accent());
        StudioIcons.draw(g,element==null?"add":element.id,x+(SIZE-16)/2,y+7);
        StudioUi.centered(g,shortName(),x+SIZE/2f,y+27,SIZE-8,.8f,StudioUi.TEXT);
        for(int i=0;i<Math.min(2,description.length);i++)
            StudioUi.centered(g,description[i],x+SIZE/2f,y+39+i*7,SIZE-8,.65f,StudioUi.MUTED);
        if(element!=null) {
            int tx=x+(SIZE-28)/2,ty=y+56;
            StudioUi.box(g,tx,ty,28,9,element.visible?StudioTheme.accent():0xff363636);
            StudioUi.centered(g,element.visible?"ON":"OFF",x+SIZE/2f,ty+2,26,.65f,element.visible?StudioTheme.textOn(StudioTheme.accent()):0xffbbbbbb);
        }
        if(hasMenu()) {
            for(int i=0;i<3;i++) g.fill(x+SIZE-10+i*2,y+5,x+SIZE-9+i*2,y+6,0xffaaaaaa);
        }
        g.disableScissor();
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
}

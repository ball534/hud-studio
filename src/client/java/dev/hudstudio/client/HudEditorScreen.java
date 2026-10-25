package dev.hudstudio.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.*;

/** A full-screen positioning canvas with only Back and Reset controls. */
public final class HudEditorScreen extends StudioScreen {
    private final HudConfig.Element selected;
    private boolean moving,resizing,shiftHeld;
    private int resizeCorner;
    private float grabX,grabY,startMouseX,startMouseY,startScale,anchorX,anchorY;
    private static final class ResizeCursors {
        static final CursorType RIGHT=CursorType.createStandardCursor(org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE,"hud_resize_right",CursorTypes.RESIZE_ALL);
        static final CursorType LEFT=CursorType.createStandardCursor(org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE,"hud_resize_left",CursorTypes.RESIZE_ALL);
    }
    public HudEditorScreen(Screen parent,HudConfig.Element selected) {super("Arrange HUD",parent);this.selected=selected;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float a) {
        if(minecraft.player==null) g.fill(0,0,width,height,0xff101010);
    }
    @Override protected void init() {
        clearWidgets();
        addRenderableWidget(new StudioUi.Action(width-100,height-24,44,18,"Back",this::onClose));
        var reset=addRenderableWidget(new StudioUi.Action(width-52,height-24,44,18,"Reset",()->{
            if(selected!=null) {selected.moved=false;selected.scale=1;HudStudioClient.config.save();}
        }));
        reset.active=selected!=null;
    }
    private boolean editable(HudConfig.Element e) {return e.visible || e==selected;}
    private static int corner(HudLayout.Bounds b,double x,double y) {
        if(Math.abs(y-(b.y()+b.h()))>5) return 0;
        double left=Math.abs(x-b.x()),right=Math.abs(x-b.x()-b.w());
        if(Math.min(left,right)>5) return 0;
        return left<right?-1:1;
    }
    private boolean overButton(double x,double y) {
        for(var child:children()) if(child.isMouseOver(x,y)) return true;return false;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        if(moving) {
            g.verticalLine(width/2,0,height,0x55777777);
            g.horizontalLine(0,width,height/2,0x55777777);
        }
        var drawOrder=HudLayout.all();
        drawOrder.remove(selected);
        if(selected!=null) drawOrder.add(selected);
        for(var e:drawOrder) {
            if(!editable(e)) continue;
            var b=HudLayout.bounds(e,width,height);
            boolean nativeElement=Arrays.asList(HudConfig.VANILLA).contains(e.id) && !e.id.equals("offhand");
            if(!nativeElement || e==selected) {
                HudLayout.begin(g,e,false);try {HudWidgets.drawLocal(g,e,true);} finally {HudLayout.end(g);}
            } else g.outline((int)b.x(),(int)b.y(),(int)b.w(),(int)b.h(),0x44777777);
            if(e==selected) {
                outline(g,b,0xffdddddd);
                handle(g,b.x()-6,b.y()+b.h()-5);handle(g,b.x()+b.w(),b.y()+b.h()-5);
                String label=HudLayout.name(e)+"  "+Math.round(e.scale*100)+"%";
                float cy=b.y()>=13?b.y()-12:b.y()+4;
                float cx=Math.clamp(b.x()+b.w()/2,50,Math.max(50,width-50));
                StudioUi.centered(g,label,cx+1,cy+1,width-12,.8f,0xff000000);
                StudioUi.centered(g,label,cx,cy,width-12,.8f,StudioUi.TEXT);
            }
        }
        super.extractRenderState(g,mx,my,a);
        if(resizing) g.requestCursor(resizeCorner<0?ResizeCursors.LEFT:ResizeCursors.RIGHT);
        else if(selected!=null && !overButton(mx,my)) {
            var b=HudLayout.bounds(selected,width,height);int c=corner(b,mx,my);
            if(c!=0) g.requestCursor(c<0?ResizeCursors.LEFT:ResizeCursors.RIGHT);
            else if(b.contains(mx,my)) g.requestCursor(CursorTypes.RESIZE_ALL);
        }
    }
    private static void rectangle(GuiGraphicsExtractor g,float x,float y,float w,float h,int color) {
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(w,h);
        g.fill(0,0,1,1,color);g.pose().popMatrix();
    }
    private static void outline(GuiGraphicsExtractor g,HudLayout.Bounds b,int color) {
        // Keep the same fractional position and extents as the rendered module.
        rectangle(g,b.x()-1,b.y()-1,b.w()+2,1,color);
        rectangle(g,b.x()-1,b.y()+b.h(),b.w()+2,1,color);
        rectangle(g,b.x()-1,b.y(),1,b.h(),color);
        rectangle(g,b.x()+b.w(),b.y(),1,b.h(),color);
    }
    private static void handle(GuiGraphicsExtractor g,float x,float y) {
        // Handles sit outside the image, so their backing cannot cut its bottom.
        rectangle(g,x,y,6,6,0xff111111);rectangle(g,x+1,y+1,4,4,0xffeeeeee);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(overButton(event.x(),event.y())) return super.mouseClicked(event,twice);
        if(selected==null || event.button()!=org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT) return false;
        shiftHeld=event.hasShiftDown() || physicalShift();
        var b=HudLayout.bounds(selected,width,height);int c=corner(b,event.x(),event.y());
        if(c!=0 || b.contains(event.x(),event.y())) {
            resizeCorner=c;resizing=c!=0;moving=!resizing;
            grabX=(float)event.x()-b.x();grabY=(float)event.y()-b.y();
            startMouseX=(float)event.x();startMouseY=(float)event.y();startScale=selected.scale;
            anchorX=c<0?b.x()+b.w():b.x();anchorY=b.y();
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if(selected==null || (!moving && !resizing)) return false;
        if(resizing) {
            var base=HudLayout.base(selected.id,width,height);
            float mx=((float)event.x()-startMouseX)*(resizeCorner<0?-1:1),my=(float)event.y()-startMouseY;
            float delta=(mx*base.w()+my*base.h())/(base.w()*base.w()+base.h()*base.h());
            selected.scale=Math.clamp(startScale+delta,.5f,3f);
            if(shiftHeld || physicalShift()) selected.scale=Math.round(selected.scale*20)/20f;
            HudLayout.move(selected,resizeCorner<0?anchorX-base.w()*selected.scale:anchorX,anchorY,width,height);
        } else {
            float x=(float)event.x()-grabX,y=(float)event.y()-grabY;
            if(HudStudioClient.config.snap && !shiftHeld && !physicalShift()) {
                var snapped=HudLayout.snap(selected,x,y,width,height);
                x=snapped.x();y=snapped.y();
            }
            HudLayout.move(selected,x,y,width,height);
        }
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled=moving || resizing;moving=false;resizing=false;
        if(handled) HudStudioClient.config.save();return super.mouseReleased(event) || handled;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.key()==InputConstants.KEY_LSHIFT || event.key()==InputConstants.KEY_RSHIFT) {shiftHeld=true;return true;}
        if(selected!=null) {
            int dx=event.key()==InputConstants.KEY_LEFT?-1:event.key()==InputConstants.KEY_RIGHT?1:0;
            int dy=event.key()==InputConstants.KEY_UP?-1:event.key()==InputConstants.KEY_DOWN?1:0;
            if(dx!=0 || dy!=0) {
                var b=HudLayout.bounds(selected,width,height);
                HudLayout.move(selected,b.x()+dx,b.y()+dy,width,height);HudStudioClient.config.save();return true;
            }
        }
        return super.keyPressed(event);
    }
    private static boolean physicalShift() {
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if(event.key()==InputConstants.KEY_LSHIFT || event.key()==InputConstants.KEY_RSHIFT) {
            shiftHeld=InputConstants.isKeyDown(event.key()==InputConstants.KEY_LSHIFT?InputConstants.KEY_RSHIFT:InputConstants.KEY_LSHIFT);
            return true;
        }
        return super.keyReleased(event);
    }
}

package dev.hudstudio.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.Locale;

public final class AppearanceScreen extends StudioScreen {
    private int left,hueY,planeY,size,previewX,previewWidth,drag;
    private float hue,saturation,value;
    private static final int[] PRESETS={0xff101010,0xffeeeeee,0xffe65d5d,0xffedb858,0xff70c18c,0xff669cdd,0xffa17cda,0xffdf83b5};
    public AppearanceScreen(Screen parent) {super("Global appearance",parent);}
    @Override protected void init() {
        clearWidgets();
        float[] hsv=StudioTheme.hsv(StudioTheme.color());hue=hsv[0];saturation=hsv[1];value=hsv[2];
        int w=Math.min(360,width-24);left=(width-w)/2;hueY=46;planeY=64;size=Math.min(116,height-108);
        previewX=left+size+20;previewWidth=left+w-previewX;
        addRenderableWidget(new OpacitySlider(previewX,142,previewWidth));
        addRenderableWidget(new StudioUi.Action(previewX,181,previewWidth,18,"Reset appearance",()->{
            HudStudioClient.config.themeColor=0xff101010;HudStudioClient.config.opacity=.85f;
            HudStudioClient.config.save();init();
        }));
        addRenderableWidget(new StudioUi.Action(width/2-30,height-24,60,18,"Back",this::onClose));
    }
    private void saveColor() {HudStudioClient.config.themeColor=StudioTheme.hsv(hue,saturation,value);HudStudioClient.config.save();}
    private void update(double mx,double my) {
        if(drag==1) hue=Math.clamp((float)(mx-left)/(size-1),0,.9999f);
        if(drag==2) {saturation=Math.clamp((float)(mx-left)/(size-1),0,1);value=1-Math.clamp((float)(my-planeY)/(size-1),0,1);}
        saveColor();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        StudioUi.centered(g,"Global appearance",width/2f,12,width-24,1f,StudioUi.TEXT);
        StudioUi.centered(g,"Choose a colour and opacity for your HUD panels.",width/2f,28,width-24,.7f,StudioUi.MUTED);
        for(int x=0;x<size;x+=2) g.fill(left+x,hueY,left+Math.min(x+2,size),hueY+10,StudioTheme.hsv(x/(float)size,1,1));
        for(int y=0;y<size;y+=4) for(int x=0;x<size;x+=4)
            g.fill(left+x,planeY+y,left+Math.min(x+4,size),planeY+Math.min(y+4,size),StudioTheme.hsv(hue,x/(float)(size-1),1-y/(float)(size-1)));
        g.outline(left-1,hueY-1,size+2,12,StudioUi.BORDER);g.outline(left-1,planeY-1,size+2,size+2,StudioUi.BORDER);
        int hx=left+Math.round(hue*(size-1)),sx=left+Math.round(saturation*(size-1)),sy=planeY+Math.round((1-value)*(size-1));
        g.outline(hx-2,hueY-2,5,14,0xff111111);g.outline(hx-1,hueY-1,3,12,0xffffffff);
        g.outline(sx-3,sy-3,7,7,0xff111111);g.outline(sx-2,sy-2,5,5,0xffffffff);
        int swatch=Math.max(8,(size-14)/8);
        for(int i=0;i<PRESETS.length;i++) {
            int x=left+i*(swatch+2),y=planeY+size+8;
            g.fill(x,y,x+swatch,y+12,PRESETS[i]);g.outline(x,y,swatch,12,PRESETS[i]==StudioTheme.color()?0xffffffff:0xff555555);
        }
        StudioUi.card(g,previewX,46,previewWidth,80,StudioUi.PANEL);
        StudioUi.centered(g,"Preview",previewX+previewWidth/2f,51,previewWidth-8,.65f,StudioUi.MUTED);
        g.pose().pushMatrix();g.pose().translate(previewX+(previewWidth-51.2f)/2,62);g.pose().scale(.8f,.8f);
        HudWidgets.drawLocal(g,HudStudioClient.config.elements.get("wasd"),true);
        g.pose().translate(0,48);HudWidgets.drawLocal(g,HudStudioClient.config.elements.get("fps"),true);g.pose().popMatrix();
        StudioUi.centered(g,String.format(Locale.ROOT,"#%06X",StudioTheme.color()&0xffffff),previewX+previewWidth/2f,132,previewWidth,.75f,StudioUi.MUTED);
        super.extractRenderState(g,mx,my,a);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice) {
        if(e.button()!=org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT) return super.mouseClicked(e,twice);
        if(e.x()>=left && e.x()<left+size) {
            if(e.y()>=hueY && e.y()<hueY+10) drag=1;
            else if(e.y()>=planeY && e.y()<planeY+size) drag=2;
            if(drag!=0) {update(e.x(),e.y());return true;}
            int swatch=Math.max(8,(size-14)/8);
            if(e.y()>=planeY+size+8 && e.y()<planeY+size+20) {
                int index=(int)(e.x()-left)/(swatch+2);
                if(index<PRESETS.length) {
                    HudStudioClient.config.themeColor=PRESETS[index];HudStudioClient.config.save();
                    float[] hsv=StudioTheme.hsv(PRESETS[index]);hue=hsv[0];saturation=hsv[1];value=hsv[2];return true;
                }
            }
        }
        return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy) {
        if(drag!=0) {update(e.x(),e.y());return true;}return super.mouseDragged(e,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {drag=0;return super.mouseReleased(e);}
    private static final class OpacitySlider extends AbstractSliderButton {
        OpacitySlider(int x,int y,int w) {super(x,y,w,30,Component.empty(),HudStudioClient.config.opacity);updateMessage();}
        @Override protected void updateMessage() {setMessage(Component.literal("Opacity  "+Math.round(HudStudioClient.config.opacity*100)+"%"));}
        @Override protected void applyValue() {HudStudioClient.config.opacity=(float)value;HudStudioClient.config.save();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
            StudioUi.centered(g,getMessage().getString(),getX()+width/2f,getY(),width,.8f,StudioUi.TEXT);
            int y=getY()+18;StudioUi.box(g,getX(),y,width,3,0xff444444);
            StudioUi.box(g,getX(),y,(int)(value*width),3,StudioTheme.accent());
            StudioUi.box(g,getX()+(int)(value*(width-5)),y-3,5,9,0xffeeeeee);handleCursor(g);
        }
    }
}

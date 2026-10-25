package dev.hudstudio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public final class StudioUi {
    public static final int BG=0xff090909,PANEL=0xff121212,CARD=0xff191919,BORDER=0xff353535,TEXT=0xffeeeeee,MUTED=0xff999999;
    public static void box(GuiGraphicsExtractor g,int x,int y,int w,int h,int color) {
        if(w>0 && h>0) g.fill(x,y,x+w,y+h,color);
    }
    public static void card(GuiGraphicsExtractor g,int x,int y,int w,int h,int fill) {
        box(g,x,y,w,h,BORDER);box(g,x+1,y+1,w-2,h-2,fill);
    }
    public static void text(GuiGraphicsExtractor g,String s,int x,int y,int color) {
        g.text(Minecraft.getInstance().font,s,x,y,color,false);
    }
    public static void centered(GuiGraphicsExtractor g,String s,float cx,float y,int maxWidth,float scale,int color) {
        var font=Minecraft.getInstance().font;
        int max=(int)(maxWidth/scale);
        if(font.width(s)>max) s=font.plainSubstrByWidth(s,Math.max(0,max-10))+"...";
        g.pose().pushMatrix();g.pose().translate(cx,y);g.pose().scale(scale,scale);
        // Use a fractional translation so odd-width strings also align precisely.
        g.pose().translate(-font.width(s)/2f,0);text(g,s,0,0,color);g.pose().popMatrix();
    }
    public static class Action extends AbstractButton {
        private final Runnable action;
        public Action(int x,int y,int w,int h,String label,Runnable action) {
            super(x,y,w,h,Component.literal(label));this.action=action;
        }
        @Override public void onPress(InputWithModifiers input) {action.run();}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float a) {
            card(g,getX(),getY(),width,height,isHoveredOrFocused()?0xff292929:CARD);
            centered(g,getMessage().getString(),getX()+width/2f,getY()+(height-7)/2f,width-8,.8f,active?TEXT:0xff666666);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
    }
}

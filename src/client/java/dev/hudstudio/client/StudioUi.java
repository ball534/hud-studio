package dev.hudstudio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public final class StudioUi {
    public static final int BG=0xff2a2a2a,PANEL=0xff464646,CARD=0xff7e7e7e,BORDER=0xff2e2e2e,TEXT=0xffeeeeee,MUTED=0xffd0d0d0;
    public static final int BUTTON=0xff6b6b6b,BUTTON_HOVER=0xff7e7e7e,BUTTON_SELECTED=0xff555555;
    public static void box(GuiGraphicsExtractor g,int x,int y,int w,int h,int color) {
        if(w>0 && h>0) g.fill(x,y,x+w,y+h,color);
    }
    public static void card(GuiGraphicsExtractor g,int x,int y,int w,int h,int fill) {
        box(g,x,y,w,h,BORDER);
        box(g,x+1,y+1,w-2,h-2,fill);
        box(g,x+1,y+1,w-2,1,0x30ffffff);
        box(g,x+1,y+2,1,h-4,0x22ffffff);
        box(g,x+1,y+h-2,w-2,1,0x66000000);
        box(g,x+w-2,y+2,1,h-4,0x55000000);
    }
    public static void diamond(GuiGraphicsExtractor g,int cx,int cy,int radius,int color) {
        for(int dy=-radius;dy<=radius;dy++){int half=radius-Math.abs(dy);g.fill(cx-half,cy+dy,cx+half+1,cy+dy+1,color);}
    }
    public static void button(GuiGraphicsExtractor g,int x,int y,int w,int h,int fill,boolean hovered) {
        card(g,x,y,w,h,fill);
        if(hovered)g.outline(x,y,w,h,0xffffffff);
    }
    public static void text(GuiGraphicsExtractor g,String s,int x,int y,int color) {
        g.text(Minecraft.getInstance().font,s,x,y,color,false);
    }
    public static void centered(GuiGraphicsExtractor g,String s,float cx,float y,int maxWidth,float scale,int color) {
        var font=Minecraft.getInstance().font;
        int max=Math.max(0,maxWidth);
        if(font.width(s)>max) s=font.plainSubstrByWidth(s,Math.max(0,max-10))+"...";
        // Bitmap glyphs must land on whole GUI pixels at their native scale.
        text(g,s,Math.round(cx-font.width(s)/2f),Math.round(y),color);
    }
    public static class Action extends AbstractButton {
        private final Runnable action;
        protected int textColor() {return active?TEXT:0xff666666;}
        public Action(int x,int y,int w,int h,String label,Runnable action) {
            super(x,y,w,h,Component.literal(label));this.action=action;
        }
        @Override public void onPress(InputWithModifiers input) {action.run();}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float a) {
            drawButton(g);
            drawLabel(g,getX(),width);
        }
        protected void drawButton(GuiGraphicsExtractor g) {
            button(g,getX(),getY(),width,height,isHoveredOrFocused()?BUTTON_HOVER:BUTTON,isHovered());
        }
        protected void drawLabel(GuiGraphicsExtractor g,int left,int availableWidth) {
            String label=getMessage().getString();
            var font=Minecraft.getInstance().font;
            if(font.width(label)>availableWidth-8)label=font.plainSubstrByWidth(label,Math.max(0,availableWidth-18))+"...";
            g.text(font,label,left+Math.round((availableWidth-font.width(label))/2f),getY()+Math.round((height-7)/2f),textColor(),true);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
    }
}

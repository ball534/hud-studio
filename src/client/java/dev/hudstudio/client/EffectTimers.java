package dev.hudstudio.client;

import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class EffectTimers {
    private EffectTimers() {}

    public static String format(int ticks) {
        if(ticks<0)return "\u221e";
        long seconds=(ticks+19L)/20;
        return seconds/60+":"+String.format(java.util.Locale.ROOT,"%02d",seconds%60);
    }

    public static void label(GuiGraphicsExtractor g,int center,int y,String text) {
        // Keep text inside the icon, including long and infinite durations.
        var font=Minecraft.getInstance().font;
        float scale=Math.min(.65f,22f/Math.max(1,font.width(text)));
        g.pose().pushMatrix();
        try {g.pose().translate(center,y);g.pose().scale(scale,scale);g.centeredText(font,text,0,0,0xffffffff);}
        finally {g.pose().popMatrix();}
    }

    public static void draw(GuiGraphicsExtractor g) {
        var mc=Minecraft.getInstance();
        if(!HudStudioClient.config.effectsShowTime || mc.player==null
            || (mc.gui.screen()!=null && mc.gui.screen().showsActiveEffects()))return;
        var effects=new ArrayList<>(mc.player.getActiveEffects());
        effects.sort(Collections.reverseOrder());
        int beneficial=0,harmful=0;
        // Match vanilla's ordering, hidden-icon filtering, rows and demo offset.
        for(var effect:effects) {
            if(!effect.showIcon())continue;
            boolean good=effect.getEffect().value().isBeneficial();
            int column=good?++beneficial:++harmful;
            int x=g.guiWidth()-25*column;
            int y=1+(mc.isDemo()?15:0)+(good?0:26);
            label(g,x+12,y+17,format(effect.getDuration()));
        }
    }
}

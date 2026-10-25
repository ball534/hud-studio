package dev.hudstudio.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small monochrome module icons drawn with the same sharp pixels as the HUD. */
public final class StudioIcons {
    private static final int C=0xffdddddd,D=0xff777777;
    private static void r(GuiGraphicsExtractor g,int x,int y,int w,int h,int c) {g.fill(x,y,x+w,y+h,c);}
    public static void draw(GuiGraphicsExtractor g,String id,int x,int y) {
        g.pose().pushMatrix();g.pose().translate(x,y);
        switch(id) {
            case "scoreboard" -> {for(int i=0;i<3;i++){r(g,0,i*5,2,2,C);r(g,4,i*5,11,2,C);}}
            case "bossbar" -> {g.outline(0,2,16,5,C);r(g,2,4,9,1,C);r(g,3,10,10,2,D);}
            case "player_tab" -> {for(int i=0;i<3;i++){r(g,1,i*5,3,3,C);r(g,6,i*5+1,9,1,C);}}
            case "effects" -> {r(g,6,0,4,2,C);r(g,6,3,4,4,D);g.outline(3,6,10,9,C);r(g,5,10,6,3,D);}
            case "offhand" -> {g.outline(2,1,12,9,C);r(g,4,10,8,2,C);r(g,6,12,4,2,C);}
            case "wasd" -> {key(g,6,0);key(g,0,6);key(g,6,6);key(g,12,6);}
            case "mouse","left_cps","right_cps" -> {
                g.outline(3,0,10,16,C);r(g,7,1,1,7,C);r(g,4,7,8,1,C);
                if(id.equals("left_cps"))r(g,4,2,3,4,C);if(id.equals("right_cps"))r(g,8,2,3,4,C);
            }
            case "space" -> {g.outline(0,5,16,7,C);r(g,3,8,10,1,C);}
            case "sneak" -> {r(g,7,1,3,3,C);r(g,5,6,5,2,C);r(g,3,7,2,6,C);r(g,5,11,7,2,C);}
            case "sprint" -> {r(g,10,0,3,3,C);r(g,7,4,5,2,C);r(g,6,6,3,4,C);r(g,3,11,5,2,C);r(g,10,9,2,6,C);r(g,0,5,4,1,D);}
            case "fps" -> {r(g,1,10,3,5,C);r(g,6,6,3,9,C);r(g,11,1,3,14,C);}
            case "speed" -> {g.outline(0,5,16,10,D);r(g,7,8,2,5,C);r(g,9,5,2,5,C);r(g,3,3,10,1,C);}
            case "clock" -> {g.outline(1,1,14,14,C);r(g,7,3,1,6,C);r(g,7,8,5,1,C);}
            case "memory" -> {g.outline(3,3,10,10,C);r(g,6,6,4,4,D);for(int i=4;i<13;i+=4){r(g,i,0,1,3,C);r(g,i,13,1,3,C);r(g,0,i,3,1,C);r(g,13,i,3,1,C);}}
            case "add" -> {r(g,7,1,2,14,C);r(g,1,7,14,2,C);}
            case "combo" -> {r(g,2,10,3,5,C);r(g,6,6,3,9,C);r(g,10,1,4,14,C);r(g,0,1,5,2,D);}
            case "zoom" -> {g.outline(1,1,10,10,C);r(g,10,10,3,3,C);r(g,13,13,3,3,C);r(g,5,3,1,6,D);r(g,3,5,6,1,D);}
            case "no_shake" -> {r(g,7,1,2,14,C);r(g,0,7,16,2,C);g.outline(4,4,8,8,D);}
            case "freecam","freelook" -> {g.outline(1,3,14,10,C);r(g,6,6,4,4,D);r(g,3,1,5,2,C);}
            case "no_explosions" -> {r(g,6,3,4,10,D);r(g,3,6,10,4,D);for(int i=0;i<15;i++)r(g,i,14-i,2,1,C);}
            default -> {for(int row=0;row<3;row++)for(int col=0;col<3;col++)g.outline(col*5,row*5,4,4,C);}
        }
        g.pose().popMatrix();
    }
    private static void key(GuiGraphicsExtractor g,int x,int y) {g.outline(x,y,4,4,C);}
}

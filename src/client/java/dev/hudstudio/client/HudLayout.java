package dev.hudstudio.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;

public final class HudLayout {
    public record Bounds(float x, float y, float w, float h) {
        public boolean contains(double px,double py) { return px>=x && py>=y && px<=x+w && py<=y+h; }
    }
    public static Bounds base(String id,int w,int h) {
        return switch(id) {
            case "scoreboard" -> new Bounds(w-126,h/2f-45,122,90);
            case "bossbar" -> new Bounds(w/2f-91,12,182,19);
            case "player_tab" -> new Bounds(w/2f-140,10,280,120);
            case "effects" -> new Bounds(w-86,1,86,26);
            case "offhand" -> new Bounds(net.minecraft.client.Minecraft.getInstance().player!=null
                && net.minecraft.client.Minecraft.getInstance().player.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT?w/2f+98:w/2f-120,h-22,22,22);
            case "wasd" -> new Bounds(8,8,64,42);
            case "mouse" -> new Bounds(8,52,64,20);
            case "space" -> new Bounds(8,74,64,20);
            case "sneak" -> new Bounds(8,96,64,20);
            case "sprint" -> new Bounds(8,118,64,20);
            case "fps" -> new Bounds(8,140,64,20);
            case "left_cps" -> new Bounds(8,162,64,20);
            case "right_cps" -> new Bounds(8,184,64,20);
            case "speed" -> new Bounds(8,206,64,20);
            case "clock" -> new Bounds(8,228,64,20);
            case "memory" -> new Bounds(8,250,64,20);
            case "combo" -> new Bounds(w-158,28,140,60);
            default -> new Bounds(w/2f-144,h-22,22,22);
        };
    }
    public static Bounds bounds(HudConfig.Element e,int w,int h) {
        Bounds b=base(e.id,w,h); float bw=b.w*e.scale,bh=b.h*e.scale;
        // Smaller windows keep the independent stats in a second column.
        float defaultX=b.x, defaultY=b.y;
        if(Arrays.asList(HudConfig.STATS).contains(e.id) && defaultY+bh>h-26) {
            defaultX=78; defaultY=8+Arrays.asList(HudConfig.STATS).indexOf(e.id)*22;
        }
        return new Bounds(e.moved?e.x*Math.max(0,w-bw):Math.clamp(defaultX,0,Math.max(0,w-bw)),
            e.moved?e.y*Math.max(0,h-bh):Math.clamp(defaultY,0,Math.max(0,h-bh)),bw,bh);
    }
    public static void move(HudConfig.Element e,float x,float y,int w,int h) {
        Bounds b=base(e.id,w,h); e.moved=true;
        e.x=Math.clamp(x/Math.max(1,w-b.w*e.scale),0,1);
        e.y=Math.clamp(y/Math.max(1,h-b.h*e.scale),0,1);
    }
    public record Position(float x,float y) {}
    public record SlotNeighbours(boolean left,boolean right,boolean up,boolean down) {}
    public static SlotNeighbours slotNeighbours(HudConfig.Element e,int w,int h,boolean preview) {
        var b=bounds(e,w,h);boolean left=false,right=false,up=false,down=false;
        for(var other:HudStudioClient.config.slots) {
            if(other==e || !other.visible || Math.abs(e.scale-other.scale)>.001f
                || (!preview && other.hideEmpty && HudWidgets.stack(other.slot).isEmpty())) continue;
            var target=bounds(other,w,h);
            float stride=20*e.scale;
            if(Math.abs(target.y-b.y)<.1f) {
                left|=Math.abs(target.x+stride-b.x)<.1f;
                right|=Math.abs(b.x+stride-target.x)<.1f;
            }
            if(Math.abs(target.x-b.x)<.1f) {
                up|=Math.abs(target.y+stride-b.y)<.1f;
                down|=Math.abs(b.y+stride-target.y)<.1f;
            }
        }
        return new SlotNeighbours(left,right,up,down);
    }
    /** Align visible key rows and module edges, including scaled modules. */
    public static Position snap(HudConfig.Element selected,float x,float y,int w,int h) {
        // Join complete slot frames at their shared two-pixel dividers.
        if(selected.id.startsWith("slot_")) {
            Position joined=null;float best=Float.MAX_VALUE;
            for(var other:HudStudioClient.config.slots) {
                if(other==selected || !other.visible || Math.abs(selected.scale-other.scale)>.001f) continue;
                var target=bounds(other,w,h);float stride=20*selected.scale,tolerance=5*selected.scale;
                for(int[] direction:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                    float candidate=target.x+direction[0]*stride,candidateY=target.y+direction[1]*stride;
                    float dx=Math.abs(x-candidate),dy=Math.abs(y-candidateY);
                    if(dx<=tolerance && dy<=tolerance && dx+dy<best) {
                        boolean occupied=false;
                        for(var slot:HudStudioClient.config.slots) {
                            if(slot==selected || !slot.visible) continue;
                            var occupiedBounds=bounds(slot,w,h);
                            if(Math.abs(occupiedBounds.x-candidate)<.1f && Math.abs(occupiedBounds.y-candidateY)<.1f) {occupied=true;break;}
                        }
                        if(!occupied) {joined=new Position(candidate,candidateY);best=dx+dy;}
                    }
                }
            }
            if(joined!=null) return joined;
        }
        var b=base(selected.id,w,h);
        float sw=b.w*selected.scale,sh=b.h*selected.scale;
        float sx=Math.round(x/2)*2,sy=Math.round(y/2)*2;
        float bestX=3,bestY=3;
        if(Math.abs(x+sw/2-w/2f)<bestX) {sx=w/2f-sw/2;bestX=Math.abs(sx-x);}
        if(Math.abs(y+sh/2-h/2f)<bestY) {sy=h/2f-sh/2;bestY=Math.abs(sy-y);}
        for(var other:all()) {
            if(other==selected || !other.visible) continue;
            var target=bounds(other,w,h);
            float gap=2*Math.max(selected.scale,other.scale);
            float reach=24*Math.max(selected.scale,other.scale);
            if(y<=target.y+target.h+reach && y+sh>=target.y-reach) {
                for(float candidate:new float[]{target.x,target.x+target.w-sw,target.x-sw-gap,target.x+target.w+gap}) {
                    float distance=Math.abs(candidate-x);
                    if(distance<bestX) {bestX=distance;sx=candidate;}
                }
            }
            if(x<=target.x+target.w+reach && x+sw>=target.x-reach) {
                for(float sourceRow:rowOffsets(selected)) for(float targetRow:rowOffsets(other)) {
                    float sourceHeight=selected.id.equals("wasd")?20*selected.scale:sh;
                    float targetHeight=other.id.equals("wasd")?20*other.scale:target.h;
                    float top=target.y+targetRow;
                    for(float candidate:new float[]{top-sourceRow,top+targetHeight-sourceHeight-sourceRow,
                            top-sourceHeight-gap-sourceRow,top+targetHeight+gap-sourceRow}) {
                        float distance=Math.abs(candidate-y);
                        if(distance<bestY) {bestY=distance;sy=candidate;}
                    }
                }
            }
        }
        return new Position(sx,sy);
    }
    private static float[] rowOffsets(HudConfig.Element e) {
        return e.id.equals("wasd")?new float[]{0,22*e.scale}:new float[]{0};
    }
    public static void begin(GuiGraphicsExtractor g,HudConfig.Element e,boolean vanilla) {
        Bounds b=base(e.id,g.guiWidth(),g.guiHeight()),t=bounds(e,g.guiWidth(),g.guiHeight());
        g.pose().pushMatrix(); g.pose().translate(t.x,t.y);g.pose().scale(e.scale,e.scale);
        if(vanilla) g.pose().translate(-b.x,-b.y);
    }
    public static void end(GuiGraphicsExtractor g) {g.pose().popMatrix();}
    public static String name(HudConfig.Element e) {
        if(e.id.startsWith("slot_")) return HudWidgets.slotName(e.slot);
        return switch(e.id) {
            case "scoreboard" -> "Scoreboard";
            case "bossbar" -> "Boss Bars";
            case "player_tab" -> "Player Tab";
            case "effects" -> "Status Effects";
            case "offhand" -> "Offhand";
            case "wasd" -> "WASD";
            case "mouse" -> "LMB / RMB";
            case "space" -> "SPACE";
            case "sneak" -> "SNEAK";
            case "sprint" -> "SPRINT";
            case "fps" -> "FPS";
            case "left_cps" -> "Left CPS";
            case "right_cps" -> "Right CPS";
            case "speed" -> "Speed";
            case "clock" -> "Clock";
            case "memory" -> "Memory";
            case "combo" -> "Combo Meter";
            case "zoom" -> "Zoom";
            case "no_shake" -> "No Camera Shake";
            case "freecam" -> "Freecam";
            case "no_explosions" -> "No Explosions";
            case "freelook" -> "Freelook";
            case "full_bright" -> "Full Bright";
            default -> e.id;
        };
    }
    public static List<HudConfig.Element> all() {
        List<HudConfig.Element> a=new ArrayList<>(HudStudioClient.config.elements.values());
        a.addAll(HudStudioClient.config.slots); return a;
    }
}

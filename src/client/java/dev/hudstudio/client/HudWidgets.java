package dev.hudstudio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.ArrayDeque;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class HudWidgets {
    public static final Identifier INVENTORY=Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final ArrayDeque<Long> LEFT=new ArrayDeque<>(),RIGHT=new ArrayDeque<>();
    public static void click(int button) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null && mc.gui.screen()==null) {
            var queue=button==org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT?LEFT:button==org.lwjgl.sdl.SDLMouse.SDL_BUTTON_RIGHT?RIGHT:null;
            if(queue!=null) {cps(queue);queue.addLast(System.currentTimeMillis());}
        }
    }
    private static int cps(ArrayDeque<Long> q) {
        long now=System.currentTimeMillis();while(!q.isEmpty() && q.peekFirst()<now-1000) q.removeFirst();return q.size();
    }
    public static String slotName(int slot) {
        return switch(slot) {
            case 36 -> "Boots";case 37 -> "Leggings";case 38 -> "Chestplate";case 39 -> "Helmet";case 40 -> "Offhand";
            default -> slot<9?"Hotbar "+(slot+1):"Inventory · Row "+((slot-9)/9+1)+", Slot "+((slot-9)%9+1);
        };
    }
    public static ItemStack stack(int slot) {
        var p=Minecraft.getInstance().player;if(p==null) return ItemStack.EMPTY;
        return switch(slot) {
            case 36 -> p.getItemBySlot(EquipmentSlot.FEET);case 37 -> p.getItemBySlot(EquipmentSlot.LEGS);
            case 38 -> p.getItemBySlot(EquipmentSlot.CHEST);case 39 -> p.getItemBySlot(EquipmentSlot.HEAD);
            case 40 -> p.getOffhandItem();default -> p.getInventory().getItem(slot);
        };
    }
    public static String statistic(String id,boolean preview) {
        var mc=Minecraft.getInstance();var p=mc.player;
        return switch(id) {
            case "fps" -> (preview && p==null?144:mc.getFps())+" FPS";
            case "left_cps" -> cps(LEFT)+" CPS";
            case "right_cps" -> cps(RIGHT)+" CPS";
            case "speed" -> String.format(java.util.Locale.ROOT,"%.1f m/s",p==null?0:Math.hypot(p.getX()-p.xo,p.getZ()-p.zo)*20);
            case "clock" -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            case "memory" -> {Runtime r=Runtime.getRuntime();yield "Mem "+((r.totalMemory()-r.freeMemory())*100/r.maxMemory())+"%";}
            default -> "";
        };
    }
    public static void customSlot(GuiGraphicsExtractor g,ItemStack item,float opacity) {
        customSlot(g,item,opacity,new HudLayout.SlotNeighbours(false,false,false,false));
    }
    private static void customSlot(GuiGraphicsExtractor g,ItemStack item,float opacity,HudLayout.SlotNeighbours joins) {
        var texture=Identifier.withDefaultNamespace("textures/gui/sprites/hud/hotbar.png");
        int color=StudioTheme.alpha(0xffffffff,opacity);
        // Shared pixels belong to the left/upper cell, applying opacity only once.
        int[] offsets={0,2,20},sizes={2,18,2};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
            if(col==0 && joins.left() || row==0 && joins.up()) continue;
            float u=col==0?0:col==1?22:joins.right()?40:180;
            float v=row==0?0:row==1?2:joins.down()?19:20;
            g.blit(RenderPipelines.GUI_TEXTURED,texture,offsets[col],offsets[row],u,v,sizes[col],sizes[row],182,22,color);
        }
        if(!item.isEmpty()) {g.item(item,3,3);g.itemDecorations(Minecraft.getInstance().font,item,3,3);}
    }
    public static void offhandSlot(GuiGraphicsExtractor g,ItemStack item,boolean rightHand) {
        // The vanilla texture has transparent padding; crop it to the visible frame.
        g.blit(RenderPipelines.GUI_TEXTURED,Identifier.withDefaultNamespace(rightHand?
            "textures/gui/sprites/hud/hotbar_offhand_right.png":"textures/gui/sprites/hud/hotbar_offhand_left.png"),
            0,0,rightHand?7f:0f,1f,22,22,29,24,0xffffffff);
        if(!item.isEmpty()) {g.item(item,3,3);g.itemDecorations(Minecraft.getInstance().font,item,3,3);}
    }
    public static boolean vanillaOffhand() {
        var e=HudStudioClient.config.elements.get("offhand");
        return e.visible && !e.moved && e.scale==1 && e.hideEmpty;
    }
    public static void draw(GuiGraphicsExtractor g,boolean preview) {
        var mc=Minecraft.getInstance();
        if(!preview && (mc.player==null || mc.gui.screen() instanceof StudioScreen || mc.gui.hud.isHidden())) return;
        for(var e:HudLayout.all()) {
            if(!e.visible && !preview) continue;
            if(!preview && e.id.equals("offhand") && vanillaOffhand()) continue;
            if(!preview && java.util.Arrays.asList(HudConfig.VANILLA).contains(e.id) && !e.id.equals("offhand")) continue;
            if((e.id.equals("offhand") || e.id.startsWith("slot_")) && e.hideEmpty && stack(e.id.equals("offhand")?40:e.slot).isEmpty() && !preview) continue;
            HudLayout.begin(g,e,false);try {drawLocal(g,e,preview);} finally {HudLayout.end(g);}
        }
    }
    public static void drawLocal(GuiGraphicsExtractor g,HudConfig.Element e,boolean preview) {
        var mc=Minecraft.getInstance();var o=mc.options;
        switch(e.id) {
            case "wasd" -> {
                tile(g,22,0,20,20,key(o.keyUp),o.keyUp.isDown());
                tile(g,0,22,20,20,key(o.keyLeft),o.keyLeft.isDown());
                tile(g,22,22,20,20,key(o.keyDown),o.keyDown.isDown());
                tile(g,44,22,20,20,key(o.keyRight),o.keyRight.isDown());
            }
            case "mouse" -> {tile(g,0,0,31,20,"LMB",mc.mouseHandler.isLeftPressed());tile(g,33,0,31,20,"RMB",mc.mouseHandler.isRightPressed());}
            case "space" -> tile(g,0,0,64,20,"SPACE",o.keyJump.isDown());
            case "sneak" -> tile(g,0,0,64,20,"SNEAK",o.keyShift.isDown());
            case "sprint" -> tile(g,0,0,64,20,"SPRINT",o.keySprint.isDown());
            case "fps","left_cps","right_cps","speed","clock","memory" -> tile(g,0,0,64,20,statistic(e.id,preview),false);
            case "combo" -> ComboMeter.draw(g,preview);
            case "scoreboard" -> {
                g.fill(0,0,122,90,0x99000000);g.centeredText(mc.font,"Scoreboard",61,6,0xffffffff);
                for(int i=0;i<5;i++) {StudioUi.text(g,"Player "+(i+1),6,24+i*12,0xffdddddd);StudioUi.text(g,""+(10-i),104,24+i*12,0xffaaaaaa);}
            }
            case "bossbar" -> {
                g.centeredText(mc.font,"Boss bar",91,0,0xffffffff);
                g.fill(0,13,182,19,0xff333333);g.fill(1,14,138,18,0xffdddddd);
            }
            case "player_tab" -> {
                g.fill(0,0,280,120,0xaa000000);g.centeredText(mc.font,"Player Tab",140,6,0xffffffff);
                for(int i=0;i<12;i++) StudioUi.text(g,"Player "+(i+1),8+(i/6)*140,24+(i%6)*15,0xffdddddd);
            }
            case "effects" -> {
                for(int i=0;i<3;i++) {
                    StudioUi.card(g,i*30,0,26,26,0xcc161616);g.item(new ItemStack(Items.POTION),i*30+5,5);
                    if(HudStudioClient.config.effectsShowTime)EffectTimers.label(g,i*30+13,18,i==2?"\u221e":i==0?"2:30":"0:45");
                }
            }
            default -> {
                if(e.id.equals("offhand") || e.id.startsWith("slot_")) {
                    var item=stack(e.id.equals("offhand")?40:e.slot);
                    boolean right=e.id.equals("offhand") && mc.player!=null && mc.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT;
                    if(e.id.startsWith("slot_")) {
                        var neighbours=HudLayout.slotNeighbours(e,g.guiWidth(),g.guiHeight(),preview);
                        customSlot(g,item,e.opacity,neighbours);
                    }
                    else offhandSlot(g,item,right);
                }
            }
        }
    }
    private static String key(net.minecraft.client.KeyMapping k) {return k.getTranslatedKeyMessage().getString();}
    private static void tile(GuiGraphicsExtractor g,int x,int y,int w,int h,String label,boolean pressed) {
        int background=StudioTheme.color();
        int foreground=StudioTheme.textOn(background);
        StudioUi.box(g,x,y,w,h,StudioTheme.alpha(pressed?foreground:background));
        StudioUi.centered(g,label,x+w/2f,y+(h-8)/2f,w-6,1f,pressed?0xff000000|(background&0xffffff):foreground);
    }
}

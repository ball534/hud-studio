package dev.hudstudio.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.*;

public final class HudSettingsScreen extends StudioScreen {
    private static final int GAP=6,STEP=ModuleCard.SIZE+GAP,SIDEBAR=96;
    private static final String[] CATEGORIES={"Game HUD","Inventory","Keystrokes","Stats","Camera","Visuals"};
    private int category,scroll,sidebarX,left,gridWidth,gridTop,gridBottom,totalHeight,columns;
    private String query="";
    private EditBox search;
    private final List<GuiEventListener> cards=new ArrayList<>();
    private HudConfig.Element context;
    private int menuX,menuY,menuHeight;
    public HudSettingsScreen(Screen parent) {super("ball's hud studio",parent);}
    @Override protected void init() {
        clearWidgets();cards.clear();
        int available=Math.min(600,width-24);
        columns=Math.max(1,(available-SIDEBAR-12+GAP)/STEP);
        gridWidth=columns*STEP-GAP;
        int bodyWidth=SIDEBAR+12+gridWidth;
        sidebarX=(width-bodyWidth)/2;left=sidebarX+SIDEBAR+12;gridTop=58;gridBottom=height-30;
        for(int i=0;i<CATEGORIES.length;i++) {
            final int index=i;
            addRenderableWidget(new StudioUi.Action(sidebarX,gridTop+i*27,SIDEBAR,22,CATEGORIES[i],()->{
                category=index;scroll=0;context=null;updateHint();refreshCards();
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float a) {
                    StudioUi.card(g,getX(),getY(),getWidth(),getHeight(),category==index?0xff292929:isHoveredOrFocused()?0xff222222:StudioUi.PANEL);
                    if(category==index) StudioUi.box(g,getX(),getY(),2,getHeight(),StudioTheme.accent());
                    StudioUi.centered(g,getMessage().getString(),getX()+getWidth()/2f,getY()+7,getWidth()-10,.8f,category==index?StudioUi.TEXT:StudioUi.MUTED);
                }
            });
        }
        addRenderableWidget(new StudioUi.Action(sidebarX,32,SIDEBAR,20,"Appearance",()->minecraft.gui.setScreen(new AppearanceScreen(this))));
        search=addRenderableWidget(new EditBox(font,left+7,38,gridWidth-14,12,Component.literal("Search modules")));
        search.setBordered(false);search.setTextColor(StudioUi.TEXT);search.setMaxLength(80);
        search.setValue(query);updateHint();
        search.setResponder(value->{query=value;scroll=0;context=null;refreshCards();});
        addRenderableWidget(new StudioUi.Action(width/2-30,height-23,60,18,"Back",this::onClose));
        refreshCards();
    }
    private void updateHint() {search.setHint(Component.literal("Search "+CATEGORIES[category]+"...").withStyle(ChatFormatting.GRAY));}
    private String[] description(HudConfig.Element e) {
        if(e.id.startsWith("slot_")) return new String[]{e.slot>=36?"Equipment":"Inventory","slot display"};
        return switch(e.id) {
            case "scoreboard" -> new String[]{"Sidebar","and scores"};
            case "bossbar" -> new String[]{"Boss health","and events"};
            case "player_tab" -> new String[]{"Player list","overlay"};
            case "effects" -> new String[]{"Active status","effects"};
            case "offhand" -> new String[]{"Offhand","item display"};
            case "wasd" -> new String[]{"Movement","keys"};
            case "mouse" -> new String[]{"Mouse","buttons"};
            case "space" -> new String[]{"Jump","input"};
            case "sneak" -> new String[]{"Sneak","input"};
            case "sprint" -> new String[]{"Sprint","input"};
            case "fps" -> new String[]{"Frames per","second"};
            case "left_cps" -> new String[]{"Left clicks","per second"};
            case "right_cps" -> new String[]{"Right clicks","per second"};
            case "speed" -> new String[]{"Movement","speed"};
            case "clock" -> new String[]{"Local","time"};
            case "memory" -> new String[]{"Heap","usage"};
            case "combo" -> new String[]{"Style ranks","and hit streaks"};
            case "zoom" -> new String[]{"Hold to zoom","Adjustable lens"};
            case "no_shake" -> new String[]{"No view bob","or damage tilt"};
            case "freecam" -> new String[]{"Detached camera","Explore nearby"};
            case "no_explosions" -> new String[]{"Hide explosion","particles"};
            case "freelook" -> new String[]{"Hold to orbit","Keep your aim"};
            case "full_bright" -> new String[]{"Maximum gamma","See in the dark"};
            default -> new String[]{"HUD","display"};
        };
    }
    private boolean matches(String text) {
        String needle=query.strip().toLowerCase(Locale.ROOT),haystack=text.toLowerCase(Locale.ROOT);
        return needle.isEmpty() || haystack.contains(needle);
    }
    private List<HudConfig.Element> entries() {
        List<HudConfig.Element> result=new ArrayList<>();
        if(category==1 && matches("Add slot inventory armour")) result.add(null);
        String[] ids=switch(category) {
            case 0->new String[]{"scoreboard","bossbar","effects","player_tab","combo"};
            case 1->new String[]{"offhand"};
            case 2->new String[]{"wasd","mouse","left_cps","right_cps","space","sneak","sprint"};
            case 3->new String[]{"fps","speed","clock","memory"};
            case 4->new String[]{"zoom","freelook","freecam"};
            default->new String[]{"no_shake","no_explosions","full_bright"};
        };
        List<HudConfig.Element> source=new ArrayList<>();
        for(String id:ids)source.add((category>=4?HudStudioClient.config.tweaks:HudStudioClient.config.elements).get(id));
        if(category==1)source.addAll(HudStudioClient.config.slots);
        for(var e:source) if(matches(HudLayout.name(e)+" "+e.id+" "+String.join(" ",description(e)))) result.add(e);
        return result;
    }
    private <T extends net.minecraft.client.gui.components.AbstractWidget> void addCard(T widget) {
        cards.add(widget);addRenderableWidget(widget);
    }
    private void refreshCards() {
        // Keep the EditBox instance, focus, selection and caret intact during live filtering.
        for(var widget:cards) removeWidget(widget);
        cards.clear();
        var all=entries();int count=all.size();
        totalHeight=count==0?0:((count+columns-1)/columns)*STEP-GAP;
        scroll=Math.clamp(scroll,0,Math.max(0,totalHeight-(gridBottom-gridTop)));
        for(int i=0;i<count;i++) {
            var e=all.get(i);int x=left+(i%columns)*STEP,y=gridTop+(i/columns)*STEP-scroll;
            if(y+ModuleCard.SIZE<=gridTop || y>=gridBottom) continue;
            Runnable open=e==null?()->minecraft.gui.setScreen(new SlotPickerScreen(this,null)):category>=4?
                ()->minecraft.gui.setScreen(new TweakSettingsScreen(this,e)):()->minecraft.gui.setScreen(new HudEditorScreen(this,e));
            addCard(new ModuleCard(x,y,e,e==null?new String[]{"Inventory or","armour"}:description(e),open,
                (mx,my)->{context=e;menuX=Math.clamp(mx,left,Math.max(left,left+gridWidth-114));menuY=my;refreshCards();},
                left,gridTop,left+gridWidth,gridBottom));
        }
        if(context!=null) {
            if(context.id.equals("effects")) {
                menuHeight=20;menuY=Math.clamp(menuY,gridTop,Math.max(gridTop,gridBottom-menuHeight));
                addCard(new StudioUi.Action(menuX,menuY,114,20,"Time remaining: "+(HudStudioClient.config.effectsShowTime?"ON":"OFF"),()->{
                    HudStudioClient.config.effectsShowTime=!HudStudioClient.config.effectsShowTime;
                    context=null;HudStudioClient.config.save();refreshCards();
                }));
                return;
            }
            boolean slot=context.id.startsWith("slot_");menuHeight=slot?60:20;
            menuY=Math.clamp(menuY,gridTop,Math.max(gridTop,gridBottom-menuHeight));int y=menuY;
            if(slot) {addCard(new StudioUi.Action(menuX,y,114,20,"Change slot",()->{var e=context;context=null;minecraft.gui.setScreen(new SlotPickerScreen(this,e));}));y+=20;}
            addCard(new StudioUi.Action(menuX,y,114,20,context.hideEmpty?"Show empty slot":"Hide empty slot",()->{context.hideEmpty=!context.hideEmpty;context=null;HudStudioClient.config.save();refreshCards();}));y+=20;
            if(slot) addCard(new StudioUi.Action(menuX,y,114,20,"Remove display",()->{HudStudioClient.config.slots.remove(context);context=null;HudStudioClient.config.save();refreshCards();}));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        StudioUi.centered(g,"ball's hud studio",width/2f,12,width-20,1f,StudioUi.TEXT);
        StudioUi.card(g,left,32,gridWidth,20,StudioUi.PANEL);
        if(search.isFocused()) g.outline(left,32,gridWidth,20,StudioTheme.accent());
        if(totalHeight==0) {
            StudioUi.centered(g,"No matching modules",left+gridWidth/2f,gridTop+34,gridWidth-12,.8f,StudioUi.TEXT);
            StudioUi.centered(g,"Try another search or category.",left+gridWidth/2f,gridTop+50,gridWidth-12,.7f,StudioUi.MUTED);
        }
        super.extractRenderState(g,mx,my,a);
        if(totalHeight>gridBottom-gridTop) {
            int track=gridBottom-gridTop,thumb=Math.max(12,track*track/totalHeight);
            int y=gridTop+(int)((track-thumb)*(scroll/(float)Math.max(1,totalHeight-track)));
            StudioUi.box(g,left+gridWidth+5,gridTop,2,track,0xff222222);
            StudioUi.box(g,left+gridWidth+5,y,2,thumb,0xff777777);
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice) {
        if(context!=null) {
            if(e.x()<menuX || e.x()>=menuX+114 || e.y()<menuY || e.y()>=menuY+menuHeight) {context=null;refreshCards();return true;}
            var widgets=children();
            for(int i=widgets.size()-1;i>=0;i--) if(widgets.get(i) instanceof StudioUi.Action a && a.isMouseOver(e.x(),e.y())) return a.mouseClicked(e,twice);
        }
        return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy) {
        if(x>=left && x<=left+gridWidth && y>=gridTop && y<gridBottom) {context=null;scroll-=(int)(sy*26);refreshCards();return true;}
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public boolean keyPressed(KeyEvent e) {
        if(context!=null && e.isEscape()) {context=null;refreshCards();return true;}return super.keyPressed(e);
    }
}

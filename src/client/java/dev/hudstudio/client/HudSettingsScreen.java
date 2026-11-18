package dev.hudstudio.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public class HudSettingsScreen extends StudioScreen {
    private static final int GAP=4,SIDEBAR=80;
    private static final String[] CATEGORIES={"HUD","Inventory","Camera","Visuals","Waypoints"};
    private int category,sidebarX,left,gridWidth,gridTop,gridBottom,totalHeight,columns;
    private int cardWidth,cardHeight;
    private final List<GuiEventListener> cards=new ArrayList<>();
    private HudConfig.Element context;
    private dev.hudstudio.client.navigation.WaypointPanel waypointPanel;
    private int menuX,menuY,menuHeight;
    public HudSettingsScreen(Screen parent) {super("Ball's HUD Studio",parent);}
    protected HudSettingsScreen(Screen parent,int category) {this(parent);this.category=category;}
    public dev.hudstudio.client.navigation.WaypointPanel waypointPanel() {return waypointPanel;}
    @Override protected void init() {
        clearWidgets();cards.clear();waypointPanel=null;
        int available=Math.min(600,width-24);
        int sidebar=SIDEBAR;
        gridWidth=available-sidebar-12;
        int bodyWidth=sidebar+12+gridWidth;
        sidebarX=(width-bodyWidth)/2;left=sidebarX+sidebar+12;gridTop=32;gridBottom=height-34;
        int count=Math.max(16,HudStudioClient.config.slots.size()+2),bestArea=0;
        for(int cols=1;cols<=count;cols++) {
            int rows=(count+cols-1)/cols;
            int cw=Math.min(96,(gridWidth-(cols-1)*GAP)/cols),ch=Math.min(60,(gridBottom-gridTop-(rows-1)*GAP)/rows);
            int area=cw*ch;
            if(cw>=40 && ch>=30 && area>bestArea){bestArea=area;columns=cols;cardWidth=cw;cardHeight=ch;}
        }
        if(bestArea==0){columns=Math.max(1,gridWidth/44);cardWidth=(gridWidth-(columns-1)*GAP)/columns;cardHeight=Math.max(20,(gridBottom-gridTop-GAP*((count+columns-1)/columns-1))/((count+columns-1)/columns));}
        // All sections share the Waypoints frame. Fill that content width with a
        // common grid, even when the selected category contains only a few cards.
        columns=Math.max(Math.min(4,Math.max(1,(gridWidth+GAP)/(40+GAP))),columns);
        cardWidth=(gridWidth-(columns-1)*GAP)/columns;
        int rows=(count+columns-1)/columns;
        cardHeight=Math.min(60,(gridBottom-gridTop-(rows-1)*GAP)/rows);
        for(int i=0;i<CATEGORIES.length;i++) {
            final int index=i;
            addRenderableWidget(new StudioUi.Action(sidebarX,58+i*27,sidebar,22,CATEGORIES[i],()->{
                if(category==index)return;category=index;context=null;init();
            }) {
                @Override protected void drawButton(GuiGraphicsExtractor g) {
                    StudioUi.button(g,getX(),getY(),getWidth(),getHeight(),category==index?StudioUi.BUTTON_SELECTED:isHoveredOrFocused()?StudioUi.BUTTON_HOVER:StudioUi.BUTTON,isHovered());
                }
            });
        }
        addRenderableWidget(new StudioUi.Action(sidebarX,32,sidebar,20,"Appearance",()->minecraft.gui.setScreen(new AppearanceScreen(this))));
        if(category==4) {
            waypointPanel=new dev.hudstudio.client.navigation.WaypointPanel(this,w->addRenderableWidget(w),this::removeWidget);
            waypointPanel.init(left,32,gridWidth,height-28);
        }
        addRenderableWidget(new StudioUi.Action(sidebarX,height-28,sidebar,20,"Back",this::onClose));
        if(category!=4)refreshCards();
    }
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
            case "zoom" -> new String[]{"Zoom in for a closer view"};
            case "no_shake" -> new String[]{"No view bob","or damage tilt"};
            case "freecam" -> new String[]{"Move the camera independently of your player"};
            case "no_explosions" -> new String[]{"Hide explosion","particles"};
            case "freelook" -> new String[]{"Look around without changing your aim"};
            case "full_bright" -> new String[]{"Brighten dark areas"};
            default -> new String[]{"HUD","display"};
        };
    }
    private List<HudConfig.Element> entries() {
        List<HudConfig.Element> result=new ArrayList<>();
        if(category==1) result.add(null);
        String[] ids=switch(category) {
            case 0->new String[]{"scoreboard","bossbar","effects","player_tab","combo","wasd","mouse","left_cps","right_cps","space","sneak","sprint","fps","speed","clock","memory"};
            case 1->new String[]{"offhand"};
            case 2->new String[]{"zoom","freelook","freecam"};
            case 3->new String[]{"no_shake","no_explosions","full_bright"};
            default->new String[]{};
        };
        List<HudConfig.Element> source=new ArrayList<>();
        for(String id:ids)source.add((category>=2?HudStudioClient.config.tweaks:HudStudioClient.config.elements).get(id));
        if(category==1)source.addAll(HudStudioClient.config.slots);
        result.addAll(source);
        return result;
    }
    private <T extends net.minecraft.client.gui.components.AbstractWidget> void addCard(T widget) {
        cards.add(widget);addRenderableWidget(widget);
    }
    private void refreshCards() {
        for(var widget:cards) removeWidget(widget);
        cards.clear();
        var all=entries();int count=all.size();
        totalHeight=count==0?0:((count+columns-1)/columns)*(cardHeight+GAP)-GAP;
        for(int i=0;i<count;i++) {
            var e=all.get(i);int x=left+(gridWidth-columns*(cardWidth+GAP)+GAP)/2+(i%columns)*(cardWidth+GAP),y=gridTop+(i/columns)*(cardHeight+GAP);
            Runnable open=e==null?()->minecraft.gui.setScreen(new SlotPickerScreen(this,null)):category>=2?
                ()->minecraft.gui.setScreen(new TweakSettingsScreen(this,e)):()->minecraft.gui.setScreen(new HudEditorScreen(this,e));
            addCard(new ModuleCard(x,y,cardWidth,cardHeight,e,e==null?new String[]{"Inventory or","armour"}:description(e),open,
                (mx,my)->{context=e;menuX=Math.clamp(mx,left,Math.max(left,left+gridWidth-114));menuY=my;refreshCards();},
                left,gridTop,left+gridWidth,gridBottom));
        }
        if(context!=null) {
            if(context.id.equals("effects")) {
                menuHeight=20;menuY=Math.clamp(menuY,gridTop,Math.max(gridTop,gridBottom-menuHeight));
                addCard(new StudioUi.Action(menuX,menuY,114,20,"Time Remaining: "+(HudStudioClient.config.effectsShowTime?"ON":"OFF"),()->{
                    HudStudioClient.config.effectsShowTime=!HudStudioClient.config.effectsShowTime;
                    context=null;HudStudioClient.config.save();refreshCards();
                }));
                return;
            }
            boolean slot=context.id.startsWith("slot_");menuHeight=slot?60:20;
            menuY=Math.clamp(menuY,gridTop,Math.max(gridTop,gridBottom-menuHeight));int y=menuY;
            if(slot) {addCard(new StudioUi.Action(menuX,y,114,20,"Change Slot",()->{var e=context;context=null;minecraft.gui.setScreen(new SlotPickerScreen(this,e));}));y+=20;}
            addCard(new StudioUi.Action(menuX,y,114,20,context.hideEmpty?"Show Empty Slot":"Hide Empty Slot",()->{context.hideEmpty=!context.hideEmpty;context=null;HudStudioClient.config.save();refreshCards();}));y+=20;
            if(slot) addCard(new StudioUi.Action(menuX,y,114,20,"Remove Display",()->{HudStudioClient.config.slots.remove(context);context=null;HudStudioClient.config.save();refreshCards();}));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        StudioUi.centered(g,"Ball's HUD Studio",width/2f,12,width-20,1f,StudioUi.TEXT);
        if(waypointPanel!=null){waypointPanel.draw(g);super.extractRenderState(g,mx,my,a);return;}
        if(totalHeight==0) {
            StudioUi.centered(g,"No modules",left+gridWidth/2f,gridTop+34,gridWidth-12,1,StudioUi.TEXT);
        }
        super.extractRenderState(g,mx,my,a);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice) {
        if(waypointPanel!=null && waypointPanel.click(e))return true;
        if(context!=null) {
            if(e.x()<menuX || e.x()>=menuX+114 || e.y()<menuY || e.y()>=menuY+menuHeight) {context=null;refreshCards();return true;}
            var widgets=children();
            for(int i=widgets.size()-1;i>=0;i--) if(widgets.get(i) instanceof StudioUi.Action a && a.isMouseOver(e.x(),e.y())) return a.mouseClicked(e,twice);
        }
        return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy) {
        if(waypointPanel!=null)return waypointPanel.scroll(x,y,sy) || super.mouseScrolled(x,y,sx,sy);
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public boolean keyPressed(KeyEvent e) {
        if(waypointPanel!=null && !waypointPanel.searchFocused()) {
            if(dev.hudstudio.client.navigation.Navigation.listKey.matches(e)){onClose();return true;}
            if(dev.hudstudio.client.navigation.Navigation.createKey.matches(e) && dev.hudstudio.client.navigation.Navigation.store()!=null){minecraft.gui.setScreen(new dev.hudstudio.client.navigation.WaypointEditorScreen(this,dev.hudstudio.client.navigation.Navigation.newWaypoint(),false));return true;}
            if(dev.hudstudio.client.navigation.Navigation.mapKey.matches(e) && dev.hudstudio.client.navigation.Navigation.store()!=null){minecraft.gui.setScreen(new dev.hudstudio.client.navigation.WorldMapScreen(this));return true;}
        }
        if(context!=null && e.isEscape()) {context=null;refreshCards();return true;}return super.keyPressed(e);
    }
    @Override public void tick(){if(waypointPanel!=null)waypointPanel.tick();}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(waypointPanel!=null && waypointPanel.drag(e))return true;return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(waypointPanel!=null)waypointPanel.release();return super.mouseReleased(e);}
}

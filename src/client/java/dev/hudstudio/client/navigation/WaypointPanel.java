package dev.hudstudio.client.navigation;

import dev.hudstudio.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

/** Shared management content for the Studio category and the U key. */
public final class WaypointPanel {
    private final Screen host;
    private final WaypointStore world;
    private final Consumer<AbstractWidget> add;
    private final Consumer<GuiEventListener> remove;
    private final List<AbstractWidget> rows=new ArrayList<>();
    private String query="",error="",dimension=Navigation.dimension();
    private boolean currentDimension=true,dragging;
    private int left,width,top,bottom,header,searchWidth;
    private double scroll;
    private EditBox search;
    private List<Waypoint> shown=List.of();
    public WaypointPanel(Screen host,Consumer<AbstractWidget> add,Consumer<GuiEventListener> remove) {
        this.host=host;this.add=add;this.remove=remove;world=Navigation.store();
    }
    public void init(int left,int header,int width,int panelBottom) {
        this.left=left;this.width=width;this.header=header;searchWidth=width-104;top=header+26;rows.clear();
        var mc=Minecraft.getInstance();
        search=new EditBox(mc.font,left+7,header+6,searchWidth-14,12,Component.literal("Search waypoints"));
        search.setBordered(false);search.setTextColor(StudioUi.TEXT);
        search.setMaxLength(64);search.setValue(query);search.setHint(Component.literal("Search waypoints..."));
        search.setResponder(v->{query=v;scroll=0;rebuild();});add.accept(search);
        add.accept(new StudioUi.Action(left+width-98,header,98,20,"Dimension",()->{currentDimension=!currentDimension;scroll=0;rebuild();}) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial) {
                setMessage(Component.literal(currentDimension?Navigation.dimensionName(dimension):"All Dimensions"));super.extractContents(g,mx,my,partial);
            }
        });
        String[] labels={"Enabled","Show Names","Show Distances"};
        int newWidth=mc.font.width("New Waypoint")+12;
        int[] sizes=Arrays.stream(labels).mapToInt(s->mc.font.width(s)+24).toArray();
        boolean compact=newWidth+Arrays.stream(sizes).sum()+6>width;
        if(compact)Arrays.fill(sizes,18);
        int footerLeft=left+(width-newWidth-Arrays.stream(sizes).sum()-6)/2;
        int footer=panelBottom;bottom=footer-6;
        var create=new StudioUi.Action(footerLeft,footer,newWidth,20,"New Waypoint",()->mc.gui.setScreen(new WaypointEditorScreen(host,Navigation.newWaypoint(),false)));
        create.active=world!=null && mc.player!=null;add.accept(create);
        int x=footerLeft+newWidth+2;
        var c=HudStudioClient.config;
        for(int i=0;i<3;i++) {
            int size=sizes[i];
            final int index=i;
            BooleanSupplier get=index==0?()->c.waypointsEnabled:index==1?()->c.waypointShowName:()->c.waypointShowDistance;
            Runnable toggle=()->{if(index==0)c.waypointsEnabled=!c.waypointsEnabled;else if(index==1)c.waypointShowName=!c.waypointShowName;else c.waypointShowDistance=!c.waypointShowDistance;c.save();};
            String label=labels[i];
            var checkbox=new StudioUi.Action(x,footer,size,20,label,toggle) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial) {
                    drawButton(g);
                    int checkX=getX()+(compact?(getWidth()-8)/2:6);
                    int checkY=getY()+(getHeight()-8)/2;
                    g.outline(checkX,checkY,8,8,StudioUi.MUTED);
                    if(get.getAsBoolean())g.fill(checkX+2,checkY+2,checkX+6,checkY+6,StudioUi.TEXT);
                    if(!compact)drawLabel(g,getX()+12,getWidth()-12);
                }
            };checkbox.setTooltip(Tooltip.create(Component.literal(label)));add.accept(checkbox);x+=size+2;
        }
        rebuild();
    }
    public boolean searchFocused(){return search!=null && search.isFocused();}
    public double scrollOffset(){return scroll;}
    public int filteredCount(){return shown.size();}
    private double maxScroll(){return Math.max(0,shown.size()*26-(bottom-top));}
    private void rebuild() {
        rows.forEach(remove);rows.clear();
        shown=world==null?List.of():world.all().stream().filter(w->!currentDimension || w.dimension().equals(dimension))
            .filter(w->w.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
            .sorted(Comparator.comparing((Waypoint w)->!w.visible()).thenComparing(Waypoint::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Waypoint::id)).toList();
        scroll=Math.clamp(scroll,0,maxScroll());
        for(int i=0;i<shown.size();i++) {
            var w=shown.get(i);int y=top+i*26-(int)scroll;if(y+24<=top || y>=bottom)continue;
            rowButton(left+width-120,y+3,30,"Edit",()->Minecraft.getInstance().gui.setScreen(new WaypointEditorScreen(host,w,true)));
            rowButton(left+width-86,y+3,36,w.visible()?"Hide":"Show",()->{if(!world.put(w.visibility(!w.visible())))error="Could not save changes.";rebuild();});
            rowButton(left+width-46,y+3,40,"Delete",()->{if(!world.delete(w.id()))error="Could not delete waypoint.";rebuild();});
        }
    }
    private void rowButton(int x,int y,int size,String label,Runnable action) {
        var button=new StudioUi.Action(x,y,size,18,label,action) {
            @Override protected int textColor(){return label.equals("Delete") && isHovered()?0xffff5555:super.textColor();}
            @Override public boolean isMouseOver(double mx,double my){return my>=top && my<bottom && super.isMouseOver(mx,my);}
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial){g.enableScissor(left,top,left+WaypointPanel.this.width,bottom);super.extractContents(g,mx,my,partial);g.disableScissor();}
        };rows.add(button);add.accept(button);
    }
    public void tick(){if(!dimension.equals(Navigation.dimension())){dimension=Navigation.dimension();currentDimension=true;scroll=0;rebuild();}}
    public boolean scroll(double x,double y,double amount){if(x<left || x>left+width || y<top || y>=bottom)return false;scroll=Math.clamp(scroll-amount*26,0,maxScroll());rebuild();return true;}
    public boolean click(MouseButtonEvent event){if(event.button()==1 && maxScroll()>0 && event.x()>=left+width+2 && event.x()<=left+width+8 && event.y()>=top && event.y()<bottom){dragging=true;moveThumb(event.y());return true;}return false;}
    private void moveThumb(double y){scroll=Math.clamp((y-top)/(bottom-top)*maxScroll(),0,maxScroll());rebuild();}
    public boolean drag(MouseButtonEvent event){if(!dragging)return false;moveThumb(event.y());return true;}
    public void release(){dragging=false;}
    public void draw(GuiGraphicsExtractor g) {
        StudioUi.card(g,left,header,searchWidth,20,StudioUi.PANEL);
        if(search.isFocused())g.outline(left,header,searchWidth,20,StudioTheme.accent());
        var font=Minecraft.getInstance().font;g.enableScissor(left,top,left+width,bottom);
        for(int i=0;i<shown.size();i++) {
            int y=top+i*26-(int)scroll;if(y+24<=top || y>=bottom)continue;var w=shown.get(i);
            StudioUi.card(g,left,y,width,24,StudioUi.PANEL);StudioUi.diamond(g,left+10,y+11,4,w.color());
            int coordX=left+30+Math.clamp((width-160)/3,36,110),nameWidth=Math.max(12,coordX-left-32);
            String coords="X: "+w.x()+", Y: "+w.y()+", Z: "+w.z();
            var name=Component.literal(font.plainSubstrByWidth(w.name(),nameWidth)).withStyle(style->style.withItalic(!w.visible()));
            g.text(font,name,left+20,y+8,w.visible()?StudioUi.TEXT:StudioUi.MUTED,false);
            StudioUi.text(g,font.plainSubstrByWidth(coords,Math.max(0,left+width-128-coordX)),coordX,y+8,StudioUi.MUTED);
        }
        g.disableScissor();
        if(shown.isEmpty())StudioUi.centered(g,world==null?"Join a world first":"No matching waypoints",left+width/2f,top+20,width,1,StudioUi.MUTED);
        if(maxScroll()>0){int track=bottom-top,thumb=Math.max(16,(int)(track*(track/(double)(shown.size()*26))));StudioUi.box(g,left+width+3,top,3,track,StudioUi.BORDER);StudioUi.box(g,left+width+3,top+(int)((track-thumb)*scroll/maxScroll()),3,thumb,StudioUi.TEXT);}
        if(world!=null && !world.readable())error="Cannot read waypoint file.";
        if(!error.isEmpty())StudioUi.centered(g,error,left+width/2f,bottom-10,width,1,0xffff7777);
    }
}

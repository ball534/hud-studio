package dev.hudstudio.client.navigation;

import dev.hudstudio.client.StudioUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Inventory-sized draft; closing the dialog never commits changes. */
public final class WaypointEditorScreen extends NavigationScreen {
    private final Waypoint original;
    private final boolean editing;
    private String name,x,y,z,error="";
    private int color,left,top;
    private EditBox nameBox;
    private StudioUi.Action save;
    public WaypointEditorScreen(Screen parent,Waypoint waypoint,boolean editing) {
        super(editing?"Edit Waypoint":"New Waypoint",parent);original=waypoint;this.editing=editing;
        name=waypoint.name();x=""+waypoint.x();y=""+waypoint.y();z=""+waypoint.z();color=waypoint.color();
    }
    @Override protected void init() {
        clearWidgets();left=(width-176)/2;top=(height-166)/2;
        nameBox=field(left+8,top+35,160,"Name",name,v->{name=v;validate();},64);
        field(left+8,top+69,48,"X",x,v->{x=v;validate();},10);
        field(left+64,top+69,48,"Y",y,v->{y=v;validate();},10);
        field(left+120,top+69,48,"Z",z,v->{z=v;validate();},10);
        for(int i=0;i<Navigation.COLORS.size();i++) {
            var choice=Navigation.COLORS.get(i);int rgb=Navigation.bannerColor(choice);
            String label=Navigation.dimensionName(choice.getName());
            var swatch=addRenderableWidget(new StudioUi.Action(left+8+i%8*20,top+103+i/8*16,16,12,label,()->color=rgb) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float a) {
                    g.fill(getX(),getY(),getX()+width,getY()+height,rgb);
                    if(color==rgb || isHoveredOrFocused())g.outline(getX()-1,getY()-1,width+2,height+2,0xffffffff);
                }
            });swatch.setTooltip(Tooltip.create(Component.literal(label)));
        }
        addRenderableWidget(new StudioUi.Action(left+8,top+140,68,18,"Cancel",this::onClose));
        save=addRenderableWidget(new StudioUi.Action(left+100,top+140,68,18,"Save",this::save));
        setInitialFocus(nameBox);if(!editing && name.equals("Waypoint"))nameBox.setHighlightPos(0);validate();
    }
    private EditBox field(int x,int y,int width,String label,String value,java.util.function.Consumer<String> changed,int max) {
        var box=addRenderableWidget(new EditBox(font,x,y,width,16,Component.literal(label)));
        box.setMaxLength(max);box.setValue(value);box.setResponder(changed);return box;
    }
    private static Integer coordinate(String value) {
        try {if(!value.matches("-?\\d+"))return null;int n=Integer.parseInt(value);return WaypointStore.validCoordinate(n)?n:null;}
        catch(NumberFormatException e){return null;}
    }
    private void validate() {
        error=!world.readable()?"Cannot read waypoint file.":name.isBlank()?"Enter a name.":coordinate(x)==null || coordinate(y)==null || coordinate(z)==null?"Use whole numbers within +/-30,000,000.":"";
        if(save!=null)save.active=error.isEmpty();
    }
    private void save() {
        validate();if(!error.isEmpty())return;
        var waypoint=new Waypoint(original.id(),name.strip(),color,coordinate(x),coordinate(y),coordinate(z),original.dimension(),original.visible());
        if(world.put(waypoint))onClose();else error="Could not save waypoint.";
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN){save();return true;}
        return super.keyPressed(event);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial) {StudioUi.card(g,left,top,176,166,StudioUi.PANEL);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        StudioUi.centered(g,editing?"Edit Waypoint":"New Waypoint",width/2f,top+8,160,1,StudioUi.TEXT);
        StudioUi.text(g,"Name",left+8,top+23,StudioUi.MUTED);
        StudioUi.text(g,"X",left+8,top+57,StudioUi.MUTED);StudioUi.text(g,"Y",left+64,top+57,StudioUi.MUTED);StudioUi.text(g,"Z",left+120,top+57,StudioUi.MUTED);
        StudioUi.text(g,"Colour",left+8,top+91,StudioUi.MUTED);
        if(!error.isEmpty()) {
            var lines=font.split(Component.literal(error),176);int y=top+172;
            g.fill(left-4,y-3,left+180,y+lines.size()*10+3,0xee090909);
            for(var line:lines){g.text(font,line,left,y,0xffff7777,false);y+=10;}
        }
        super.extractRenderState(g,mx,my,partial);
    }
}

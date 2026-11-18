package dev.hudstudio.client.navigation;

import dev.hudstudio.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.util.*;

/** A 2:1 vanilla parchment map with transparent surroundings and mouse/key navigation. */
public final class WorldMapScreen extends NavigationScreen {
    private static final int EDGE_PADDING=16;
    private static final int PAPER_BORDER=8;
    private static final Identifier PAPER=Identifier.withDefaultNamespace("textures/map/map_background.png");
    private static final Identifier PLAYER=Identifier.withDefaultNamespace("textures/map/decorations/player.png");
    private static final Map<Integer,Identifier> BANNERS=new HashMap<>();
    private static final int MENU_ROW_HEIGHT=16;
    private final MapAtlas atlas;
    private String dimension;
    private double centerX,centerZ,zoom=1;
    private int frameLeft,frameTop,frameWidth,frameHeight,mapLeft,mapTop,mapRight,mapBottom;
    private boolean dragging;
    private final Map<String,Float> labelOpacity=new HashMap<>();
    private long labelTime=System.nanoTime();
    private final FogOutline fogOutline=new FogOutline();
    private long openedAt,copiedAt;
    private float mapOpacity=1;
    private int mapSlide;
    private String copiedCoordinates="";
    private final List<StudioUi.Action> popup=new ArrayList<>();
    public WorldMapScreen(Screen parent) {
        super("World Map",parent);atlas=Navigation.atlas();dimension=Navigation.dimension();
        var player=net.minecraft.client.Minecraft.getInstance().player;centerX=player.getX();centerZ=player.getZ();
    }
    @Override protected void init() {
        clearWidgets();popup.clear();dragging=false;
        openedAt=System.nanoTime();labelTime=openedAt;labelOpacity.clear();
        frameHeight=Math.max(PAPER_BORDER*2+1,Math.min((width-EDGE_PADDING*2)/2,height-EDGE_PADDING*2));
        frameWidth=frameHeight*2;frameLeft=(width-frameWidth)/2;frameTop=(height-frameHeight)/2;
        mapLeft=frameLeft+PAPER_BORDER;mapTop=frameTop+PAPER_BORDER;
        mapRight=frameLeft+frameWidth-PAPER_BORDER;mapBottom=frameTop+frameHeight-PAPER_BORDER;
        zoom=Math.clamp(zoom,minZoom(),4);
        addRenderableWidget(new StudioUi.Action(frameLeft+frameWidth-23,frameTop+8,15,15,"x",this::onClose) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial) {
                boolean hot=isHovered();
                for(int row=0;row<height;row++) {
                    int shade=40-row*32/(height-1);
                    g.fill(getX(),getY()+row,getX()+width,getY()+row+1,mapColor(hot?0x80ff3333:0x80000000|shade<<16|shade<<8|shade));
                }
                // The font advance includes a trailing blank pixel; centre the ink.
                mapText(g,"x",getX()+Math.round((width-font.width("x")+1)/2f),getY()+(height-7)/2-1,hot?0xff000000:0xffff3333);
            }
        });
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial) {}
    @Override public void tick() {super.tick();if(!dimension.equals(Navigation.dimension()))closePopup();dimension=Navigation.dimension();}
    private double minZoom() {
        int mapWidth=Math.max(1,mapRight-mapLeft),mapHeight=Math.max(1,mapBottom-mapTop);
        return Math.max(.125,Math.max(Math.sqrt((double)mapWidth*mapHeight)/(MapAtlas.SIZE*12),Math.max(mapWidth,mapHeight)/(MapAtlas.SIZE*24.0)));
    }
    private double mapCenterX() {return (mapLeft+mapRight)/2.0;}
    private double mapCenterY() {return (mapTop+mapBottom)/2.0;}
    public double blockX(double screenX) {return centerX+(screenX-mapCenterX())/zoom;}
    public double blockZ(double screenY) {return centerZ+(screenY-mapCenterY())/zoom;}
    private int screenX(double x) {return (int)Math.round(mapCenterX()+(x-centerX)*zoom);}
    private int screenY(double z) {return (int)Math.round(mapCenterY()+(z-centerZ)*zoom);}
    public double zoom() {return zoom;}
    private boolean inside(double x,double y) {return x>=mapLeft && x<mapRight && y>=mapTop && y<mapBottom;}
    private List<Waypoint> markers() {return world.visible(dimension);}
    private Waypoint marker(double x,double y) {
        if(!inside(x,y))return null;
        return markers().stream().filter(w->Math.abs(screenX(w.x()+.5)-x)<=6 && screenY(w.z()+.5)-y>=0 && screenY(w.z()+.5)-y<=16)
            .min(Comparator.comparingDouble(w->Math.hypot(screenX(w.x()+.5)-x,screenY(w.z()+.5)-y))).orElse(null);
    }
    private void closePopup(){popup.forEach(this::removeWidget);popup.clear();dragging=false;}
    private void showPopup(double mouseX,double mouseY,Waypoint selected) {
        closePopup();
        int x=selected==null?(int)Math.floor(blockX(mouseX)):selected.x();
        int z=selected==null?(int)Math.floor(blockZ(mouseY)):selected.z();
        Integer floor=atlas.height(dimension,x,z);
        int y=selected!=null?selected.y():floor==null?minecraft.player.getBlockY():floor+1;
        String[] labels=selected==null?new String[]{"New Waypoint","Copy Coords","Teleport","Close"}:new String[]{"Edit","Copy Coords","Teleport","Delete","Close"};
        int menuWidth=Arrays.stream(labels).mapToInt(font::width).max().orElse(70)+12;
        int menuX=Math.clamp((int)mouseX,mapLeft+1,Math.max(mapLeft+1,mapRight-menuWidth-1));
        int menuY=Math.clamp((int)mouseY,mapTop+1,Math.max(mapTop+1,mapBottom-labels.length*MENU_ROW_HEIGHT-1));
        for(int i=0;i<labels.length;i++) {
            String label=labels[i];
            var action=new StudioUi.Action(menuX,menuY+i*MENU_ROW_HEIGHT,menuWidth,MENU_ROW_HEIGHT,label,()->{
                closePopup();
                switch(label) {
                    case "New Waypoint" -> minecraft.gui.setScreen(new WaypointEditorScreen(this,Navigation.draft(x,y,z,dimension),false));
                    case "Edit" -> minecraft.gui.setScreen(new WaypointEditorScreen(this,selected,true));
                    case "Copy Coords" -> {
                        minecraft.keyboardHandler.setClipboard(x+" "+y+" "+z);
                        copiedCoordinates="X: "+x+" Y: "+y+" Z: "+z;copiedAt=System.nanoTime();
                    }
                    case "Teleport" -> {if(minecraft.getConnection()!=null){minecraft.getConnection().sendCommand("tp "+x+" "+y+" "+z);onClose();}}
                    case "Delete" -> world.delete(selected.id());
                    default -> {}
                }
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial) {
                    g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),mapColor(isHovered()?0xff333333:0xff171717));
                    mapText(g,getMessage().getString(),getX()+(getWidth()-font.width(label))/2,getY()+4,label.equals("Delete") && isHovered()?0xffff5555:StudioUi.TEXT);
                }
            };popup.add(action);addRenderableWidget(action);
        }
        setFocused(null);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        event=new MouseButtonEvent(event.x(),event.y()-mapSlide,event.buttonInfo());
        if(!popup.isEmpty()) {
            for(var action:List.copyOf(popup))if(action.isMouseOver(event.x(),event.y()) && action.mouseClicked(event,twice))return true;
            closePopup();
            if(event.button()!=org.lwjgl.sdl.SDLMouse.SDL_BUTTON_RIGHT)return true;
        }
        if(super.mouseClicked(event,twice))return true;
        if(!inside(event.x(),event.y()))return false;
        if(event.button()==org.lwjgl.sdl.SDLMouse.SDL_BUTTON_RIGHT) {
            showPopup(event.x(),event.y(),marker(event.x(),event.y()));return true;
        }
        if(event.button()==org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT) {
            var waypoint=marker(event.x(),event.y());
            if(waypoint==null)dragging=true;
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if(dragging){centerX=Math.clamp(centerX-dx/zoom,-30_000_000,30_000_000);centerZ=Math.clamp(centerZ-dy/zoom,-30_000_000,30_000_000);return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {dragging=false;return super.mouseReleased(event);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        y-=mapSlide;
        if(!popup.isEmpty())return true;
        if(!inside(x,y))return false;
        double anchorX=blockX(x),anchorZ=blockZ(y);zoom=Math.clamp(zoom*Math.pow(1.25,Math.clamp(vertical,-4,4)),minZoom(),4);
        centerX=anchorX-(x-mapCenterX())/zoom;centerZ=anchorZ-(y-mapCenterY())/zoom;return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(!popup.isEmpty() && event.isEscape()){closePopup();return true;}
        if(Navigation.mapKey.matches(event)){onClose();return true;}
        if(Navigation.listKey.matches(event)){minecraft.gui.setScreen(new WaypointListScreen(this));return true;}
        if(Navigation.createKey.matches(event)){minecraft.gui.setScreen(new WaypointEditorScreen(this,Navigation.newWaypoint(),false));return true;}
        return super.keyPressed(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        float progress=Math.clamp((System.nanoTime()-openedAt)/240_000_000f,0,1);
        mapOpacity=1-(1-progress)*(1-progress)*(1-progress);
        mapSlide=Math.round(12*(1-mapOpacity));
        if(mapOpacity<.02f)return;
        g.pose().pushMatrix();g.pose().translate(0,mapSlide);
        try {
        my-=mapSlide;
        drawPaper(g);
        String layer=dimension;g.enableScissor(mapLeft,mapTop,mapRight,mapBottom);
        fogOutline.draw(g,atlas,layer,mapLeft,mapTop,mapRight,mapBottom,blockX(mapLeft),blockZ(mapTop),zoom,mapColor(0xffffffff));
        long now=System.nanoTime();float fadeStep=Math.min(.1f,(now-labelTime)/1_000_000_000f)/.18f;labelTime=now;
        Waypoint hovered=popup.isEmpty()?marker(mx,my):null;
        for(var waypoint:markers()) {
            int x=screenX(waypoint.x()+.5),y=screenY(waypoint.z()+.5);if(!inside(x,y))continue;
            drawMarker(g,waypoint,x,y);
            double distance=minecraft.player.position().distanceTo(new net.minecraft.world.phys.Vec3(waypoint.x()+.5,waypoint.y()+.5,waypoint.z()+.5));
            String label=waypoint.name()+" "+Math.round(distance)+"m";
            float opacity=Math.clamp(labelOpacity.getOrDefault(waypoint.id(),0f)+(hovered==waypoint?fadeStep:-fadeStep),0,1);
            labelOpacity.put(waypoint.id(),opacity);
            if(!label.isEmpty() && opacity*mapOpacity>0.02f) {
                label=font.plainSubstrByWidth(label,150);
                int boxWidth=font.width(label)+7,boxHeight=13;
                int bx=Math.clamp(x-boxWidth/2,mapLeft,Math.max(mapLeft,mapRight-boxWidth));
                float eased=opacity*opacity*(3-2*opacity);
                int by=Math.max(mapTop,y-16-4-boxHeight+Math.round(5*(1-eased)));
                g.fill(bx,by,bx+boxWidth,by+boxHeight,mapColor(((int)(170*opacity)<<24)|0x090909));
                mapText(g,label,bx+(boxWidth-font.width(label)+1)/2,by+(boxHeight-7)/2-1,
                    ((int)(255*opacity)<<24)|(WaypointRenderer.labelColor(waypoint.color())&0xffffff));
            }
        }
        int x=screenX(minecraft.player.getX()),y=screenY(minecraft.player.getZ());
        g.pose().pushMatrix();g.pose().translate(x,y);
        // Rotate the vanilla sprite half a turn to align its tip with player yaw.
        g.pose().rotate((float)Math.toRadians(Math.floorMod(Math.round(minecraft.player.getYRot()/22.5f)+8,16)*22.5));
        g.blit(RenderPipelines.GUI_TEXTURED,PLAYER,-6,-6,0,0,12,12,8,8,8,8,mapColor(0xffffffff));g.pose().popMatrix();
        drawCopyNotice(g);
        g.disableScissor();
        if(!popup.isEmpty()) {
            var first=popup.getFirst();var last=popup.getLast();
            g.fill(first.getX()-1,first.getY()-1,first.getX()+first.getWidth()+1,last.getY()+last.getHeight()+1,mapColor(0xff000000));
        }
        super.extractRenderState(g,mx,my,partial);
        } finally {g.pose().popMatrix();}
    }
    private int mapColor(int color){return (Math.round((color>>>24)*mapOpacity)<<24)|(color&0xffffff);}
    private void mapText(GuiGraphicsExtractor g,String text,int x,int y,int color) {
        int tint=mapColor(color);if((tint>>>24)>=4)StudioUi.text(g,text,x,y,tint);
    }
    private void drawCopyNotice(GuiGraphicsExtractor g) {
        float age=(System.nanoTime()-copiedAt)/1_000_000_000f;
        if(copiedCoordinates.isEmpty() || age>2.5f)return;
        int alpha=Math.round(255*Math.clamp((2.5f-age)/.35f,0,1));
        if(alpha*mapOpacity<4)return;
        int y=mapBottom-25;
        String title="Coords Copied!";
        g.text(font,title,(int)Math.round(mapCenterX()-font.width(title)/2f),y,mapColor(alpha<<24|0x55ff55),true);
        g.text(font,copiedCoordinates,(int)Math.round(mapCenterX()-font.width(copiedCoordinates)/2f),y+11,mapColor(alpha<<24|0xffffff),true);
    }
    @Override public void removed(){fogOutline.close();super.removed();}
    private void drawMarker(GuiGraphicsExtractor g,Waypoint waypoint,int x,int y) {
        Identifier banner=BANNERS.computeIfAbsent(waypoint.color(),color->
            Identifier.withDefaultNamespace("textures/map/decorations/"+Navigation.bannerDye(color).getName()+"_banner.png"));
        g.blit(RenderPipelines.GUI_TEXTURED,banner,x-8,y-16,0,0,16,16,8,8,8,8,mapColor(0xffffffff));
    }
    private void drawPaper(GuiGraphicsExtractor g) {
        // Tile each slice at one GUI pixel per texture pixel. Stretching the long
        // edge/centre slices makes the parchment notches and grain look distorted.
        int[] source={0,PAPER_BORDER,64-PAPER_BORDER,64};
        int[] xs={frameLeft,mapLeft,mapRight,frameLeft+frameWidth};
        int[] ys={frameTop,mapTop,mapBottom,frameTop+frameHeight};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
            int sw=source[col+1]-source[col],sh=source[row+1]-source[row];
            int width=xs[col+1]-xs[col],height=ys[row+1]-ys[row];
            for(int dy=0;dy<height;dy+=sh)for(int dx=0;dx<width;dx+=sw) {
                int w=Math.min(sw,width-dx),h=Math.min(sh,height-dy);
                g.blit(RenderPipelines.GUI_TEXTURED,PAPER,xs[col]+dx,ys[row]+dy,source[col],source[row],w,h,w,h,64,64,mapColor(0xffffffff));
            }
        }
    }
}

package dev.hudstudio.client.navigation;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.util.*;

/** One composite keeps terrain, the rounded fog transition and its ink contour aligned. */
final class FogOutline implements AutoCloseable {
    private DynamicTexture texture;
    private NativeImage fog;
    private Identifier id;
    private int width,height;
    private double originX,originZ,lastZoom;
    private String lastLayer="";
    private long updated;
    void draw(GuiGraphicsExtractor g,MapAtlas atlas,String layer,int left,int top,int right,int bottom,double x,double z,double zoom,int tint) {
        int w=right-left,h=bottom-top;
        if(w<=0 || h<=0)return;
        // Keep the sampling phase on a world-anchored GUI-pixel grid. A pan now
        // translates the same pixels instead of changing which blocks every pixel samples.
        x=Math.round(x*zoom)/zoom;z=Math.round(z*zoom)/zoom;
        long now=System.nanoTime();
        if(texture==null || w!=width || h!=height || x!=originX || z!=originZ || zoom!=lastZoom || !layer.equals(lastLayer) || now-updated>250_000_000L) {
            if(texture==null || w!=width || h!=height) {
                close();width=w;height=h;
                id=Identifier.fromNamespaceAndPath("hud-studio","map/fog_outline");
                texture=new DynamicTexture(()->"HUD Studio map and fog",new NativeImage(w,h,true));
                Minecraft.getInstance().getTextureManager().register(id,texture);
                var resource=Identifier.fromNamespaceAndPath("hud-studio","textures/map/unexplored_fog.png");
                try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(resource).open()) {fog=NativeImage.read(stream);}
                catch(java.io.IOException e){throw new IllegalStateException("Cannot load map fog",e);}
            }
            originX=x;originZ=z;lastZoom=zoom;lastLayer=layer;updated=now;
            update(atlas,layer,x,z,zoom);
        }
        g.blit(RenderPipelines.GUI_TEXTURED,id,left,top,0,0,w,h,w,h,tint);
    }
    private void update(MapAtlas atlas,String layer,double x,double z,double zoom) {
        int pad=10,w=width+pad*2,h=height+pad*2;
        int[] colors=new int[w*h],distance=new int[w*h];
        float[] coverage=new float[w*h],scratch=new float[w*h];
        Arrays.fill(distance,10000);
        long rasterX=Math.round(x*zoom),rasterZ=Math.round(z*zoom);
        Map<Long,MapAtlas.Region> regions=new HashMap<>();
        for(int py=0;py<h;py++)for(int px=0;px<w;px++) {
            int bx=(int)Math.floor((rasterX+px-pad+.5)/zoom),bz=(int)Math.floor((rasterZ+py-pad+.5)/zoom);
            int rx=Math.floorDiv(bx,MapAtlas.SIZE),rz=Math.floorDiv(bz,MapAtlas.SIZE);
            long key=((long)rx<<32)|(rz&0xffffffffL);
            if(!regions.containsKey(key))regions.put(key,atlas.region(layer,rx,rz,false));
            var region=regions.get(key);int i=py*w+px;
            colors[i]=region==null?0:region.colors[Math.floorMod(bz,MapAtlas.SIZE)*MapAtlas.SIZE+Math.floorMod(bx,MapAtlas.SIZE)];
            if(colors[i]!=0){coverage[i]=1;distance[i]=0;}
        }
        // Extend edge colours underneath the blend, so rounded corners never expose white seams.
        for(int py=1;py<h;py++)for(int px=1;px<w;px++) {
            int i=py*w+px;extend(colors,distance,i,i-1);extend(colors,distance,i,i-w);
        }
        for(int py=h-2;py>=0;py--)for(int px=w-2;px>=0;px--) {
            int i=py*w+px;extend(colors,distance,i,i+1);extend(colors,distance,i,i+w);
        }
        // Repeated Gaussian filtering rounds the surveyed outline before compositing.
        for(int pass=0;pass<3;pass++) {
            blur(coverage,scratch,w,h,true);blur(scratch,coverage,w,h,false);
        }
        var image=texture.getPixels();
        for(int py=0;py<height;py++)for(int px=0;px<width;px++) {
            int i=(py+pad)*w+px+pad;
            float amount=smooth(.18f,.82f,coverage[i]);
            int paper=fog.getPixel(Math.floorMod(px+(int)rasterX,fog.getWidth()),Math.floorMod(py+(int)rasterZ,fog.getHeight()));
            paper=blend(paper,0xff808080,.08f);
            int color=blend(paper,colors[i],amount);
            float ink=(1-smooth(.04f,.36f,Math.abs(coverage[i]-.5f)))*.96f;
            image.setPixel(px,py,blend(color,0xff111116,ink));
        }
        texture.upload();
    }
    private static void extend(int[] colors,int[] distance,int i,int neighbour) {
        if(distance[neighbour]+1<distance[i]){distance[i]=distance[neighbour]+1;colors[i]=colors[neighbour];}
    }
    private static void blur(float[] source,float[] target,int w,int h,boolean horizontal) {
        int[] weights={1,4,6,4,1};
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            float value=0;
            for(int k=-2;k<=2;k++)value+=source[horizontal?y*w+Math.clamp(x+k,0,w-1):Math.clamp(y+k,0,h-1)*w+x]*weights[k+2];
            target[y*w+x]=value/16;
        }
    }
    private static float smooth(float low,float high,float value){float t=Math.clamp((value-low)/(high-low),0,1);return t*t*(3-2*t);}
    private static int blend(int a,int b,float t) {
        int r=Math.round((a>>16&255)*(1-t)+(b>>16&255)*t),g=Math.round((a>>8&255)*(1-t)+(b>>8&255)*t),blue=Math.round((a&255)*(1-t)+(b&255)*t);
        return 0xff000000|r<<16|g<<8|blue;
    }
    @Override public void close() {
        if(id!=null)Minecraft.getInstance().getTextureManager().release(id);
        if(fog!=null)fog.close();texture=null;id=null;fog=null;
    }
}

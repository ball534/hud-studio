package dev.hudstudio.client.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class MapRasterSmokeTest {
    public static void run(Minecraft mc) {
        try(var atlas=new MapAtlas(Files.createTempDirectory(Path.of("."),"stable-map-raster-"));var raster=new FogOutline()) {
            for(int rz=-3;rz<=3;rz++)for(int rx=-3;rx<=3;rx++) {
                var r=atlas.region("minecraft:overworld",rx,rz,true);
                for(int z=0;z<128;z++)for(int x=0;x<128;x++) {
                    int bx=rx*128+x,bz=rz*128+z;
                    r.colors[z*128+x]=bx*bx+bz*bz<160*160?switch(Math.floorMod(bx*31+bz*17,3)) {
                        case 0->0xff55aa33;case 1->0xff996633;default->0xff2244bb;
                    }:0;
                }
            }
            for(double zoom:new double[]{.125,.256,.4096,.8,1.25}) {
                double x=-64.1/zoom,z=-32.1/zoom;
                int[] base=snapshot(mc,raster,atlas,x,z,zoom);
                if(!Arrays.equals(base,snapshot(mc,raster,atlas,x+.15/zoom,z+.15/zoom,zoom)))throw new AssertionError("Subpixel pan changed map pixels at "+zoom);
                for(int[] shift:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                    int[] moved=snapshot(mc,raster,atlas,x+shift[0]/zoom,z+shift[1]/zoom,zoom);
                    for(int py=12;py<52;py++)for(int px=12;px<84;px++)
                        if(moved[py*96+px]!=base[(py+shift[1])*96+px+shift[0]])throw new AssertionError("Map pixels deformed while panning at "+zoom+" direction "+Arrays.toString(shift));
                }
            }
        }catch(java.io.IOException|ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    private static int[] snapshot(Minecraft mc,FogOutline raster,MapAtlas atlas,double x,double z,double zoom) throws ReflectiveOperationException {
        var g=new GuiGraphicsExtractor(mc,new GuiRenderState(),96,64);
        raster.draw(g,atlas,"minecraft:overworld",0,0,96,64,x,z,zoom,0xffffffff);
        var field=FogOutline.class.getDeclaredField("texture");field.setAccessible(true);
        var image=((DynamicTexture)field.get(raster)).getPixels();int[] pixels=new int[96*64];
        for(int py=0;py<64;py++)for(int px=0;px<96;px++)pixels[py*96+px]=image.getPixel(px,py);
        return pixels;
    }
}

package dev.hudstudio.client;

public final class StudioTheme {
    public static int color() {return HudStudioClient.config==null?0xff101010:HudStudioClient.config.themeColor;}
    public static int alpha(int rgb) {
        float opacity=HudStudioClient.config==null?.85f:HudStudioClient.config.opacity;
        return alpha(rgb,opacity);
    }
    public static int alpha(int rgb,float opacity) {return Math.round(opacity*255)<<24 | rgb&0xffffff;}
    public static int textOn(int rgb) {
        return brightness(rgb)>.53f?0xff111111:0xffeeeeee;
    }
    private static float brightness(int rgb) {
        return (((rgb>>16)&255)*.2126f+((rgb>>8)&255)*.7152f+(rgb&255)*.0722f)/255;
    }
    public static int accent() {int rgb=color();return brightness(rgb)<.14f?0xffc6c6c6:rgb;}
    public static int hsv(float h,float s,float v) {
        float c=v*s,x=c*(1-Math.abs((h*6)%2-1)),m=v-c;
        float r=0,g=0,b=0;
        switch((int)(h*6)%6) {
            case 0 -> {r=c;g=x;}case 1 -> {r=x;g=c;}case 2 -> {g=c;b=x;}
            case 3 -> {g=x;b=c;}case 4 -> {r=x;b=c;}case 5 -> {r=c;b=x;}
        }
        return 0xff000000|Math.round((r+m)*255)<<16|Math.round((g+m)*255)<<8|Math.round((b+m)*255);
    }
    public static float[] hsv(int rgb) {
        float r=(rgb>>16&255)/255f,g=(rgb>>8&255)/255f,b=(rgb&255)/255f;
        float max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),d=max-min,h=0;
        if(d!=0) {
            if(max==r) h=((g-b)/d)%6;
            else if(max==g) h=(b-r)/d+2;
            else h=(r-g)/d+4;
            h/=6;if(h<0)h+=1;
        }
        return new float[]{h,max==0?0:d/max,max};
    }
}

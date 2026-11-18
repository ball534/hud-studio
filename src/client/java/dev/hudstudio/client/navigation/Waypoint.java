package dev.hudstudio.client.navigation;

/** A waypoint belongs to one dimension of one locally stored world/server. */
public record Waypoint(String id,String name,int color,int x,int y,int z,String dimension,boolean visible) {
    public Waypoint {
        color=Navigation.bannerColor(Navigation.bannerDye(color));
    }

    public Waypoint visibility(boolean value) {return new Waypoint(id,name,color,x,y,z,dimension,value);}
}

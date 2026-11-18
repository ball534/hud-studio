package dev.hudstudio.client.navigation;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hudstudio.HudStudio;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

public final class Navigation {
    public static KeyMapping createKey,listKey,mapKey;
    static final List<DyeColor> COLORS=List.of(DyeColor.values());

    static int bannerColor(DyeColor dye) {
        // Vanilla's white map banner has a slightly darker fill than white dye.
        return dye==DyeColor.WHITE?0xffeeeeee:0xff000000|dye.getTextureDiffuseColor();
    }

    static DyeColor bannerDye(int color) {
        DyeColor closest=DyeColor.WHITE;
        int bestDistance=Integer.MAX_VALUE;
        for(var dye:COLORS) {
            if(color==bannerColor(dye))return dye;
            // Preserve the map icon chosen for saved waypoints using the old chat palette.
            int rgb=dye.getTextureDiffuseColor();
            int red=(color>>16&255)-(rgb>>16&255);
            int green=(color>>8&255)-(rgb>>8&255);
            int blue=(color&255)-(rgb&255);
            int distance=red*red+green*green+blue*blue;
            if(distance<bestDistance){closest=dye;bestDistance=distance;}
        }
        return closest;
    }
    private static Object connection;
    private static WaypointStore store;
    private static MapAtlas atlas;
    public static void init() {
        var category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("hud-studio","navigation"));
        createKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hud-studio.new_waypoint",InputConstants.KEY_N,category));
        listKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hud-studio.waypoints",InputConstants.KEY_U,category));
        mapKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hud-studio.world_map",InputConstants.KEY_M,category));
        ClientTickEvents.END_CLIENT_TICK.register(Navigation::tick);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register((level,chunk)->{if(atlas!=null)atlas.chunkLoaded();});
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_UNLOAD.register((level,chunk)->{if(atlas!=null)atlas.unloading(level,chunk);});
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(mc->{WaypointRenderer.clearLabels();if(atlas!=null)atlas.close();MapAtlas.finishWrites();});
    }
    public static WaypointStore store() {return store;}
    public static MapAtlas atlas() {return atlas;}
    public static String dimension() {var level=Minecraft.getInstance().level;return level==null?"minecraft:overworld":level.dimension().identifier().toString();}
    public static Path root() {return FabricLoader.getInstance().getConfigDir().resolve("hud-studio-worlds");}
    private static String singleplayerIdentity(Path world) {
        return "singleplayer:"+world.toAbsolutePath().normalize();
    }
    public static void deleteWorldMap(Path world) {
        MapAtlas.deleteSavedMap(root().resolve(hash(singleplayerIdentity(world))));
    }
    public static String hash(String identity) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8))).substring(0,24);}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void tick(Minecraft mc) {
        Object active=mc.level==null || mc.player==null?null:mc.getConnection();
        if(connection!=active) {
            if(atlas!=null)atlas.close();
            store=null;atlas=null;connection=active;
            if(active!=null) {
                String identity,label;
                var server=mc.getSingleplayerServer();
                if(server!=null) {
                    identity=singleplayerIdentity(server.getWorldPath(LevelResource.ROOT));label=server.getWorldData().getLevelName();
                }else {
                    var info=mc.getCurrentServer();
                    String address=info==null?mc.getConnection().getConnection().getRemoteAddress().toString():info.ip;
                    identity="server:"+address.toLowerCase(Locale.ROOT);label=info==null?address:info.name;
                }
                store=new WaypointStore(root().resolve(hash(identity)),label);atlas=new MapAtlas(store.directory);
                try {java.nio.file.Files.createDirectories(store.directory);WaypointStore.atomicWrite(store.directory.resolve("world.txt"),label);}
                catch(Exception e){HudStudio.LOGGER.warn("Cannot save world map label",e);}
            }
        }
        // Pausing simulation does not prevent surveying already loaded terrain.
        if(atlas!=null)atlas.tick(mc);
        WaypointRenderer.updateLabels(mc);
        // Drain keys even in menus so an old press cannot create a marker later.
        while(createKey.consumeClick())if(ready(mc))mc.gui.setScreen(new WaypointEditorScreen(null,newWaypoint(),false));
        while(listKey.consumeClick())if(ready(mc))mc.gui.setScreen(new WaypointListScreen(null));
        while(mapKey.consumeClick())if(ready(mc))mc.gui.setScreen(new WorldMapScreen(null));
    }
    private static boolean ready(Minecraft mc) {return store!=null && mc.player!=null && mc.gui.screen()==null;}
    public static Waypoint newWaypoint() {
        var p=Minecraft.getInstance().player;
        var feet=p.blockPosition();
        return draft(feet.getX(),feet.getY(),feet.getZ(),dimension());
    }
    public static Waypoint draft(int x,int y,int z,String dimension) {
        var color=COLORS.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(COLORS.size()));
        return new Waypoint(UUID.randomUUID().toString(),"Waypoint",bannerColor(color),x,y,z,dimension,true);
    }
    public static String dimensionName(String id) {
        String text=id.replace("minecraft:","").replace('_',' ');StringBuilder title=new StringBuilder();boolean first=true;
        for(char c:text.toCharArray()){title.append(first?Character.toTitleCase(c):c);first=Character.isWhitespace(c) || c==':';}
        return title.toString();
    }
}

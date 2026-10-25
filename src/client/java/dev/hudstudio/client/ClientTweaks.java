package dev.hudstudio.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Camera-only controls: never relocates the player or changes their aim. */
public final class ClientTweaks {
    public static KeyMapping zoomKey,freecamKey,freelookKey;
    private static boolean freecam,freelook;
    private static CameraType previousCamera;
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static Vec3 position=Vec3.ZERO,oldPosition=Vec3.ZERO;
    private static float yaw,pitch;
    public static void init() {
        var category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("hud-studio","tweaks"));
        zoomKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hud-studio.zoom",InputConstants.KEY_C,category));
        freecamKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hud-studio.freecam",InputConstants.KEY_F6,category));
        freelookKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hud-studio.freelook",InputConstants.KEY_LALT,category));
        ClientTickEvents.START_CLIENT_TICK.register(ClientTweaks::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ComboMeter::tick);
    }
    public static boolean enabled(String id) {
        var config=HudStudioClient.config;
        return config!=null && config.tweaks.get(id)!=null && config.tweaks.get(id).visible;
    }
    public static KeyMapping key(String id) {return switch(id){case "zoom"->zoomKey;case "freecam"->freecamKey;case "freelook"->freelookKey;default->null;};}
    public static boolean freecam() {return freecam;}
    public static boolean freelook() {return freelook;}
    public static boolean detached() {return freecam || freelook;}
    public static boolean zooming() {
        var mc=Minecraft.getInstance();
        return enabled("zoom") && mc.player!=null && mc.gui.screen()==null && zoomKey!=null && zoomKey.isDown();
    }
    public static float zoomFactor() {return zooming()?HudStudioClient.config.zoomFactor:1;}
    public static float yaw() {return yaw;}
    public static float pitch() {return pitch;}
    public static Vec3 position(float partial) {return oldPosition.lerp(position,Math.clamp(partial,0,1));}
    public static void turn(double dx,double dy) {
        yaw+=(float)(dx*.15/zoomFactor());pitch=Math.clamp(pitch+(float)(dy*.15/zoomFactor()),-90,90);
    }
    public static void toggleFreecam(Minecraft mc) {
        if(freecam) {stop(mc);return;}
        if(mc.player==null || !enabled("freecam"))return;
        if(mc.gameMode!=null) {
            mc.gameMode.stopDestroyBlock();
            if(mc.player.isUsingItem())mc.gameMode.releaseUsingItem(mc.player);
        }
        stop(mc);previousCamera=mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        position=oldPosition=mc.player.getEyePosition();
        yaw=mc.player.getYRot();pitch=mc.player.getXRot();level=mc.level;freecam=true;
    }
    public static void stop(Minecraft mc) {
        if(detached() && previousCamera!=null)mc.options.setCameraType(previousCamera);
        freecam=false;freelook=false;previousCamera=null;
    }
    private static void tick(Minecraft mc) {
        if(mc.player==null || mc.level!=level && detached() || mc.player!=null && !mc.player.isAlive()) {stop(mc);return;}
        if(freecam && !enabled("freecam") || freelook && !enabled("freelook"))stop(mc);
        while(freecamKey.consumeClick())if(mc.gui.screen()==null)toggleFreecam(mc);
        boolean holding=enabled("freelook") && freelookKey.isDown() && mc.gui.screen()==null && !freecam;
        if(holding && !freelook) {
            previousCamera=mc.options.getCameraType();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            yaw=mc.player.getYRot();pitch=mc.player.getXRot();level=mc.level;freelook=true;
        } else if(!holding && freelook)stop(mc);
        oldPosition=position;
        if(!freecam || mc.gui.screen()!=null || mc.isPaused())return;
        var options=mc.options;
        double forward=(options.keyUp.isDown()?1:0)-(options.keyDown.isDown()?1:0);
        double right=(options.keyRight.isDown()?1:0)-(options.keyLeft.isDown()?1:0);
        double vertical=(options.keyJump.isDown()?1:0)-(options.keyShift.isDown()?1:0);
        double radians=Math.toRadians(yaw);
        var movement=Vec3.directionFromRotation(pitch,yaw).scale(forward)
            .add(Math.cos(radians)*right,vertical,Math.sin(radians)*right);
        if(movement.lengthSqr()>1)movement=movement.normalize();
        position=position.add(movement.scale(HudStudioClient.config.freecamSpeed*(options.keySprint.isDown()?3:1)));
    }
}

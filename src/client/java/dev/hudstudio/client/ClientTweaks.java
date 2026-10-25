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
    private static net.minecraft.client.player.RemotePlayer camera;
    private static net.minecraft.world.entity.Entity previousEntity;
    private static float health;
    private static int hurtTime;
    private static boolean sprinting;
    private static boolean freecamHoldBlocked;
    private static float currentZoom=3;
    private static boolean wasZooming;
    public static void init() {
        var category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("hud-studio","tweaks"));
        zoomKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hud-studio.zoom",InputConstants.KEY_Z,category));
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
    public static float zoomFactor() {
        boolean active=zooming();
        if(!active || !wasZooming)currentZoom=HudStudioClient.config==null?3:HudStudioClient.config.zoomFactor;
        wasZooming=active;
        return active?currentZoom:1;
    }
    public static boolean scrollZoom(double amount) {
        zoomFactor();
        if(!zooming())return false;
        currentZoom=Math.clamp(currentZoom*(amount>0?1.1f:amount<0?.9f:1),1,50);
        return true;
    }
    public static float yaw() {return yaw;}
    public static float pitch() {return pitch;}
    public static boolean hideHud() {
        var c=HudStudioClient.config;
        return c!=null && ((zooming() && !c.zoomShowHud) || (freecam && !c.freecamShowHud));
    }
    public static boolean cinematicZoom() {return zooming() && HudStudioClient.config.zoomCinematic;}
    public static Vec3 position(float partial) {return oldPosition.lerp(position,Math.clamp(partial,0,1));}
    public static void turn(double dx,double dy) {
        yaw+=(float)(dx*.15);pitch=Math.clamp(pitch+(float)(dy*.15),-90,90);
        syncCamera();
    }
    private static void syncCamera() {
        if(camera!=null) {
            camera.setPos(position.x,position.y-camera.getEyeHeight(),position.z);
            camera.setYRot(yaw);camera.setXRot(pitch);
        }
    }
    public static void toggleFreecam(Minecraft mc) {
        if(freecam) {stop(mc);return;}
        if(mc.player==null || mc.level==null || !enabled("freecam"))return;
        if(mc.gameMode!=null) {
            mc.gameMode.stopDestroyBlock();
            if(mc.player.isUsingItem())mc.gameMode.releaseUsingItem(mc.player);
        }
        stop(mc);previousCamera=mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        position=oldPosition=mc.player.getEyePosition();
        yaw=mc.player.getYRot();pitch=mc.player.getXRot();level=mc.level;freecam=true;
        health=mc.player.getHealth()+mc.player.getAbsorptionAmount();hurtTime=mc.player.hurtTime;sprinting=false;
        previousEntity=mc.getCameraEntity();
        camera=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"StudioCamera"));
        camera.noPhysics=true;
        syncCamera();camera.setOldPosAndRot();
        mc.setCameraEntity(camera);
    }
    public static void stop(Minecraft mc) {
        if(freecam && HudStudioClient.config.freecamHold && freecamKey.isDown())freecamHoldBlocked=true;
        if(camera!=null && mc.getCameraEntity()==camera)mc.setCameraEntity(mc.level==level && previousEntity!=null?previousEntity:mc.player);
        if(detached() && previousCamera!=null)mc.options.setCameraType(previousCamera);
        freecam=false;freelook=false;previousCamera=null;camera=null;previousEntity=null;sprinting=false;
    }
    private static void tick(Minecraft mc) {
        zoomFactor();
        if(mc.player==null || mc.level!=level && detached() || mc.player!=null && !mc.player.isAlive()) {stop(mc);return;}
        if(freecam && !enabled("freecam") || freelook && !enabled("freelook"))stop(mc);
        if(freecam) {
            float total=mc.player.getHealth()+mc.player.getAbsorptionAmount();
            if(HudStudioClient.config.freecamDisableOnDamage && (total<health || mc.player.hurtTime>hurtTime))stop(mc);
            health=total;hurtTime=mc.player.hurtTime;
        }
        var config=HudStudioClient.config;
        if(!freecamKey.isDown())freecamHoldBlocked=false;
        while(freecamKey.consumeClick())if(!config.freecamHold && mc.gui.screen()==null)toggleFreecam(mc);
        if(config.freecamHold) {
            if(!freecam && !freecamHoldBlocked && freecamKey.isDown() && mc.gui.screen()==null)toggleFreecam(mc);
            else if(freecam && !freecamKey.isDown())stop(mc);
        }
        boolean holding=enabled("freelook") && freelookKey.isDown() && mc.gui.screen()==null && !freecam;
        if(holding && !freelook) {
            previousCamera=mc.options.getCameraType();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            yaw=mc.player.getYRot();pitch=mc.player.getXRot();level=mc.level;freelook=true;
        } else if(!holding && freelook)stop(mc);
        oldPosition=position;
        if(camera!=null) {camera.setOldPosAndRot();camera.tickCount++;}
        if(!freecam || mc.gui.screen()!=null || mc.isPaused())return;
        var options=mc.options;
        double forward=(options.keyUp.isDown()?1:0)-(options.keyDown.isDown()?1:0);
        double right=(options.keyRight.isDown()?1:0)-(options.keyLeft.isDown()?1:0);
        double vertical=(options.keyJump.isDown()?1:0)-(options.keyShift.isDown()?1:0);
        double radians=Math.toRadians(yaw);
        // Reference freecam keeps flight level and normalizes horizontal diagonals only.
        sprinting=forward>0 && (options.keySprint.isDown() || sprinting);
        double diagonal=forward!=0 && right!=0?1/Math.sqrt(2):1;
        // At yaw 0 the camera looks south: screen-right is WEST (negative X).
        var movement=Vec3.directionFromRotation(config.freecamLookFlight?pitch:0,yaw).scale(forward)
            .add(-Math.cos(radians)*right,0,-Math.sin(radians)*right).scale(diagonal);
        double speed=HudStudioClient.config.freecamSpeed;
        movement=movement.scale(speed*(sprinting?config.freecamSprintMultiplier:1)).add(0,vertical*config.freecamVerticalSpeed,0);
        if(config.freecamCollision) {
            var box=new net.minecraft.world.phys.AABB(position.x-.2,position.y-.2,position.z-.2,position.x+.2,position.y+.2,position.z+.2);
            movement=net.minecraft.world.entity.Entity.collideBoundingBox(camera,movement,box,mc.level,java.util.List.of());
        }
        position=position.add(movement);
        syncCamera();
    }
}

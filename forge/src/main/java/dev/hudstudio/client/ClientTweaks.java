package dev.hudstudio.client;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class ClientTweaks {
   public static KeyBinding zoomKey;
   public static KeyBinding freecamKey;
   public static KeyBinding freelookKey;
   private static boolean freecam;
   private static boolean freelook;
   private static boolean holdBlocked;
   private static boolean sprinting;
   private static boolean wasZooming;
   private static int previousView;
   private static World world;
   private static Entity previousEntity;
   private static EntityOtherPlayerMP camera;
   private static float yaw;
   private static float pitch;
   private static float health;
   private static float currentZoom = 3.0F;
   private static int hurtTime;

   public static void init() {
      zoomKey = register("zoom", 44);
      freecamKey = register("freecam", 64);
      freelookKey = register("freelook", 56);
   }

   private static KeyBinding register(String id, int code) {
      KeyBinding key = new KeyBinding("key.hud-studio." + id, code, "key.categories.hud-studio");
      ClientRegistry.registerKeyBinding(key);
      return key;
   }

   public static boolean down(KeyBinding key) {
      if (key == null) {
         return false;
      }

      int code = key.getKeyCode();
      return code < 0
         ? Mouse.isCreated() && code + 100 >= 0 && code + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(code + 100)
         : code > 0 && code < 256 && Keyboard.isCreated() && Keyboard.isKeyDown(code);
   }

   public static KeyBinding key(String id) {
      switch (id) {
         case "zoom":
            return zoomKey;
         case "freecam":
            return freecamKey;
         case "freelook":
            return freelookKey;
         default:
            return null;
      }
   }

   public static boolean enabled(String id) {
      return HudStudioClient.config != null && HudStudioClient.config.tweaks.get(id) != null && HudStudioClient.config.tweaks.get(id).visible;
   }

   public static boolean freecam() {
      return freecam;
   }

   public static boolean freelook() {
      return freelook;
   }

   public static boolean detached() {
      return freecam || freelook;
   }

   public static float yaw() {
      return yaw;
   }

   public static float pitch() {
      return pitch;
   }

   public static boolean zooming() {
      Minecraft mc = Minecraft.getMinecraft();
      return enabled("zoom") && mc.thePlayer != null && mc.currentScreen == null && down(zoomKey);
   }

   public static float zoomFactor() {
      boolean active = zooming();
      if (!active || !wasZooming) {
         currentZoom = HudStudioClient.config == null ? 3.0F : HudStudioClient.config.zoomFactor;
      }

      wasZooming = active;
      return active ? currentZoom : 1.0F;
   }

   public static float sensitivity(float original) {
      if (!zooming()) {
         return original;
      }

      double curve = original * 0.6 + 0.2;
      return (float)((Math.cbrt(curve * curve * curve / zoomFactor()) - 0.2) / 0.6);
   }

   public static boolean scrollZoom(double amount) {
      zoomFactor();
      if (!zooming()) {
         return false;
      }

      currentZoom = Numbers.clamp(currentZoom * (amount > 0.0 ? 1.1F : 0.9F), 1.0F, 50.0F);
      return true;
   }

   public static boolean hideHud() {
      HudConfig c = HudStudioClient.config;
      return c != null && (zooming() && !c.zoomShowHud || freecam && !c.freecamShowHud);
   }

   public static boolean cinematicZoom() {
      return zooming() && HudStudioClient.config.zoomCinematic;
   }

   public static void turn(float dx, float dy) {
      yaw += dx * 0.15F;
      pitch = Numbers.clamp(pitch - dy * 0.15F, -90.0F, 90.0F);
      if (camera != null) {
         camera.rotationYaw = yaw;
         camera.rotationPitch = pitch;
      }
   }

   public static void toggleFreecam() {
      Minecraft mc = Minecraft.getMinecraft();
      if (freecam) {
         stop();
      } else if (mc.thePlayer != null && mc.theWorld != null && enabled("freecam")) {
         stop();
         world = mc.theWorld;
         previousView = mc.gameSettings.thirdPersonView;
         previousEntity = mc.getRenderViewEntity();
         mc.playerController.resetBlockRemoving();
         if (mc.thePlayer.isUsingItem()) {
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
         }

         yaw = mc.thePlayer.rotationYaw;
         pitch = mc.thePlayer.rotationPitch;
         camera = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(UUID.randomUUID(), "StudioCamera"));
         camera.copyLocationAndAnglesFrom(mc.thePlayer);
         camera.noClip = true;
         camera.prevPosX = camera.lastTickPosX = camera.posX;
         camera.prevPosY = camera.lastTickPosY = camera.posY;
         camera.prevPosZ = camera.lastTickPosZ = camera.posZ;
         camera.prevRotationYaw = yaw;
         camera.prevRotationPitch = pitch;
         health = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
         hurtTime = mc.thePlayer.hurtTime;
         freecam = true;
         sprinting = false;
         mc.gameSettings.thirdPersonView = 0;
         mc.setRenderViewEntity(camera);
      }
   }

   public static void stop() {
      Minecraft mc = Minecraft.getMinecraft();
      if (freecam && HudStudioClient.config != null && HudStudioClient.config.freecamHold && down(freecamKey)) {
         holdBlocked = true;
      }

      if (camera != null && mc.getRenderViewEntity() == camera) {
         mc.setRenderViewEntity((Entity)(mc.theWorld == world && previousEntity != null ? previousEntity : mc.thePlayer));
      }

      if (detached()) {
         mc.gameSettings.thirdPersonView = previousView;
      }

      freelook = false;
      freecam = false;
      camera = null;
      previousEntity = null;
      sprinting = false;
   }

   public static void tick() {
      Minecraft mc = Minecraft.getMinecraft();
      zoomFactor();
      if (mc.thePlayer != null && (mc.theWorld == world || !detached()) && (mc.thePlayer == null || mc.thePlayer.isEntityAlive())) {
         if (freecam && !enabled("freecam") || freelook && !enabled("freelook")) {
            stop();
         }

         if (freecam) {
            float total = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
            if (HudStudioClient.config.freecamDisableOnDamage && (total < health || mc.thePlayer.hurtTime > hurtTime)) {
               stop();
            }

            health = total;
            hurtTime = mc.thePlayer.hurtTime;
         }

         HudConfig c = HudStudioClient.config;
         if (!down(freecamKey)) {
            holdBlocked = false;
         }

         while (freecamKey.isPressed()) {
            if (!c.freecamHold && mc.currentScreen == null) {
               toggleFreecam();
            }
         }

         if (c.freecamHold) {
            if (!freecam && !holdBlocked && down(freecamKey) && mc.currentScreen == null) {
               toggleFreecam();
            } else if (freecam && !down(freecamKey)) {
               stop();
            }
         }

         boolean holding = enabled("freelook") && down(freelookKey) && mc.currentScreen == null && !freecam;
         if (holding && !freelook) {
            previousView = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 1;
            yaw = mc.thePlayer.rotationYaw;
            pitch = mc.thePlayer.rotationPitch;
            world = mc.theWorld;
            freelook = true;
         } else if (!holding && freelook) {
            stop();
         }

         if (camera != null) {
            camera.prevPosX = camera.lastTickPosX = camera.posX;
            camera.prevPosY = camera.lastTickPosY = camera.posY;
            camera.prevPosZ = camera.lastTickPosZ = camera.posZ;
            camera.prevRotationYaw = camera.rotationYaw;
            camera.prevRotationPitch = camera.rotationPitch;
            camera.ticksExisted++;
         }

         if (freecam && mc.currentScreen == null && !mc.isGamePaused()) {
            double forward = (down(mc.gameSettings.keyBindForward) ? 1 : 0) - (down(mc.gameSettings.keyBindBack) ? 1 : 0);
            double right = (down(mc.gameSettings.keyBindRight) ? 1 : 0) - (down(mc.gameSettings.keyBindLeft) ? 1 : 0);
            double vertical = (down(mc.gameSettings.keyBindJump) ? 1 : 0) - (down(mc.gameSettings.keyBindSneak) ? 1 : 0);
            sprinting = forward > 0.0 && (down(mc.gameSettings.keyBindSprint) || sprinting);
            double radians = Math.toRadians(yaw);
            double angle = Math.toRadians(c.freecamLookFlight ? pitch : 0.0);
            double diagonal = forward != 0.0 && right != 0.0 ? 1.0 / Math.sqrt(2.0) : 1.0;
            double speed = c.freecamSpeed * (sprinting ? c.freecamSprintMultiplier : 1.0F) * diagonal;
            double dx = (-Math.sin(radians) * Math.cos(angle) * forward - Math.cos(radians) * right) * speed;
            double dz = (Math.cos(radians) * Math.cos(angle) * forward - Math.sin(radians) * right) * speed;
            double dy = -Math.sin(angle) * forward * speed + vertical * c.freecamVerticalSpeed;
            if (c.freecamCollision) {
               AxisAlignedBB box = camera.getEntityBoundingBox();
               List<AxisAlignedBB> blocks = mc.theWorld.getCollidingBoundingBoxes(camera, box.addCoord(dx, dy, dz));

               for (AxisAlignedBB block : blocks) {
                  dy = block.calculateYOffset(box, dy);
               }

               box = box.offset(0.0, dy, 0.0);

               for (AxisAlignedBB block : blocks) {
                  dx = block.calculateXOffset(box, dx);
               }

               box = box.offset(dx, 0.0, 0.0);

               for (AxisAlignedBB block : blocks) {
                  dz = block.calculateZOffset(box, dz);
               }
            }

            camera.setPosition(camera.posX + dx, camera.posY + dy, camera.posZ + dz);
         }
      } else {
         stop();
         drain();
      }
   }

   private static void drain() {
      if (freecamKey != null) {
         while (freecamKey.isPressed()) {
         }
      }
   }
}

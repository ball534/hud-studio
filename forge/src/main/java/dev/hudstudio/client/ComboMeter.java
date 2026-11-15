package dev.hudstudio.client;

import dev.hudstudio.client.ui.DrawContext;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public final class ComboMeter {
   public static final ComboState state = new ComboState();
   private static final Deque<ComboMeter.Attack> pending = new ArrayDeque<>();
   private static final Map<Integer, Float> observedHealth = new HashMap<>();
   private static World world;
   private static int ticks;
   private static int sadNotes;
   private static float health = -1.0F;
   private static ISound rankSound;

   private static boolean enabled() {
      return HudStudioClient.config != null && HudStudioClient.config.elements.get("combo").visible;
   }

   private static float remaining(EntityLivingBase target) {
      return target.getHealth() + target.getAbsorptionAmount();
   }

   public static void attack(Entity target) {
      Minecraft mc = Minecraft.getMinecraft();
      if (enabled() && target instanceof EntityLivingBase && !ClientTweaks.freecam() && mc.thePlayer != null) {
         boolean critical = mc.thePlayer.fallDistance > 0.0F
            && !mc.thePlayer.onGround
            && !mc.thePlayer.isOnLadder()
            && !mc.thePlayer.isInWater()
            && !mc.thePlayer.isPotionActive(Potion.blindness)
            && mc.thePlayer.ridingEntity == null;
         pending.addLast(new ComboMeter.Attack((EntityLivingBase)target, critical ? ComboState.Feedback.CRIT : ComboState.Feedback.DAMAGE));

         while (pending.size() > 24) {
            pending.removeFirst();
         }
      }
   }

   public static void hurt(Entity target) {
      Minecraft mc = Minecraft.getMinecraft();
      if (enabled() && mc.thePlayer != null) {
         if (target == mc.thePlayer) {
            interrupt();
         } else if (target instanceof EntityLivingBase && !ClientTweaks.freecam()) {
            for (ComboMeter.Attack attack : pending) {
               if (attack.target == target && !attack.confirmed && ticks - attack.tick <= 10) {
                  attack.confirmed = true;
                  return;
               }
            }

            for (Entity entity : mc.theWorld.loadedEntityList) {
               if (entity instanceof EntityArrow) {
                  EntityArrow arrow = (EntityArrow)entity;
                  if (arrow.shootingEntity == mc.thePlayer && arrow.getDistanceSqToEntity(target) < 9.0) {
                     ComboMeter.Attack attack = new ComboMeter.Attack((EntityLivingBase)target, ComboState.Feedback.PROJECTILE);
                     Float before = observedHealth.get(target.getEntityId());
                     if (before != null) {
                        attack.health = before;
                     }

                     attack.confirmed = true;
                     pending.addLast(attack);
                     return;
                  }
               }
            }
         }
      }
   }

   public static void tick(Minecraft mc) {
      if (mc.theWorld == world && mc.thePlayer != null && enabled()) {
         if (!mc.isGamePaused()) {
            ticks++;
            if (health >= 0.0F && remaining(mc.thePlayer) < health || !mc.thePlayer.isEntityAlive()) {
               interrupt();
            }

            health = remaining(mc.thePlayer);
            if (state.expire(ticks)) {
               sad();
            }

            Iterator<ComboMeter.Attack> iterator = pending.iterator();

            while (iterator.hasNext()) {
               ComboMeter.Attack attack = iterator.next();
               if (ticks - attack.tick > 20) {
                  iterator.remove();
               } else if (attack.confirmed) {
                  float now = remaining(attack.target);
                  double dealt = attack.health - now;
                  if (dealt > 0.0) {
                     iterator.remove();

                     for (ComboMeter.Attack other : pending) {
                        if (other.target == attack.target) {
                           other.health = Math.min(other.health, now);
                        }
                     }

                     boolean promoted = state.hitDamage(dealt, attack.feedback, ticks);
                     if (promoted || state.rank == ComboState.NAMES.length) {
                        playRank(state.rank);
                     }
                  }
               }
            }

            observedHealth.clear();

            for (Entity entity : mc.theWorld.loadedEntityList) {
               if (entity instanceof EntityLivingBase) {
                  observedHealth.put(entity.getEntityId(), remaining((EntityLivingBase)entity));
               }
            }

            if (sadNotes > 0 && ticks % 5 == 0) {
               mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("note.bass"), 0.5F + sadNotes * 0.16F));
               sadNotes--;
            }
         }
      } else {
         state.clear();
         pending.clear();
         observedHealth.clear();
         world = mc.theWorld;
         health = mc.thePlayer == null ? -1.0F : remaining(mc.thePlayer);
         sadNotes = 0;
         stopRankSound();
      }
   }

   private static void interrupt() {
      pending.clear();
      if (state.breakCombo(ticks)) {
         sad();
      }
   }

   private static void stopRankSound() {
      if (rankSound != null) {
         Minecraft.getMinecraft().getSoundHandler().stopSound(rankSound);
         rankSound = null;
      }
   }

   private static void sad() {
      stopRankSound();
      sadNotes = 3;
   }

   private static void playRank(int rank) {
      stopRankSound();
      sadNotes = 0;
      rankSound = PositionedSoundRecord.create(new ResourceLocation("hud-studio", "combo_" + rank), 1.0F);
      Minecraft.getMinecraft().getSoundHandler().playSound(rankSound);
   }

   private static String number(double value) {
      return String.format(Locale.ROOT, "%.1f", value);
   }

   public static void draw(DrawContext g, boolean preview) {
      boolean demo = preview && !state.active();
      int rank = demo ? 5 : state.rank;
      boolean broken = !demo && rank == 0 && ticks - state.brokenAt < 80;
      if (rank != 0 || broken) {
         int[] colors = new int[]{-4657525, -1447568, -14242, -27812, -37499, -2455297, -6046977, -8845};
         int accent = broken ? -4806231 : colors[rank - 1];
         StudioUi.box(g, 0, 0, 140, 60, StudioTheme.alpha(StudioTheme.color()));
         if (broken) {
            StudioUi.centered(g, "COMBO ENDED", 70.0F, 9.0F, 130, 1.0F, accent);
            StudioUi.centered(g, "Total damage " + number(state.brokenDamage), 70.0F, 27.0F, 128, 0.8F, -1118482);
            StudioUi.centered(g, state.brokenHits + " HITS", 70.0F, 41.0F, 128, 0.8F, -6710887);
         } else {
            int age = demo ? 12 : ticks - state.lastHit;
            float pulse = 1.0F + Math.max(0.0F, 1.0F - age / 7.0F) * 0.18F;
            StudioUi.centered(g, ComboState.NAMES[rank - 1], 70.0F, 6.0F, 128, 1.45F * pulse, accent);
            StudioUi.centered(g, number(demo ? 96.0 : state.damage) + " DMG  /  " + (demo ? 6 : state.hits) + " HITS", 70.0F, 26.0F, 128, 0.8F, -1118482);
            String gain = demo ? "Crit +10.5" : state.feedback.label + " +" + number(state.lastGain);
            StudioUi.centered(g, gain, 70.0F, 38.0F - Math.max(0, 6 - age) * 0.35F, 128, 0.75F, age < 16 ? accent : -6710887);
            StudioUi.box(g, 7, 53, 126, 3, -13027015);
            float remaining = demo ? 0.7F : Numbers.clamp((float)(state.deadline - ticks) / state.duration(), 0.0F, 1.0F);
            StudioUi.box(g, 7, 53, Math.round(126.0F * remaining), 3, accent);
         }
      }
   }

   private static final class Attack {
      final EntityLivingBase target;
      final ComboState.Feedback feedback;
      final int tick;
      float health;
      boolean confirmed;

      Attack(EntityLivingBase target, ComboState.Feedback feedback) {
         this.target = target;
         this.feedback = feedback;
         this.tick = ComboMeter.ticks;
         this.health = ComboMeter.remaining(target);
      }
   }
}

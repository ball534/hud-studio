package dev.hudstudio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import java.util.*;

public final class ComboMeter {
    public static final ComboState state=new ComboState();
    private static final class Attack {
        final LivingEntity target;
        final ComboState.Feedback feedback;
        final int tick;
        float health;
        int confirmedAt=-1;
        Attack(LivingEntity target,ComboState.Feedback feedback,int tick) {
            this.target=target;this.feedback=feedback;this.tick=tick;health=remainingHealth(target);
        }
    }
    private static final Deque<Attack> pending=new ArrayDeque<>();
    private record ObservedHealth(LivingEntity entity,float health) {}
    private record ObservedProjectile(boolean pearl,int seenAt) {}
    private static final Map<Integer,ObservedHealth> observedHealth=new HashMap<>();
    private static final Map<Integer,ObservedProjectile> projectiles=new HashMap<>();
    private static int spearUseAt=-1000,spearSwingAt=-1000;
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static int ticks;
    private static float health=-1;
    private static SimpleSoundInstance rankSound;
    private static int sadNotes;
    private static boolean enabled(){return HudStudioClient.config!=null && HudStudioClient.config.elements.get("combo").visible;}
    public static int ticks(){return ticks;}
    public static void attack(net.minecraft.world.entity.player.Player player,Entity target) {
        if(!enabled() || !(target instanceof LivingEntity living) || ClientTweaks.freecam())return;
        float strength=player.getAttackStrengthScale(.5f);
        boolean crit=strength>.9 && player.fallDistance>0 && !player.onGround() && !player.onClimbable()
            && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger() && !player.isSprinting();
        var feedback=player.getMainHandItem().is(Items.MACE)?ComboState.Feedback.CRUSHED:
            player.getMainHandItem().is(ItemTags.SPEARS)?ComboState.Feedback.DAMAGE:
            crit?ComboState.Feedback.CRIT:ComboState.Feedback.DAMAGE;
        pending.addLast(new Attack(living,feedback,ticks));
        while(pending.size()>24)pending.removeFirst();
    }
    public static void piercingAttack() {spearSwingAt=ticks;}
    public static void damage(ClientboundDamageEventPacket packet) {
        var mc=Minecraft.getInstance();if(!enabled() || mc.player==null || mc.level==null)return;
        if(packet.entityId()==mc.player.getId()) {interrupt();return;}
        if(ClientTweaks.freecam() || packet.sourceCauseId()!=mc.player.getId() || packet.sourceType().is(DamageTypes.ENDER_PEARL))return;
        if(!(mc.level.getEntity(packet.entityId()) instanceof LivingEntity target))return;
        var direct=mc.level.getEntity(packet.sourceDirectId());
        var cached=projectiles.get(packet.sourceDirectId());
        if(direct instanceof ThrownEnderpearl || cached!=null && cached.pearl)return;
        boolean projectile=packet.sourceDirectId()!=mc.player.getId() &&
            (direct instanceof Projectile || cached!=null || packet.sourceType().is(DamageTypeTags.IS_PROJECTILE)
                || packet.sourceType().is(DamageTypes.FIREWORKS));
        if(projectile) {confirm(target,ComboState.Feedback.PROJECTILE);return;}
        if(packet.sourceDirectId()!=mc.player.getId())return;
        if(packet.sourceType().is(DamageTypes.SPEAR)) {
            pending.removeIf(candidate->candidate.confirmedAt<0 && candidate.target==target);
            confirm(target,spearUseAt>spearSwingAt && ticks-spearUseAt<=30?ComboState.Feedback.SPEARED:ComboState.Feedback.DAMAGE);
            return;
        }
        for(var candidate:pending)if(candidate.confirmedAt<0 && candidate.target.getId()==packet.entityId() && ticks-candidate.tick<=30) {
            candidate.confirmedAt=ticks;return;
        }
        // Smash damage may arrive without a queued normal melee attack.
        if(packet.sourceType().is(DamageTypes.MACE_SMASH))confirm(target,ComboState.Feedback.CRUSHED);
    }
    private static void confirm(LivingEntity target,ComboState.Feedback feedback) {
        var attack=new Attack(target,feedback,ticks);
        var before=observedHealth.get(target.getId());
        if(before!=null && before.entity==target)attack.health=before.health;
        attack.confirmedAt=ticks;pending.addLast(attack);
        while(pending.size()>128)pending.removeFirst();
    }
    private static void observe(Minecraft mc) {
        observedHealth.clear();
        for(var entity:mc.level.entitiesForRendering()) {
            if(entity instanceof LivingEntity living)observedHealth.put(entity.getId(),new ObservedHealth(living,remainingHealth(living)));
            if(entity instanceof Projectile projectile)projectiles.put(entity.getId(),new ObservedProjectile(projectile instanceof ThrownEnderpearl,ticks));
        }
        projectiles.values().removeIf(projectile->ticks-projectile.seenAt>40);
        if(mc.player.isUsingItem() && mc.player.getUseItem().is(ItemTags.SPEARS))spearUseAt=ticks;
    }
    private static float remainingHealth(LivingEntity target) {return target.getHealth()+target.getAbsorptionAmount();}
    private static void resolveDamage() {
        // Health metadata can arrive after the damage event. Wait for a measured loss,
        // rather than awarding predicted weapon damage or multiplying a crit twice.
        for(var iterator=pending.iterator();iterator.hasNext();) {
            var attack=iterator.next();
            if(attack.confirmedAt<0)continue;
            float remaining=remainingHealth(attack.target);
            double dealt=attack.health-remaining;
            if(dealt>0) {
                iterator.remove();
                for(var other:pending)if(other.target==attack.target)other.health=Math.min(other.health,remaining);
                boolean promoted=state.hitDamage(dealt,attack.feedback,ticks);
                if(promoted || state.rank==ComboState.NAMES.length)playRank(state.rank);
            } else if(ticks-attack.confirmedAt>=10)iterator.remove();
        }
    }
    public static void tick(Minecraft mc) {
        if(mc.level!=level || mc.player==null || !enabled()) {
            state.clear();pending.clear();observedHealth.clear();projectiles.clear();spearUseAt=spearSwingAt=-1000;
            health=mc.player==null?-1:mc.player.getHealth();level=mc.level;sadNotes=0;stopRankSound();return;
        }
        if(mc.isPaused())return;
        ticks++;
        pending.removeIf(attack->ticks-attack.tick>30);
        if(health>=0 && mc.player.getHealth()<health)interrupt();
        health=mc.player.getHealth();
        if(state.expire(ticks))sad();
        if(!mc.player.isAlive())interrupt();
        else resolveDamage();
        observe(mc);
        if(sadNotes>0 && ticks%5==0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(),.5f+sadNotes*.16f,.65f));sadNotes--;
        }
    }
    private static void interrupt(){pending.clear();if(state.breakCombo(ticks))sad();}
    private static void stopRankSound(){if(rankSound!=null){Minecraft.getInstance().getSoundManager().stop(rankSound);rankSound=null;}}
    private static void sad(){stopRankSound();sadNotes=3;}
    private static void playRank(int rank) {
        stopRankSound();sadNotes=0;
        var sound=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("hud-studio","combo_"+rank));
        rankSound=SimpleSoundInstance.forUI(sound,1,.8f);Minecraft.getInstance().getSoundManager().play(rankSound);
    }
    private static String number(double value){return String.format(Locale.ROOT,"%.1f",value);}
    public static void draw(GuiGraphicsExtractor g,boolean preview) {
        boolean demo=preview && !state.active();
        int rank=demo?5:state.rank;
        boolean broken=!demo && rank==0 && ticks-state.brokenAt<80;
        if(rank==0 && !broken)return;
        int[] colors={0xffb8ee8b,0xffe9e970,0xffffc85e,0xffff935c,0xffff6d85,0xffda88ff,0xffa3baff,0xffffdd73};
        int accent=broken?0xffb6a9a9:colors[rank-1];
        StudioUi.box(g,0,0,140,60,StudioTheme.alpha(StudioTheme.color()));
        if(broken) {
            StudioUi.centered(g,"COMBO ENDED",70,9,130,1,accent);
            StudioUi.centered(g,"Total damage "+number(state.brokenDamage),70,27,128,.8f,StudioUi.TEXT);
            StudioUi.centered(g,state.brokenHits+" HITS",70,41,128,.8f,StudioUi.MUTED);return;
        }
        int age=demo?12:ticks-state.lastHit;
        float pulse=1+Math.max(0,1-age/7f)*.18f;
        StudioUi.centered(g,ComboState.NAMES[rank-1],70,6,128,1.45f*pulse,accent);
        double damage=demo?96:state.damage;
        StudioUi.centered(g,number(damage)+" DMG  /  "+(demo?6:state.hits)+" HITS",70,26,128,.8f,StudioUi.TEXT);
        String gain=demo?"Crit +10.5":state.feedback.label+" +"+number(state.lastGain);
        StudioUi.centered(g,gain,70,38-Math.max(0,6-age)*.35f,128,.75f,age<16?accent:StudioUi.MUTED);
        StudioUi.box(g,7,53,126,3,0xff393939);
        float remaining=demo?.7f:Math.clamp((state.deadline-ticks)/(float)state.duration(),0,1);
        StudioUi.box(g,7,53,Math.round(126*remaining),3,accent);
    }
}

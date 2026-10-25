package dev.hudstudio.client;

/** Deterministic damage and hit tracking, independent of rendering and networking. */
public final class ComboState {
    public enum Feedback {
        DAMAGE("Damage"), CRIT("Crit"), PROJECTILE("HIT"), SPEARED("Speared"), CRUSHED("CRUSHED");
        public final String label;
        Feedback(String label) {this.label=label;}
    }
    public Feedback feedback=Feedback.DAMAGE;
    public static final String[] NAMES={"GOOD","GREAT","BRUTAL","SAVAGE","STYLISH","SENSATIONAL","UNSTOPPABLE","LEGENDARY"};
    public int hits,rank,deadline,lastHit=-1000,brokenAt=-1000,brokenHits;
    public double damage,lastGain,lastMultiplier=1,brokenDamage;
    public int duration() {return 80-Math.max(0,rank-1)*6;}
    public boolean active(){return rank>0;}
    public boolean hit(double base,double multiplier,int tick) {
        return hitDamage(base*multiplier,multiplier>1,tick);
    }
    public boolean hitDamage(double amount,boolean critical,int tick) {
        return hitDamage(amount,critical?Feedback.CRIT:Feedback.DAMAGE,tick);
    }
    public boolean hitDamage(double amount,Feedback kind,int tick) {
        if(!Double.isFinite(amount) || amount<=0)return false;
        if(hits>0 && tick>=deadline)breakCombo(tick);
        feedback=kind;lastMultiplier=kind==Feedback.CRIT?1.5:1;lastGain=amount;damage+=lastGain;hits++;lastHit=tick;brokenAt=-1000;
        int previous=rank;
        rank=Math.min(hits-1,NAMES.length);
        deadline=tick+duration();
        return rank!=previous;
    }
    public boolean expire(int tick) {return hits>0 && tick>=deadline && breakCombo(tick);}
    public boolean breakCombo(int tick) {
        boolean active=active();
        if(active) {brokenDamage=damage;brokenHits=hits;brokenAt=tick;}
        hits=0;rank=0;damage=0;deadline=0;return active;
    }
    public void clear(){hits=rank=deadline=brokenHits=0;damage=lastGain=brokenDamage=0;lastMultiplier=1;feedback=Feedback.DAMAGE;lastHit=brokenAt=-1000;}
}

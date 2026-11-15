package dev.hudstudio.client;

public final class ComboState {
   public ComboState.Feedback feedback = ComboState.Feedback.DAMAGE;
   public static final String[] NAMES = new String[]{"GOOD", "GREAT", "BRUTAL", "SAVAGE", "STYLISH", "SENSATIONAL", "UNSTOPPABLE", "LEGENDARY"};
   public int hits;
   public int rank;
   public int deadline;
   public int lastHit = -1000;
   public int brokenAt = -1000;
   public int brokenHits;
   public double damage;
   public double lastGain;
   public double lastMultiplier = 1.0;
   public double brokenDamage;

   public int duration() {
      return 80 - Math.max(0, this.rank - 1) * 6;
   }

   public boolean active() {
      return this.rank > 0;
   }

   public boolean hit(double base, double multiplier, int tick) {
      return this.hitDamage(base * multiplier, multiplier > 1.0, tick);
   }

   public boolean hitDamage(double amount, boolean critical, int tick) {
      return this.hitDamage(amount, critical ? ComboState.Feedback.CRIT : ComboState.Feedback.DAMAGE, tick);
   }

   public boolean hitDamage(double amount, ComboState.Feedback kind, int tick) {
      if (Double.isFinite(amount) && !(amount <= 0.0)) {
         if (this.hits > 0 && tick >= this.deadline) {
            this.breakCombo(tick);
         }

         this.feedback = kind;
         this.lastMultiplier = kind == ComboState.Feedback.CRIT ? 1.5 : 1.0;
         this.lastGain = amount;
         this.damage = this.damage + this.lastGain;
         this.hits++;
         this.lastHit = tick;
         this.brokenAt = -1000;
         int previous = this.rank;
         this.rank = Math.min(this.hits - 1, NAMES.length);
         this.deadline = tick + this.duration();
         return this.rank != previous;
      } else {
         return false;
      }
   }

   public boolean expire(int tick) {
      return this.hits > 0 && tick >= this.deadline && this.breakCombo(tick);
   }

   public boolean breakCombo(int tick) {
      boolean active = this.active();
      if (active) {
         this.brokenDamage = this.damage;
         this.brokenHits = this.hits;
         this.brokenAt = tick;
      }

      this.hits = 0;
      this.rank = 0;
      this.damage = 0.0;
      this.deadline = 0;
      return active;
   }

   public void clear() {
      this.hits = this.rank = this.deadline = this.brokenHits = 0;
      this.damage = this.lastGain = this.brokenDamage = 0.0;
      this.lastMultiplier = 1.0;
      this.feedback = ComboState.Feedback.DAMAGE;
      this.lastHit = this.brokenAt = -1000;
   }

   public enum Feedback {
      DAMAGE("Damage"),
      CRIT("Crit"),
      PROJECTILE("HIT"),
      SPEARED("Speared"),
      CRUSHED("CRUSHED");

      public final String label;

      Feedback(String label) {
         this.label = label;
      }
   }
}

package net.enderwish.HUD_Visuals_Subpack.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

/**
 * Single source of truth for Player Stats: BPM, Energy, and Limb Health.
 * Balanced for a high school student setting with unique toughness for each part.
 */
public class WristCapability {
    private int bpm = 70;
    private float energy = 100.0f;
    private boolean watchEquipped = false;

    // Current health of limbs
    private float headHealth;
    private float torsoHealth;
    private float leftArmHealth;
    private float rightArmHealth;
    private float leftLegHealth;
    private float rightLegHealth;
    private float leftFootHealth;
    private float rightFootHealth;

    // --- TOUGHNESS SETTINGS (Maximums) ---
    // These define how much "damage" each part can take before breaking.
    public float getMaxHead() { return 10.0f; }   // Sensitive
    public float getMaxTorso() { return 25.0f; }  // Very Tough
    public float getMaxArm() { return 15.0f; }    // Standard
    public float getMaxLeg() { return 18.0f; }    // Stronger
    public float getMaxFoot() { return 12.0f; }   // Sensitive to jumps

    public WristCapability() {
        healAll();
    }

    // Full constructor for the Codec and sync packets
    public WristCapability(int bpm, float energy, boolean watchEquipped, float head, float torso,
                           float lArm, float rArm, float lLeg, float rLeg, float lFoot, float rFoot) {
        this.bpm = bpm;
        this.energy = energy;
        this.watchEquipped = watchEquipped;
        this.headHealth = head;
        this.torsoHealth = torso;
        this.leftArmHealth = lArm;
        this.rightArmHealth = rArm;
        this.leftLegHealth = lLeg;
        this.rightLegHealth = rLeg;
        this.leftFootHealth = lFoot;
        this.rightFootHealth = rFoot;
    }

    public static final Codec<WristCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("bpm").forGetter(WristCapability::getBPM),
            Codec.FLOAT.fieldOf("energy").forGetter(WristCapability::getEnergy),
            Codec.BOOL.fieldOf("watch_equipped").forGetter(WristCapability::hasWatchEquipped),
            Codec.FLOAT.fieldOf("head").forGetter(WristCapability::getHeadHealth),
            Codec.FLOAT.fieldOf("torso").forGetter(WristCapability::getTorsoHealth),
            Codec.FLOAT.fieldOf("left_arm").forGetter(WristCapability::getLeftArmHealth),
            Codec.FLOAT.fieldOf("right_arm").forGetter(WristCapability::getRightArmHealth),
            Codec.FLOAT.fieldOf("left_leg").forGetter(WristCapability::getLeftLegHealth),
            Codec.FLOAT.fieldOf("right_leg").forGetter(WristCapability::getRightLegHealth),
            Codec.FLOAT.fieldOf("left_foot").forGetter(WristCapability::getLeftFootHealth),
            Codec.FLOAT.fieldOf("right_foot").forGetter(WristCapability::getRightFootHealth)
    ).apply(inst, WristCapability::new));

    // --- GETTERS (Raw Values) ---
    public int getBPM() { return bpm; }
    public float getEnergy() { return energy; }
    public boolean hasWatchEquipped() { return watchEquipped; }
    public float getHeadHealth() { return headHealth; }
    public float getTorsoHealth() { return torsoHealth; }
    public float getLeftArmHealth() { return leftArmHealth; }
    public float getRightArmHealth() { return rightArmHealth; }
    public float getLeftLegHealth() { return leftLegHealth; }
    public float getRightLegHealth() { return rightLegHealth; }
    public float getLeftFootHealth() { return leftFootHealth; }
    public float getRightFootHealth() { return rightFootHealth; }

    // --- PERCENTAGES (For HUD Colors) ---
    // Use these in SportsWatchHUD to determine if a limb is Green, Yellow, or Red.
    public float getHeadPct() { return headHealth / getMaxHead(); }
    public float getTorsoPct() { return torsoHealth / getMaxTorso(); }
    public float getLArmPct() { return leftArmHealth / getMaxArm(); }
    public float getRArmPct() { return rightArmHealth / getMaxArm(); }
    public float getLLegPct() { return leftLegHealth / getMaxLeg(); }
    public float getRLegPct() { return rightLegHealth / getMaxLeg(); }
    public float getLFootPct() { return leftFootHealth / getMaxFoot(); }
    public float getRFootPct() { return rightFootHealth / getMaxFoot(); }

    // --- SETTERS ---
    public void setBPM(int bpm) { this.bpm = bpm; }
    public void setEnergy(float energy) { this.energy = Mth.clamp(energy, 0, 100); }
    public void setWatchEquipped(boolean state) { this.watchEquipped = state; }

    // --- DAMAGE METHODS ---
    // Clamps damage between 0 and the unique Max for that limb.
    public void damageHead(float amount) { this.headHealth = Mth.clamp(headHealth - amount, 0, getMaxHead()); }
    public void damageTorso(float amount) { this.torsoHealth = Mth.clamp(torsoHealth - amount, 0, getMaxTorso()); }
    public void damageLeftArm(float amount) { this.leftArmHealth = Mth.clamp(leftArmHealth - amount, 0, getMaxArm()); }
    public void damageRightArm(float amount) { this.rightArmHealth = Mth.clamp(rightArmHealth - amount, 0, getMaxArm()); }
    public void damageLeftLeg(float amount) { this.leftLegHealth = Mth.clamp(leftLegHealth - amount, 0, getMaxLeg()); }
    public void damageRightLeg(float amount) { this.rightLegHealth = Mth.clamp(rightLegHealth - amount, 0, getMaxLeg()); }
    public void damageLeftFoot(float amount) { this.leftFootHealth = Mth.clamp(leftFootHealth - amount, 0, getMaxFoot()); }
    public void damageRightFoot(float amount) { this.rightFootHealth = Mth.clamp(rightFootHealth - amount, 0, getMaxFoot()); }

    public void healAll() {
        this.headHealth = getMaxHead();
        this.torsoHealth = getMaxTorso();
        this.leftArmHealth = getMaxArm();
        this.rightArmHealth = getMaxArm();
        this.leftLegHealth = getMaxLeg();
        this.rightLegHealth = getMaxLeg();
        this.leftFootHealth = getMaxFoot();
        this.rightFootHealth = getMaxFoot();
    }

    public void copyFrom(WristCapability source) {
        this.bpm = source.bpm;
        this.energy = source.energy;
        this.watchEquipped = source.watchEquipped;
        this.headHealth = source.headHealth;
        this.torsoHealth = source.torsoHealth;
        this.leftArmHealth = source.leftArmHealth;
        this.rightArmHealth = source.rightArmHealth;
        this.leftLegHealth = source.leftLegHealth;
        this.rightLegHealth = source.rightLegHealth;
        this.leftFootHealth = source.leftFootHealth;
        this.rightFootHealth = source.rightFootHealth;
    }
}
package net.enderwish.HUD_Visuals_Subpack.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Tracks BPM, Energy, Watch State, and Limb Damage.
 */
public class WristCapability {
    private int bpm;
    private float energy;
    private boolean watchEquipped;

    // Limb Health (1.0 = Healthy, 0.0 = Broken)
    private float headHealth;
    private float torsoHealth;
    private float leftArmHealth;
    private float rightArmHealth;
    private float leftLegHealth;
    private float rightLegHealth;

    // Updated Codec to include all limb health fields
    public static final Codec<WristCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("bpm").forGetter(WristCapability::getBPM),
            Codec.FLOAT.fieldOf("energy").forGetter(WristCapability::getEnergy),
            Codec.BOOL.fieldOf("watch_equipped").forGetter(WristCapability::hasWatchEquipped),
            Codec.FLOAT.fieldOf("head").orElse(1.0f).forGetter(WristCapability::getHeadHealth),
            Codec.FLOAT.fieldOf("torso").orElse(1.0f).forGetter(WristCapability::getTorsoHealth),
            Codec.FLOAT.fieldOf("left_arm").orElse(1.0f).forGetter(WristCapability::getLeftArmHealth),
            Codec.FLOAT.fieldOf("right_arm").orElse(1.0f).forGetter(WristCapability::getRightArmHealth),
            Codec.FLOAT.fieldOf("left_leg").orElse(1.0f).forGetter(WristCapability::getLeftLegHealth),
            Codec.FLOAT.fieldOf("right_leg").orElse(1.0f).forGetter(WristCapability::getRightLegHealth)
    ).apply(inst, WristCapability::new));

    // Default constructor for new players
    public WristCapability() {
        this.bpm = 70;
        this.energy = 1.0f;
        this.watchEquipped = false;
        this.headHealth = 1.0f;
        this.torsoHealth = 1.0f;
        this.leftArmHealth = 1.0f;
        this.rightArmHealth = 1.0f;
        this.leftLegHealth = 1.0f;
        this.rightLegHealth = 1.0f;
    }

    // Constructor used by the Codec
    public WristCapability(int bpm, float energy, boolean watchEquipped,
                           float head, float torso, float lArm, float rArm, float lLeg, float rLeg) {
        this.bpm = bpm;
        this.energy = energy;
        this.watchEquipped = watchEquipped;
        this.headHealth = head;
        this.torsoHealth = torso;
        this.leftArmHealth = lArm;
        this.rightArmHealth = rArm;
        this.leftLegHealth = lLeg;
        this.rightLegHealth = rLeg;
    }

    // --- GETTERS AND SETTERS ---

    public int getBPM() { return bpm; }
    public void setBPM(int bpm) { this.bpm = bpm; }

    public float getEnergy() { return energy; }
    public void setEnergy(float energy) { this.energy = energy; }

    public boolean hasWatchEquipped() { return watchEquipped; }
    public void setWatchEquipped(boolean watchEquipped) { this.watchEquipped = watchEquipped; }

    // Limb Health Getters
    public float getHeadHealth() { return headHealth; }
    public float getTorsoHealth() { return torsoHealth; }
    public float getLeftArmHealth() { return leftArmHealth; }
    public float getRightArmHealth() { return rightArmHealth; }
    public float getLeftLegHealth() { return leftLegHealth; }
    public float getRightLegHealth() { return rightLegHealth; }

    // Limb Health Setters (clamped between 0.0 and 1.0)
    public void setHeadHealth(float health) { this.headHealth = Math.clamp(health, 0.0f, 1.0f); }
    public void setTorsoHealth(float health) { this.torsoHealth = Math.clamp(health, 0.0f, 1.0f); }
    public void setLeftArmHealth(float health) { this.leftArmHealth = Math.clamp(health, 0.0f, 1.0f); }
    public void setRightArmHealth(float health) { this.rightArmHealth = Math.clamp(health, 0.0f, 1.0f); }
    public void setLeftLegHealth(float health) { this.leftLegHealth = Math.clamp(health, 0.0f, 1.0f); }
    public void setRightLegHealth(float health) { this.rightLegHealth = Math.clamp(health, 0.0f, 1.0f); }

    // Logic for damaging limbs
    public void damageHead(float amount) { this.headHealth = Math.max(0, this.headHealth - amount); }
    public void damageTorso(float amount) { this.torsoHealth = Math.max(0, this.torsoHealth - amount); }
    public void damageLeftArm(float amount) { this.leftArmHealth = Math.max(0, this.leftArmHealth - amount); }
    public void damageRightArm(float amount) { this.rightArmHealth = Math.max(0, this.rightArmHealth - amount); }
    public void damageLeftLeg(float amount) { this.leftLegHealth = Math.max(0, this.leftLegHealth - amount); }
    public void damageRightLeg(float amount) { this.rightLegHealth = Math.max(0, this.rightLegHealth - amount); }

    public void healAll() {
        this.headHealth = 1.0f;
        this.torsoHealth = 1.0f;
        this.leftArmHealth = 1.0f;
        this.rightArmHealth = 1.0f;
        this.leftLegHealth = 1.0f;
        this.rightLegHealth = 1.0f;
    }

    /**
     * Helper to copy data from one capability instance to another.
     * Useful for PlayerEvent.Clone (respawning).
     */
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
    }
}
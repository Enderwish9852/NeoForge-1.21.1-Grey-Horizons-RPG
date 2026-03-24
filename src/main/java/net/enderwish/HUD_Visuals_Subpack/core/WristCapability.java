package net.enderwish.HUD_Visuals_Subpack.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

/**
 * Single source of truth for Player Stats: BPM, Energy, Thirst, Hunger, and Limb Health.
 * Updated: Added Hunger (Fuel Tank) logic and tick-based stat drainage/regeneration.
 */
public class WristCapability {
    private int bpm = 70;
    private float energy = 100.0f;
    private float thirst = 100.0f; // Hydration level (0 to 100)
    private float hunger = 100.0f; // Fuel level (0 to 100) - Replaces vanilla food
    private boolean watchEquipped = false;
    private int starvationTimer = 0; // Ticks elapsed since hunger hit zero (0 to 12000)

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
    public float getMaxHead() { return 10.0f; }
    public float getMaxTorso() { return 25.0f; }
    public float getMaxArm() { return 15.0f; }
    public float getMaxLeg() { return 18.0f; }
    public float getMaxFoot() { return 12.0f; }

    public WristCapability() {
        healAll();
    }

    /**
     * Full constructor used by Codec and Sync Packets.
     * Updated to 14 arguments to include Hunger.
     */
    public WristCapability(int bpm, float energy, float thirst, float hunger, boolean watchEquipped,
                           float head, float torso, float lArm, float rArm,
                           float lLeg, float rLeg, float lFoot, float rFoot,
                           int starvationTimer) {
        this.bpm = bpm;
        this.energy = energy;
        this.thirst = thirst;
        this.hunger = hunger;
        this.watchEquipped = watchEquipped;
        this.headHealth = head;
        this.torsoHealth = torso;
        this.leftArmHealth = lArm;
        this.rightArmHealth = rArm;
        this.leftLegHealth = lLeg;
        this.rightLegHealth = rLeg;
        this.leftFootHealth = lFoot;
        this.rightFootHealth = rFoot;
        this.starvationTimer = starvationTimer;
    }

    /**
     * Codec for saving/loading data to the player's NBT file.
     */
    public static final Codec<WristCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("bpm").forGetter(WristCapability::getBPM),
            Codec.FLOAT.fieldOf("energy").forGetter(WristCapability::getEnergy),
            Codec.FLOAT.fieldOf("thirst").forGetter(WristCapability::getThirst),
            Codec.FLOAT.fieldOf("hunger").forGetter(WristCapability::getHunger),
            Codec.BOOL.fieldOf("watch_equipped").forGetter(WristCapability::hasWatchEquipped),
            Codec.FLOAT.fieldOf("head").forGetter(WristCapability::getHeadHealth),
            Codec.FLOAT.fieldOf("torso").forGetter(WristCapability::getTorsoHealth),
            Codec.FLOAT.fieldOf("left_arm").forGetter(WristCapability::getLeftArmHealth),
            Codec.FLOAT.fieldOf("right_arm").forGetter(WristCapability::getRightArmHealth),
            Codec.FLOAT.fieldOf("left_leg").forGetter(WristCapability::getLeftLegHealth),
            Codec.FLOAT.fieldOf("right_leg").forGetter(WristCapability::getRightLegHealth),
            Codec.FLOAT.fieldOf("left_foot").forGetter(WristCapability::getLeftFootHealth),
            Codec.FLOAT.fieldOf("right_foot").forGetter(WristCapability::getRightFootHealth),
            Codec.INT.fieldOf("starvation_timer").forGetter(WristCapability::getStarvationTimer)
    ).apply(inst, WristCapability::new));

    /**
     * --- FUEL TANK LOGIC ---
     * This should be called from a PlayerTickEvent on the server side.
     */
    public void tickStats(boolean isSprinting, boolean isMoving) {
        // 1. Hunger (Fuel) Drains over time
        float hungerDrain = 0.001f;
        if (isSprinting) hungerDrain += 0.008f;
        else if (isMoving) hungerDrain += 0.002f;
        setHunger(this.hunger - hungerDrain);

        // 2. Thirst Drains over time
        setThirst(this.thirst - 0.0015f);

        // 3. Energy Regeneration (Fueled by Hunger)
        if (!isSprinting && this.energy < 100.0f) {
            // Regeneration is slower when hunger is low
            float efficiency = Math.max(0.05f, this.hunger / 100.0f);
            setEnergy(this.energy + (0.15f * efficiency));
        }

        // 4. Energy Consumption
        if (isSprinting) {
            setEnergy(this.energy - 0.3f);
        }
    }

    // --- GETTERS ---
    public int getBPM() { return bpm; }
    public float getEnergy() { return energy; }
    public float getThirst() { return thirst; }
    public float getHunger() { return hunger; }
    public boolean hasWatchEquipped() { return watchEquipped; }
    public int getStarvationTimer() { return starvationTimer; }

    public float getHeadHealth() { return headHealth; }
    public float getTorsoHealth() { return torsoHealth; }
    public float getLeftArmHealth() { return leftArmHealth; }
    public float getRightArmHealth() { return rightArmHealth; }
    public float getLeftLegHealth() { return leftLegHealth; }
    public float getRightLegHealth() { return rightLegHealth; }
    public float getLeftFootHealth() { return leftFootHealth; }
    public float getRightFootHealth() { return rightFootHealth; }

    // --- PERCENTAGES ---
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
    public void setThirst(float thirst) { this.thirst = Mth.clamp(thirst, 0, 100); }
    public void setHunger(float hunger) { this.hunger = Mth.clamp(hunger, 0, 100); }
    public void setWatchEquipped(boolean state) { this.watchEquipped = state; }
    public void setStarvationTimer(int ticks) { this.starvationTimer = Math.max(0, ticks); }

    public void setHeadHealth(float val) { this.headHealth = Mth.clamp(val, 0, getMaxHead()); }
    public void setTorsoHealth(float val) { this.torsoHealth = Mth.clamp(val, 0, getMaxTorso()); }
    public void setLeftArmHealth(float val) { this.leftArmHealth = Mth.clamp(val, 0, getMaxArm()); }
    public void setRightArmHealth(float val) { this.rightArmHealth = Mth.clamp(val, 0, getMaxArm()); }
    public void setLeftLegHealth(float val) { this.leftLegHealth = Mth.clamp(val, 0, getMaxLeg()); }
    public void setRightLegHealth(float val) { this.rightLegHealth = Mth.clamp(val, 0, getMaxLeg()); }
    public void setLeftFootHealth(float val) { this.leftFootHealth = Mth.clamp(val, 0, getMaxFoot()); }
    public void setRightFootHealth(float val) { this.rightFootHealth = Mth.clamp(val, 0, getMaxFoot()); }

    // --- DAMAGE METHODS ---
    public void damageHead(float amount) { setHeadHealth(headHealth - amount); }
    public void damageTorso(float amount) { setTorsoHealth(torsoHealth - amount); }
    public void damageLeftArm(float amount) { setLeftArmHealth(leftArmHealth - amount); }
    public void damageRightArm(float amount) { setRightArmHealth(rightArmHealth - amount); }
    public void damageLeftLeg(float amount) { setLeftLegHealth(leftLegHealth - amount); }
    public void damageRightLeg(float amount) { setRightLegHealth(rightLegHealth - amount); }
    public void damageLeftFoot(float amount) { setLeftFootHealth(leftFootHealth - amount); }
    public void damageRightFoot(float amount) { setRightFootHealth(rightFootHealth - amount); }

    public void healAll() {
        this.headHealth = getMaxHead();
        this.torsoHealth = getMaxTorso();
        this.leftArmHealth = getMaxArm();
        this.rightArmHealth = getMaxArm();
        this.leftLegHealth = getMaxLeg();
        this.rightLegHealth = getMaxLeg();
        this.leftFootHealth = getMaxFoot();
        this.rightFootHealth = getMaxFoot();
        this.energy = 100.0f;
        this.thirst = 100.0f;
        this.hunger = 100.0f;
        this.starvationTimer = 0;
        this.bpm = 70;
    }

    public void copyFrom(WristCapability source) {
        this.bpm = source.bpm;
        this.energy = source.energy;
        this.thirst = source.thirst;
        this.hunger = source.hunger;
        this.watchEquipped = source.watchEquipped;
        this.starvationTimer = source.starvationTimer;
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
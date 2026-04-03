package net.enderwish.HUD_Visuals_Subpack.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

/**
 * Single source of truth for Player Stats.
 * Updated: Added Core Temperature and Wetness levels.
 */
public class PlayerCapability {
    private int bpm = 70;
    private float energy = 100.0f;
    private float thirst = 100.0f;
    private float hunger = 100.0f;
    private boolean watchEquipped = false;
    private int starvationTimer = 0;
    private int pollenExposure = 0;

    // --- NEW: CLIMATE STATS ---
    private float coreTemp = 0.7f; // 0.7f is neutral/healthy
    private float wetness = 0.0f;  // 0.0f to 1.0f

    // Limb health
    private float headHealth, torsoHealth, leftArmHealth, rightArmHealth;
    private float leftLegHealth, rightLegHealth, leftFootHealth, rightFootHealth;

    public float getMaxHead() { return 10.0f; }
    public float getMaxTorso() { return 25.0f; }
    public float getMaxArm() { return 15.0f; }
    public float getMaxLeg() { return 18.0f; }
    public float getMaxFoot() { return 12.0f; }

    public PlayerCapability() {
        healAll();
    }

    /**
     * Updated Constructor for Codec/Syncing (17 arguments).
     */
    public PlayerCapability(int bpm, float energy, float thirst, float hunger, boolean watchEquipped,
                            float head, float torso, float lArm, float rArm,
                            float lLeg, float rLeg, float lFoot, float rFoot,
                            int starvationTimer, int pollenExposure, float coreTemp, float wetness) {
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
        this.pollenExposure = pollenExposure;
        this.coreTemp = coreTemp;
        this.wetness = wetness;
    }

    // 1. Group for Stats (BPM, Energy, Thirst, Hunger, Watch, Timers, Climate) - 9 Fields
    private static final MapCodec<StatData> STAT_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.fieldOf("bpm").forGetter(StatData::bpm),
            Codec.FLOAT.fieldOf("energy").forGetter(StatData::energy),
            Codec.FLOAT.fieldOf("thirst").forGetter(StatData::thirst),
            Codec.FLOAT.fieldOf("hunger").forGetter(StatData::hunger),
            Codec.BOOL.fieldOf("watch_equipped").forGetter(StatData::watch),
            Codec.INT.fieldOf("starvation_timer").forGetter(StatData::starve),
            Codec.INT.fieldOf("pollen_exposure").forGetter(StatData::pollen),
            Codec.FLOAT.fieldOf("core_temp").forGetter(StatData::temp),
            Codec.FLOAT.fieldOf("wetness").forGetter(StatData::wet)
    ).apply(inst, StatData::new));
    // 2. Group for Body Parts (Head, Torso, Arms, Legs, Feet) - 8 Fields
    private static final MapCodec<BodyData> BODY_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("head").forGetter(BodyData::head),
            Codec.FLOAT.fieldOf("torso").forGetter(BodyData::torso),
            Codec.FLOAT.fieldOf("left_arm").forGetter(BodyData::lArm),
            Codec.FLOAT.fieldOf("right_arm").forGetter(BodyData::rArm),
            Codec.FLOAT.fieldOf("left_leg").forGetter(BodyData::lLeg),
            Codec.FLOAT.fieldOf("right_leg").forGetter(BodyData::rLeg),
            Codec.FLOAT.fieldOf("left_foot").forGetter(BodyData::lFoot),
            Codec.FLOAT.fieldOf("right_foot").forGetter(BodyData::rFoot)
    ).apply(inst, BodyData::new));
    // 3. The Final Combined CODEC
    public static final Codec<PlayerCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            STAT_CODEC.forGetter(p -> new StatData(p.getBPM(), p.getEnergy(), p.getThirst(), p.getHunger(), p.hasWatchEquipped(), p.getStarvationTimer(), p.getPollenExposure(), p.getCoreTemp(), p.getWetness())),
            BODY_CODEC.forGetter(p -> new BodyData(p.getHeadHealth(), p.getTorsoHealth(), p.getLeftArmHealth(), p.getRightArmHealth(), p.getLeftLegHealth(), p.getRightLegHealth(), p.getLeftFootHealth(), p.getRightFootHealth()))
    ).apply(inst, (stats, body) -> new PlayerCapability(
            stats.bpm, stats.energy, stats.thirst, stats.hunger, stats.watch,
            body.head, body.torso, body.lArm, body.rArm, body.lLeg, body.rLeg, body.lFoot, body.rFoot,
            stats.starve, stats.pollen, stats.temp, stats.wet
    )));

    public void damageHead(float amount) { setHeadHealth(this.headHealth - amount); }
    public void damageTorso(float amount) { setTorsoHealth(this.torsoHealth - amount); }
    public void damageLArm(float amount) { setLeftArmHealth(this.leftArmHealth - amount); }
    public void damageRArm(float amount) { setRightArmHealth(this.rightArmHealth - amount); }
    public void damageLLeg(float amount) { setLeftLegHealth(this.leftLegHealth - amount); }
    public void damageRLeg(float amount) { setRightLegHealth(this.rightLegHealth - amount); }
    public void damageLFoot(float amount) { setLeftFootHealth(this.leftFootHealth - amount); }
    public void damageRFoot(float amount) { setRightFootHealth(this.rightFootHealth - amount); }

    /**
     * Updated Tick Logic.
     * @param tempOffset The temperature offset from the current weather/season.
     */
    public void tickStats(boolean isSprinting, boolean isMoving, float tempOffset, boolean inRain, boolean inWater) {
        // 1. Basic Drains
        setHunger(this.hunger - (isSprinting ? 0.009f : (isMoving ? 0.003f : 0.001f)));
        setThirst(this.thirst - (tempOffset > 0.5f ? 0.004f : 0.0015f)); // Thirstier in heat

        // 2. Core Temp Logic
        float targetTemp = 0.7f + tempOffset;
        // Wetness makes you cool down significantly faster
        float approachSpeed = (this.wetness > 0.5f && tempOffset < 0) ? 0.0005f : 0.0001f;
        setCoreTemp(Mth.lerp(approachSpeed, this.coreTemp, targetTemp));

        // 3. Wetness Logic
        if (inWater) setWetness(this.wetness + 0.05f);
        else if (inRain) setWetness(this.wetness + 0.01f);
        else setWetness(this.wetness - 0.005f); // Natural drying

        // 4. Energy Logic
        if (isSprinting) setEnergy(this.energy - 0.3f);
        else if (this.energy < 100.0f) {
            float efficiency = Math.max(0.05f, this.hunger / 100.0f);
            setEnergy(this.energy + (0.15f * efficiency));
        }
    }

    // --- GETTERS ---
    public float getCoreTemp() { return coreTemp; }
    public float getWetness() { return wetness; }
    public int getBPM() { return bpm; }
    public float getEnergy() { return energy; }
    public float getThirst() { return thirst; }
    public float getHunger() { return hunger; }
    public boolean hasWatchEquipped() { return watchEquipped; }
    public int getStarvationTimer() { return starvationTimer; }
    public int getPollenExposure() { return pollenExposure; }
    public float getHeadHealth() { return headHealth; }
    public float getTorsoHealth() { return torsoHealth; }
    public float getLeftArmHealth() { return leftArmHealth; }
    public float getRightArmHealth() { return rightArmHealth; }
    public float getLeftLegHealth() { return leftLegHealth; }
    public float getRightLegHealth() { return rightLegHealth; }
    public float getLeftFootHealth() { return leftFootHealth; }
    public float getRightFootHealth() { return rightFootHealth; }

    // --- SETTERS ---
    public void setCoreTemp(float val) { this.coreTemp = Mth.clamp(val, 0.0f, 2.0f); }
    public void setWetness(float val) { this.wetness = Mth.clamp(val, 0.0f, 1.0f); }
    public void setBPM(int bpm) { this.bpm = bpm; }
    public void setEnergy(float energy) { this.energy = Mth.clamp(energy, 0, 100); }
    public void setThirst(float thirst) { this.thirst = Mth.clamp(thirst, 0, 100); }
    public void setHunger(float hunger) { this.hunger = Mth.clamp(hunger, 0, 100); }
    public void setWatchEquipped(boolean state) { this.watchEquipped = state; }
    public void setStarvationTimer(int ticks) { this.starvationTimer = Math.max(0, ticks); }
    public void setPollenExposure(int val) { this.pollenExposure = Math.max(0, val); }

    public void setHeadHealth(float val) { this.headHealth = Mth.clamp(val, 0, getMaxHead()); }
    public void setTorsoHealth(float val) { this.torsoHealth = Mth.clamp(val, 0, getMaxTorso()); }
    public void setLeftArmHealth(float val) { this.leftArmHealth = Mth.clamp(val, 0, getMaxArm()); }
    public void setRightArmHealth(float val) { this.rightArmHealth = Mth.clamp(val, 0, getMaxArm()); }
    public void setLeftLegHealth(float val) { this.leftLegHealth = Mth.clamp(val, 0, getMaxLeg()); }
    public void setRightLegHealth(float val) { this.rightLegHealth = Mth.clamp(val, 0, getMaxLeg()); }
    public void setLeftFootHealth(float val) { this.leftFootHealth = Mth.clamp(val, 0, getMaxFoot()); }
    public void setRightFootHealth(float val) { this.rightFootHealth = Mth.clamp(val, 0, getMaxFoot()); }

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
        this.coreTemp = 0.7f;
        this.wetness = 0.0f;
        this.starvationTimer = 0;
        this.pollenExposure = 0;
        this.bpm = 70;
    }

    public void copyFrom(PlayerCapability source) {
        this.bpm = source.bpm;
        this.energy = source.energy;
        this.thirst = source.thirst;
        this.hunger = source.hunger;
        this.watchEquipped = source.watchEquipped;
        this.starvationTimer = source.starvationTimer;
        this.pollenExposure = source.pollenExposure;
        this.coreTemp = source.coreTemp;
        this.wetness = source.wetness;
        this.headHealth = source.headHealth;
        this.torsoHealth = source.torsoHealth;
        this.leftArmHealth = source.leftArmHealth;
        this.rightArmHealth = source.rightArmHealth;
        this.leftLegHealth = source.leftLegHealth;
        this.rightLegHealth = source.rightLegHealth;
        this.leftFootHealth = source.leftFootHealth;
        this.rightFootHealth = source.rightFootHealth;
    }
    // Helper Records for the Split (Add these to the bottom of the file or as inner classes)
    private record StatData(int bpm, float energy, float thirst, float hunger, boolean watch, int starve, int pollen, float temp, float wet) {}
    private record BodyData(float head, float torso, float lArm, float rArm, float lLeg, float rLeg, float lFoot, float rFoot) {}

    public float getHeadPct() { return headHealth / getMaxHead(); }
    public float getTorsoPct() { return torsoHealth / getMaxTorso(); }
    public float getLArmPct() { return leftArmHealth / getMaxArm(); }
    public float getRArmPct() { return rightArmHealth / getMaxArm(); }
    public float getLLegPct() { return leftLegHealth / getMaxLeg(); }
    public float getRLegPct() { return rightLegHealth / getMaxLeg(); }
    public float getLFootPct() { return leftFootHealth / getMaxFoot(); }
    public float getRFootPct() { return rightFootHealth / getMaxFoot(); }
}
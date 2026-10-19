package net.enderwish.Belliarium_Monstrarium_Subpack.core.survival;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public class SurvivalCapability {

    private float energy = 100.0f;
    private float stamina = 100.0f;
    private boolean collapsed = false;
    private float thirst = 100.0f;
    private float bodyTemp = 37.0f;

    public SurvivalCapability() {}

    /** Old 3-arg shape kept for compatibility; new stats default full/normal. */
    public SurvivalCapability(float energy, float stamina, boolean collapsed) {
        this(energy, stamina, collapsed, 100.0f, 37.0f);
    }

    public SurvivalCapability(float energy, float stamina, boolean collapsed,
                               float thirst, float bodyTemp) {
        this.energy = energy;
        this.stamina = stamina;
        this.collapsed = collapsed;
        this.thirst = thirst;
        this.bodyTemp = bodyTemp;
    }

    public static final Codec<SurvivalCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("energy").forGetter(SurvivalCapability::getEnergy),
            Codec.FLOAT.fieldOf("stamina").forGetter(SurvivalCapability::getStamina),
            Codec.BOOL.fieldOf("collapsed").forGetter(SurvivalCapability::isCollapsed),
            Codec.FLOAT.optionalFieldOf("thirst", 100.0f).forGetter(SurvivalCapability::getThirst),
            Codec.FLOAT.optionalFieldOf("bodyTemp", 37.0f).forGetter(SurvivalCapability::getBodyTemp)
    ).apply(inst, SurvivalCapability::new));

    public float getEnergy() { return energy; }
    public float getStamina() { return stamina; }
    public boolean isCollapsed() { return collapsed; }
    public float getThirst() { return thirst; }
    public float getBodyTemp() { return bodyTemp; }

    public void setEnergy(float v) { energy = Mth.clamp(v, 0, 100); }
    public void setStamina(float v) { stamina = Mth.clamp(v, 0, 100); }
    public void setCollapsed(boolean v) { collapsed = v; }
    public void setThirst(float v) { thirst = Mth.clamp(v, 0, 100); }
    public void setBodyTemp(float v) { bodyTemp = Mth.clamp(v, 30.0f, 44.0f); }

    public void healAll() { energy = 100f; stamina = 100f; collapsed = false; thirst = 100f; bodyTemp = 37.0f; }
}

package net.enderwish.Belliarium_Monstrarium_Subpack.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public class SurvivalCapability {

    private float energy = 100.0f;
    private float stamina = 100.0f;
    private boolean collapsed = false;

    public SurvivalCapability() {}

    public SurvivalCapability(float energy, float stamina, boolean collapsed) {
        this.energy = energy;
        this.stamina = stamina;
        this.collapsed = collapsed;
    }

    public static final Codec<SurvivalCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("energy").forGetter(SurvivalCapability::getEnergy),
            Codec.FLOAT.fieldOf("stamina").forGetter(SurvivalCapability::getStamina),
            Codec.BOOL.fieldOf("collapsed").forGetter(SurvivalCapability::isCollapsed)
    ).apply(inst, SurvivalCapability::new));

    public float getEnergy() { return energy; }
    public float getStamina() { return stamina; }
    public boolean isCollapsed() { return collapsed; }
    public void setEnergy(float v) { energy = Mth.clamp(v, 0, 100); }
    public void setStamina(float v) { stamina = Mth.clamp(v, 0, 100); }
    public void setCollapsed(boolean v) { collapsed = v; }

    public void healAll() { energy = 100f; stamina = 100f; collapsed = false; }
}

package net.enderwish.Belliarium_Monstrarium_Subpack.core.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Same shape/purpose as Farming's SpoilageComponent -- a persistent per-stack data component. */
public record RepairChargesComponent(int chargesRemaining) {
    public static final Codec<RepairChargesComponent> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("charges_remaining").forGetter(RepairChargesComponent::chargesRemaining)
    ).apply(inst, RepairChargesComponent::new));

    public RepairChargesComponent spend() {
        return new RepairChargesComponent(Math.max(0, chargesRemaining - 1));
    }

    public boolean hasChargesLeft() {
        return chargesRemaining > 0;
    }
}

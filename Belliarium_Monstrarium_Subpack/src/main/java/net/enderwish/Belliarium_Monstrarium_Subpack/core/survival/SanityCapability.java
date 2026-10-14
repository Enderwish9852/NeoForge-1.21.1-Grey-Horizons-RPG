package net.enderwish.Belliarium_Monstrarium_Subpack.core.survival;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

/**
 * SanityCapability
 *
 * NEW -- the sanity stat. Starts at 100 and NEVER changes on its own;
 * only sources (status effects like Hive Mind Influence, and later
 * scripted events) move it. Reaching 0 kills the player with a custom
 * death ("You went insane.") that BYPASSES the Survivor's Diary rewind.
 *
 * Milestones at 50/25/1 are wired as crossing-detection hooks in
 * SanityTickHandler; their effects are intentionally TODO for now.
 */
public class SanityCapability {

    public static final float MAX = 100.0f;

    private float sanity = MAX;

    public SanityCapability() {}

    public SanityCapability(float sanity) { this.sanity = sanity; }

    public static final Codec<SanityCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("sanity").forGetter(SanityCapability::get)
    ).apply(inst, SanityCapability::new));

    public float get() { return sanity; }

    public void set(float v) { sanity = Mth.clamp(v, 0, MAX); }

    public void add(float delta) { set(sanity + delta); }
}

package net.enderwish.Belliarium_Monstrarium_Subpack.core.body;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * BodyPart
 *
 * Unifies the 8 body parts behind one enum with method-reference-bound
 * getters/healers, so new systems (triage, targeting screens) can iterate
 * "all parts" or "all subsystems" generically instead of hand-writing an
 * 8-branch switch every time something new needs to touch all of them.
 * BodyHealthCapability itself is untouched -- this just wraps its
 * existing per-part methods.
 */
public enum BodyPart {

    HEAD("Head", BodyHealthCapability.HEAD_MAX, false,
            BodyHealthCapability::getHead, BodyHealthCapability::getHeadPct, BodyHealthCapability::healHead),

    TORSO("Torso", BodyHealthCapability.TORSO_MAX, false,
            BodyHealthCapability::getTorso, BodyHealthCapability::getTorsoPct, BodyHealthCapability::healTorso),

    LEFT_ARM("Left Arm", BodyHealthCapability.ARM_MAX, true,
            BodyHealthCapability::getLeftArm, BodyHealthCapability::getLeftArmPct, BodyHealthCapability::healLeftArm),

    RIGHT_ARM("Right Arm", BodyHealthCapability.ARM_MAX, true,
            BodyHealthCapability::getRightArm, BodyHealthCapability::getRightArmPct, BodyHealthCapability::healRightArm),

    LEFT_LEG("Left Leg", BodyHealthCapability.LEG_MAX, true,
            BodyHealthCapability::getLeftLeg, BodyHealthCapability::getLeftLegPct, BodyHealthCapability::healLeftLeg),

    RIGHT_LEG("Right Leg", BodyHealthCapability.LEG_MAX, true,
            BodyHealthCapability::getRightLeg, BodyHealthCapability::getRightLegPct, BodyHealthCapability::healRightLeg),

    LEFT_FOOT("Left Foot", BodyHealthCapability.FOOT_MAX, true,
            BodyHealthCapability::getLeftFoot, BodyHealthCapability::getLeftFootPct, BodyHealthCapability::healLeftFoot),

    RIGHT_FOOT("Right Foot", BodyHealthCapability.FOOT_MAX, true,
            BodyHealthCapability::getRightFoot, BodyHealthCapability::getRightFootPct, BodyHealthCapability::healRightFoot);

    private final String displayName;
    private final float max;
    private final boolean subsystem;
    private final Function<BodyHealthCapability, Float> currentGetter;
    private final Function<BodyHealthCapability, Float> pctGetter;
    private final BiConsumer<BodyHealthCapability, Float> healer;

    BodyPart(String displayName, float max, boolean subsystem,
             Function<BodyHealthCapability, Float> currentGetter,
             Function<BodyHealthCapability, Float> pctGetter,
             BiConsumer<BodyHealthCapability, Float> healer) {
        this.displayName = displayName;
        this.max = max;
        this.subsystem = subsystem;
        this.currentGetter = currentGetter;
        this.pctGetter = pctGetter;
        this.healer = healer;
    }

    public String getDisplayName() { return displayName; }
    public float getMax() { return max; }
    public boolean isSubsystem() { return subsystem; }
    public float getCurrent(BodyHealthCapability cap) { return currentGetter.apply(cap); }
    public float getPct(BodyHealthCapability cap) { return pctGetter.apply(cap); }

    /** Raises this part UP to the given fraction of max. Never lowers it -- treatments only heal. */
    public void healToFraction(BodyHealthCapability cap, float targetFraction) {
        float target = max * targetFraction;
        float current = getCurrent(cap);
        if (target > current) {
            healer.accept(cap, target - current);
        }
    }

    /** Bridges to the plain display-name strings used elsewhere (combat log, click targets). */
    public static BodyPart fromDisplayName(String name) {
        for (BodyPart part : values()) {
            if (part.displayName.equals(name)) return part;
        }
        return null;
    }
}

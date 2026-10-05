package net.enderwish.Belliarium_Monstrarium_Subpack.core.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

/**
 * BodyHealthCapability
 *
 * Head/torso are MAIN systems: reaching 0 kills the player outright.
 * Arms/legs/feet are SUB systems: reaching 0 doesn't kill, but disables
 * that limb (isDisabled() below) -- what "disabled" actually DOES
 * (movement/attack penalty) lives in event handlers, not here.
 *
 * Max values below are a first-pass proposal, not a cited medical
 * source -- relative sizing reasons: torso holds vital organs (biggest
 * pool, but ANY zero-out is fatal), head is small but equally fatal at
 * zero, legs are sized larger specifically because they need to absorb
 * real fall-impact energy before it reaches the torso (see BodyDamageHandler).
 */
public class BodyHealthCapability {

    public static final float HEAD_MAX  = 12.0f;
    public static final float TORSO_MAX = 30.0f;
    public static final float ARM_MAX   = 14.0f;
    public static final float LEG_MAX   = 20.0f;
    public static final float FOOT_MAX  = 10.0f;

    private float head, torso, leftArm, rightArm, leftLeg, rightLeg, leftFoot, rightFoot;

    public BodyHealthCapability() {
        healAll();
    }

    public BodyHealthCapability(float head, float torso, float leftArm, float rightArm,
                                float leftLeg, float rightLeg, float leftFoot, float rightFoot) {
        this.head = head; this.torso = torso;
        this.leftArm = leftArm; this.rightArm = rightArm;
        this.leftLeg = leftLeg; this.rightLeg = rightLeg;
        this.leftFoot = leftFoot; this.rightFoot = rightFoot;
    }

    public static final Codec<BodyHealthCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("head").forGetter(BodyHealthCapability::getHead),
            Codec.FLOAT.fieldOf("torso").forGetter(BodyHealthCapability::getTorso),
            Codec.FLOAT.fieldOf("left_arm").forGetter(BodyHealthCapability::getLeftArm),
            Codec.FLOAT.fieldOf("right_arm").forGetter(BodyHealthCapability::getRightArm),
            Codec.FLOAT.fieldOf("left_leg").forGetter(BodyHealthCapability::getLeftLeg),
            Codec.FLOAT.fieldOf("right_leg").forGetter(BodyHealthCapability::getRightLeg),
            Codec.FLOAT.fieldOf("left_foot").forGetter(BodyHealthCapability::getLeftFoot),
            Codec.FLOAT.fieldOf("right_foot").forGetter(BodyHealthCapability::getRightFoot)
    ).apply(inst, BodyHealthCapability::new));

    public void healAll() {
        head = HEAD_MAX; torso = TORSO_MAX;
        leftArm = rightArm = ARM_MAX;
        leftLeg = rightLeg = LEG_MAX;
        leftFoot = rightFoot = FOOT_MAX;
    }

    public void damageHead(float a)     { head = Mth.clamp(head - a, 0, HEAD_MAX); }
    public void damageTorso(float a)    { torso = Mth.clamp(torso - a, 0, TORSO_MAX); }
    public void damageLeftArm(float a)  { leftArm = Mth.clamp(leftArm - a, 0, ARM_MAX); }
    public void damageRightArm(float a) { rightArm = Mth.clamp(rightArm - a, 0, ARM_MAX); }
    public void damageLeftLeg(float a)  { leftLeg = Mth.clamp(leftLeg - a, 0, LEG_MAX); }
    public void damageRightLeg(float a) { rightLeg = Mth.clamp(rightLeg - a, 0, LEG_MAX); }
    public void damageLeftFoot(float a) { leftFoot = Mth.clamp(leftFoot - a, 0, FOOT_MAX); }
    public void damageRightFoot(float a){ rightFoot = Mth.clamp(rightFoot - a, 0, FOOT_MAX); }

    public boolean isHeadFatal()  { return head <= 0; }
    public boolean isTorsoFatal() { return torso <= 0; }
    /** "Fatal damage" to a sub-system per your definition -- reaches zero, doesn't kill, disables. */
    public boolean isLeftArmDisabled()  { return leftArm <= 0; }
    public boolean isRightArmDisabled() { return rightArm <= 0; }
    public boolean isLeftLegDisabled()  { return leftLeg <= 0; }
    public boolean isRightLegDisabled() { return rightLeg <= 0; }
    public boolean isLeftFootDisabled() { return leftFoot <= 0; }
    public boolean isRightFootDisabled(){ return rightFoot <= 0; }

    public float getHead() { return head; }
    public float getTorso() { return torso; }
    public float getLeftArm() { return leftArm; }
    public float getRightArm() { return rightArm; }
    public float getLeftLeg() { return leftLeg; }
    public float getRightLeg() { return rightLeg; }
    public float getLeftFoot() { return leftFoot; }
    public float getRightFoot() { return rightFoot; }

    public float getHeadPct()  { return head / HEAD_MAX; }
    public float getTorsoPct() { return torso / TORSO_MAX; }
    public float getLeftArmPct()  { return leftArm / ARM_MAX; }
    public float getRightArmPct() { return rightArm / ARM_MAX; }
    public float getLeftLegPct()  { return leftLeg / LEG_MAX; }
    public float getRightLegPct() { return rightLeg / LEG_MAX; }
    public float getLeftFootPct()  { return leftFoot / FOOT_MAX; }
    public float getRightFootPct() { return rightFoot / FOOT_MAX; }
}
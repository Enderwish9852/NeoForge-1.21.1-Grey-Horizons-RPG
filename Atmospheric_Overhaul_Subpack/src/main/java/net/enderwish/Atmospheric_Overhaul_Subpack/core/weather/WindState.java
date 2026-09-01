package net.enderwish.Atmospheric_Overhaul_Subpack.core.weather;

/**
 * WindState
 *
 * Holds the current wind conditions. Smoothly interpolates
 * speed toward a target and occasionally shifts direction
 * based on turbulence.
 *
 * Ticked server-side by WindManager. Synced to clients
 * every 20 ticks via WindSyncPacket.
 *
 * Speed scale:
 *   0.0 = dead calm
 *   0.25 = gentle breeze
 *   0.5  = moderate wind
 *   0.75 = strong wind / storm
 *   1.0  = full gale / blizzard
 */
public class WindState {

    // ── State ─────────────────────────────────────────────────────────────────

    private WindDirection direction;
    private float speed;           // current speed 0.0-1.0
    private float targetSpeed;     // speed interpolating toward
    private float gustFactor;      // 0.0-1.0 multiplier for gusts
    private float turbulence;      // 0.0-1.0 how erratic direction changes are

    // ── Interpolation config ──────────────────────────────────────────────────

    /** How fast speed interpolates toward target (per tick) */
    private static final float SPEED_LERP_RATE = 0.004f;

    /** How fast gusts change */
    private static final float GUST_LERP_RATE  = 0.01f;

    // ── Constructor ───────────────────────────────────────────────────────────

    public WindState() {
        this.direction   = WindDirection.SOUTHWEST;
        this.speed       = 0.1f;
        this.targetSpeed = 0.1f;
        this.gustFactor  = 1.0f;
        this.turbulence  = 0.1f;
    }

    public WindState(WindDirection direction, float speed,
                     float gustFactor, float turbulence) {
        this.direction   = direction;
        this.speed       = speed;
        this.targetSpeed = speed;
        this.gustFactor  = gustFactor;
        this.turbulence  = turbulence;
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    /**
     * Called every server tick by WindManager.
     * Smoothly interpolates speed toward target.
     * Occasionally shifts direction based on turbulence.
     */
    public void tick(net.minecraft.util.RandomSource random) {
        // Interpolate speed toward target
        float delta = targetSpeed - speed;
        speed += delta * SPEED_LERP_RATE;
        speed = net.minecraft.util.Mth.clamp(speed, 0f, 1f);

        // Interpolate gust factor — random variation around 1.0
        float gustTarget = 0.7f + random.nextFloat() * 0.6f;
        gustFactor += (gustTarget - gustFactor) * GUST_LERP_RATE;

        // Direction shift — probability based on turbulence
        if (random.nextFloat() < turbulence * 0.002f) {
            int shift = random.nextBoolean() ? 1 : -1;
            direction = direction.rotate(shift);
        }
    }

    // ── Setters (called by WindManager on weather change) ─────────────────────

    public void setTargetSpeed(float target) {
        this.targetSpeed = net.minecraft.util.Mth.clamp(target, 0f, 1f);
    }

    public void setDirection(WindDirection dir) {
        this.direction = dir;
    }

    public void setTurbulence(float t) {
        this.turbulence = net.minecraft.util.Mth.clamp(t, 0f, 1f);
    }

    public void snapToTarget() {
        this.speed = this.targetSpeed;
    }

    // ── Derived getters ───────────────────────────────────────────────────────

    /**
     * Effective X wind velocity — direction + speed + gust.
     * Multiply by a particle's wind influence factor to get
     * the velocity delta to apply.
     */
    public float getEffectiveDx() {
        return direction.dx * speed * gustFactor;
    }

    /**
     * Effective Z wind velocity.
     */
    public float getEffectiveDz() {
        return direction.dz * speed * gustFactor;
    }

    public boolean isCalm()       { return speed < 0.10f; }
    public boolean isBreeze()     { return speed >= 0.10f && speed < 0.35f; }
    public boolean isModerate()   { return speed >= 0.35f && speed < 0.55f; }
    public boolean isStrong()     { return speed >= 0.55f && speed < 0.75f; }
    public boolean isGale()       { return speed >= 0.75f; }

    // ── Plain getters ─────────────────────────────────────────────────────────

    public WindDirection getDirection()  { return direction; }
    public float getSpeed()              { return speed; }
    public float getTargetSpeed()        { return targetSpeed; }
    public float getGustFactor()         { return gustFactor; }
    public float getTurbulence()         { return turbulence; }

    // ── Serialisation helpers (used by packet) ────────────────────────────────

    public byte directionByte() {
        return (byte) direction.ordinal();
    }

    public static WindDirection directionFromByte(byte b) {
        WindDirection[] values = WindDirection.values();
        return values[b % values.length];
    }
}

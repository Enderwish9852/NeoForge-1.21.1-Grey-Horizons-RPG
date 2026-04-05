package net.enderwish.HUD_Visuals_Subpack.core.weather;

import net.enderwish.HUD_Visuals_Subpack.api.WeatherType;

public class WeatherState {
    private WeatherType currentType;
    private float intensity;
    private int ticksRemaining;
    private float windAngle;

    public WeatherState(WeatherType type, float intensity, int duration) {
        this.currentType = type;
        this.intensity = intensity;
        this.ticksRemaining = duration;
        this.windAngle = (float) (Math.random() * 360.0);
    }

    public WeatherType getType() { return currentType; }
    public float getIntensity() { return intensity; }
    public int getTicksRemaining() { return ticksRemaining; }
    public float getWindAngle() { return windAngle; }

    public void tick() {
        if (ticksRemaining > 0) ticksRemaining--;
    }

    public boolean isFinished() {
        return ticksRemaining <= 0;
    }
}
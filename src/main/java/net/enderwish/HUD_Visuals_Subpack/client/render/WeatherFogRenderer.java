package net.enderwish.HUD_Visuals_Subpack.client.render;

import net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Injects custom fog settings based on the current weather.
 * All imports updated to net.neoforged for 1.21.1 compatibility.
 */
@OnlyIn(Dist.CLIENT)
public class WeatherFogRenderer {

    /**
     * Adjusts the distance at which fog starts and ends.
     */
    @SubscribeEvent
    public void onFogRender(ViewportEvent.RenderFog event) {
        WeatherType type = ClientWeatherHandler.getCurrentType();
        float alpha = ClientWeatherHandler.getVisualAlpha();

        // Only modify if there's an active transition or weather
        if (alpha <= 0.0f) return;

        float farPlane = event.getFarPlaneDistance();
        float nearPlane = 0.05f * farPlane; // Vanilla default start

        float targetNear = nearPlane;
        float targetFar = farPlane;

        // Using your specific weather types from WeatherManager
        switch (type) {
            case BLIZZARD -> {
                targetNear = 2.0f;
                targetFar = 20.0f;
            }
            case THUNDER -> {
                targetNear = 10.0f;
                targetFar = 60.0f;
            }
            case HEATWAVE -> {
                // Heatwaves often have "hazy" distant fog
                targetNear = 30.0f;
                targetFar = 120.0f;
            }
            default -> { /* CLEAR - keep defaults */ }
        }

        // Interpolate between vanilla fog and weather fog based on alpha
        event.setNearPlaneDistance(lerp(nearPlane, targetNear, alpha));
        event.setFarPlaneDistance(lerp(farPlane, targetFar, alpha));

        // In NeoForge, setting the distance and returning is usually enough,
        // but setCanceled(true) tells the engine we have handled the calculation.
        event.setCanceled(true);
    }

    /**
     * Changes the color of the fog to match the atmosphere.
     */
    @SubscribeEvent
    public void onFogColor(ViewportEvent.ComputeFogColor event) {
        WeatherType type = ClientWeatherHandler.getCurrentType();
        float alpha = ClientWeatherHandler.getVisualAlpha();

        if (alpha <= 0.0f) return;

        float r = event.getRed();
        float g = event.getGreen();
        float b = event.getBlue();

        float tr = r, tg = g, tb = b;

        switch (type) {
            case BLIZZARD -> { tr = 0.9f; tg = 0.95f; tb = 1.0f; } // White/Blueish
            case THUNDER -> { tr = 0.1f; tg = 0.1f; tb = 0.15f; } // Very Dark
            case HEATWAVE -> { tr = 0.8f; tg = 0.7f; tb = 0.5f; } // Hazy Yellow/Orange
            default -> {}
        }

        event.setRed(lerp(r, tr, alpha));
        event.setGreen(lerp(g, tg, alpha));
        event.setBlue(lerp(b, tb, alpha));
    }

    private float lerp(float start, float end, float pct) {
        return start + (end - start) * pct;
    }
}
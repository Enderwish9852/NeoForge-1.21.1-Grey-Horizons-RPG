package net.enderwish.TerraForma_Subpack.core.worldgen.noise;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/**
 * NoiseLayer
 *
 * A single layer of Perlin noise with configurable
 * scale, amplitude, and octaves.
 *
 * Multiple layers are stacked in GHNoiseRouter to
 * produce realistic terrain.
 */
public class NoiseLayer {

    private final ImprovedNoise noise;
    private final double scale;
    private final double amplitude;
    private final int octaves;
    private final String name;

    public NoiseLayer(String name, RandomSource random,
                      double scale, double amplitude, int octaves) {
        this.name      = name;
        this.noise     = new ImprovedNoise(random);
        this.scale     = scale;
        this.amplitude = amplitude;
        this.octaves   = octaves;
    }

    /**
     * Samples the noise at (x, z) using fractal Brownian motion
     * (multiple octaves for more natural-looking detail).
     *
     * Returns value in range [-amplitude, +amplitude].
     */
    public double sample(double x, double z) {
        double value     = 0.0;
        double frequency = 1.0;
        double gain      = 1.0;
        double maxGain   = 0.0;

        for (int i = 0; i < octaves; i++) {
            value   += noise.noise(
                    x * frequency / scale,
                    0,
                    z * frequency / scale) * gain;
            maxGain   += gain;
            frequency *= 2.0; // each octave doubles frequency
            gain      *= 0.5; // each octave halves amplitude
        }

        // Normalize to [-1, 1] then apply amplitude
        return (value / maxGain) * amplitude;
    }

    /**
     * Samples at a specific Y — used for 3D cave noise.
     */
    public double sample3D(double x, double y, double z) {
        double value     = 0.0;
        double frequency = 1.0;
        double gain      = 1.0;
        double maxGain   = 0.0;

        for (int i = 0; i < octaves; i++) {
            value   += noise.noise(
                    x * frequency / scale,
                    y * frequency / scale,
                    z * frequency / scale) * gain;
            maxGain   += gain;
            frequency *= 2.0;
            gain      *= 0.5;
        }

        return (value / maxGain) * amplitude;
    }

    public String getName() { return name; }
}
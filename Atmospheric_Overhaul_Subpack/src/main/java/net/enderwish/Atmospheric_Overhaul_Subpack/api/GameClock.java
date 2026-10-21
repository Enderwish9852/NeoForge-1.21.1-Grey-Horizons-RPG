package net.enderwish.Atmospheric_Overhaul_Subpack.api;

import net.minecraft.client.Minecraft;

/**
 * GameClock
 *
 * A wall clock that STOPS when the game is paused.
 *
 * The system's own clock keeps running behind the pause screen, so
 * anything driven by System.currentTimeMillis() keeps going while the
 * world is frozen: a kill feed's rows expire, a HUD message fades out,
 * an item's in-hand animation keeps spinning. The user noticed the
 * clouds doing exactly this, and the fix belongs in one place rather
 * than in every call site.
 *
 * This is the same clock, but it only advances while the game is
 * actually running. Note that Minecraft.isPaused() is only true for a
 * singleplayer pause screen -- in multiplayer the world keeps running
 * and so does this clock, which is right.
 *
 * The absolute value is meaningless (it starts near zero); only
 * differences between two readings matter, which is all a fade or a
 * timer needs.
 */
public final class GameClock {

    private GameClock() {}

    private static long lastReal = System.currentTimeMillis();
    private static long accumulated = 0L;

    /** Milliseconds since this clock was first read, excluding paused time. */
    public static synchronized long millis() {
        long now = System.currentTimeMillis();
        if (!Minecraft.getInstance().isPaused()) {
            accumulated += now - lastReal;
        }
        lastReal = now;
        return accumulated;
    }
}

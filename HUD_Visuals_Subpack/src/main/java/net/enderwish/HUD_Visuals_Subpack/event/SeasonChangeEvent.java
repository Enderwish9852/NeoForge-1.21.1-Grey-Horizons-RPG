package net.enderwish.HUD_Visuals_Subpack.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * This event is fired whenever a new season begins (every 20 days).
 */
public class SeasonChangeEvent extends Event {
    private final ServerLevel level;

    public SeasonChangeEvent(ServerLevel level) {
        this.level = level;
    }

    public ServerLevel getLevel() {
        return level;
    }
}
package net.enderwish.Atmospheric_Overhaul_Subpack.client.sound;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, AtmosphericOverhaulSubpack.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> RAIN_AMBIENCE =
            SOUND_EVENTS.register("ambient.rain_loop", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "ambient.rain_loop")));

    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_HOWL =
            SOUND_EVENTS.register("ambient.wind_howl", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "ambient.wind_howl")));

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}

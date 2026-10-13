package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumConfig;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.SurvivorsDiaryItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.OpenDiaryPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DiaryBook
 *
 * Server-side helpers for the Survivor's Diary item: finding it, opening it
 * (which starts tracking), and writing the printed parts of an entry.
 * The diary counts as "carried" in the main inventory, hotbar or off-hand
 * (not armor slots).
 */
public final class DiaryBook {
    private DiaryBook() {}

    public static final int MAX_NOTES_CHARS = 4000;

    // ── Finding ───────────────────────────────────────────────────────────────

    public static int findSlot(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (i >= 36 && i <= 39) continue; // armor
            if (inv.getItem(i).getItem() instanceof SurvivorsDiaryItem) return i;
        }
        return -1;
    }

    /** The live stack (not a copy), or ItemStack.EMPTY. */
    public static ItemStack find(Player player) {
        int slot = findSlot(player);
        return slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
    }

    // ── Opening / activation ──────────────────────────────────────────────────

    public static void openDiary(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        ServerLevel overworld = player.getServer().overworld();
        DiaryContent content = stack.getOrDefault(ModDataComponents.DIARY_CONTENT.get(), DiaryContent.EMPTY);

        boolean canTrack = BelliariumConfig.trackingEnabled()
                && player.level().dimension() == Level.OVERWORLD
                && DayTracker.getActiveData() == null;

        if (canTrack && DayTracker.activate(overworld, player) == DayTracker.ActivationResult.STARTED) {
            content = content.withEntryAdded(
                    new DayEntry(title(overworld), serverLines(overworld, player), List.of(), ""));
            player.displayClientMessage(Component.literal("A new day begins. The diary remembers."), true);
        }
        if (content.isEmpty()) {
            content = content.withEntryAdded(new DayEntry("Notes", List.of(), List.of(), ""));
        }

        stack.set(ModDataComponents.DIARY_CONTENT.get(), content);
        ModMessages.sendToPlayer(new OpenDiaryPacket(hand.ordinal(), content), player);
    }

    // ── Printed text ──────────────────────────────────────────────────────────

    public static String title(ServerLevel level) {
        return SeasonsAPI.getPhase(level).displayName() + " " + SeasonsAPI.getSeason(level).displayName()
                + ", Year " + (SeasonsAPI.getYear(level) + 1)
                + " (day " + (SeasonsAPI.getYearDay(level) + 1) + ")";
    }

    /** The server's values at the instant the day begins. */
    public static List<String> serverLines(ServerLevel level, ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        long t = level.getDayTime() % 24000L;
        int hours = (int) ((t / 1000L + 6L) % 24L);
        int minutes = (int) ((t % 1000L) * 60L / 1000L);
        String weather = SeasonsAPI.getActiveWeatherId(level).replace('_', ' ');
        int intensity = Math.round(SeasonsAPI.getWeatherIntensity(level) * 100f);

        return List.of(
                String.format(Locale.ROOT, "Time: %02d:%02d", hours, minutes),
                "Weather: " + weather + (intensity > 0 ? " (" + intensity + "%)" : ""),
                "Temperature: " + SeasonsAPI.getTemperatureCelsius(level, pos) + " C ("
                        + SeasonsAPI.getTemperatureLabel(level, pos) + ")",
                "Feels like: " + SeasonsAPI.getLocalChunkTempCelsius(level, pos) + " C",
                String.format(Locale.ROOT, "Wind: %s, %.2f",
                        SeasonsAPI.getWindDirection(level).name().toLowerCase(Locale.ROOT),
                        SeasonsAPI.getWindSpeed(level)),
                "Position: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    // ── End-of-day summary (Part 4) ───────────────────────────────────────────

    public static List<String> buildSummary(DayStats stats, boolean woke) {
        List<String> lines = new ArrayList<>();
        lines.add("Traveled " + Math.round(stats.getBlocksTraveled()) + " blocks.");

        if (stats.getGathered().isEmpty()) {
            lines.add("Gathered nothing worth noting.");
        } else {
            lines.add("Gathered:");
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(stats.getGathered().entrySet());
            sorted.sort((a, b) -> b.getValue() - a.getValue());
            int shown = 0;
            for (Map.Entry<String, Integer> entry : sorted) {
                if (shown >= 10) break;
                lines.add("  " + entry.getValue() + "x " + displayName(entry.getKey()));
                shown++;
            }
            if (sorted.size() > shown) lines.add("  ...and " + (sorted.size() - shown) + " more kinds.");
        }
        if (woke) lines.add("Then I woke up.");
        return lines;
    }

    /** Writes a summary onto the LAST entry, if it doesn't have one yet. */
    public static DiaryContent withSummary(DiaryContent content, List<String> summary) {
        if (content.isEmpty()) return content;
        int last = content.entries().size() - 1;
        DayEntry entry = content.entries().get(last);
        if (!entry.summaryLines().isEmpty()) return content;
        return content.withEntryReplaced(last, entry.withSummary(summary));
    }

    /** Called when a window ends normally: prints the day's summary into the carried diary. */
    public static void finishDay(ServerPlayer owner, DayTrackerData data) {
        ItemStack diary = find(owner);
        if (diary.isEmpty()) return;
        DiaryContent content = diary.getOrDefault(ModDataComponents.DIARY_CONTENT.get(), DiaryContent.EMPTY);
        diary.set(ModDataComponents.DIARY_CONTENT.get(),
                withSummary(content, buildSummary(data.getStats(), false)));
    }

    /**
     * Called when a wake-up rewind finishes: prints the summary with "Then I woke up.", bumps the
     * wake-up count, then rolls the stacking chance for the diary itself to disappear (starts at
     * {@link BelliariumConfig#BOOK_LOSS_CHANCE_START}, +{@link BelliariumConfig#BOOK_LOSS_CHANCE_STEP}
     * per wake-up). The player is left with no diary on a hit -- lost in the recurring nightmare.
     */
    public static void finishDayAfterWake(ServerPlayer owner, DayTrackerData data) {
        int slot = findSlot(owner);
        if (slot < 0) return;
        ItemStack diary = owner.getInventory().getItem(slot);

        DiaryContent content = diary.getOrDefault(ModDataComponents.DIARY_CONTENT.get(), DiaryContent.EMPTY);
        content = withSummary(content, buildSummary(data.getStats(), true));
        int wakeUps = content.wakeUps() + 1;
        content = content.withWakeUps(wakeUps);

        if (owner.getRandom().nextDouble() < BelliariumConfig.bookLossChance(wakeUps)) {
            owner.getInventory().setItem(slot, ItemStack.EMPTY);
            owner.displayClientMessage(Component.literal("The diary is gone. You can't remember how."), true);
            return;
        }
        diary.set(ModDataComponents.DIARY_CONTENT.get(), content);
    }

    private static String displayName(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? itemId : item.getDescription().getString();
    }
}

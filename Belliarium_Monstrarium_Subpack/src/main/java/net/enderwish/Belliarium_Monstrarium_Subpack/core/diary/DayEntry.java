package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** One day in the diary: header lines and end-of-day summary are printed by the system, notes are the player's. */
public record DayEntry(String title, List<String> serverLines, List<String> summaryLines, String notes) {

    public static final Codec<DayEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("title", "").forGetter(DayEntry::title),
            Codec.STRING.listOf().optionalFieldOf("server_lines", List.of()).forGetter(DayEntry::serverLines),
            Codec.STRING.listOf().optionalFieldOf("summary_lines", List.of()).forGetter(DayEntry::summaryLines),
            Codec.STRING.optionalFieldOf("notes", "").forGetter(DayEntry::notes)
    ).apply(i, DayEntry::new));

    public DayEntry withNotes(String notes) {
        return new DayEntry(title, serverLines, summaryLines, notes);
    }

    public DayEntry withSummary(List<String> summary) {
        return new DayEntry(title, serverLines, List.copyOf(summary), notes);
    }
}

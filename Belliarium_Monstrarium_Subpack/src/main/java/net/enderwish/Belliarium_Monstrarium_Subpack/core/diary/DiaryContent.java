package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/** Everything stored on a Survivor's Diary item: its day entries and how many times it has rewound the player. */
public record DiaryContent(List<DayEntry> entries, int wakeUps) {

    public static final int MAX_ENTRIES = 400;
    public static final DiaryContent EMPTY = new DiaryContent(List.of(), 0);

    public static final Codec<DiaryContent> CODEC = RecordCodecBuilder.create(i -> i.group(
            DayEntry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(DiaryContent::entries),
            Codec.INT.optionalFieldOf("wake_ups", 0).forGetter(DiaryContent::wakeUps)
    ).apply(i, DiaryContent::new));

    public boolean isEmpty() { return entries.isEmpty(); }

    public DiaryContent withEntryAdded(DayEntry entry) {
        List<DayEntry> list = new ArrayList<>(entries);
        list.add(entry);
        while (list.size() > MAX_ENTRIES) list.remove(0);
        return new DiaryContent(list, wakeUps);
    }

    public DiaryContent withEntryReplaced(int index, DayEntry entry) {
        List<DayEntry> list = new ArrayList<>(entries);
        list.set(index, entry);
        return new DiaryContent(list, wakeUps);
    }

    public DiaryContent withWakeUps(int count) {
        return new DiaryContent(entries, count);
    }
}

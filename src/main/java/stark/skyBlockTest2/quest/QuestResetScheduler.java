package stark.skyBlockTest2.quest;

import org.bukkit.Bukkit;
import stark.skyBlockTest2.SkyBlockTest2;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

/**
 * Planuje automatyczne resety questów na godziny/dni z config.yml.
 * Konfiguracja (config.yml):
 *   quests:
 *     daily-reset-time: "06:00"        # Godzina resetu dziennego (HH:mm)
 *     weekly-reset-day: "MONDAY"       # Dzień tygodnia resetu (np. MONDAY, SUNDAY)
 *     weekly-reset-time: "06:00"       # Godzina resetu tygodniowego
 */
public class QuestResetScheduler {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final SkyBlockTest2 plugin;
    private final QuestManager questManager;

    public QuestResetScheduler(SkyBlockTest2 plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
    }

    public void start() {
        scheduleDailyReset();
        scheduleWeeklyReset();
    }

    // -------------------------------------------------------------------------
    // Reset dzienny
    // -------------------------------------------------------------------------

    private void scheduleDailyReset() {
        String timeStr = plugin.getConfig().getString("quests.daily-reset-time", "06:00");

        LocalTime resetTime;
        try {
            resetTime = LocalTime.parse(timeStr, TIME_FORMAT);
        } catch (Exception e) {
            plugin.getLogger().warning("[QuestManager] Nieprawidłowy format daily-reset-time: "
                    + timeStr + " — używam 06:00.");
            resetTime = LocalTime.of(6, 0);
        }

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now  = ZonedDateTime.now(zone);
        ZonedDateTime next = now.toLocalDate().atTime(resetTime).atZone(zone);
        if (!next.isAfter(now)) next = next.plusDays(1);

        long delayTicks = toTicks(Duration.between(now, next).getSeconds());
        long periodTicks = toTicks(24L * 60 * 60);

        final ZonedDateTime logTime = next;
        Bukkit.getScheduler().runTaskTimer(plugin, questManager::resetDaily, delayTicks, periodTicks);

        plugin.getLogger().info("[QuestManager] Reset dzienny zaplanowany na " + logTime.toLocalDateTime());
    }

    // -------------------------------------------------------------------------
    // Reset tygodniowy
    // -------------------------------------------------------------------------

    private void scheduleWeeklyReset() {
        String dayStr  = plugin.getConfig().getString("quests.weekly-reset-day", "MONDAY");
        String timeStr = plugin.getConfig().getString("quests.weekly-reset-time", "06:00");

        DayOfWeek resetDay;
        LocalTime resetTime;
        try {
            resetDay  = DayOfWeek.valueOf(dayStr.toUpperCase());
            resetTime = LocalTime.parse(timeStr, TIME_FORMAT);
        } catch (Exception e) {
            plugin.getLogger().warning("[QuestManager] Nieprawidłowa konfiguracja resetu tygodniowego — używam MONDAY 06:00.");
            resetDay  = DayOfWeek.MONDAY;
            resetTime = LocalTime.of(6, 0);
        }

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now  = ZonedDateTime.now(zone);
        ZonedDateTime next = now.with(TemporalAdjusters.nextOrSame(resetDay))
                .toLocalDate().atTime(resetTime).atZone(zone);
        if (!next.isAfter(now)) next = next.plusWeeks(1);

        long delayTicks = toTicks(Duration.between(now, next).getSeconds());
        long periodTicks = toTicks(7L * 24 * 60 * 60);

        final ZonedDateTime logTime = next;
        Bukkit.getScheduler().runTaskTimer(plugin, questManager::resetWeekly, delayTicks, periodTicks);

        plugin.getLogger().info("[QuestManager] Reset tygodniowy zaplanowany na " + logTime.toLocalDateTime());
    }

    private long toTicks(long seconds) {
        return Math.max(1L, seconds * 20L);
    }
}
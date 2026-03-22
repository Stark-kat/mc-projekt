package stark.skyBlockTest2.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.quest.QuestManager;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Pośrednie GUI po kliknięciu "Questy i Osiągnięcia" w MenuGui.
 * Zawiera 3 kafelki: Dzienne | Tygodniowe | Osiągnięcia.
 * Questy dzienne i tygodniowe są zablokowane do poziomu 10 wyspy.
 */
public class QuestsMenuGui {

    private static final int REQUIRED_LEVEL = 10;

    private final IslandManager islandManager;
    private final QuestManager questManager;

    public QuestsMenuGui(IslandManager islandManager, QuestManager questManager) {
        this.islandManager = islandManager;
        this.questManager  = questManager;
    }

    public void open(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());
        int level = island != null ? questManager.getIslandXpLevel(island) : 1;
        boolean questsUnlocked = level >= REQUIRED_LEVEL;

        String dailyTimer  = formatReset(questManager.getNextDailyReset(), false);
        String weeklyTimer = formatReset(questManager.getNextWeeklyReset(), true);

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 27, "§8Questy i Osiagniecia");
        GuiBuilder builder = new GuiBuilder(gui);

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        // Questy Dzienne — slot 11
        if (questsUnlocked) {
            builder.set(11, new ItemBuilder(Material.SUNFLOWER)
                    .name("§e§lQuesty Dzienne")
                    .lore(
                            "§7Zestaw zadan resetowany",
                            "§7kazdego dnia.",
                            " ",
                            "§aKliknij, aby otworzyc!",
                            " ",
                            "§7Reset za: §f" + dailyTimer
                    )
                    .setString("action", "QuestsOpen")
                    .setString("view", "DAILY")
                    .build());
        } else {
            builder.set(11, new ItemBuilder(Material.GRAY_DYE)
                    .name("§7§lQuesty Dzienne §8(Zablokowane)")
                    .lore(
                            "§7Zestaw zadan resetowany",
                            "§7kazdego dnia.",
                            " ",
                            "§cOdblokuj na poziomie §e" + REQUIRED_LEVEL + " §cwyspy.",
                            "§7Aktualny poziom: §e" + level,
                            " ",
                            "§7Reset za: §f" + dailyTimer
                    )
                    .build());
        }

        // Questy Tygodniowe — slot 13
        if (questsUnlocked) {
            builder.set(13, new ItemBuilder(Material.CLOCK)
                    .name("§b§lQuesty Tygodniowe")
                    .lore(
                            "§7Trudniejsze zadania resetowane",
                            "§7raz w tygodniu.",
                            " ",
                            "§aKliknij, aby otworzyc!",
                            " ",
                            "§7Reset za: §f" + weeklyTimer
                    )
                    .setString("action", "QuestsOpen")
                    .setString("view", "WEEKLY")
                    .build());
        } else {
            builder.set(13, new ItemBuilder(Material.GRAY_DYE)
                    .name("§7§lQuesty Tygodniowe §8(Zablokowane)")
                    .lore(
                            "§7Trudniejsze zadania resetowane",
                            "§7raz w tygodniu.",
                            " ",
                            "§cOdblokuj na poziomie §e" + REQUIRED_LEVEL + " §cwyspy.",
                            "§7Aktualny poziom: §e" + level,
                            " ",
                            "§7Reset za: §f" + weeklyTimer
                    )
                    .build());
        }

        // Osiągnięcia — slot 15 (zawsze dostępne — Tutorial nie jest blokowany)
        builder.set(15, new ItemBuilder(Material.NETHER_STAR)
                .name("§d§lOsiagniecia")
                .lore(
                        "§7Trwale osiagniecia wyspy.",
                        "§7Nigdy sie nie resetuja.",
                        " ",
                        questsUnlocked
                                ? "§aKliknij, aby otworzyc!"
                                : "§7Tutorial dostepny od razu.",
                        questsUnlocked
                                ? ""
                                : "§7Pozostale kategorie odblokuja sie na lvl §e" + REQUIRED_LEVEL + "§7."
                )
                .setString("action", "QuestsOpen")
                .setString("view", "ACHIEVEMENTS")
                .build());

        // Powrót
        builder.set(22, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "MenuGui")
                .build());

        player.openInventory(gui);
    }

    private String formatReset(ZonedDateTime next, boolean weekly) {
        long totalSeconds = Math.max(0, Duration.between(ZonedDateTime.now(), next).getSeconds());
        if (weekly) {
            long days    = totalSeconds / 86400;
            long hours   = (totalSeconds % 86400) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            return days + "d " + hours + "h " + String.format("%02dm", minutes);
        } else {
            long hours   = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        }
    }
}
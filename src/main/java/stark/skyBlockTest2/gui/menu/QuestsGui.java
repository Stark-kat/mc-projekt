package stark.skyBlockTest2.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.quest.*;

import java.util.*;

/**
 * Trzy osobne GUI: Questy Dzienne | Questy Tygodniowe | Osiągnięcia.
 * Osiągnięcia mają dodatkowy ekran wyboru kategorii przed listą.
 */
public class QuestsGui {

    public enum View { DAILY, WEEKLY, ACHIEVEMENTS }

    private final IslandManager islandManager;
    private final QuestManager questManager;

    // Sloty na zawartość questów/osiągnięć — pasują do 54-slotowego inventory
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    // Sloty na kafelki kategorii — rząd środkowy 27-slotowego inventory
    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    public static final String TITLE_DAILY               = "§8Questy Dzienne";
    public static final String TITLE_WEEKLY              = "§8Questy Tygodniowe";
    public static final String TITLE_ACHIEVEMENT_CATS    = "§8Osiagniecia — Kategorie";

    private static final int REQUIRED_LEVEL = 10;

    public QuestsGui(IslandManager islandManager, QuestManager questManager) {
        this.islandManager = islandManager;
        this.questManager  = questManager;
    }

    // =========================================================================
    // Publiczne metody otwierania
    // =========================================================================

    public void open(Player player, View view) {
        switch (view) {
            case DAILY        -> openDaily(player);
            case WEEKLY       -> openWeekly(player);
            case ACHIEVEMENTS -> openAchievements(player);
        }
    }

    public void openDaily(Player player) {
        Island island = getIsland(player);
        IslandQuestData data = getData(island);
        int level = island != null ? questManager.getIslandXpLevel(island) : 1;

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 27, TITLE_DAILY);
        GuiBuilder builder = new GuiBuilder(gui);
        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        if (level < REQUIRED_LEVEL) {
            builder.set(13, buildLockedItem(level));
        } else {
            fillQuests(builder, questManager.getDailyQuests(), data, false);
            builder.set(4, buildBonusTile(data, false));
        }
        builder.set(22, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "OpenQuestsMenu")
                .build());

        if (island != null) builder.set(26, buildXpItem(island));

        player.openInventory(gui);
    }

    public void openWeekly(Player player) {
        Island island = getIsland(player);
        IslandQuestData data = getData(island);
        int level = island != null ? questManager.getIslandXpLevel(island) : 1;

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 27, TITLE_WEEKLY);
        GuiBuilder builder = new GuiBuilder(gui);
        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        if (level < REQUIRED_LEVEL) {
            builder.set(13, buildLockedItem(level));
        } else {
            fillQuests(builder, questManager.getWeeklyQuests(), data, true);
            builder.set(4, buildBonusTile(data, true));
        }
        builder.set(22, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "OpenQuestsMenu")
                .build());

        if (island != null) builder.set(26, buildXpItem(island));

        player.openInventory(gui);
    }

    /**
     * Otwiera ekran wyboru kategorii osiągnięć.
     * Nazwa zachowana ze względu na kompatybilność z istniejącym GuiListenerem
     * (action=QuestsOpen, view=ACHIEVEMENTS prowadzi tutaj).
     */
    public void openAchievements(Player player) {
        Island island = getIsland(player);
        IslandQuestData data = getData(island);
        Map<String, List<AchievementDefinition>> byCategory = questManager.getAchievementsByCategory();
        int level = island != null ? questManager.getIslandXpLevel(island) : 1;
        boolean questsUnlocked = level >= REQUIRED_LEVEL;

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 54, TITLE_ACHIEVEMENT_CATS);
        GuiBuilder builder = new GuiBuilder(gui);
        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        int slotIdx = 0;
        for (Map.Entry<String, List<AchievementDefinition>> entry : byCategory.entrySet()) {
            if (slotIdx >= CONTENT_SLOTS.length) break;

            String categoryId                       = entry.getKey();
            List<AchievementDefinition> catList     = entry.getValue();
            boolean isTutorial                      = "Tutorial".equals(categoryId);
            boolean locked                          = !isTutorial && !questsUnlocked;

            if (locked) {
                List<String> lore = new ArrayList<>();
                lore.add("§7Osiagniecia: §8???");
                lore.add(" ");
                lore.add("§cOdblokuj na poziomie §e" + REQUIRED_LEVEL + " §cwyspy.");
                lore.add("§7Aktualny poziom: §e" + level);
                builder.set(CONTENT_SLOTS[slotIdx++],
                        new ItemBuilder(Material.GRAY_DYE)
                                .name("§7§l" + categoryId + " §8(Zablokowane)")
                                .lore(lore.toArray(new String[0]))
                                .build());
                continue;
            }

            boolean allDone    = isCategoryComplete(catList, data);
            int completedCount = countCompletedAchievements(catList, data);
            int totalCount     = catList.size();
            Material icon      = allDone
                    ? Material.NETHER_STAR
                    : (catList.isEmpty() ? Material.PAPER : catList.get(0).getIcon());

            List<String> lore = new ArrayList<>();
            lore.add("§7Osiagniecia: §f" + completedCount + " §7/ §f" + totalCount);
            lore.add(" ");
            lore.add(allDone ? "§a§lW PELNI UKONCZONE ✓" : "§aKliknij, aby otworzyc!");

            builder.set(CONTENT_SLOTS[slotIdx++],
                    new ItemBuilder(icon)
                            .name((allDone ? "§a" : "§e§l") + categoryId)
                            .lore(lore.toArray(new String[0]))
                            .glow(allDone)
                            .setString("action", "QuestsCategoryOpen")
                            .setString("category", categoryId)
                            .build());
        }

        builder.set(49, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "OpenQuestsMenu")
                .build());

        if (island != null) builder.set(53, buildXpItem(island));

        player.openInventory(gui);
    }

    /**
     * Otwiera osiągnięcia filtrowane po kategorii.
     * Wywoływane z GuiListenera przy action=QuestsCategoryOpen.
     */
    public void openAchievementsByCategory(Player player, String category) {
        Island island = getIsland(player);
        IslandQuestData data = getData(island);
        List<AchievementDefinition> catList = questManager.getAchievementsByCategory()
                .getOrDefault(category, Collections.emptyList());

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 54,
                "§8Osiagniecia: §e" + category);
        GuiBuilder builder = new GuiBuilder(gui);
        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        if (catList.isEmpty()) {
            builder.set(CONTENT_SLOTS[0], new ItemBuilder(Material.BARRIER)
                    .name("§cBrak osiagniec w tej kategorii")
                    .build());
        } else {
            for (int i = 0; i < catList.size() && i < CONTENT_SLOTS.length; i++) {
                builder.set(CONTENT_SLOTS[i], buildAchievementItem(catList.get(i), data));
            }
        }

        // Powrót do ekranu kategorii
        builder.set(49, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "OpenAchievementCategories")
                .build());

        if (island != null) builder.set(53, buildXpItem(island));

        player.openInventory(gui);
    }

    // =========================================================================
    // Wypełnianie treścią — questy
    // =========================================================================

    private void fillQuests(GuiBuilder builder, List<QuestDefinition> quests,
                            IslandQuestData data, boolean weekly) {
        if (quests.isEmpty()) {
            builder.set(CONTENT_SLOTS[0], new ItemBuilder(Material.BARRIER)
                    .name("§cBrak questów")
                    .build());
            return;
        }
        for (int i = 0; i < quests.size() && i < CONTENT_SLOTS.length; i++) {
            QuestDefinition q    = quests.get(i);
            int progress         = weekly ? data.getWeeklyProgress(q.getId()) : data.getDailyProgress(q.getId());
            boolean done         = weekly ? data.isWeeklyCompleted(q.getId()) : data.isDailyCompleted(q.getId());
            builder.set(CONTENT_SLOTS[i], buildQuestItem(q, progress, done));
        }
    }

    private ItemStack buildQuestItem(QuestDefinition q, int progress, boolean done) {
        List<String> lore = new ArrayList<>();
        lore.add("§8" + q.getDescription());
        lore.add(" ");

        if (done) {
            lore.add("§a§lUKONCZONE ✓");
        } else {
            int clamped = Math.min(progress, q.getAmount());
            lore.add("§7Postep: §f" + formatNum(clamped) + " §7/ §f" + formatNum(q.getAmount()));
            lore.add(progressBar(clamped, q.getAmount()));
        }

        lore.add(" ");
        lore.add("§6Nagrody:");
        if (q.getXpReward() > 0) lore.add("  §e+" + formatNum(q.getXpReward()) + " XP wyspy");
        for (ItemStack item : q.getItemRewards()) lore.add("  §7" + itemLabel(item));

        return new ItemBuilder(done ? Material.LIME_DYE : q.getIcon())
                .name((done ? "§a" : "§e") + q.getDisplayName())
                .lore(lore.toArray(new String[0]))
                .glow(done)
                .build();
    }

    private ItemStack buildBonusTile(IslandQuestData data, boolean weekly) {
        List<QuestDefinition> quests = weekly ? questManager.getWeeklyQuests() : questManager.getDailyQuests();
        long bonusXp                 = weekly ? questManager.getWeeklyBonusXp() : questManager.getDailyBonusXp();
        List<ItemStack> bonusItems   = weekly ? questManager.getWeeklyBonusItems() : questManager.getDailyBonusItems();
        boolean claimed              = weekly ? questManager.isWeeklyBonusClaimed(data) : questManager.isDailyBonusClaimed(data);
        int completed = 0;
        for (QuestDefinition q : quests) {
            boolean done = weekly ? data.isWeeklyCompleted(q.getId()) : data.isDailyCompleted(q.getId());
            if (done) completed++;
        }
        int total = quests.size();
        List<String> lore = new ArrayList<>();
        lore.add("§7Ukonczenie wszystkich questow");
        lore.add("§7z tej kategorii daje bonus!");
        lore.add(" ");
        lore.add("§7Postep: " + (claimed ? "§a" : "§f") + completed + " §7/ §f" + total);
        lore.add(progressBar(completed, total));
        lore.add(" ");
        lore.add("§6Nagroda bonusowa:");
        if (bonusXp > 0) lore.add("  §e+" + formatNum(bonusXp) + " XP wyspy");
        if (bonusItems != null) {
            for (ItemStack item : bonusItems) lore.add("  §7" + itemLabel(item));
        }
        if (bonusXp == 0 && (bonusItems == null || bonusItems.isEmpty())) {
            lore.add("  §8Brak skonfigurowanej nagrody");
        }
        lore.add(" ");
        if (claimed) lore.add("§a§lODEBRANA ✓");
        else         lore.add("§7Ukonczono: §f" + completed + " §7/ §f" + total);
        Material mat      = claimed ? Material.LIME_STAINED_GLASS_PANE : Material.GOLD_INGOT;
        String namePrefix = claimed ? "§a" : "§6";
        return new ItemBuilder(mat)
                .name(namePrefix + "Nagroda za wszystkie questy")
                .lore(lore.toArray(new String[0]))
                .glow(claimed)
                .build();
    }

    // =========================================================================
    // Wypełnianie treścią — osiągnięcia
    // =========================================================================

    private ItemStack buildAchievementItem(AchievementDefinition achievement, IslandQuestData data) {
        int completedTiers = data.getCompletedTiers(achievement.getId());
        int totalTiers     = achievement.getTierCount();
        boolean fullyDone  = achievement.isFullyCompleted(completedTiers);
        int progress       = data.getAchievementProgress(achievement.getId());
        AchievementDefinition.Tier nextTier = achievement.getNextTier(completedTiers);

        List<String> lore = new ArrayList<>();
        lore.add("§8" + achievement.getDescription());
        lore.add(" ");

        if (fullyDone) {
            lore.add("§a§lW PELNI UKONCZONE ✓");
        } else {
            if (nextTier.isMultiTarget()) {
                // Pasek ogólny: ile wymagań ukończono
                long doneReqs = nextTier.requirements().stream()
                        .filter(r -> data.getAchievementTargetProgress(achievement.getId(), r.target()) >= r.amount())
                        .count();
                long totalReqs = nextTier.requirements().size();
                lore.add(progressBar(doneReqs, totalReqs));
                lore.add(" ");
                // Checklist per-target
                for (AchievementDefinition.TargetRequirement req : nextTier.requirements()) {
                    int got  = data.getAchievementTargetProgress(achievement.getId(), req.target());
                    int need = req.amount();
                    boolean done = got >= need;
                    String label = req.target().replace("_", " ").toLowerCase();
                    label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
                    lore.add((done ? "§a✓ " : "§7◻ ") + label
                            + " §8(" + formatNum(Math.min(got, need)) + "/" + formatNum(need) + ")"
                            + (done ? " §a✓" : ""));
                }
            } else {
                lore.add("§f" + formatNum(progress) + " §8/ §f" + formatNum(nextTier.getSingleAmount()));
                lore.add(progressBar(progress, nextTier.getSingleAmount()));
            }
            lore.add(" ");
            lore.add("§7Poziomy: §f" + completedTiers + " §7/ §f" + totalTiers);
            lore.add(" ");
            lore.add("§6Nagroda (Poziom " + (completedTiers + 1) + "):");
            if (nextTier.xpReward() > 0)    lore.add("  §e+" + formatNum(nextTier.xpReward()) + " XP wyspy");
            if (nextTier.moneyReward() > 0)  lore.add("  §6+" + formatNum((long) nextTier.moneyReward()) + " monet");
            for (ItemStack item : nextTier.itemRewards()) lore.add("  §7" + itemLabel(item));
        }

        if (completedTiers > 0) {
            lore.add(" ");
            lore.add("§7Ukonczone progi:");
            for (int i = 0; i < completedTiers; i++) {
                AchievementDefinition.Tier t = achievement.getTiers().get(i);
                if (t.isMultiTarget()) {
                    lore.add("  §a✓ §7Poziom " + (i + 1) + " (" + t.requirements().size() + " zadania)");
                } else {
                    lore.add("  §a✓ §7Poziom " + (i + 1) + " (" + formatNum(t.getSingleAmount()) + ")");
                }
            }
        }

        String nameColor = fullyDone ? "§a" : completedTiers > 0 ? "§e" : "§7";
        return new ItemBuilder(fullyDone ? Material.NETHER_STAR : achievement.getIcon())
                .name(nameColor + achievement.getDisplayName())
                .lore(lore.toArray(new String[0]))
                .glow(fullyDone)
                .build();
    }

    private ItemStack buildLockedItem(int currentLevel) {
        return new ItemBuilder(Material.BARRIER)
                .name("§c§lZablokowane")
                .lore(
                        "§7Ta sekcja jest niedostepna.",
                        " ",
                        "§cOdblokuj na poziomie §e" + REQUIRED_LEVEL + " §cwyspy.",
                        "§7Aktualny poziom wyspy: §e" + currentLevel
                )
                .build();
    }

    // =========================================================================
    // XP wyspy
    // =========================================================================

    private ItemStack buildXpItem(Island island) {
        int level         = questManager.getIslandXpLevel(island);
        long xp           = island.getXp();
        long levelStart   = questManager.xpThresholdForLevel(level);
        long levelEnd     = questManager.xpThresholdForLevel(level + 1);
        long progressInLevel = xp - levelStart;
        long neededInLevel   = levelEnd - levelStart;

        List<String> lore = new ArrayList<>();
        lore.add("§7Poziom: §e" + level);
        lore.add("§7XP:     §f" + progressInLevel + " §8/ " + neededInLevel);
        lore.add(progressBar(progressInLevel, neededInLevel));
        lore.add("§7Do nastepnego: §f" + (neededInLevel - progressInLevel));
        lore.add(" ");
        lore.add("§8XP zdobywasz ukonczone questy");
        lore.add("§8i osiagniecia wyspy.");

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("§aXP Wyspy")
                .lore(lore.toArray(new String[0]))
                .build();
    }

    // =========================================================================
    // Pomocnicze — kategorie
    // =========================================================================

    private boolean isCategoryComplete(List<AchievementDefinition> list, IslandQuestData data) {
        if (list.isEmpty()) return false;
        for (AchievementDefinition a : list) {
            if (!a.isFullyCompleted(data.getCompletedTiers(a.getId()))) return false;
        }
        return true;
    }

    private int countCompletedAchievements(List<AchievementDefinition> list, IslandQuestData data) {
        int count = 0;
        for (AchievementDefinition a : list) {
            if (a.isFullyCompleted(data.getCompletedTiers(a.getId()))) count++;
        }
        return count;
    }

    // =========================================================================
    // Pomocnicze — ogólne
    // =========================================================================

    private Island getIsland(Player player) {
        return islandManager.getIsland(player.getUniqueId());
    }

    private IslandQuestData getData(Island island) {
        return island != null
                ? questManager.getQuestData(island.getOwner())
                : new IslandQuestData();
    }

    private String progressBar(long current, long max) {
        if (max <= 0) return "";
        int bars    = 18;
        int filled  = (int) Math.min(bars, Math.round(((double) current / max) * bars));
        int percent = (int) Math.min(100, Math.round(((double) current / max) * 100));
        return "§a" + "█".repeat(filled) + "§7" + "█".repeat(bars - filled) + " §f" + percent + "%";
    }

    private String formatNum(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private String itemLabel(ItemStack item) {
        if (item == null) return "?";
        String name = item.getType().name().replace("_", " ").toLowerCase();
        return item.getAmount() + "x " + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
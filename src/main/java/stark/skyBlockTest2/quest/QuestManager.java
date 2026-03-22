package stark.skyBlockTest2.quest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.database.DatabaseManager;
import stark.skyBlockTest2.economy.EconomyManager;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandBossBarManager;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.IslandType;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.logging.Level;

public class QuestManager {

    private final SkyBlockTest2 plugin;
    private final IslandManager islandManager;
    private final DatabaseManager db;

    // Pełna pula wszystkich zdefiniowanych questów (z sekcji pool:)
    private final Map<String, QuestDefinition> dailyPool  = new LinkedHashMap<>();
    private final Map<String, QuestDefinition> weeklyPool = new LinkedHashMap<>();

    // Aktualnie aktywne questy (podzbiór puli)
    private final List<QuestDefinition>       dailyQuests  = new ArrayList<>();
    private final List<QuestDefinition>       weeklyQuests = new ArrayList<>();
    private final List<AchievementDefinition> achievements = new ArrayList<>();

    // Osiągnięcia pogrupowane po kategorii (zachowuje kolejność z YAML)
    private final Map<String, List<AchievementDefinition>> achievementsByCategory = new LinkedHashMap<>();

    // Dane runtime
    private final Map<UUID, IslandQuestData> questDataMap = new HashMap<>();

    private IslandBossBarManager bossBarManager;
    private EconomyManager economyManager;

    // Bonusy za ukończenie wszystkich questów danego rodzaju
    private long             dailyBonusXp;
    private List<ItemStack>  dailyBonusItems;
    private long             weeklyBonusXp;
    private List<ItemStack>  weeklyBonusItems;

    /**
     * Klucze specjalne wstawiane do completedDaily/completedWeekly gdy bonus już oddany.
     * Automatycznie usuwane przez resetDaily()/resetWeekly() razem z resztą setu.
     */
    private static final String DAILY_BONUS_KEY  = "__daily_bonus__";
    private static final String WEEKLY_BONUS_KEY = "__weekly_bonus__";

    public QuestManager(SkyBlockTest2 plugin, IslandManager islandManager) {
        this.plugin         = plugin;
        this.islandManager  = islandManager;
        this.db             = plugin.getDatabaseManager();

        loadDefinitions();
        db.loadAllQuestData(questDataMap);
        new QuestResetScheduler(plugin, this).start();
    }

    // =========================================================================
    // Ładowanie definicji z plików YML
    // =========================================================================

    private void loadDefinitions() {
        loadBonusRewards();
        loadDailyQuests();
        loadWeeklyQuests();
        loadAchievements();
    }

    private void loadBonusRewards() {
        FileConfiguration cfg = plugin.getConfig();
        dailyBonusXp    = cfg.getLong("quests.daily-bonus.xp", 0L);
        dailyBonusItems = parseItemRewards(cfg, "quests.daily-bonus.items");
        weeklyBonusXp   = cfg.getLong("quests.weekly-bonus.xp", 0L);
        weeklyBonusItems = parseItemRewards(cfg, "quests.weekly-bonus.items");
    }

    private void loadDailyQuests() {
        FileConfiguration cfg = loadOrCreate("daily_quests.yml");

        loadPoolSection(cfg, "pool", QuestType.DAILY, dailyPool);

        List<String> activeIds = cfg.getStringList("active");
        if (!activeIds.isEmpty()) {
            activateQuests(activeIds, dailyPool, dailyQuests, "dziennych");
        } else {
            int count = plugin.getConfig().getInt("quests.daily-count", 3);
            rotateQuests(dailyPool, dailyQuests, count);
            saveActiveQuests(cfg, "daily_quests.yml", dailyQuests);
        }

        plugin.getLogger().info("[QuestManager] Pula dziennych: " + dailyPool.size()
                + ", aktywnych: " + dailyQuests.size());
    }

    private void loadWeeklyQuests() {
        FileConfiguration cfg = loadOrCreate("weekly_quests.yml");

        loadPoolSection(cfg, "pool", QuestType.WEEKLY, weeklyPool);

        List<String> activeIds = cfg.getStringList("active");
        if (!activeIds.isEmpty()) {
            activateQuests(activeIds, weeklyPool, weeklyQuests, "tygodniowych");
        } else {
            int count = plugin.getConfig().getInt("quests.weekly-count", 2);
            rotateQuests(weeklyPool, weeklyQuests, count);
            saveActiveQuests(cfg, "weekly_quests.yml", weeklyQuests);
        }

        plugin.getLogger().info("[QuestManager] Pula tygodniowych: " + weeklyPool.size()
                + ", aktywnych: " + weeklyQuests.size());
    }

    private void loadPoolSection(FileConfiguration cfg, String section,
                                 QuestType type, Map<String, QuestDefinition> target) {
        ConfigurationSection cs = cfg.getConfigurationSection(section);
        if (cs == null) return;

        for (String id : cs.getKeys(false)) {
            try {
                String path        = section + "." + id;
                QuestTrigger trigger = parseTrigger(cfg.getString(path + ".trigger"), id);
                String targetName  = cfg.getString(path + ".target", "").toUpperCase();
                int amount         = cfg.getInt(path + ".amount", 1);
                String displayName = cfg.getString(path + ".display-name", id);
                String description = cfg.getString(path + ".description", "");
                long xpReward      = cfg.getLong(path + ".xp-reward", 0L);
                Material icon      = parseMaterial(cfg.getString(path + ".icon", "PAPER"), id);
                List<ItemStack> items = parseItemRewards(cfg, path + ".item-rewards");

                target.put(id.toLowerCase(), new QuestDefinition(id, type, trigger, targetName,
                        amount, displayName, description, xpReward, items, icon));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[QuestManager] Błąd przy ładowaniu questa '" + id + "'", e);
            }
        }
    }

    private void activateQuests(List<String> ids, Map<String, QuestDefinition> pool,
                                List<QuestDefinition> target, String label) {
        target.clear();
        for (String id : ids) {
            QuestDefinition def = pool.get(id.toLowerCase());
            if (def != null) {
                target.add(def);
            } else {
                plugin.getLogger().warning("[QuestManager] Aktywny quest '" + id
                        + "' nie istnieje w puli " + label + " — pomijam.");
            }
        }
    }

    private void loadAchievements() {
        File file = new File(plugin.getDataFolder(), "achievements.yml");
        if (!file.exists()) plugin.saveResource("achievements.yml", false);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection cs = cfg.getConfigurationSection("achievements");
        if (cs == null) return;

        for (String id : cs.getKeys(false)) {
            try {
                String path          = "achievements." + id;
                String category      = cfg.getString(path + ".category", "Inne");
                QuestTrigger trigger = parseTrigger(cfg.getString(path + ".trigger"), id);
                String target        = cfg.getString(path + ".target", "").toUpperCase();
                String displayName   = cfg.getString(path + ".display-name", id);
                String description   = cfg.getString(path + ".description", "");
                Material icon        = parseMaterial(cfg.getString(path + ".icon", "NETHER_STAR"), id);

                String globalTarget = cfg.getString(path + ".target", "").toUpperCase();

                List<AchievementDefinition.Tier> tiers = new ArrayList<>();
                ConfigurationSection tiersCs = cfg.getConfigurationSection(path + ".tiers");
                if (tiersCs != null) {
                    for (String tierKey : tiersCs.getKeys(false)) {
                        String tp   = path + ".tiers." + tierKey;
                        long tierXp = cfg.getLong(tp + ".xp-reward", 0L);
                        double tierMoney = cfg.getDouble(tp + ".money-reward", 0.0);
                        List<ItemStack> tierItems = parseItemRewards(cfg, tp + ".item-rewards");

                        ConfigurationSection targetsCs = cfg.getConfigurationSection(tp + ".targets");
                        List<AchievementDefinition.TargetRequirement> requirements;
                        if (targetsCs != null) {
                            // Nowy format: multi-target
                            requirements = new ArrayList<>();
                            for (String t : targetsCs.getKeys(false)) {
                                requirements.add(new AchievementDefinition.TargetRequirement(
                                        t.toUpperCase(), targetsCs.getInt(t)));
                            }
                        } else {
                            // Stary format: single-target
                            int tierAmount = cfg.getInt(tp + ".amount", 1);
                            requirements = List.of(new AchievementDefinition.TargetRequirement(globalTarget, tierAmount));
                        }
                        tiers.add(new AchievementDefinition.Tier(requirements, tierXp, tierItems, tierMoney));
                    }
                }
                tiers.sort(Comparator.comparingInt(AchievementDefinition.Tier::getSingleAmount));

                AchievementDefinition achievement = new AchievementDefinition(
                        id, category, trigger, target, displayName, description, icon, tiers);
                achievements.add(achievement);
                achievementsByCategory
                        .computeIfAbsent(category, k -> new ArrayList<>())
                        .add(achievement);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[QuestManager] Błąd przy ładowaniu osiągnięcia '" + id + "'", e);
            }
        }
        plugin.getLogger().info("[QuestManager] Załadowano " + achievements.size()
                + " osiągnięć w " + achievementsByCategory.size() + " kategoriach.");
    }

    // =========================================================================
    // Reload definicji (bez kasowania danych gracza)
    // =========================================================================

    public void reload() {
        dailyPool.clear();
        weeklyPool.clear();
        dailyQuests.clear();
        weeklyQuests.clear();
        achievements.clear();
        achievementsByCategory.clear();

        loadDefinitions();

        plugin.getLogger().info("[QuestManager] Przeladowano definicje questow i osiagniec.");
    }

    // =========================================================================
    // Rotacja questów przy resecie
    // =========================================================================

    private void performRotation(String fileName, Map<String, QuestDefinition> pool,
                                 List<QuestDefinition> active, int count) {
        FileConfiguration cfg = loadOrCreate(fileName);
        List<String> nextIds = cfg.getStringList("next");

        active.clear();

        if (!nextIds.isEmpty()) {
            for (String id : nextIds) {
                QuestDefinition def = pool.get(id.toLowerCase());
                if (def != null) active.add(def);
                else plugin.getLogger().warning("[QuestManager] Quest z next: '" + id
                        + "' nie istnieje w puli — pomijam.");
            }
            plugin.getLogger().info("[QuestManager] Użyto " + active.size()
                    + " questów z sekcji next: w " + fileName);
            cfg.set("next", new ArrayList<>());
        } else {
            rotateQuests(pool, active, count);
            plugin.getLogger().info("[QuestManager] Wylosowano " + active.size()
                    + " questów z puli " + fileName);
        }

        saveActiveQuests(cfg, fileName, active);
    }

    private void rotateQuests(Map<String, QuestDefinition> pool,
                              List<QuestDefinition> active, int count) {
        Set<String> currentIds = new HashSet<>();
        for (QuestDefinition q : active) currentIds.add(q.getId().toLowerCase());

        List<QuestDefinition> candidates = new ArrayList<>();
        for (QuestDefinition q : pool.values()) {
            if (!currentIds.contains(q.getId().toLowerCase())) candidates.add(q);
        }

        if (candidates.size() < count) {
            candidates.addAll(pool.values());
        }

        Collections.shuffle(candidates);
        active.clear();
        for (int i = 0; i < Math.min(count, candidates.size()); i++) {
            active.add(candidates.get(i));
        }
    }

    private void saveActiveQuests(FileConfiguration cfg, String fileName,
                                  List<QuestDefinition> active) {
        List<String> ids = new ArrayList<>();
        for (QuestDefinition q : active) ids.add(q.getId());
        cfg.set("active", ids);

        File file = new File(plugin.getDataFolder(), fileName);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "[QuestManager] Nie można zapisać " + fileName, e);
        }
    }

    private FileConfiguration loadOrCreate(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) plugin.saveResource(fileName, false);
        return YamlConfiguration.loadConfiguration(file);
    }

    // =========================================================================
    // Rejestrowanie postępu — wejście z eventów
    // =========================================================================

    public void addProgress(Player player, QuestTrigger trigger, String target, int amount) {
        addProgress(player.getUniqueId(), trigger, target, amount);
    }

    /** Wersja UUID — działa też dla offline graczy (np. sprzedaż aukcji gdy sprzedający offline). */
    public void addProgress(UUID playerUUID, QuestTrigger trigger, String target, int amount) {
        Island island = islandManager.getIsland(playerUUID);
        if (island == null) return;

        UUID ownerUUID = island.getOwner();
        IslandQuestData data = getOrCreateData(ownerUUID);
        boolean changed = false;
        int islandLevel = getIslandXpLevel(island);

        if (islandLevel >= 10) {
            for (QuestDefinition quest : dailyQuests) {
                if (!matches(quest, trigger, target)) continue;
                if (data.isDailyCompleted(quest.getId())) continue;
                data.addDailyProgress(quest.getId(), amount);
                changed = true;
                checkQuestCompletion(island, data, quest, playerUUID);
            }

            for (QuestDefinition quest : weeklyQuests) {
                if (!matches(quest, trigger, target)) continue;
                if (data.isWeeklyCompleted(quest.getId())) continue;
                data.addWeeklyProgress(quest.getId(), amount);
                changed = true;
                checkQuestCompletion(island, data, quest, playerUUID);
            }
        }

        for (AchievementDefinition achievement : achievements) {
            if (achievement.getTrigger() != trigger) continue;
            int completedTiers = data.getCompletedTiers(achievement.getId());
            if (achievement.isFullyCompleted(completedTiers)) continue;
            if (!achievement.getCategory().equals("Tutorial") && islandLevel < 10) continue;

            AchievementDefinition.Tier nextTier = achievement.getNextTier(completedTiers);
            if (nextTier == null) continue;

            if (nextTier.isMultiTarget()) {
                // Aktualizuj tylko konkretny target który pasuje do wymagania
                AchievementDefinition.TargetRequirement req = nextTier.findRequirement(target);
                if (req == null) continue;
                data.addAchievementTargetProgress(achievement.getId(), target, amount);
            } else {
                // Single-target: stary schemat
                if (!matchesTarget(achievement.getTarget(), target)) continue;
                data.addAchievementProgress(achievement.getId(), amount);
            }
            changed = true;
            checkAchievementCompletion(island, data, achievement, playerUUID);
        }

        if (changed) db.saveQuestData(ownerUUID, data);
    }

    // =========================================================================
    // Sprawdzanie ukończenia questów
    // =========================================================================

    private void checkQuestCompletion(Island island, IslandQuestData data,
                                      QuestDefinition quest, UUID triggerPlayerUUID) {
        int progress = quest.getType() == QuestType.DAILY
                ? data.getDailyProgress(quest.getId())
                : data.getWeeklyProgress(quest.getId());

        if (progress < quest.getAmount()) return;

        if (quest.getType() == QuestType.DAILY) data.setDailyCompleted(quest.getId());
        else                                     data.setWeeklyCompleted(quest.getId());

        notifyIslandMembers(island, "§a§lQuest ukończony! §r§f" + quest.getDisplayName()
                + " §8(+" + quest.getXpReward() + " XP wyspy)");
        giveRewards(island, quest.getXpReward(), quest.getItemRewards());

        // Sprawdź czy ukończono cały zestaw i należy się bonus
        checkAllQuestsBonus(island, data, triggerPlayerUUID, quest.getType());
    }

    /**
     * Sprawdza czy wszystkie aktywne questy danego rodzaju zostały ukończone.
     * Jeśli tak — daje bonus i odpala triggery osiągnięć COMPLETE_*_QUEST_SET.
     * Bonus może być dany tylko raz na reset (śledzone przez klucz specjalny w secie).
     */
    private void checkAllQuestsBonus(Island island, IslandQuestData data,
                                     UUID triggerPlayerUUID, QuestType type) {
        String bonusKey           = type == QuestType.DAILY ? DAILY_BONUS_KEY : WEEKLY_BONUS_KEY;
        List<QuestDefinition> all = type == QuestType.DAILY ? dailyQuests : weeklyQuests;
        long bonusXp              = type == QuestType.DAILY ? dailyBonusXp : weeklyBonusXp;
        List<ItemStack> bonusItems = type == QuestType.DAILY ? dailyBonusItems : weeklyBonusItems;
        QuestTrigger achieveTrigger = type == QuestType.DAILY
                ? QuestTrigger.COMPLETE_DAILY_QUEST_SET
                : QuestTrigger.COMPLETE_WEEKLY_QUEST_SET;

        // Bonus już dany w tej sesji resetowej
        boolean alreadyGiven = type == QuestType.DAILY
                ? data.isDailyCompleted(bonusKey)
                : data.isWeeklyCompleted(bonusKey);
        if (alreadyGiven) return;

        // Sprawdź czy wszystkie questy ukończone
        for (QuestDefinition q : all) {
            boolean done = type == QuestType.DAILY
                    ? data.isDailyCompleted(q.getId())
                    : data.isWeeklyCompleted(q.getId());
            if (!done) return;
        }

        // Oznacz jako dany (klucz usunie się automatycznie przy resecie)
        if (type == QuestType.DAILY) data.setDailyCompleted(bonusKey);
        else                         data.setWeeklyCompleted(bonusKey);

        // Daj nagrodę bonusową
        if (bonusXp > 0 || (bonusItems != null && !bonusItems.isEmpty())) {
            giveRewards(island, bonusXp, bonusItems);
            String typeLabel = type == QuestType.DAILY ? "dzienne" : "tygodniowe";
            notifyIslandMembers(island,
                    "§e§lBONUS! §7Ukonczyłes wszystkie questy " + typeLabel
                            + "! §8(+" + bonusXp + " XP wyspy)");
        }

        // Odpala triggery osiągnięć COMPLETE_*_QUEST_SET bezpośrednio na danych
        // (nie przez addProgress — unikamy rekurencji)
        for (AchievementDefinition achievement : achievements) {
            if (achievement.getTrigger() != achieveTrigger) continue;
            if (!matchesTarget(achievement.getTarget(), "")) continue;
            if (achievement.isFullyCompleted(data.getCompletedTiers(achievement.getId()))) continue;
            data.addAchievementProgress(achievement.getId(), 1);
            checkAchievementCompletion(island, data, achievement, triggerPlayerUUID);
        }
        // Zapis wykona outer addProgress po powrocie — dane są na tym samym obiekcie
    }

    // =========================================================================
    // Sprawdzanie ukończenia osiągnięć
    // =========================================================================

    private void checkAchievementCompletion(Island island, IslandQuestData data,
                                            AchievementDefinition achievement, UUID triggerPlayerUUID) {
        int completedTiers = data.getCompletedTiers(achievement.getId());
        AchievementDefinition.Tier nextTier = achievement.getNextTier(completedTiers);
        if (nextTier == null) return;

        if (nextTier.isMultiTarget()) {
            for (AchievementDefinition.TargetRequirement req : nextTier.requirements()) {
                if (data.getAchievementTargetProgress(achievement.getId(), req.target()) < req.amount()) return;
            }
        } else {
            if (data.getAchievementProgress(achievement.getId()) < nextTier.getSingleAmount()) return;
        }

        data.incrementCompletedTiers(achievement.getId());
        giveRewards(island, nextTier.xpReward(), nextTier.itemRewards());
        if (nextTier.moneyReward() > 0 && economyManager != null && economyManager.isAvailable()) {
            Player triggerPlayer = Bukkit.getPlayer(triggerPlayerUUID);
            if (triggerPlayer != null) {
                economyManager.deposit(triggerPlayer, nextTier.moneyReward());
            }
        }

        String moneyPart = (nextTier.moneyReward() > 0 && economyManager != null)
                ? " §8(+" + economyManager.format(nextTier.moneyReward()) + ")" : "";
        String baseMsg = "§6§lOsiągnięcie! §r§f" + achievement.getDisplayName()
                + " §7(Poziom " + (completedTiers + 1) + ")"
                + " §8(+" + nextTier.xpReward() + " XP wyspy)" + moneyPart;
        notifyAchievementMembers(island, triggerPlayerUUID, baseMsg);

        // Rekurencja dla wielu progów ukończonych jednym ruchem
        checkAchievementCompletion(island, data, achievement, triggerPlayerUUID);
    }

    // =========================================================================
    // Nagrody
    // =========================================================================

    public void setBossBarManager(IslandBossBarManager bossBarManager) {
        this.bossBarManager = bossBarManager;
    }

    public void setEconomyManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    private void giveRewards(Island island, long xp, List<ItemStack> items) {
        if (xp > 0) {
            int  levelBefore = getIslandXpLevel(island);
            long xpBefore    = island.getXp();
            island.addXp(xp);
            db.saveIsland(island, IslandType.OVERWORLD);
            int levelAfter = getIslandXpLevel(island);
            if (levelAfter > levelBefore) {
                notifyIslandMembers(island,
                        "§6§l★ §eWyspa osiągnęła poziom §6§l" + levelAfter + "§e! §6§l★");
            }
            if (bossBarManager != null) {
                bossBarManager.showXpGainForMembers(island, levelBefore, levelAfter, xpBefore);
            }
        }
        if (items == null || items.isEmpty()) return;
        for (UUID uuid : getAllMembers(island)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            for (ItemStack item : items) {
                if (item == null) continue;
                p.getInventory().addItem(item.clone()).forEach((slot, overflow) ->
                        p.getWorld().dropItemNaturally(p.getLocation(), overflow));
            }
        }
    }

    private void notifyIslandMembers(Island island, String message) {
        for (UUID uuid : getAllMembers(island)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    /**
     * Wysyła powiadomienie o osiągnięciu:
     * - graczowi który je zdobył: standardowa wiadomość
     * - pozostałym członkom wyspy: wiadomość z dopiskiem kto je zdobył
     */
    private void notifyAchievementMembers(Island island, UUID triggerPlayerUUID, String message) {
        String triggerName = null;
        for (UUID uuid : getAllMembers(island)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (uuid.equals(triggerPlayerUUID)) {
                p.sendMessage(message);
            } else {
                if (triggerName == null) {
                    org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(triggerPlayerUUID);
                    triggerName = op.getName() != null ? op.getName() : "Nieznany";
                }
                p.sendMessage(message + " §7(zdobyte przez §f" + triggerName + "§7)");
            }
        }
    }

    private List<UUID> getAllMembers(Island island) {
        List<UUID> all = new ArrayList<>();
        all.add(island.getOwner());
        all.addAll(island.getMembers());
        return all;
    }

    // =========================================================================
    // Resety
    // =========================================================================

    public void resetDaily() {
        int count = plugin.getConfig().getInt("quests.daily-count", 3);
        performRotation("daily_quests.yml", dailyPool, dailyQuests, count);

        questDataMap.values().forEach(IslandQuestData::resetDaily);
        questDataMap.forEach(db::saveQuestData);

        Bukkit.broadcastMessage("§e§lSkyBlock §8» §7Questy dzienne zostały zresetowane!");
        plugin.getLogger().info("[QuestManager] Zresetowano questy dzienne.");
    }

    public void resetWeekly() {
        int count = plugin.getConfig().getInt("quests.weekly-count", 2);
        performRotation("weekly_quests.yml", weeklyPool, weeklyQuests, count);

        questDataMap.values().forEach(IslandQuestData::resetWeekly);
        questDataMap.forEach(db::saveQuestData);

        Bukkit.broadcastMessage("§e§lSkyBlock §8» §7Questy tygodniowe zostały zresetowane!");
        plugin.getLogger().info("[QuestManager] Zresetowano questy tygodniowe.");
    }

    // =========================================================================
    // Publiczne metody testowe / administracyjne
    // =========================================================================

    /** Przyznaje wyspie XP (z animacją BossBar i powiadomieniem o awansie). */
    public void grantXp(Island island, long xp) {
        giveRewards(island, xp, null);
    }

    /**
     * Ustawia XP wyspy tak, by znajdowała się dokładnie na początku danego poziomu.
     * Wyzwala animację BossBar.
     */
    public void setIslandXpLevel(Island island, int level) {
        long targetXp   = xpThresholdForLevel(level);
        int  lvlBefore  = getIslandXpLevel(island);
        long xpBefore   = island.getXp();
        island.setXp(targetXp);
        db.saveIsland(island, IslandType.OVERWORLD);
        int lvlAfter = getIslandXpLevel(island);
        if (bossBarManager != null) {
            bossBarManager.showXpGainForMembers(island, lvlBefore, lvlAfter, xpBefore);
        }
    }

    // =========================================================================
    // XP → Poziom wyspy
    // =========================================================================

    /**
     * Skumulowany próg XP potrzebny do osiągnięcia danego poziomu (licząc od 1).
     * Poziom 1 = 0 XP. Każdy kolejny krok to: base + multiplier * n^exponent.
     */
    public long xpThresholdForLevel(int level) {
        if (level <= 1) return 0;
        double base       = plugin.getConfig().getDouble("island-xp.formula.base", 20.0);
        double multiplier = plugin.getConfig().getDouble("island-xp.formula.multiplier", 3.0);
        double exponent   = plugin.getConfig().getDouble("island-xp.formula.exponent", 1.7);
        long total = 0;
        for (int n = 1; n < level; n++) {
            total += (long) (base + multiplier * Math.pow(n, exponent));
        }
        return total;
    }

    public int getIslandXpLevel(Island island) {
        long xp = island.getXp();
        int level = 1;
        while (xp >= xpThresholdForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    /** Skumulowany XP potrzebny do osiągnięcia następnego poziomu (do paska postępu). */
    public long getXpForNextLevel(Island island) {
        return xpThresholdForLevel(getIslandXpLevel(island) + 1);
    }

    // =========================================================================
    // Czas do następnego resetu
    // =========================================================================

    public ZonedDateTime getNextDailyReset() {
        String timeStr = plugin.getConfig().getString("quests.daily-reset-time", "06:00");
        LocalTime resetTime;
        try {
            resetTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            resetTime = LocalTime.of(6, 0);
        }
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now  = ZonedDateTime.now(zone);
        ZonedDateTime next = now.toLocalDate().atTime(resetTime).atZone(zone);
        if (!next.isAfter(now)) next = next.plusDays(1);
        return next;
    }

    public ZonedDateTime getNextWeeklyReset() {
        String dayStr  = plugin.getConfig().getString("quests.weekly-reset-day", "MONDAY");
        String timeStr = plugin.getConfig().getString("quests.weekly-reset-time", "06:00");
        DayOfWeek resetDay;
        LocalTime resetTime;
        try {
            resetDay  = DayOfWeek.valueOf(dayStr.toUpperCase());
            resetTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            resetDay  = DayOfWeek.MONDAY;
            resetTime = LocalTime.of(6, 0);
        }
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now  = ZonedDateTime.now(zone);
        ZonedDateTime next = now.with(TemporalAdjusters.nextOrSame(resetDay))
                .toLocalDate().atTime(resetTime).atZone(zone);
        if (!next.isAfter(now)) next = next.plusWeeks(1);
        return next;
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    private IslandQuestData getOrCreateData(UUID ownerUUID) {
        return questDataMap.computeIfAbsent(ownerUUID, k -> new IslandQuestData());
    }

    public IslandQuestData getQuestData(UUID ownerUUID) {
        return questDataMap.computeIfAbsent(ownerUUID, k -> new IslandQuestData());
    }

    private boolean matches(QuestDefinition quest, QuestTrigger trigger, String target) {
        return quest.getTrigger() == trigger && matchesTarget(quest.getTarget(), target);
    }

    private boolean matchesTarget(String expected, String actual) {
        return expected == null || expected.isEmpty() || expected.equalsIgnoreCase(actual);
    }

    private QuestTrigger parseTrigger(String name, String context) {
        try {
            return QuestTrigger.valueOf(name != null ? name.toUpperCase() : "");
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[QuestManager] Nieznany trigger '" + name
                    + "' w: " + context + " — używam BREAK_BLOCK.");
            return QuestTrigger.BREAK_BLOCK;
        }
    }

    private Material parseMaterial(String name, String context) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[QuestManager] Nieznany materiał '" + name + "' w: " + context);
            return Material.PAPER;
        }
    }

    private List<ItemStack> parseItemRewards(FileConfiguration cfg, String path) {
        List<ItemStack> result = new ArrayList<>();
        if (!cfg.contains(path)) return result;
        for (String entry : cfg.getStringList(path)) {
            try {
                ItemStack item = parseRewardEntry(entry);
                if (item != null) result.add(item);
                else plugin.getLogger().warning("[QuestManager] Nie rozpoznano nagrody: '" + entry + "'");
            } catch (Exception e) {
                plugin.getLogger().warning("[QuestManager] Blad parsowania nagrody: '"
                        + entry + "' — " + e.getMessage());
            }
        }
        return result;
    }

    private ItemStack parseRewardEntry(String entry) {
        if (entry == null || entry.isBlank()) return null;
        String lower = entry.toLowerCase().trim();

        if (lower.startsWith("custom:")) {
            String id = lower.substring(7);
            ItemStack custom = stark.skyBlockTest2.item.CustomItemRegistry.get(id);
            if (custom == null) plugin.getLogger().warning("[QuestManager] Nieznany custom item: '" + id + "'");
            return custom;
        }

        if (lower.startsWith("spawner:")) {
            String entityName  = entry.substring(8).toUpperCase();
            String registryKey = "spawner_" + entityName.toLowerCase();
            ItemStack fromRegistry = stark.skyBlockTest2.item.CustomItemRegistry.get(registryKey);
            if (fromRegistry != null) return fromRegistry;
            try {
                org.bukkit.entity.EntityType et = org.bukkit.entity.EntityType.valueOf(entityName);
                return stark.skyBlockTest2.item.CustomItemRegistry.spawner(et);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[QuestManager] Nieznany mob: '" + entityName + "'");
                return null;
            }
        }

        String[] parts = entry.split(":");
        Material mat = Material.valueOf(parts[0].toUpperCase());
        int qty = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        return new ItemStack(mat, qty);
    }

    public void resetIslandData(UUID ownerUUID) {
        questDataMap.remove(ownerUUID);
        db.deleteQuestData(ownerUUID);
        plugin.getLogger().info("[QuestManager] Zresetowano dane questow wyspy: " + ownerUUID);
    }

    // =========================================================================
    // Gettery publiczne
    // =========================================================================

    public int getCompletedDailyCount(Island island) {
        IslandQuestData data = questDataMap.get(island.getOwner());
        if (data == null) return 0;
        return (int) dailyQuests.stream().filter(q -> data.isDailyCompleted(q.getId())).count();
    }

    public int getCompletedWeeklyCount(Island island) {
        IslandQuestData data = questDataMap.get(island.getOwner());
        if (data == null) return 0;
        return (int) weeklyQuests.stream().filter(q -> data.isWeeklyCompleted(q.getId())).count();
    }

    public List<QuestDefinition>                           getDailyQuests()           { return dailyQuests; }
    public List<QuestDefinition>                           getWeeklyQuests()          { return weeklyQuests; }
    public List<AchievementDefinition>                     getAchievements()          { return achievements; }
    public Map<String, List<AchievementDefinition>>        getAchievementsByCategory(){ return achievementsByCategory; }
    public long            getDailyBonusXp()     { return dailyBonusXp; }
    public List<ItemStack> getDailyBonusItems()  { return dailyBonusItems; }
    public long            getWeeklyBonusXp()    { return weeklyBonusXp; }
    public List<ItemStack> getWeeklyBonusItems() { return weeklyBonusItems; }
    public boolean isDailyBonusClaimed(IslandQuestData data)  { return data.isDailyCompleted(DAILY_BONUS_KEY); }
    public boolean isWeeklyBonusClaimed(IslandQuestData data) { return data.isWeeklyCompleted(WEEKLY_BONUS_KEY); }
}
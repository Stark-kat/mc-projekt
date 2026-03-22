package stark.skyBlockTest2.quest;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import stark.skyBlockTest2.SkyBlockTest2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class QuestStorage {

    private final SkyBlockTest2 plugin;
    private final File file;
    private final FileConfiguration config;

    public QuestStorage(SkyBlockTest2 plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "quest_data.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[QuestStorage] Nie można utworzyć quest_data.yml!");
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    // -------------------------------------------------------------------------
    // Zapis
    // -------------------------------------------------------------------------

    public void saveQuestData(UUID islandOwner, IslandQuestData data) {
        String path = "quests." + islandOwner;

        // Dzienne
        config.set(path + ".daily.lastReset", data.getLastDailyReset());
        config.set(path + ".daily.completed", new ArrayList<>(data.getCompletedDailySet()));
        data.getDailyProgressMap().forEach((k, v) ->
                config.set(path + ".daily.progress." + k, v));

        // Tygodniowe
        config.set(path + ".weekly.lastReset", data.getLastWeeklyReset());
        config.set(path + ".weekly.completed", new ArrayList<>(data.getCompletedWeeklySet()));
        data.getWeeklyProgressMap().forEach((k, v) ->
                config.set(path + ".weekly.progress." + k, v));

        // Osiągnięcia
        data.getAchievementProgressMap().forEach((k, v) ->
                config.set(path + ".achievements.progress." + k, v));
        data.getAchievementTiersMap().forEach((k, v) ->
                config.set(path + ".achievements.tiers." + k, v));
        data.getAchievementTargetProgressMap().forEach((achieveId, targetMap) ->
                targetMap.forEach((target, v) ->
                        config.set(path + ".achievements.target-progress." + achieveId + "." + target, v)));

        saveAsync();
    }

    // -------------------------------------------------------------------------
    // Odczyt
    // -------------------------------------------------------------------------

    public IslandQuestData loadQuestData(UUID islandOwner) {
        String path = "quests." + islandOwner;
        IslandQuestData data = new IslandQuestData();

        if (!config.contains(path)) return data;

        // Dzienne
        data.setLastDailyReset(config.getLong(path + ".daily.lastReset", 0L));
        config.getStringList(path + ".daily.completed")
                .forEach(data.getCompletedDailySet()::add);
        ConfigurationSection dp = config.getConfigurationSection(path + ".daily.progress");
        if (dp != null) dp.getKeys(false).forEach(k ->
                data.getDailyProgressMap().put(k, dp.getInt(k)));

        // Tygodniowe
        data.setLastWeeklyReset(config.getLong(path + ".weekly.lastReset", 0L));
        config.getStringList(path + ".weekly.completed")
                .forEach(data.getCompletedWeeklySet()::add);
        ConfigurationSection wp = config.getConfigurationSection(path + ".weekly.progress");
        if (wp != null) wp.getKeys(false).forEach(k ->
                data.getWeeklyProgressMap().put(k, wp.getInt(k)));

        // Osiągnięcia
        ConfigurationSection ap = config.getConfigurationSection(path + ".achievements.progress");
        if (ap != null) ap.getKeys(false).forEach(k ->
                data.getAchievementProgressMap().put(k, ap.getInt(k)));
        ConfigurationSection at = config.getConfigurationSection(path + ".achievements.tiers");
        if (at != null) at.getKeys(false).forEach(k ->
                data.getAchievementTiersMap().put(k, at.getInt(k)));
        ConfigurationSection atp = config.getConfigurationSection(path + ".achievements.target-progress");
        if (atp != null) {
            for (String achieveId : atp.getKeys(false)) {
                ConfigurationSection inner = atp.getConfigurationSection(achieveId);
                if (inner != null) {
                    inner.getKeys(false).forEach(target ->
                            data.addAchievementTargetProgress(achieveId, target, inner.getInt(target)));
                }
            }
        }

        return data;
    }

    /** Ładuje dane wszystkich wysp do podanej mapy przy starcie serwera. */
    public void loadAllQuestData(Map<UUID, IslandQuestData> target) {
        ConfigurationSection section = config.getConfigurationSection("quests");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                target.put(uuid, loadQuestData(uuid));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[QuestStorage] Nieprawidłowy UUID: " + key + " — pomijam.");
            }
        }

        plugin.getLogger().info("[QuestStorage] Załadowano dane questów dla " + target.size() + " wysp.");
    }

    // -------------------------------------------------------------------------
    // Zapis asynchroniczny (tak samo jak IslandStorage)
    // -------------------------------------------------------------------------

    private void saveAsync() {
        String yaml = config.saveToString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
            try (FileWriter writer = new FileWriter(tmp)) {
                writer.write(yaml);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "[QuestStorage] Błąd zapisu quest_data.yml!", e);
                return;
            }
            if (!tmp.renameTo(file)) {
                file.delete();
                tmp.renameTo(file);
            }
        });
    }

    public void deleteQuestData(UUID islandOwner) {
        config.set("quests." + islandOwner, null);
        saveAsync();
    }
}
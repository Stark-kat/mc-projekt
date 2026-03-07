package stark.skyBlockTest2.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import stark.skyBlockTest2.SkyBlockTest2;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class IslandStorage {

    private final SkyBlockTest2 plugin;
    private final File file;
    private final FileConfiguration config;

    public IslandStorage(SkyBlockTest2 plugin) {
        this.plugin = plugin;

        file = new File(plugin.getDataFolder(), "islands.yml");

        if (!file.exists()) {
            plugin.saveResource("islands.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    // -------------------------------------------------------------------------
    // Zapis wyspy
    // -------------------------------------------------------------------------

    public void saveIsland(Island island, IslandType type) {
        String path = "islands." + type.name() + "." + island.getOwner().toString();

        // Centrum
        config.set(path + ".world", island.getCenter().getWorld().getName());
        config.set(path + ".x", island.getCenter().getX());
        config.set(path + ".y", island.getCenter().getY());
        config.set(path + ".z", island.getCenter().getZ());
        config.set(path + ".size", island.getSize());
        config.set(path + ".index", island.getIndex());

        // Punkt domowy
        config.set(path + ".home.x",     island.getHome().getX());
        config.set(path + ".home.y",     island.getHome().getY());
        config.set(path + ".home.z",     island.getHome().getZ());
        config.set(path + ".home.yaw",   island.getHome().getYaw());
        config.set(path + ".home.pitch", island.getHome().getPitch());

        // Ustawienia gości (tylko OVERWORLD)
        if (type == IslandType.OVERWORLD) {
            for (IslandAction action : IslandAction.values()) {
                config.set(path + ".settings." + action.name(), island.canVisitorDo(action));
            }

            List<String> memberStrings = island.getMembers().stream()
                    .map(UUID::toString).toList();
            config.set(path + ".members", memberStrings);

            List<String> bannedStrings = island.getBannedPlayers().stream()
                    .map(UUID::toString).toList();
            config.set(path + ".banned", bannedStrings);

            island.getMemberRoles().forEach((uuid, role) ->
                    config.set(path + ".roles." + uuid, role.name()));
        }

        saveFileAsync();
    }

    public void deleteIsland(UUID uuid, IslandType type) {
        config.set("islands." + type.name() + "." + uuid.toString(), null);
        saveFileAsync();
    }

    // -------------------------------------------------------------------------
    // Indeks spiral per typ
    // -------------------------------------------------------------------------

    public void saveIndexState(IslandType type, int currentIndex, List<Integer> freeIndexes) {
        config.set("currentIndex." + type.name(), currentIndex);
        config.set("freeIndexes." + type.name(), freeIndexes);
        saveFileAsync();
    }

    public int getCurrentIndex(IslandType type) {
        return config.getInt("currentIndex." + type.name(), 0);
    }

    public List<Integer> getFreeIndexes(IslandType type) {
        return config.getIntegerList("freeIndexes." + type.name());
    }

    // -------------------------------------------------------------------------
    // Ładowanie wysp przy starcie
    // -------------------------------------------------------------------------

    public Map<UUID, Island> loadIslands(IslandType type) {
        Map<UUID, Island> loaded = new HashMap<>();

        String sectionPath = "islands." + type.name();
        if (!config.contains(sectionPath)) return loaded;

        ConfigurationSection islandsSection = config.getConfigurationSection(sectionPath);
        if (islandsSection == null) return loaded;

        for (String key : islandsSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = sectionPath + "." + key;

                String worldName = config.getString(path + ".world");
                World world = Bukkit.getWorld(worldName != null ? worldName : "");

                // Świat nie załadowany lub nieznany — logujemy i pomijamy
                if (world == null) {
                    plugin.getLogger().warning("[IslandStorage] Nie można załadować wyspy " + key
                            + " — świat '" + worldName + "' nie istnieje. Wyspa zostanie pominięta.");
                    continue;
                }

                double x     = config.getDouble(path + ".x");
                double y     = config.getDouble(path + ".y");
                double z     = config.getDouble(path + ".z");
                int    size  = config.getInt(path + ".size");
                int    index = config.getInt(path + ".index");

                Location center = new Location(world, x, y, z);

                Location home;
                if (config.contains(path + ".home")) {
                    double hX    = config.getDouble(path + ".home.x");
                    double hY    = config.getDouble(path + ".home.y");
                    double hZ    = config.getDouble(path + ".home.z");
                    float  yaw   = (float) config.getDouble(path + ".home.yaw");
                    float  pitch = (float) config.getDouble(path + ".home.pitch");
                    home = new Location(world, hX, hY, hZ, yaw, pitch);
                } else {
                    home = center.clone().add(0.5, 1, 0.5);
                }

                List<UUID> members = new ArrayList<>();
                if (config.contains(path + ".members")) {
                    for (String mId : config.getStringList(path + ".members")) {
                        try {
                            members.add(UUID.fromString(mId));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("[IslandStorage] Nieprawidłowy UUID członka '" + mId
                                    + "' w wyspie " + key + " — pomijam.");
                        }
                    }
                }

                Island island = new Island(uuid, center, size, index, members);
                island.setHome(home);

                // Ustawienia gości, bany i role — tylko dla OVERWORLD
                if (type == IslandType.OVERWORLD) {
                    ConfigurationSection settings = config.getConfigurationSection(path + ".settings");
                    if (settings != null) {
                        for (String actionName : settings.getKeys(false)) {
                            try {
                                IslandAction action = IslandAction.valueOf(actionName);
                                island.setVisitorSetting(action, settings.getBoolean(actionName));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("[IslandStorage] Nieznana akcja '" + actionName
                                        + "' w ustawieniach wyspy " + key + " — pomijam.");
                            }
                        }
                    }

                    for (String bannedId : config.getStringList(path + ".banned")) {
                        try {
                            island.banPlayer(UUID.fromString(bannedId));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("[IslandStorage] Nieprawidłowy UUID bana '"
                                    + bannedId + "' w wyspie " + key + " — pomijam.");
                        }
                    }

                    ConfigurationSection roles = config.getConfigurationSection(path + ".roles");
                    if (roles != null) {
                        for (String roleUuid : roles.getKeys(false)) {
                            try {
                                island.setRole(UUID.fromString(roleUuid),
                                        IslandRole.valueOf(roles.getString(roleUuid)));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("[IslandStorage] Nieprawidłowa rola dla '"
                                        + roleUuid + "' w wyspie " + key + " — pomijam.");
                            }
                        }
                    }
                }

                loaded.put(uuid, island);

            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING,
                        "[IslandStorage] Nieprawidłowy klucz UUID '" + key + "' — pomijam.", e);
            }
        }

        return loaded;
    }

    // -------------------------------------------------------------------------
    // Zapis asynchroniczny — nie blokuje głównego wątku
    // -------------------------------------------------------------------------

    private void saveFileAsync() {
        // Serializujemy config do stringa na głównym wątku (thread-safe),
        // a sam zapis I/O wykonujemy asynchronicznie
        String yaml = config.saveToString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write(yaml);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "[IslandStorage] Nie udało się zapisać islands.yml!", e);
            }
        });
    }
}
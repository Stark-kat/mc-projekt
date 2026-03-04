package stark.skyBlockTest2.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.island.listener.IslandProtectionListener;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    public void saveIsland(Island island) {

        String path = "islands." + island.getOwner().toString();

        config.set(path + ".world", island.getCenter().getWorld().getName());
        config.set(path + ".x", island.getCenter().getX());
        config.set(path + ".y", island.getCenter().getY());
        config.set(path + ".z", island.getCenter().getZ());
        config.set(path + ".size", island.getSize());
        config.set(path + ".index", island.getIndex());
        config.set(path + ".home.x", island.getHome().getX());
        config.set(path + ".home.y", island.getHome().getY());
        config.set(path + ".home.z", island.getHome().getZ());
        config.set(path + ".home.yaw", island.getHome().getYaw());
        config.set(path + ".home.pitch", island.getHome().getPitch());
        for (IslandProtectionListener.IslandAction action : IslandProtectionListener.IslandAction.values()) {
            config.set(path + ".settings." + action.name(), island.canVisitorDo(action));
        }
        List<String> memberStrings = island.getMembers().stream()
                .map(UUID::toString)
                .toList();
        config.set(path + ".members", memberStrings);

        saveFile();
    }

    public void deleteIsland(UUID uuid) {
        config.set("islands." + uuid.toString(), null);
        saveFile();
    }

    public Map<UUID, Island> loadIslands() {

        Map<UUID, Island> loaded = new HashMap<>();

        if (!config.contains("islands")) return loaded;

        for (String key : config.getConfigurationSection("islands").getKeys(false)) {

            UUID uuid = UUID.fromString(key);

            String worldName = config.getString("islands." + key + ".world");
            World world = Bukkit.getWorld(worldName);

            double x = config.getDouble("islands." + key + ".x");
            double y = config.getDouble("islands." + key + ".y");
            double z = config.getDouble("islands." + key + ".z");
            int size = config.getInt("islands." + key + ".size");
            int index = config.getInt("islands." + key + ".index");

            Location center = new Location(world, x, y, z);

            Location home;
            if (config.contains("islands." + key + ".home")) {
                double hX = config.getDouble("islands." + key + ".home.x");
                double hY = config.getDouble("islands." + key + ".home.y");
                double hZ = config.getDouble("islands." + key + ".home.z");
                float yaw = (float) config.getDouble("islands." + key + ".home.yaw");
                float pitch = (float) config.getDouble("islands." + key + ".home.pitch");
                home = new Location(world, hX, hY, hZ, yaw, pitch);
            } else {
                home = center.clone().add(0.5, 1, 0.5);
            }

            List<UUID> members = new ArrayList<>();
            if (config.contains("islands." + key + ".members")) {
                for (String mId : config.getStringList("islands." + key + ".members")) {
                    members.add(UUID.fromString(mId));
                }
            }

            Island island = new Island(uuid, center, size, index, members);
            island.setHome(home);

            if (config.contains("islands." + key + ".settings")) {
                for (String actionName : config.getConfigurationSection("islands." + key + ".settings").getKeys(false)) {
                    try {
                        IslandProtectionListener.IslandAction action = IslandProtectionListener.IslandAction.valueOf(actionName);
                        boolean allowed = config.getBoolean("islands." + key + ".settings." + actionName);
                        island.setVisitorSetting(action, allowed);
                    } catch (IllegalArgumentException e) {
                    }
                }
            }

            loaded.put(uuid, island);
        }
        return loaded;
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCurrentIndex() {
        return config.getInt("currentIndex", 0);
    }

    public void setCurrentIndex(int index) {
        config.set("currentIndex", index);
        saveFile();
    }

    public List<Integer> getFreeIndexes() {
        return config.getIntegerList("freeIndexes");
    }

    public void setFreeIndexes(List<Integer> list) {
        config.set("freeIndexes", list);
        saveFile();
    }
}

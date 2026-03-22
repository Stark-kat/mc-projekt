package stark.skyBlockTest2.util;

import org.bukkit.plugin.java.JavaPlugin;

public class CounterManager {

    private final JavaPlugin plugin;

    public CounterManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void add (String name) {
        String path = "liczniki." + name;
        int value = plugin.getConfig().getInt(path, 0);
        plugin.getConfig().set(path, value + 1);
        plugin.saveConfig();
    }
}

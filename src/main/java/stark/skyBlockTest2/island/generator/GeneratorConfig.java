package stark.skyBlockTest2.island.generator;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class GeneratorConfig {

    private final Map<GeneratorType, GeneratorDefinition> definitions = new EnumMap<>(GeneratorType.class);

    public GeneratorConfig(JavaPlugin plugin) {
        load(plugin);
    }

    private void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "generator.yml");
        if (!file.exists()) {
            plugin.saveResource("generator.yml", false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection generators = cfg.getConfigurationSection("generators");
        if (generators == null) {
            plugin.getLogger().warning("[Generator] Brak sekcji 'generators' w generator.yml!");
            return;
        }

        for (String key : generators.getKeys(false)) {
            GeneratorType type;
            try {
                type = GeneratorType.valueOf(key.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Generator] Nieznany typ: " + key);
                continue;
            }

            ConfigurationSection section = generators.getConfigurationSection(key);
            if (section == null) continue;

            List<GeneratorLevel> levels = parseLevels(section, plugin);
            if (levels.isEmpty()) {
                plugin.getLogger().warning("[Generator] Brak poziomów dla: " + key);
                continue;
            }

            if (type.isMachineType()) {
                String blockName = section.getString("machine_block", "LODESTONE");
                Material machineBlock = parseMaterial(blockName, plugin, key);
                int intervalTicks = section.getInt("machine_interval_ticks", 40);
                int radius        = section.getInt("machine_radius", 1);
                definitions.put(type, new GeneratorDefinition(type, levels, machineBlock, intervalTicks, radius));
            } else {
                definitions.put(type, new GeneratorDefinition(type, levels));
            }

            plugin.getLogger().info("[Generator] Załadowano " + key + " (" + levels.size() + " poziomów)");
        }
    }

    private List<GeneratorLevel> parseLevels(ConfigurationSection section, JavaPlugin plugin) {
        ConfigurationSection levelsSection = section.getConfigurationSection("levels");
        if (levelsSection == null) return Collections.emptyList();

        List<GeneratorLevel> result = new ArrayList<>();

        // Sortujemy klucze numerycznie
        List<String> keys = new ArrayList<>(levelsSection.getKeys(false));
        keys.sort(Comparator.comparingInt(k -> {
            try { return Integer.parseInt(k); } catch (NumberFormatException e) { return 0; }
        }));

        for (String key : keys) {
            ConfigurationSection lvl = levelsSection.getConfigurationSection(key);
            if (lvl == null) continue;

            int levelNum   = Integer.parseInt(key);
            String name    = lvl.getString("name", "Poziom " + levelNum);
            double cost    = lvl.getDouble("cost", 0);

            ConfigurationSection dropsSec = lvl.getConfigurationSection("drops");
            if (dropsSec == null) {
                plugin.getLogger().warning("[Generator] Brak sekcji 'drops' dla poziomu " + key);
                continue;
            }

            Map<Material, Integer> drops = new LinkedHashMap<>();
            for (String matName : dropsSec.getKeys(false)) {
                Material mat = parseMaterial(matName, plugin, "drops." + key);
                if (mat != null) {
                    if (!mat.isBlock()) {
                        plugin.getLogger().warning("[Generator] Materiał '" + matName + "' nie jest blokiem — pomijam.");
                    } else {
                        drops.put(mat, dropsSec.getInt(matName));
                    }
                }
            }

            result.add(new GeneratorLevel(levelNum, name, cost, drops));
        }

        return result;
    }

    private Material parseMaterial(String name, JavaPlugin plugin, String context) {
        Material mat = Material.matchMaterial(name);
        if (mat == null) {
            plugin.getLogger().log(Level.WARNING, "[Generator] Nieznany materiał ''{0}'' w ''{1}''",
                    new Object[]{name, context});
        }
        return mat;
    }

    public void reload(JavaPlugin plugin) {
        definitions.clear();
        load(plugin);
    }

    public GeneratorDefinition getDefinition(GeneratorType type) {
        return definitions.get(type);
    }

    public Map<GeneratorType, GeneratorDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }
}
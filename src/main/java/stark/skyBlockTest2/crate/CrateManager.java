package stark.skyBlockTest2.crate;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.item.CustomItemRegistry;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class CrateManager {

    private final SkyBlockTest2 plugin;
    private final Map<String, CrateDefinition> crates = new LinkedHashMap<>();

    public CrateManager(SkyBlockTest2 plugin) {
        this.plugin = plugin;
        loadCrates();
    }

    // =========================================================================
    // Ładowanie z YML
    // =========================================================================

    private void loadCrates() {
        File file = new File(plugin.getDataFolder(), "loot_crates.yml");
        if (!file.exists()) plugin.saveResource("loot_crates.yml", false);

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("crates");
        if (section == null) {
            plugin.getLogger().warning("[CrateManager] Brak sekcji 'crates' w loot_crates.yml!");
            return;
        }

        for (String id : section.getKeys(false)) {
            try {
                String path        = "crates." + id;
                String displayName = cfg.getString(path + ".display-name", id);
                CrateRarity rarity = CrateRarity.fromString(
                        cfg.getString(path + ".rarity", "COMMON"));

                List<CrateReward> rewards = loadRewards(cfg, path + ".rewards", id);
                if (rewards.isEmpty()) {
                    plugin.getLogger().warning("[CrateManager] Skrzynka '" + id + "' nie ma nagrod — pomijam.");
                    continue;
                }

                crates.put(id.toLowerCase(), new CrateDefinition(id, displayName, rarity, rewards));

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[CrateManager] Blad ladowania skrzynki '" + id + "'", e);
            }
        }

        plugin.getLogger().info("[CrateManager] Zaladowano " + crates.size() + " skrzynek.");

        // Rejestrujemy itemy skrzynek w CustomItemRegistry
        registerCrateItems();
    }

    private List<CrateReward> loadRewards(FileConfiguration cfg, String path, String crateId) {
        List<CrateReward> result = new ArrayList<>();

        if (!cfg.isList(path)) return result;

        for (var entry : cfg.getMapList(path)) {
            try {
                String itemStr    = String.valueOf(entry.get("item"));
                int weight        = entry.containsKey("weight")
                        ? Integer.parseInt(String.valueOf(entry.get("weight"))) : 10;
                String rarityStr  = entry.containsKey("rarity")
                        ? String.valueOf(entry.get("rarity")) : "COMMON";
                String name       = entry.containsKey("name")
                        ? String.valueOf(entry.get("name")) : null;
                int amount        = entry.containsKey("amount")
                        ? Integer.parseInt(String.valueOf(entry.get("amount"))) : 1;

                CrateRarity rewardRarity = CrateRarity.fromString(rarityStr);
                ItemStack item = parseItem(itemStr, amount);
                if (item == null) {
                    plugin.getLogger().warning("[CrateManager] Nieznany item '" + itemStr
                            + "' w skrzynce '" + crateId + "' — pomijam.");
                    continue;
                }

                result.add(new CrateReward(item, rewardRarity, weight, name));

            } catch (Exception e) {
                plugin.getLogger().warning("[CrateManager] Blad parsowania nagrody w '"
                        + crateId + "': " + e.getMessage());
            }
        }
        return result;
    }

    private ItemStack parseItem(String itemStr, int amount) {
        if (itemStr == null) return null;
        String lower = itemStr.toLowerCase().trim();

        // custom:<id>
        if (lower.startsWith("custom:")) {
            return CustomItemRegistry.get(lower.substring(7));
        }

        // spawner:<ENTITY>
        if (lower.startsWith("spawner:")) {
            String entityName  = itemStr.substring(8).toUpperCase();
            String registryKey = "spawner_" + entityName.toLowerCase();
            ItemStack fromReg  = CustomItemRegistry.get(registryKey);
            if (fromReg != null) return fromReg;
            try {
                return CustomItemRegistry.spawner(
                        org.bukkit.entity.EntityType.valueOf(entityName));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        // MATERIAL
        try {
            Material mat = Material.valueOf(itemStr.toUpperCase());
            return new ItemStack(mat, amount);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // =========================================================================
    // Rejestracja skrzynek jako custom itemów
    // =========================================================================

    private void registerCrateItems() {
        for (CrateDefinition crate : crates.values()) {
            String itemId = "crate_" + crate.getId().toLowerCase();
            if (CustomItemRegistry.isRegistered(itemId)) continue;

            CustomItemRegistry.register(itemId,
                    CustomItemRegistry.customItem(
                            new stark.skyBlockTest2.gui.builder.ItemBuilder(Material.CHEST)
                                    .name(crate.getRarity().color + crate.getDisplayName())
                                    .lore(
                                            crate.getRarity().displayName,
                                            " ",
                                            "§7Uzyj prawym przyciskiem aby otworzyc!"
                                    )
                                    .glow(true)
                                    .setString("action_on_use", "open_crate")
                                    .setString("crate_id", crate.getId())
                    ));
        }
    }

    // =========================================================================
    // Reload
    // =========================================================================

    public void reload() {
        crates.clear();
        loadCrates();
    }

    // =========================================================================
    // API
    // =========================================================================

    public CrateDefinition getCrate(String id) {
        return id != null ? crates.get(id.toLowerCase()) : null;
    }

    public Map<String, CrateDefinition> getAllCrates() {
        return Collections.unmodifiableMap(crates);
    }
}
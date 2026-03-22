package stark.skyBlockTest2.shop;

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

public class ShopManager {

    private final SkyBlockTest2         plugin;
    private final List<ShopCategory>    categories = new ArrayList<>();
    private final Map<String, ShopItem> itemsById  = new LinkedHashMap<>();

    // -------------------------------------------------------------------------
    // Przeceny na KUPNO (%)
    // -------------------------------------------------------------------------
    private double              globalDiscount     = 0.0;
    private Map<String, Double> categoryDiscounts  = new HashMap<>(); // categoryId → %
    private Map<String, Double> itemDiscounts      = new HashMap<>(); // itemId → %

    // -------------------------------------------------------------------------
    // Bonusy na SPRZEDAŻ (%)
    // -------------------------------------------------------------------------
    private double              globalSellBonus       = 0.0;
    private Map<String, Double> categorySellBonuses   = new HashMap<>(); // categoryId → %
    private Map<String, Double> itemSellBonuses       = new HashMap<>(); // itemId → %

    public ShopManager(SkyBlockTest2 plugin) {
        this.plugin = plugin;
        reload();
    }

    // =========================================================================
    // Ładowanie
    // =========================================================================

    public void reload() {
        categories.clear();
        itemsById.clear();
        categoryDiscounts.clear();
        itemDiscounts.clear();
        categorySellBonuses.clear();
        itemSellBonuses.clear();

        File file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) plugin.saveResource("shop.yml", false);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        loadDiscounts(cfg);
        loadSellBonuses(cfg);
        loadCategories(cfg);

        plugin.getLogger().info("[ShopManager] Załadowano " + categories.size()
                + " kategorii, " + itemsById.size() + " przedmiotów."
                + " Przecena globalna: " + (int) globalDiscount + "%"
                + " | Bonus sprzedaży globalny: " + (int) globalSellBonus + "%");
    }

    private void loadDiscounts(FileConfiguration cfg) {
        globalDiscount = cfg.getDouble("discounts.global", 0.0);

        ConfigurationSection cats = cfg.getConfigurationSection("discounts.categories");
        if (cats != null) {
            for (String id : cats.getKeys(false)) {
                categoryDiscounts.put(id.toLowerCase(), cats.getDouble(id));
            }
        }

        ConfigurationSection items = cfg.getConfigurationSection("discounts.items");
        if (items != null) {
            for (String id : items.getKeys(false)) {
                itemDiscounts.put(id.toLowerCase(), items.getDouble(id));
            }
        }
    }

    private void loadSellBonuses(FileConfiguration cfg) {
        globalSellBonus = cfg.getDouble("sell-bonuses.global", 0.0);

        ConfigurationSection cats = cfg.getConfigurationSection("sell-bonuses.categories");
        if (cats != null) {
            for (String id : cats.getKeys(false)) {
                categorySellBonuses.put(id.toLowerCase(), cats.getDouble(id));
            }
        }

        ConfigurationSection items = cfg.getConfigurationSection("sell-bonuses.items");
        if (items != null) {
            for (String id : items.getKeys(false)) {
                itemSellBonuses.put(id.toLowerCase(), items.getDouble(id));
            }
        }
    }

    private void loadCategories(FileConfiguration cfg) {
        ConfigurationSection catSection = cfg.getConfigurationSection("categories");
        if (catSection == null) {
            plugin.getLogger().warning("[ShopManager] Brak sekcji 'categories' w shop.yml!");
            return;
        }

        for (String catId : catSection.getKeys(false)) {
            try {
                String catPath     = "categories." + catId;
                String displayName = cfg.getString(catPath + ".display-name", catId);
                Material icon      = parseMaterial(cfg.getString(catPath + ".icon", "CHEST"), catId);

                List<ShopItem> items = new ArrayList<>();
                ConfigurationSection itemsSection = cfg.getConfigurationSection(catPath + ".items");
                if (itemsSection != null) {
                    for (String itemId : itemsSection.getKeys(false)) {
                        try {
                            ShopItem shopItem = loadItem(cfg, catPath + ".items." + itemId,
                                    itemId, catId);
                            if (shopItem != null) {
                                items.add(shopItem);
                                itemsById.put(itemId.toLowerCase(), shopItem);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING,
                                    "[ShopManager] Błąd ładowania itemu '" + itemId + "'", e);
                        }
                    }
                }

                categories.add(new ShopCategory(catId, displayName, icon, items));

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[ShopManager] Błąd ładowania kategorii '" + catId + "'", e);
            }
        }
    }

    private ShopItem loadItem(FileConfiguration cfg, String path,
                              String itemId, String catId) {
        String itemStr   = cfg.getString(path + ".item", itemId);
        double buyPrice  = cfg.getDouble(path + ".buy-price", 0.0);
        double sellPrice = cfg.getDouble(path + ".sell-price", 0.0);
        int    amount    = cfg.getInt(path + ".amount", 1);

        ItemStack item = parseItem(itemStr, amount);
        if (item == null) {
            plugin.getLogger().warning("[ShopManager] Nieznany item '" + itemStr
                    + "' w kategorii '" + catId + "' — pomijam.");
            return null;
        }

        return new ShopItem(itemId, item, buyPrice, sellPrice, catId);
    }

    // =========================================================================
    // Obliczanie cen
    // =========================================================================

    /**
     * Finalna cena zakupu — stosuje przecenę z sekcji discounts.
     * Priorytet: item > category > global
     */
    public double getFinalBuyPrice(ShopItem item) {
        double discount = getEffectiveDiscount(item);
        return applyPercent(item.getBuyPrice(), discount);
    }

    /**
     * Finalna cena sprzedaży — stosuje bonus z sekcji sell-bonuses.
     * Priorytet: item > category > global
     */
    public double getFinalSellPrice(ShopItem item) {
        double bonus = getEffectiveSellBonus(item);
        return applyBonus(item.getSellPrice(), bonus);
    }

    /**
     * Aktywna przecena na kupno dla danego przedmiotu.
     */
    public double getEffectiveDiscount(ShopItem item) {
        String itemKey = item.getId().toLowerCase();
        if (itemDiscounts.containsKey(itemKey)) return itemDiscounts.get(itemKey);

        String catKey = item.getCategoryId().toLowerCase();
        if (categoryDiscounts.containsKey(catKey)) return categoryDiscounts.get(catKey);

        return globalDiscount;
    }

    /**
     * Aktywny bonus sprzedaży dla danego przedmiotu.
     */
    public double getEffectiveSellBonus(ShopItem item) {
        String itemKey = item.getId().toLowerCase();
        if (itemSellBonuses.containsKey(itemKey)) return itemSellBonuses.get(itemKey);

        String catKey = item.getCategoryId().toLowerCase();
        if (categorySellBonuses.containsKey(catKey)) return categorySellBonuses.get(catKey);

        return globalSellBonus;
    }

    public boolean hasDiscount(ShopItem item)   { return getEffectiveDiscount(item) > 0; }
    public boolean hasSellBonus(ShopItem item)  { return getEffectiveSellBonus(item) > 0; }

    /** Obniża cenę o podany procent. */
    private double applyPercent(double price, double discountPercent) {
        if (discountPercent <= 0 || price <= 0) return price;
        return Math.max(0, price * (1.0 - discountPercent / 100.0));
    }

    /** Podwyższa cenę o podany procent. */
    private double applyBonus(double price, double bonusPercent) {
        if (bonusPercent <= 0 || price <= 0) return price;
        return price * (1.0 + bonusPercent / 100.0);
    }

    // =========================================================================
    // Dynamiczne ustawianie przecen w runtime (np. event weekendowy)
    // =========================================================================

    public void setGlobalDiscount(double percent)  { this.globalDiscount = percent; }
    public void setGlobalSellBonus(double percent) { this.globalSellBonus = percent; }
    public double getGlobalDiscount()              { return globalDiscount; }
    public double getGlobalSellBonus()             { return globalSellBonus; }

    // =========================================================================
    // Parsowanie
    // =========================================================================

    private ItemStack parseItem(String itemStr, int amount) {
        if (itemStr == null) return null;
        String lower = itemStr.toLowerCase().trim();

        if (lower.startsWith("custom:")) {
            return CustomItemRegistry.get(lower.substring(7));
        }

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

        try {
            Material mat = Material.valueOf(itemStr.toUpperCase());
            return new ItemStack(mat, amount);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Material parseMaterial(String name, String context) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[ShopManager] Nieznany materiał '" + name
                    + "' w: " + context);
            return Material.CHEST;
        }
    }

    /** Aktywna przecena na kupno dla całej kategorii (bez konkretnego przedmiotu). */
    public double getDiscountForCategory(String categoryId) {
        String key = categoryId.toLowerCase();
        if (categoryDiscounts.containsKey(key)) return categoryDiscounts.get(key);
        return globalDiscount;
    }

    /** Aktywny bonus sprzedaży dla całej kategorii. */
    public double getSellBonusForCategory(String categoryId) {
        String key = categoryId.toLowerCase();
        if (categorySellBonuses.containsKey(key)) return categorySellBonuses.get(key);
        return globalSellBonus;
    }

    // =========================================================================
    // Gettery
    // =========================================================================

    public List<ShopCategory> getCategories()        { return Collections.unmodifiableList(categories); }
    public ShopItem           getItemById(String id) { return itemsById.get(id.toLowerCase()); }

    public ShopCategory getCategoryById(String id) {
        return categories.stream()
                .filter(c -> c.getId().equalsIgnoreCase(id))
                .findFirst().orElse(null);
    }
}
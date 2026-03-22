package stark.skyBlockTest2.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataType;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.gui.builder.ItemBuilder;

import java.util.*;

public class CustomItemRegistry {

    public static final String PDC_KEY       = "custom_item_id";
    public static final String WORLD_OVERWORLD = "world_skyblock";
    public static final String WORLD_NETHER    = "world_skyblock_nether";

    private static final Map<String, ItemStack> registry = new LinkedHashMap<>();

    // =========================================================================
    // Rejestracja i pobieranie
    // =========================================================================

    public static void register(String id, ItemStack item) {
        registry.put(id.toLowerCase(), tag(item, id));
    }

    public static ItemStack get(String id) {
        ItemStack base = registry.get(id.toLowerCase());
        return base != null ? base.clone() : null;
    }

    public static boolean isRegistered(String id) {
        return registry.containsKey(id.toLowerCase());
    }

    public static String getId(ItemStack item) {
        if (item == null) return null;
        var meta = item.getItemMeta();
        if (meta == null) return null;
        NamespacedKey key = new NamespacedKey(SkyBlockTest2.getInstance(), PDC_KEY);
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static boolean isCustom(ItemStack item) {
        return getId(item) != null;
    }

    public static Collection<String> getAllIds() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    // =========================================================================
    // Wymagany świat — jedna metoda używana wszędzie
    // =========================================================================

    /**
     * Zwraca nazwę świata w którym można postawić dany spawner.
     * Spawnery netherowe → world_skyblock_nether, pozostałe → world_skyblock.
     */
    public static String getRequiredWorld(String customId) {
        if (customId.contains("blaze") || customId.contains("magma_cube")
                || customId.contains("ghast") || customId.contains("wither_skeleton")) {
            return WORLD_NETHER;
        }
        return WORLD_OVERWORLD;
    }

    /** Czytelna etykieta świata do wyświetlenia graczowi. */
    public static String getWorldLabel(String requiredWorld) {
        return requiredWorld.equals(WORLD_NETHER) ? "Wyspa Nether" : "Wyspa Overworld";
    }

    // =========================================================================
    // Fabryki itemów
    // =========================================================================

    public static ItemStack spawner(EntityType entityType) {
        String mobName    = formatName(entityType.name());
        String spawnerId  = "spawner_" + entityType.name().toLowerCase();
        String world      = getRequiredWorld(spawnerId);
        String worldLabel = getWorldLabel(world);

        ItemStack item = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return item;

        CreatureSpawner spawner = (CreatureSpawner) meta.getBlockState();
        spawner.setSpawnedType(entityType);
        meta.setBlockState(spawner);

        meta.setDisplayName("§6Spawner: §f" + mobName);
        meta.setLore(List.of(
                " ",
                "§7Mozna postawic tylko na:",
                "§e" + worldLabel
        ));

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack enchantedBook(String displayName, Map<Enchantment, Integer> enchants) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(displayName);

        enchants.forEach((ench, lvl) -> meta.addStoredEnchant(ench, lvl, true));

        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_STORED_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack customItem(ItemBuilder builder) {
        return builder.build();
    }

    // =========================================================================
    // Rejestracja wszystkich itemów
    // =========================================================================

    public static void registerAll() {

        // ---- Spawnery ----
        register("spawner_blaze",       spawner(EntityType.BLAZE));
        register("spawner_zombie",      spawner(EntityType.ZOMBIE));
        register("spawner_skeleton",    spawner(EntityType.SKELETON));
        register("spawner_spider",      spawner(EntityType.SPIDER));
        register("spawner_cave_spider", spawner(EntityType.CAVE_SPIDER));
        register("spawner_creeper",     spawner(EntityType.CREEPER));
        register("spawner_enderman",    spawner(EntityType.ENDERMAN));
        register("spawner_witch",       spawner(EntityType.WITCH));
        register("spawner_guardian",    spawner(EntityType.GUARDIAN));
        register("spawner_magma_cube",  spawner(EntityType.MAGMA_CUBE));

        // ---- Enchanted books ----
        register("book_sharpness_5",  enchantedBook("§bKsiazka Ostrza V",        Map.of(Enchantment.SHARPNESS, 5)));
        register("book_protection_4", enchantedBook("§bKsiazka Ochrony IV",       Map.of(Enchantment.PROTECTION, 4)));
        register("book_efficiency_5", enchantedBook("§bKsiazka Wydajnosci V",     Map.of(Enchantment.EFFICIENCY, 5)));
        register("book_fortune_3",    enchantedBook("§bKsiazka Fortuny III",      Map.of(Enchantment.FORTUNE, 3)));
        register("book_silk_touch",   enchantedBook("§bKsiazka Jedwabnego Dotyku",Map.of(Enchantment.SILK_TOUCH, 1)));
        register("book_looting_3",    enchantedBook("§bKsiazka Lupiestwa III",    Map.of(Enchantment.LOOTING, 3)));

        // ---- Custom itemy z akcją ----
        register("island_key", customItem(
                new ItemBuilder(Material.TRIPWIRE_HOOK)
                        .name("§6Klucz Wyspy")
                        .lore("§7Tajemniczy klucz...", "§8Kto wie do czego sluzy?")
                        .glow(true)
                        .setString("action_on_use", "island_key_use")
        ));
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    private static ItemStack tag(ItemStack item, String id) {
        ItemStack clone = item.clone();
        var meta = clone.getItemMeta();
        if (meta == null) return clone;
        NamespacedKey key = new NamespacedKey(SkyBlockTest2.getInstance(), PDC_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id.toLowerCase());
        clone.setItemMeta(meta);
        return clone;
    }

    private static String formatName(String enumName) {
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty())
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String formatEnchantName(Enchantment ench) {
        return formatName(ench.getKey().getKey());
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III";
            case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(n);
        };
    }
}
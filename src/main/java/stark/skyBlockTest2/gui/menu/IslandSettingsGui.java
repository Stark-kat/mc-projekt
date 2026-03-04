package stark.skyBlockTest2.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.listener.IslandProtectionListener;
import stark.skyBlockTest2.island.listener.IslandProtectionListener.IslandAction;
import java.util.Arrays;
import java.util.List;



public class IslandSettingsGui {

    private final IslandManager islandManager;

    public IslandSettingsGui(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void open(Player player, IslandProtectionListener.ActionCategory selectedCategory) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) return;

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 54, "§8Ustawienia: " + selectedCategory.getDisplayName());
        GuiBuilder builder = new GuiBuilder(gui);

        // Tło
        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        // --- PASEK KATEGORII (Sloty: 1, 10, 19, 28, 37, 46) ---
        int[] catSlots = {0, 8, 18, 26, 36, 44};
        IslandProtectionListener.ActionCategory[] allCats = IslandProtectionListener.ActionCategory.values();
        for (int i = 0; i < allCats.length; i++) {
            IslandProtectionListener.ActionCategory cat = allCats[i];
            boolean active = (cat == selectedCategory);

            builder.set(catSlots[i], new ItemBuilder(cat.getIcon())
                    .name((active ? "§b➔ " : "§7") + cat.getDisplayName())
                    .lore(active ? "§fJesteś tutaj" : "§7Kliknij, aby przejść")
                    .glow(active)
                    .hideAll()
                    .setString("action", "ChangeCategory")
                    .setString("category_name", cat.name())
                    .build());
        }

        // --- FILTROWANIE I RENDEROWANIE AKCJI ---
        List<IslandAction> actions = Arrays.stream(IslandAction.values())
                .filter(a -> a.getCategory() == selectedCategory && a.isEditable())
                .toList();

        // Układ: Kolumna 3&4 oraz 6&7
        int[] iconSlots = {2, 5, 11, 14, 20, 23, 29, 32, 38, 41};

        for (int i = 0; i < actions.size() && i < iconSlots.length; i++) {
            IslandAction action = actions.get(i);
            int slot = iconSlots[i];

            boolean allowed = island.canVisitorDo(action);

            // Ikona (Slot 12, 15...)
            builder.set(slot, new ItemBuilder(getMaterialFor(action))
                    .name("§e" + action.getDisplayName())
                    .lore("§7Zarządzaj dostępem dla gości.")
                    .build());

            // Barwnik (Slot 13, 16...)
            builder.set(slot + 1, new ItemBuilder(allowed ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name(allowed ? "§aWłączone" : "§cWyłączone")
                    .lore("§7Kliknij, aby przełączyć!")
                    .setString("action", "ToggleIslandSetting")
                    .setString("island_action", action.name())
                    .setString("current_category", selectedCategory.name()) // KLUCZ DO PAMIĘCI
                    .build());
        }

        builder.set(49, new ItemBuilder(Material.ARROW).name("§cCofnij").setString("action", "MenuGui").build());
        player.openInventory(gui);
    }

    private Material getMaterialFor(IslandAction action) {
        return switch (action) {
            // --- OGÓLNE ---
            case BREAK_BLOCKS -> Material.DIAMOND_PICKAXE;
            case PLACE_BLOCKS -> Material.GRASS_BLOCK;
            case USE_BUCKETS -> Material.WATER_BUCKET;
            case FIRE_SPREAD -> Material.FLINT_AND_STEEL;
            case TELEPORT_VISIT -> Material.ENDER_PEARL;

            // --- BLOKI UŻYTKOWE (UTILITY) ---
            case OPEN_CONTAINERS -> Material.CHEST;
            case USE_CRAFTING -> Material.CRAFTING_TABLE;
            case USE_ANVIL -> Material.ANVIL;
            case USE_ENCHANTING -> Material.ENCHANTING_TABLE;
            case USE_BREWING -> Material.BREWING_STAND;

            // --- MECHANIZMY (REDSTONE) ---
            case USE_DOORS -> Material.OAK_DOOR;
            case USE_BUTTONS -> Material.STONE_BUTTON;
            case USE_PRESSURE_PLATES -> Material.OAK_PRESSURE_PLATE;

            // --- ISTOTY (MOBS) ---
            case FEED_ANIMALS -> Material.WHEAT;
            case MILK_COWS -> Material.MILK_BUCKET;
            case SHEAR_SHEEP -> Material.SHEARS;
            case KILL_ANIMALS -> Material.IRON_SWORD;
            case VILLAGER_TRADE -> Material.EMERALD;
            default -> Material.PAPER;
        };
    }
}
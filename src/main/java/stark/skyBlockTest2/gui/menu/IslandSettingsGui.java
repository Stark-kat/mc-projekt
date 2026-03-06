package stark.skyBlockTest2.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.island.ActionCategory;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandAction;
import stark.skyBlockTest2.island.IslandManager;

import java.util.Arrays;
import java.util.List;

public class IslandSettingsGui {

    private final IslandManager islandManager;

    public IslandSettingsGui(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void open(Player player, ActionCategory selectedCategory) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) return;

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 54, "§8Ustawienia: " + selectedCategory.getDisplayName());
        GuiBuilder builder = new GuiBuilder(gui);

        // Tło
        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        // --- PASEK KATEGORII ---
        int[] catSlots = {0, 8, 18, 26, 36, 44};
        ActionCategory[] allCats = ActionCategory.values();
        for (int i = 0; i < allCats.length; i++) {
            ActionCategory cat = allCats[i];
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

        // --- AKCJE FILTROWANE PO KATEGORII ---
        List<IslandAction> actions = Arrays.stream(IslandAction.values())
                .filter(a -> a.getCategory() == selectedCategory && a.isEditable())
                .toList();

        // Pary: ikona + toggle, w kolumnach 3&4 oraz 6&7
        int[] iconSlots = {2, 5, 11, 14, 20, 23, 29, 32, 38, 41};

        for (int i = 0; i < actions.size() && i < iconSlots.length; i++) {
            IslandAction action = actions.get(i);
            int slot = iconSlots[i];
            boolean allowed = island.canVisitorDo(action);

            // Ikona akcji
            builder.set(slot, new ItemBuilder(getMaterialFor(action))
                    .name("§e" + action.getDisplayName())
                    .lore("§7Zarządzaj dostępem dla gości.")
                    .build());

            // Przycisk toggle
            builder.set(slot + 1, new ItemBuilder(allowed ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name(allowed ? "§aWłączone" : "§cWyłączone")
                    .lore("§7Kliknij, aby przełączyć!")
                    .setString("action", "ToggleIslandSetting")
                    .setString("island_action", action.name())
                    .setString("current_category", selectedCategory.name())
                    .build());
        }

        builder.set(49, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "MenuGui")
                .build());

        player.openInventory(gui);
    }

    private Material getMaterialFor(IslandAction action) {
        return switch (action) {
            case TELEPORT_VISIT       -> Material.ENDER_PEARL;
            case PICKUP_ITEMS         -> Material.HOPPER;
            case DROP_ITEMS           -> Material.DROPPER;
            case USE_PORTALS          -> Material.OBSIDIAN;
            case BREAK_BLOCKS         -> Material.DIAMOND_PICKAXE;
            case PLACE_BLOCKS         -> Material.GRASS_BLOCK;
            case USE_BUCKETS          -> Material.WATER_BUCKET;
            case FIRE_SPREAD          -> Material.FLINT_AND_STEEL;
            case CROP_TRAMPLE         -> Material.WHEAT;
            case OPEN_CONTAINERS      -> Material.CHEST;
            case USE_CRAFTING         -> Material.CRAFTING_TABLE;
            case USE_ANVIL            -> Material.ANVIL;
            case USE_ENCHANTING       -> Material.ENCHANTING_TABLE;
            case USE_BREWING          -> Material.BREWING_STAND;
            case INTERACT_DECORATIONS -> Material.FLOWER_POT;
            case INTERACT_UTILITY     -> Material.ITEM_FRAME;
            case ARMOR_STAND_INTERACT -> Material.ARMOR_STAND;
            case USE_DOORS            -> Material.OAK_DOOR;
            case USE_BUTTONS          -> Material.STONE_BUTTON;
            case USE_PRESSURE_PLATES  -> Material.OAK_PRESSURE_PLATE;
            case INTERACT_ANIMALS     -> Material.WHEAT_SEEDS;
            case MILK_COWS            -> Material.MILK_BUCKET;
            case SHEAR_SHEEP          -> Material.SHEARS;
            case KILL_ANIMALS         -> Material.IRON_SWORD;
            case VILLAGER_TRADE       -> Material.EMERALD;
        };
    }
}
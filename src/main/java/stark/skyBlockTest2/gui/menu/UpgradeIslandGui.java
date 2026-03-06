package stark.skyBlockTest2.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.economy.EconomyManager;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;

public class UpgradeIslandGui {

    private final IslandManager islandManager;
    private final EconomyManager economyManager;

    // Rozmiary wyspy per poziom (musi zgadzać się z islandSizes w IslandManager)
    private static final int[] SIZES = {0, 1, 2, 3, 4};

    // Sloty dla poziomów 1-5
    private static final int[] LEVEL_SLOTS = {19, 21, 23, 25, 27};

    public UpgradeIslandGui(IslandManager islandManager, EconomyManager economyManager) {
        this.islandManager = islandManager;
        this.economyManager = economyManager;
    }

    public void open(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage("§cNie masz wyspy!");
            return;
        }

        int currentLevel = getCurrentLevel(island.getSize());

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 54, "§8Ulepszanie wyspy");
        GuiBuilder builder = new GuiBuilder(gui);

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        // Info o aktualnym poziomie — slot 4 (środek górnego rzędu)
        builder.set(4, new ItemBuilder(Material.NETHER_STAR)
                .name("§e§lTwoja wyspa")
                .lore(
                        "§7Aktualny poziom: §b" + currentLevel,
                        "§7Rozmiar: §f" + getSizeLabel(island.getSize()),
                        " ",
                        "§7Kliknij poziom aby go zakupić."
                )
                .build());

        // Renderujemy 5 poziomów
        for (int lvl = 1; lvl <= 5; lvl++) {
            int slot = LEVEL_SLOTS[lvl - 1];
            int size = SIZES[lvl - 1];
            boolean owned  = lvl <= currentLevel;
            boolean isNext = lvl == currentLevel + 1;

            if (owned) {
                // Poziom już posiadany
                builder.set(slot, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                        .name("§a§lPoziom " + lvl)
                        .lore(
                                "§7Rozmiar: §f" + getSizeLabel(size),
                                " ",
                                "§a✔ Posiadasz ten poziom"
                        )
                        .glow(lvl == currentLevel) // Podświetlamy aktualny
                        .build());

            } else if (isNext) {
                // Następny dostępny poziom
                double cost      = islandManager.getUpgradeCost(lvl);
                double balance   = economyManager.getBalance(player);
                boolean canAfford = economyManager.has(player, cost);

                builder.set(slot, new ItemBuilder(canAfford ? Material.GOLD_BLOCK : Material.REDSTONE_BLOCK)
                        .name((canAfford ? "§6§l" : "§c§l") + "Poziom " + lvl)
                        .lore(
                                "§7Rozmiar: §f" + getSizeLabel(size),
                                " ",
                                "§7Koszt: §e" + economyManager.format(cost),
                                "§7Twój balans: §f" + economyManager.format(balance),
                                " ",
                                canAfford ? "§aKliknij, aby kupić!" : "§cNiewystarczające środki!"
                        )
                        .setString("action", "UpgradeToLevel")
                        .setString("target_level", String.valueOf(lvl))
                        .build());

            } else {
                // Zablokowane — jeszcze niedostępne
                double cost = islandManager.getUpgradeCost(lvl);

                builder.set(slot, new ItemBuilder(Material.BARRIER)
                        .name("§7§lPoziom " + lvl + " §8(Zablokowany)")
                        .lore(
                                "§7Rozmiar: §f" + getSizeLabel(size),
                                " ",
                                "§7Koszt: §8" + economyManager.format(cost),
                                " ",
                                "§8Odblokuj poprzednie poziomy."
                        )
                        .build());
            }
        }

        builder.set(49, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "MenuGui")
                .build());

        player.openInventory(gui);
    }

    private int getCurrentLevel(int size) {
        for (int i = 0; i < SIZES.length; i++) {
            if (SIZES[i] == size) return i + 1;
        }
        return 1;
    }

    private String getSizeLabel(int size) {
        int blocks = (size * 2 + 1) * 16;
        return blocks + "x" + blocks + " bloków";
    }
}
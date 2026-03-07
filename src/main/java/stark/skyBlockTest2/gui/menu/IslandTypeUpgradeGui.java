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
import stark.skyBlockTest2.island.IslandType;

public class IslandTypeUpgradeGui {

    private final IslandManager islandManager;
    private final EconomyManager economyManager;

    private static final int[] SIZES      = {0, 1, 2, 3, 4};
    private static final int[] LEVEL_SLOTS = {18, 20, 22, 24, 26};

    public IslandTypeUpgradeGui(IslandManager islandManager, EconomyManager economyManager) {
        this.islandManager  = islandManager;
        this.economyManager = economyManager;
    }

    public void open(Player player, IslandType type) {
        Island island = islandManager.getIsland(player.getUniqueId(), type);
        if (island == null) {
            player.sendMessage("§cNie masz " + type.displayName + "!");
            return;
        }

        int currentLevel = islandManager.getCurrentLevel(player.getUniqueId(), type);

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 45, "§8Ulepszanie: " + type.displayName);
        GuiBuilder builder = new GuiBuilder(gui);
        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        builder.set(4, new ItemBuilder(type.icon)
                .name("§e§l" + type.displayName)
                .lore(
                        "§7Aktualny poziom: §b" + currentLevel,
                        "§7Rozmiar: §f" + getSizeLabel(island.getSize()),
                        " ",
                        "§7Kliknij poziom aby go zakupić."
                )
                .build());

        for (int lvl = 1; lvl <= 5; lvl++) {
            int slot  = LEVEL_SLOTS[lvl - 1];
            int size  = SIZES[lvl - 1];
            boolean owned  = lvl <= currentLevel;
            boolean isNext = lvl == currentLevel + 1;

            if (owned) {
                builder.set(slot, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                        .name("§a§lPoziom " + lvl)
                        .lore(
                                "§7Rozmiar: §f" + getSizeLabel(size),
                                " ",
                                "§a✔ Posiadasz ten poziom"
                        )
                        .glow(lvl == currentLevel)
                        .build());

            } else if (isNext) {
                double cost      = islandManager.getUpgradeCost(type, lvl);
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
                        .setString("action", "IslandTypeUpgradeToLevel")
                        .setString("island_type", type.name())
                        .setString("target_level", String.valueOf(lvl))
                        .build());

            } else {
                double cost = islandManager.getUpgradeCost(type, lvl);
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

        builder.set(40, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "IslandTypeOpenSettings")
                .setString("island_type", type.name())
                .build());

        player.openInventory(gui);
    }

    private String getSizeLabel(int size) {
        int blocks = (size * 2 + 1) * 16;
        return blocks + "x" + blocks + " bloków";
    }
}

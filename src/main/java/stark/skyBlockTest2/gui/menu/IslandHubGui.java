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

public class IslandHubGui {

    private final IslandManager islandManager;
    private final EconomyManager economyManager;

    // Sloty dla kolejnych typów wysp (OVERWORLD=11, NETHER=15, ...)
    private static final int[] TYPE_SLOTS = {11, 15};

    public IslandHubGui(IslandManager islandManager, EconomyManager economyManager) {
        this.islandManager  = islandManager;
        this.economyManager = economyManager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(new MenuHolder(), 27, "§8Moje Wyspy");
        GuiBuilder builder = new GuiBuilder(gui);
        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        IslandType[] types = IslandType.values();
        for (int i = 0; i < types.length && i < TYPE_SLOTS.length; i++) {
            IslandType type = types[i];
            int slot = TYPE_SLOTS[i];

            if (islandManager.hasIsland(player.getUniqueId(), type)) {
                Island island = islandManager.getIsland(player.getUniqueId(), type);
                int level = islandManager.getCurrentLevel(player.getUniqueId(), type);
                int size  = island.getSize();
                String sizeLabel = ((size * 2 + 1) * 16) + "x" + ((size * 2 + 1) * 16);

                builder.set(slot, new ItemBuilder(type.icon)
                        .name("§a§l" + type.displayName)
                        .lore(
                                "§7Poziom: §e" + level,
                                "§7Rozmiar: §f" + sizeLabel + " bloków",
                                " ",
                                "§aKliknij, aby zarządzać"
                        )
                        .glow(true)
                        .setString("action", "HubOpenSettings")
                        .setString("island_type", type.name())
                        .build());

            } else if (type != IslandType.OVERWORLD) {
                double cost      = islandManager.getPurchaseCost(type);
                boolean canAfford = economyManager.isAvailable() && economyManager.has(player, cost);

                builder.set(slot, new ItemBuilder(canAfford ? type.icon : Material.BARRIER)
                        .name((canAfford ? "§6§l" : "§c§l") + type.displayName)
                        .lore(
                                "§7Odblokuj §f" + type.displayName + "§7!",
                                " ",
                                "§7Koszt: §e" + economyManager.format(cost),
                                " ",
                                canAfford ? "§aKliknij, aby kupić!" : "§cNiewystarczające środki!"
                        )
                        .setString("action", "HubBuyIsland")
                        .setString("island_type", type.name())
                        .build());
            }
        }

        builder.set(22, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "MenuGui")
                .build());

        player.openInventory(gui);
    }
}

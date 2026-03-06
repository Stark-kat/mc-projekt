package stark.skyBlockTest2.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.IslandType;

public class IslandTypeSettingsGui {

    private final IslandManager islandManager;

    public IslandTypeSettingsGui(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void open(Player player, IslandType type) {
        Island island = islandManager.getIsland(player.getUniqueId(), type);
        if (island == null) return;

        int level = islandManager.getCurrentLevel(player.getUniqueId(), type);
        int size  = island.getSize();
        String sizeLabel = ((size * 2 + 1) * 16) + "x" + ((size * 2 + 1) * 16);

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 27, "§8" + type.displayName);
        GuiBuilder builder = new GuiBuilder(gui);
        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        // Info o wyspie
        builder.set(11, new ItemBuilder(type.icon)
                .name("§a§l" + type.displayName)
                .lore(
                        "§7Poziom: §e" + level,
                        "§7Rozmiar: §f" + sizeLabel + " bloków"
                )
                .glow(true)
                .build());

        // Teleport home
        builder.set(13, new ItemBuilder(Material.ENDER_PEARL)
                .name("§bTeleportuj się")
                .lore("§7Teleportuj się na swoją wyspę")
                .setString("action", "IslandTypeTeleportHome")
                .setString("island_type", type.name())
                .build());

        // Ulepszanie
        builder.set(15, new ItemBuilder(Material.DIAMOND_PICKAXE)
                .name("§eUlepszanie")
                .lore("§7Kliknij, aby ulepszyć wyspę")
                .setString("action", "IslandTypeOpenUpgrade")
                .setString("island_type", type.name())
                .build());

        // Cofnij do hubu
        builder.set(22, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "OpenIslandHub")
                .build());

        player.openInventory(gui);
    }
}

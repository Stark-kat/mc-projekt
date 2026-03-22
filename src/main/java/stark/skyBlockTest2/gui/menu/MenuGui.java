package stark.skyBlockTest2.gui.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.item.GuiItems;

public class MenuGui {

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(), 54, Component.text("Menu"));

        new GuiBuilder(inventory)
                .set(10, new ItemBuilder(Material.GRASS_BLOCK)
                        .name("§a§lMoje Wyspy")
                        .lore("§7Zarządzaj swoimi wyspami,", "§7teleportuj się i ulepszaj!")
                        .setString("action", "OpenIslandHub")
                        .build())
                .set(12, GuiItems.members())
                .set(14, GuiItems.islandSettings())
                .set(16, GuiItems.quests())
                .set(20, new ItemBuilder(Material.EMERALD)
                        .name("§a§lSklep i Aukcje")
                        .lore("§7Sklep serwera i dom aukcyjny.")
                        .setString("action", "OpenShopHub")
                        .build())
                .set(22, new ItemBuilder(Material.CHISELED_BOOKSHELF)
                        .name("§a§lPoczta")
                        .lore("§7skrzynka odbiorcza")
                        .setString("action", "MailPage")
                        .build())
                .set(24, new ItemBuilder(Material.COMPARATOR)
                        .name("§a§lUstawienia")
                        .lore("§7Border, cząsteczki, TPA, MSG...")
                        .setString("action", "OpenPlayerSettings")
                        .build())
                .fill(GuiItems.grayBackground());

        player.openInventory(inventory);
    }
}
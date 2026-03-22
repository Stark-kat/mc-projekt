package stark.skyBlockTest2.shop.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.menu.MenuHolder;
import stark.skyBlockTest2.shop.ShopManager;

public class ShopHubGui {

    private final ShopManager shopManager;

    public ShopHubGui(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(), 27,
                Component.text("§6Sklep i Aukcje"));

        double globalDiscount = shopManager.getGlobalDiscount();
        boolean hasGlobalSale = globalDiscount > 0;

        new GuiBuilder(inv)
                .set(11, new ItemBuilder(Material.EMERALD)
                        .name("§a§lSklep Serwera")
                        .lore(
                                "§7Kupuj i sprzedawaj przedmioty",
                                "§7po cenach ustalonych przez serwer.",
                                " ",
                                hasGlobalSale
                                        ? "§c§lPRZECENA §e-" + (int) globalDiscount + "% §cNA WSZYSTKO!"
                                        : "§8Brak aktywnych przecen.",
                                " ",
                                "§eKliknij aby otworzyć »"
                        )
                        .glow(hasGlobalSale)
                        .setString("action", "OpenShopCategories")
                        .build())

                .set(15, new ItemBuilder(Material.GOLD_INGOT)
                        .name("§6§lDom Aukcyjny")
                        .lore(
                                "§7Kupuj i sprzedawaj przedmioty",
                                "§7od innych graczy.",
                                " ",
                                "§7Wystawiaj własne aukcje",
                                "§7lub licytuj cudze!",
                                " ",
                                "§eKliknij aby otworzyć »"
                        )
                        .setString("action", "OpenAuctions")
                        .build())

                .set(22, new ItemBuilder(Material.ARROW)
                        .name("§7Powrót")
                        .setString("action", "MenuGui")
                        .build())

                .fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        player.openInventory(inv);
    }
}
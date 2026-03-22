package stark.skyBlockTest2.shop.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.menu.MenuHolder;
import stark.skyBlockTest2.shop.ShopCategory;
import stark.skyBlockTest2.shop.ShopItem;
import stark.skyBlockTest2.shop.ShopManager;

import java.util.ArrayList;
import java.util.List;

public class ShopCategoryGui {

    private final ShopManager shopManager;

    public ShopCategoryGui(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void open(Player player) {
        List<ShopCategory> categories = shopManager.getCategories();

        int rows = Math.max(3, (int) Math.ceil((categories.size() + 9) / 9.0) + 1);
        rows = Math.min(rows, 6);
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(new MenuHolder(), size,
                Component.text("§6Sklep — Kategorie"));

        GuiBuilder builder = new GuiBuilder(inv);

        int[] contentSlots = buildContentSlots(size);
        int slotIndex = 0;

        for (ShopCategory cat : categories) {
            if (slotIndex >= contentSlots.length) break;

            double discount   = shopManager.getDiscountForCategory(cat.getId());
            double sellBonus  = shopManager.getSellBonusForCategory(cat.getId());
            boolean hasDiscount  = discount > 0;
            boolean hasSellBonus = sellBonus > 0;

            List<String> lore = new ArrayList<>();
            lore.add("§7Przedmiotów: §f" + cat.getItems().size());
            lore.add(" ");
            if (hasDiscount) {
                lore.add("§c§lPRZECENA §e-" + (int) discount + "%§7 na zakup");
            }
            if (hasSellBonus) {
                lore.add("§a§lBONUS §e+" + (int) sellBonus + "%§7 za sprzedaż");
            }
            if (hasDiscount || hasSellBonus) {
                lore.add(" ");
            }
            lore.add("§eKliknij aby otworzyć »");

            builder.set(contentSlots[slotIndex++],
                    new ItemBuilder(cat.getIcon())
                            .name("§6§l" + cat.getDisplayName())
                            .lore(lore.toArray(new String[0]))
                            .glow(hasDiscount || hasSellBonus)
                            .setString("action", "OpenShopItems")
                            .setString("category_id", cat.getId())
                            .build());
        }

        // Ostatni rząd — nawigacja
        int lastRow = size - 9;

        builder.set(lastRow + 4, new ItemBuilder(Material.ARROW)
                .name("§7Powrót")
                .setString("action", "OpenShopHub")
                .build());

        double totalValue = calculateTotalSellValue(player);
        builder.set(lastRow + 8, new ItemBuilder(totalValue > 0 ? Material.HOPPER : Material.GRAY_DYE)
                .name(totalValue > 0
                        ? "§a§lSprzedaj wszystko §7(§e" + formatPrice(totalValue) + "§7)"
                        : "§8Sprzedaj wszystko §7(nic do sprzedania)")
                .lore(
                        "§7Sprzedaje wszystkie przedmioty",
                        "§7z całego sklepu które masz w eq.",
                        " ",
                        totalValue > 0
                                ? "§7Łączna wartość: §e" + formatPrice(totalValue)
                                : "§8Brak przedmiotów do sprzedania"
                )
                .setString("action", "ShopSellAll")
                .build());

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        player.openInventory(inv);
    }

    // =========================================================================
    // Liczenie wartości wszystkiego, co gracz może sprzedać
    // =========================================================================

    private double calculateTotalSellValue(Player player) {
        double total = 0;
        for (ShopCategory cat : shopManager.getCategories()) {
            for (ShopItem shopItem : cat.getItems()) {
                if (!shopItem.canSell()) continue;
                double priceEach = shopManager.getFinalSellPrice(shopItem);
                Material mat = shopItem.getItem().getType();
                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem != null && invItem.getType() == mat) {
                        total += priceEach * invItem.getAmount();
                    }
                }
            }
        }
        return total;
    }

    private String formatPrice(double price) {
        return stark.skyBlockTest2.util.PriceFormat.format(price);
    }

    private int[] buildContentSlots(int size) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 9; i < size - 9; i++) slots.add(i);
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }
}
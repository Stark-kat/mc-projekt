package stark.skyBlockTest2.shop.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.economy.EconomyManager;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.menu.MenuHolder;
import stark.skyBlockTest2.quest.QuestManager;
import stark.skyBlockTest2.quest.QuestTrigger;
import stark.skyBlockTest2.shop.ShopCategory;
import stark.skyBlockTest2.shop.ShopItem;
import stark.skyBlockTest2.shop.ShopManager;

import java.util.ArrayList;
import java.util.List;

public class ShopItemsGui {

    private static final int   PAGE_SIZE = 28;
    private static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    private final SkyBlockTest2  plugin;
    private final ShopManager    shopManager;
    private final EconomyManager economy;
    private QuestManager questManager;

    public ShopItemsGui(SkyBlockTest2 plugin, ShopManager shopManager, EconomyManager economy) {
        this.plugin      = plugin;
        this.shopManager = shopManager;
        this.economy     = economy;
    }

    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void open(Player player, String categoryId, int page) {
        ShopCategory category = shopManager.getCategoryById(categoryId);
        if (category == null) {
            player.sendMessage("§cNieznana kategoria sklepu!");
            return;
        }

        List<ShopItem> items = category.getItems();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(new MenuHolder(), 54,
                Component.text("§6" + category.getDisplayName()
                        + (totalPages > 1 ? " §8(" + (page + 1) + "/" + totalPages + ")" : "")));

        GuiBuilder builder = new GuiBuilder(inv);

        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, items.size());

        for (int i = start; i < end; i++) {
            builder.set(CONTENT_SLOTS[i - start], buildShopItemStack(items.get(i), player));
        }

        if (page > 0) {
            builder.set(45, new ItemBuilder(Material.ARROW)
                    .name("§7« Poprzednia strona")
                    .setString("action", "ShopPage")
                    .setString("category_id", categoryId)
                    .setString("page", String.valueOf(page - 1))
                    .build());
        }

        if (page < totalPages - 1) {
            builder.set(53, new ItemBuilder(Material.ARROW)
                    .name("§7Następna strona »")
                    .setString("action", "ShopPage")
                    .setString("category_id", categoryId)
                    .setString("page", String.valueOf(page + 1))
                    .build());
        }

        builder.set(49, new ItemBuilder(Material.BARRIER)
                .name("§7Powrót do kategorii")
                .setString("action", "OpenShopCategories")
                .build());

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        player.openInventory(inv);
    }

    // =========================================================================
    // Sprzedaj wszystko z całego sklepu
    // =========================================================================

    public double sellAll(Player player) {
        double total = 0;
        int totalCount = 0;
        for (ShopCategory cat : shopManager.getCategories()) {
            for (ShopItem shopItem : cat.getItems()) {
                if (!shopItem.canSell()) continue;
                double priceEach = shopManager.getFinalSellPrice(shopItem);
                Material mat = shopItem.getItem().getType();

                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem == null || invItem.getType() != mat) continue;
                    totalCount += invItem.getAmount();
                    total += priceEach * invItem.getAmount();
                    invItem.setAmount(0);
                }
            }
        }

        if (total > 0) {
            economy.deposit(player, total);
            if (questManager != null) {
                questManager.addProgress(player, QuestTrigger.SELL_TO_SHOP, "TRANSACTION", totalCount);
                questManager.addProgress(player, QuestTrigger.SELL_TO_SHOP,
                        "VALUE", (int) Math.min(total, 1_000_000_000.0));
            }
        }
        return total;
    }

    // =========================================================================
    // Budowanie itemu z cenami i przecenami
    // =========================================================================

    private ItemStack buildShopItemStack(ShopItem shopItem, Player player) {
        double finalBuy      = shopManager.getFinalBuyPrice(shopItem);
        double finalSell     = shopManager.getFinalSellPrice(shopItem);
        boolean hasDiscount  = shopManager.hasDiscount(shopItem);
        boolean hasSellBonus = shopManager.hasSellBonus(shopItem);
        double discount      = shopManager.getEffectiveDiscount(shopItem);
        double sellBonus     = shopManager.getEffectiveSellBonus(shopItem);

        List<String> lore = new ArrayList<>();

        if (hasDiscount || hasSellBonus) {
            if (hasDiscount)  lore.add("§c§lPRZECENA §e-" + (int) discount + "%§7 na zakup");
            if (hasSellBonus) lore.add("§a§lBONUS §e+" + (int) sellBonus + "%§7 za sprzedaż");
            lore.add(" ");
        }

        if (shopItem.canBuy()) {
            lore.add(hasDiscount
                    ? "§7Kup: §m§8" + formatPrice(shopItem.getBuyPrice()) + " §a" + formatPrice(finalBuy)
                    : "§7Kup: §a" + formatPrice(finalBuy));
        } else {
            lore.add("§7Kup: §8Niedostępne");
        }

        if (shopItem.canSell()) {
            lore.add(hasSellBonus
                    ? "§7Sprzedaj: §m§8" + formatPrice(shopItem.getSellPrice()) + " §e" + formatPrice(finalSell)
                    : "§7Sprzedaj: §e" + formatPrice(finalSell));
        } else {
            lore.add("§7Sprzedaj: §8Niedostępne");
        }

        lore.add(" ");
        lore.add("§7Twój balans: §f" + formatPrice(economy.getBalance(player)));
        lore.add(" ");
        if (shopItem.canBuy())  lore.add("§eLPM §7— Kup x1  §7| §eShift+LPM §7— Kup x64");
        if (shopItem.canSell()) lore.add("§ePPM §7— Sprzedaj x1 §7| §eShift+PPM §7— Sprzedaj wszystko");

        ItemStack result = shopItem.getItem();
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            List<String> combined = new ArrayList<>();
            if (meta.hasLore()) { combined.addAll(meta.getLore()); combined.add(" "); }
            combined.addAll(lore);
            meta.setLore(combined);

            if (hasDiscount || hasSellBonus) {
                meta.addEnchant(Enchantment.FORTUNE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            meta.getPersistentDataContainer()
                    .set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "ShopBuySell");
            meta.getPersistentDataContainer()
                    .set(new NamespacedKey(plugin, "shop_item_id"), PersistentDataType.STRING, shopItem.getId());

            result.setItemMeta(meta);
        }

        return result;
    }

    public void handleTransaction(Player player, String shopItemId, boolean isRightClick, boolean isShiftClick) {
        ShopItem shopItem = shopManager.getItemById(shopItemId);
        if (shopItem == null) return;

        if (!isRightClick) {
            if (!shopItem.canBuy()) {
                player.sendMessage("§cTego przedmiotu nie można kupić!");
                return;
            }

            double unitPrice = shopManager.getFinalBuyPrice(shopItem);
            int amount = isShiftClick ? 64 : 1;
            double total = unitPrice * amount;

            if (!economy.has(player, total)) {
                player.sendMessage("§cNie masz §e" + formatPrice(total) + "§c!");
                return;
            }

            economy.withdraw(player, total);
            ItemStack item = shopItem.getItem().clone();
            item.setAmount(amount);

            player.getInventory().addItem(item).forEach((s, overflow) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow));

            player.sendMessage("§aKupiono §f" + amount + "x " + shopItemId + " §7za §e" + formatPrice(total));
        } else {
            if (!shopItem.canSell()) {
                player.sendMessage("§cTego przedmiotu nie można sprzedać!");
                return;
            }

            double priceEach = shopManager.getFinalSellPrice(shopItem);
            ItemStack ref = shopItem.getItem();
            int count = 0;

            if (isShiftClick) {
                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem != null && invItem.isSimilar(ref)) {
                        count += invItem.getAmount();
                        invItem.setAmount(0);
                    }
                }
            } else {
                if (player.getInventory().containsAtLeast(ref, 1)) {
                    count = 1;
                    ItemStack toRemove = ref.clone();
                    toRemove.setAmount(1);
                    player.getInventory().removeItem(toRemove);
                }
            }

            if (count == 0) {
                player.sendMessage("§cNie masz tego przedmiotu!");
                return;
            }

            double totalEarned = priceEach * count;
            economy.deposit(player, totalEarned);
            if (questManager != null) {
                questManager.addProgress(player, QuestTrigger.SELL_TO_SHOP, "TRANSACTION", count);
                questManager.addProgress(player, QuestTrigger.SELL_TO_SHOP,
                        "VALUE", (int) Math.min(totalEarned, 1_000_000_000.0));
            }
            player.sendMessage("§aSprzedano §f" + count + "x " + shopItemId + " §7za §e" + formatPrice(totalEarned));
        }
    }

    private String formatPrice(double price) {
        return stark.skyBlockTest2.util.PriceFormat.format(price);
    }
}
package stark.skyBlockTest2.shop;

import org.bukkit.inventory.ItemStack;

/**
 * Pojedynczy przedmiot w sklepie serwera.
 * buyPrice  = cena zakupu od serwera (0 = nie można kupić)
 * sellPrice = cena sprzedaży do serwera (0 = nie można sprzedać)
 */
public class ShopItem {

    private final String    id;
    private final ItemStack item;
    private final double    buyPrice;
    private final double    sellPrice;
    private final String    categoryId;

    public ShopItem(String id, ItemStack item, double buyPrice, double sellPrice, String categoryId) {
        this.id         = id;
        this.item       = item;
        this.buyPrice   = buyPrice;
        this.sellPrice  = sellPrice;
        this.categoryId = categoryId;
    }

    public String    getId()         { return id; }
    public ItemStack getItem()       { return item.clone(); }
    public double    getBuyPrice()   { return buyPrice; }
    public double    getSellPrice()  { return sellPrice; }
    public String    getCategoryId() { return categoryId; }

    public boolean canBuy()  { return buyPrice > 0; }
    public boolean canSell() { return sellPrice > 0; }
}
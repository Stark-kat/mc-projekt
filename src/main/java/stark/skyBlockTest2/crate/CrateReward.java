package stark.skyBlockTest2.crate;

import org.bukkit.inventory.ItemStack;

/**
 * Pojedyncza nagroda w skrzynce.
 * weight — waga losowania (im wyższa, tym częściej wypada).
 */
public class CrateReward {

    private final ItemStack item;
    private final CrateRarity rarity;
    private final int weight;
    private final String displayName; // opcjonalna własna nazwa w animacji

    public CrateReward(ItemStack item, CrateRarity rarity, int weight, String displayName) {
        this.item = item;
        this.rarity = rarity;
        this.weight = weight;
        this.displayName = displayName;
    }

    public ItemStack getItem()        { return item.clone(); }
    public CrateRarity getRarity()    { return rarity; }
    public int getWeight()            { return weight; }
    public String getDisplayName()    { return displayName; }

    /**
     * Zwraca item z nazwą pokolorowaną według rangi.
     * Jeśli displayName jest ustawiony — użyj go, w przeciwnym razie zachowaj oryginalną nazwę.
     */
    public ItemStack getDisplayItem() {
        ItemStack display = item.clone();
        var meta = display.getItemMeta();
        if (meta == null) return display;

        if (displayName != null && !displayName.isBlank()) {
            meta.setDisplayName(rarity.color + displayName);
        } else if (meta.hasDisplayName()) {
            meta.setDisplayName(rarity.color + meta.getDisplayName()
                    .replaceAll("§[0-9a-fk-orA-FK-OR]", ""));
        } else {
            String name = display.getType().name().replace("_", " ").toLowerCase();
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            meta.setDisplayName(rarity.color + name);
        }

        // Dodaj rangę do lore
        var lore = meta.getLore() != null ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<String>();
        lore.add(0, rarity.displayName);
        meta.setLore(lore);

        display.setItemMeta(meta);
        return display;
    }
}
package stark.skyBlockTest2.crate;

import org.bukkit.Material;

public enum CrateRarity {

    COMMON   ("§f", "§7Common",    Material.STONE),
    RARE     ("§9", "§9Rare",      Material.LAPIS_LAZULI),
    EPIC     ("§5", "§5Epic",      Material.AMETHYST_SHARD),
    LEGENDARY("§6", "§6Legendary", Material.GOLD_INGOT);

    public final String color;        // kolor nazwy przedmiotu
    public final String displayName;  // wyświetlana etykieta rangi
    public final Material glassColor; // kolor szyby w GUI animacji

    CrateRarity(String color, String displayName, Material glassColor) {
        this.color = color;
        this.displayName = displayName;
        this.glassColor = glassColor;
    }

    public static CrateRarity fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}
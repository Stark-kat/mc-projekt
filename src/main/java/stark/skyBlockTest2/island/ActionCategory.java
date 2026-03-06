package stark.skyBlockTest2.island;

import org.bukkit.Material;

/**
 * Kategorie ustawień wyspy widoczne w GUI.
 * Wyciągnięte z IslandProtectionListener.
 */
public enum ActionCategory {

    GENERAL("§6Ogólne",          Material.GRASS_BLOCK),
    UTILITY("§eBloki Użytkowe",  Material.CRAFTING_TABLE),
    REDSTONE("§cMechanizmy",     Material.REDSTONE),
    MOBS("§aIstoty",             Material.COW_SPAWN_EGG);

    private final String displayName;
    private final Material icon;

    ActionCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon        = icon;
    }

    public String getDisplayName() { return displayName; }
    public Material getIcon()      { return icon; }
}
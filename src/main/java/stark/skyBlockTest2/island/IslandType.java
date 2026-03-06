package stark.skyBlockTest2.island;

import org.bukkit.Material;
import org.bukkit.World;

public enum IslandType {

    OVERWORLD(
            "world_skyblock",
            World.Environment.NORMAL,
            "Wyspa Główna",
            "island",
            Material.GRASS_BLOCK
    ),
    NETHER(
            "world_skyblock_nether",
            World.Environment.NETHER,
            "Wyspa Nether",
            "nether-island",
            Material.NETHERRACK
    );

    /** Nazwa świata Bukkit */
    public final String worldName;
    /** Środowisko świata */
    public final World.Environment environment;
    /** Nazwa wyświetlana w GUI i wiadomościach */
    public final String displayName;
    /** Prefiks klucza w config.yml (np. "island.upgrade-costs.2") */
    public final String configPath;
    /** Ikona w GUI */
    public final Material icon;

    IslandType(String worldName, World.Environment environment,
               String displayName, String configPath, Material icon) {
        this.worldName   = worldName;
        this.environment = environment;
        this.displayName = displayName;
        this.configPath  = configPath;
        this.icon        = icon;
    }
}

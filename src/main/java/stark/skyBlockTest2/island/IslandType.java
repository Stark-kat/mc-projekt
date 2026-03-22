package stark.skyBlockTest2.island;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;

public enum IslandType {

    OVERWORLD(
            "world_skyblock",
            World.Environment.NORMAL,
            "Wyspa Główna",
            "island",
            Material.GRASS_BLOCK,
            "Default_Island",
            Biome.PLAINS
    ),
    NETHER(
            "world_skyblock_nether",
            World.Environment.NETHER,
            "Wyspa Nether",
            "nether-island",
            Material.NETHERRACK,
            "NetherIsland",
            Biome.NETHER_WASTES
    ),
    END(
            "world_skyblock_end",
            World.Environment.THE_END,
            "Wyspa Endu",
            "end-island",
            Material.END_STONE,
            "EndIsland",
            Biome.THE_END
    ),
    CAVE(
    "world_skyblock_cave",
    World.Environment.NORMAL,
    "Wyspa Jaskini",
            "cave-island",
    Material.DRIPSTONE_BLOCK,
    "CaveIsland",
    Biome.DRIPSTONE_CAVES
    );

    public final String worldName;
    public final World.Environment environment;
    public final String displayName;
    public final String configPath;
    public final Material icon;
    public final String schematicName;
    public final Biome defaultBiome;

    IslandType(String worldName, World.Environment environment,
               String displayName, String configPath, Material icon,
               String schematicName, Biome defaultBiome) {
        this.worldName    = worldName;
        this.environment  = environment;
        this.displayName  = displayName;
        this.configPath   = configPath;
        this.icon         = icon;
        this.schematicName = schematicName;
        this.defaultBiome = defaultBiome;
    }
}
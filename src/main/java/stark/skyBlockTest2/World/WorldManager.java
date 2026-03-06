package stark.skyBlockTest2.World;

import org.bukkit.*;
import org.bukkit.generator.ChunkGenerator;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.island.IslandType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class WorldManager {

    private final SkyBlockTest2 plugin;
    private final Map<IslandType, World> worlds = new EnumMap<>(IslandType.class);

    public WorldManager(SkyBlockTest2 plugin) {
        this.plugin = plugin;
    }

    public void createWorlds() {
        for (IslandType type : IslandType.values()) {
            World existing = Bukkit.getWorld(type.worldName);
            if (existing != null) {
                worlds.put(type, existing);
                continue;
            }

            WorldCreator creator = new WorldCreator(type.worldName);
            creator.environment(type.environment);
            creator.type(WorldType.NORMAL);
            creator.generator(new VoidChunkGenerator());

            World world = creator.createWorld();
            worlds.put(type, world);
            plugin.getLogger().info("[WorldManager] Stworzono świat: " + type.worldName);
        }
    }

    public World getWorld(IslandType type) {
        return worlds.get(type);
    }

    /** Skrót dla OVERWORLD — kompatybilność wsteczna. */
    public World getWorld() {
        return getWorld(IslandType.OVERWORLD);
    }

    private static class VoidChunkGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            return createChunkData(world);
        }
    }
}
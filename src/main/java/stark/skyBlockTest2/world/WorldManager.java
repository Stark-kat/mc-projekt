package stark.skyBlockTest2.world;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.island.IslandType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

            ChunkGenerator generator = (type == IslandType.CAVE)
                    ? new CaveChunkGenerator(type.defaultBiome)
                    : new VoidChunkGenerator(type.defaultBiome);

            WorldCreator creator = new WorldCreator(type.worldName);
            creator.environment(type.environment);
            creator.type(WorldType.NORMAL);
            creator.generator(generator);
            creator.generateStructures(false);

            World world = creator.createWorld();
            worlds.put(type, world);
            plugin.getLogger().info("[WorldManager] Stworzono świat: " + type.worldName);
        }
    }

    public World getWorld(IslandType type) {
        return worlds.get(type);
    }

    public World getWorld() {
        return getWorld(IslandType.OVERWORLD);
    }

    private static class VoidChunkGenerator extends ChunkGenerator {

        private final Biome biome;

        VoidChunkGenerator(Biome biome) {
            this.biome = biome;
        }

        @Override
        public boolean shouldGenerateStructures() {
            return false; // ← dodaj
        }

        @Override
        public ChunkData generateChunkData(World world, java.util.Random random, int x, int z, BiomeGrid grid) {
            return createChunkData(world);
        }

        @Override
        public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
            return new BiomeProvider() {
                @Override
                public Biome getBiome(WorldInfo info, int x, int y, int z) {
                    return biome;
                }

                @Override
                public List<Biome> getBiomes(WorldInfo info) {
                    return List.of(biome);
                }
            };
        }
    }

    private static class CaveChunkGenerator extends ChunkGenerator {

        private final Biome biome;

        CaveChunkGenerator(Biome biome) {
            this.biome = biome;
        }

        @Override
        public boolean shouldGenerateStructures() {
            return false;
        }

        @Override
        public ChunkData generateChunkData(World world, java.util.Random random, int x, int z, BiomeGrid grid) {
            ChunkData data = createChunkData(world);
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();

            for (int bx = 0; bx < 16; bx++) {
                for (int bz = 0; bz < 16; bz++) {
                    data.setBlock(bx, -1, bz, Material.BEDROCK);
                    data.setBlock(bx, 180, bz, Material.BEDROCK);

                    for (int by = 0; by < 180; by++) {
                        data.setBlock(bx, by, bz, Material.STONE);
                    }
                }
            }
            return data;
        }

        @Override
        public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
            return new BiomeProvider() {
                @Override
                public Biome getBiome(WorldInfo info, int x, int y, int z) {
                    return biome;
                }

                @Override
                public List<Biome> getBiomes(WorldInfo info) {
                    return List.of(biome);
                }
            };
        }
    }
}
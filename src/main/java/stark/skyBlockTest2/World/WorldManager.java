package stark.skyBlockTest2.World;

import org.bukkit.*;
import org.bukkit.generator.ChunkGenerator;
import stark.skyBlockTest2.SkyBlockTest2;

import java.util.Random;

public class WorldManager {

    private final SkyBlockTest2 plugin;
    private World skyblockWorld;

    public static final String WORLD_NAME = "world_skyblock";

    public WorldManager(SkyBlockTest2 plugin) {
        this.plugin = plugin;
    }

    public void createWorld() {

        if (Bukkit.getWorld(WORLD_NAME) != null) {
            skyblockWorld = Bukkit.getWorld(WORLD_NAME);
            return;
        }

        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.NORMAL);
        creator.generator(new VoidChunkGenerator());

        skyblockWorld = creator.createWorld();

    }

    public World getWorld() {
        return skyblockWorld;
    }

    private static class VoidChunkGenerator extends ChunkGenerator {

        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            return createChunkData(world);
        }
    }
}
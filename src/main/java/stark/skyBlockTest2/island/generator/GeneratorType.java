package stark.skyBlockTest2.island.generator;

public enum GeneratorType {
    COBBLESTONE("world_skyblock"),
    STONE("world_skyblock_cave"),
    NETHER("world_skyblock_nether"),
    END("world_skyblock_end");

    public final String worldName;

    GeneratorType(String worldName) {
        this.worldName = worldName;
    }

    public static GeneratorType fromWorld(String worldName) {
        if (worldName == null) return null;
        for (GeneratorType t : values()) {
            if (t.worldName.equals(worldName)) return t;
        }
        return null;
    }

    public boolean isMachineType() {
        return this == NETHER || this == END;
    }
}
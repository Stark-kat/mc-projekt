package stark.skyBlockTest2.island.generator;

import org.bukkit.Material;

import java.util.List;

public class GeneratorDefinition {

    private final GeneratorType type;
    private final List<GeneratorLevel> levels;

    // Tylko dla typów maszynowych (NETHER, END)
    private final Material machineBlock;
    private final int intervalTicks;
    private final int radius;

    // Konstruktor dla OVERWORLD/CAVE (BlockForm)
    public GeneratorDefinition(GeneratorType type, List<GeneratorLevel> levels) {
        this.type         = type;
        this.levels       = levels;
        this.machineBlock = null;
        this.intervalTicks = 0;
        this.radius       = 0;
    }

    // Konstruktor dla NETHER/END (maszyna)
    public GeneratorDefinition(GeneratorType type, List<GeneratorLevel> levels,
                               Material machineBlock, int intervalTicks, int radius) {
        this.type          = type;
        this.levels        = levels;
        this.machineBlock  = machineBlock;
        this.intervalTicks = intervalTicks;
        this.radius        = radius;
    }

    /**
     * Zwraca poziom dla danego numeru (1-based).
     * Jeśli poziom przekracza max — zwraca ostatni.
     */
    public GeneratorLevel getLevel(int level) {
        int idx = Math.min(level, levels.size()) - 1;
        return levels.get(Math.max(0, idx));
    }

    public int getMaxLevel()         { return levels.size(); }
    public GeneratorType getType()   { return type; }
    public List<GeneratorLevel> getLevels() { return levels; }
    public Material getMachineBlock()       { return machineBlock; }
    public int getIntervalTicks()           { return intervalTicks; }
    public int getRadius()                  { return radius; }
}
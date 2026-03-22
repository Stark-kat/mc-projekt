package stark.skyBlockTest2.island.generator;

import org.bukkit.Material;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GeneratorLevel {

    private final int level;
    private final String name;
    private final double cost;
    private final Map<Material, Integer> drops; // Material -> waga (suma = 100)

    public GeneratorLevel(int level, String name, double cost, Map<Material, Integer> drops) {
        this.level = level;
        this.name  = name;
        this.cost  = cost;
        this.drops = drops;
    }

    /**
     * Ważone losowanie materiału z tabeli dropów.
     */
    public Material roll() {
        int total = drops.values().stream().mapToInt(i -> i).sum();
        int roll  = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (Map.Entry<Material, Integer> entry : drops.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) return entry.getKey();
        }
        // Fallback — nigdy nie powinien się wykonać przy poprawnym yml
        return drops.keySet().iterator().next();
    }

    public int getLevel()              { return level; }
    public String getName()            { return name; }
    public double getCost()            { return cost; }
    public Map<Material, Integer> getDrops() { return drops; }
}
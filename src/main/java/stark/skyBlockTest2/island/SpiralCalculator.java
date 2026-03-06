package stark.skyBlockTest2.island;

import java.util.HashMap;
import java.util.Map;

/**
 * Oblicza pozycję siatki (x, z) dla podanego indeksu wyspy,
 * układając wyspy w spiralę od środka.
 *
 * Wyniki są cachowane — każdy indeks liczony jest tylko raz,
 * co ma znaczenie przy starcie serwera ze szczytową liczbą wysp.
 */
public class SpiralCalculator {

    private static final Map<Integer, int[]> cache = new HashMap<>();

    public static int[] getSpiralPosition(int index) {
        // Zwracamy cachedowany wynik jeśli już był liczony
        if (cache.containsKey(index)) {
            return cache.get(index);
        }

        int[] result = calculate(index);
        cache.put(index, result);
        return result;
    }

    private static int[] calculate(int index) {
        if (index == 0) return new int[]{0, 0};

        int layer     = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2);
        int legLength = layer * 2;
        int maxValue  = (2 * layer + 1) * (2 * layer + 1);
        int diff      = maxValue - index;

        int x, z;

        if (diff < legLength) {
            x = layer;
            z = -layer + diff;
        } else if (diff < legLength * 2) {
            x = layer - (diff - legLength);
            z = layer;
        } else if (diff < legLength * 3) {
            x = -layer;
            z = layer - (diff - legLength * 2);
        } else {
            x = -layer + (diff - legLength * 3);
            z = -layer;
        }

        return new int[]{x, z};
    }

    /**
     * Czyści cache — przydatne przy testach lub przeładowaniu pluginu.
     */
    public static void clearCache() {
        cache.clear();
    }
}
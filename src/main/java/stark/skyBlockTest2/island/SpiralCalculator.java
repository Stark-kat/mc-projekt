package stark.skyBlockTest2.island;

public class SpiralCalculator {

    public static int[] getSpiralPosition(int index) {

        if (index == 0) {
            return new int[]{0, 0};
        }

        int layer = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2);
        int legLength = layer * 2;
        int maxValue = (2 * layer + 1) * (2 * layer + 1);
        int diff = maxValue - index;

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
}

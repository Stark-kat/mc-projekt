package stark.skyBlockTest2.crate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CrateDefinition {

    private static final Random RANDOM = new Random();

    private final String id;
    private final String displayName;
    private final CrateRarity rarity;
    private final List<CrateReward> rewards;
    private final int totalWeight;

    public CrateDefinition(String id, String displayName, CrateRarity rarity,
                           List<CrateReward> rewards) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.rewards = rewards;
        this.totalWeight = rewards.stream().mapToInt(CrateReward::getWeight).sum();
    }

    /**
     * Losuje nagrodę na podstawie wag.
     * Im wyższa waga, tym większa szansa na wylosowanie.
     */
    public CrateReward rollReward() {
        if (rewards.isEmpty()) return null;

        int roll = RANDOM.nextInt(totalWeight);
        int cumulative = 0;

        for (CrateReward reward : rewards) {
            cumulative += reward.getWeight();
            if (roll < cumulative) return reward;
        }

        return rewards.get(rewards.size() - 1);
    }

    /**
     * Zwraca listę nagród do wypełnienia animacji bębna.
     * Miesza nagrody losowo tak żeby bęben wyglądał dynamicznie.
     */
    public List<CrateReward> getShuffledRewardsForAnimation(int count) {
        List<CrateReward> pool = new ArrayList<>();

        // Budujemy pulę ważoną — każdy reward pojawia się proporcjonalnie do wagi
        // Normalizujemy do max 5 kopii żeby nie pompować listy przy dużych wagach
        int maxWeight = rewards.stream().mapToInt(CrateReward::getWeight).max().orElse(1);
        for (CrateReward reward : rewards) {
            int copies = Math.max(1, Math.round((float) reward.getWeight() / maxWeight * 5));
            for (int i = 0; i < copies; i++) pool.add(reward);
        }

        // Tasujemy i powielamy do wymaganego rozmiaru
        List<CrateReward> result = new ArrayList<>();
        while (result.size() < count) {
            List<CrateReward> copy = new ArrayList<>(pool);
            Collections.shuffle(copy);
            result.addAll(copy);
        }

        return new ArrayList<>(result.subList(0, count));
    }

    public String getId()                    { return id; }
    public String getDisplayName()           { return displayName; }
    public CrateRarity getRarity()           { return rarity; }
    public List<CrateReward> getRewards()    { return rewards; }
    public int getTotalWeight()              { return totalWeight; }
}
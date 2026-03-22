package stark.skyBlockTest2.quest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AchievementDefinition {

    /**
     * Wymaganie na konkretny target w ramach jednego tiera.
     * Dla single-target: jedna instancja, target = globalny target osiągnięcia.
     * Dla multi-target: wiele instancji, każda z własnym targetem i ilością.
     */
    public record TargetRequirement(String target, int amount) {}

    /**
     * Jeden próg osiągnięcia.
     * requirements.size() == 1 → single-target (stary format)
     * requirements.size() >  1 → multi-target  (nowy format)
     */
    public record Tier(List<TargetRequirement> requirements, long xpReward, List<ItemStack> itemRewards, double moneyReward) {

        public boolean isMultiTarget() {
            return requirements.size() > 1;
        }

        /** Dla single-target: ilość potrzebna do ukończenia. */
        public int getSingleAmount() {
            return requirements.isEmpty() ? 0 : requirements.get(0).amount();
        }

        /** Zwraca requirement pasujący do podanego targetu lub null. */
        public TargetRequirement findRequirement(String target) {
            for (TargetRequirement req : requirements) {
                if (req.target().isEmpty() || req.target().equalsIgnoreCase(target)) return req;
            }
            return null;
        }
    }

    private final String id;
    private final String category;
    private final QuestTrigger trigger;
    private final String target;       // globalny filtr; dla multi-target = ""
    private final String displayName;
    private final String description;
    private final Material icon;
    private final List<Tier> tiers;

    public AchievementDefinition(String id, String category, QuestTrigger trigger, String target,
                                 String displayName, String description,
                                 Material icon, List<Tier> tiers) {
        this.id          = id;
        this.category    = category;
        this.trigger     = trigger;
        this.target      = target;
        this.displayName = displayName;
        this.description = description;
        this.icon        = icon;
        this.tiers       = tiers;
    }

    public String getId()            { return id; }
    public String getCategory()      { return category; }
    public QuestTrigger getTrigger() { return trigger; }
    public String getTarget()        { return target; }
    public String getDisplayName()   { return displayName; }
    public String getDescription()   { return description; }
    public Material getIcon()        { return icon; }
    public List<Tier> getTiers()     { return tiers; }

    /** Zwraca następny nieukończony tier lub null jeśli wszystkie ukończone. */
    public Tier getNextTier(int completedTiers) {
        if (completedTiers >= tiers.size()) return null;
        return tiers.get(completedTiers);
    }

    public boolean isFullyCompleted(int completedTiers) {
        return completedTiers >= tiers.size();
    }

    public int getTierCount() {
        return tiers.size();
    }
}

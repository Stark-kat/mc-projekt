package stark.skyBlockTest2.quest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class QuestDefinition {

    private final String id;
    private final QuestType type;
    private final QuestTrigger trigger;
    private final String target;          // np. "STONE", "ZOMBIE", "" = dowolny
    private final int amount;
    private final String displayName;
    private final String description;
    private final long xpReward;
    private final List<ItemStack> itemRewards;
    private final Material icon;

    public QuestDefinition(String id, QuestType type, QuestTrigger trigger,
                           String target, int amount, String displayName,
                           String description, long xpReward,
                           List<ItemStack> itemRewards, Material icon) {
        this.id = id;
        this.type = type;
        this.trigger = trigger;
        this.target = target;
        this.amount = amount;
        this.displayName = displayName;
        this.description = description;
        this.xpReward = xpReward;
        this.itemRewards = itemRewards;
        this.icon = icon;
    }

    public String getId()                     { return id; }
    public QuestType getType()                { return type; }
    public QuestTrigger getTrigger()          { return trigger; }
    public String getTarget()                 { return target; }
    public int getAmount()                    { return amount; }
    public String getDisplayName()            { return displayName; }
    public String getDescription()            { return description; }
    public long getXpReward()                 { return xpReward; }
    public List<ItemStack> getItemRewards()   { return itemRewards; }
    public Material getIcon()                 { return icon; }
}
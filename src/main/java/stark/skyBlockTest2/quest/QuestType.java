package stark.skyBlockTest2.quest;

public enum QuestType {
    DAILY("Dzienne"),
    WEEKLY("Tygodniowe");

    public final String displayName;

    QuestType(String displayName) {
        this.displayName = displayName;
    }
}
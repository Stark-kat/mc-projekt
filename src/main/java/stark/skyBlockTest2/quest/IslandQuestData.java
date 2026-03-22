package stark.skyBlockTest2.quest;

import java.util.*;

/**
 * Przechowuje postęp questów i osiągnięć dla jednej wyspy (wspólny dla wszystkich członków).
 * Klucz to UUID właściciela wyspy.
 */
public class IslandQuestData {

    // Postęp questów (questId -> ilość)
    private final Map<String, Integer> dailyProgress  = new HashMap<>();
    private final Map<String, Integer> weeklyProgress = new HashMap<>();

    // Ukończone questy (nagroda już odebrana)
    private final Set<String> completedDaily  = new HashSet<>();
    private final Set<String> completedWeekly = new HashSet<>();

    // Osiągnięcia: łączny postęp single-target (kumulatywny, nigdy nie resetowany)
    private final Map<String, Integer> achievementProgress = new HashMap<>();

    // Osiągnięcia multi-target: achievementId -> (target -> łączna ilość)
    private final Map<String, Map<String, Integer>> achievementTargetProgress = new HashMap<>();

    // Osiągnięcia: ile tierów już ukończono
    private final Map<String, Integer> achievementTiers = new HashMap<>();

    // Czasy ostatnich resetów (epoch millis — do ewentualnej weryfikacji przy starcie)
    private long lastDailyReset  = 0L;
    private long lastWeeklyReset = 0L;

    // ---- Questy dzienne ----

    public int getDailyProgress(String questId) {
        return dailyProgress.getOrDefault(questId, 0);
    }

    public void addDailyProgress(String questId, int amount) {
        dailyProgress.merge(questId, amount, Integer::sum);
    }

    public boolean isDailyCompleted(String questId) {
        return completedDaily.contains(questId);
    }

    public void setDailyCompleted(String questId) {
        completedDaily.add(questId);
    }

    public void resetDaily() {
        dailyProgress.clear();
        completedDaily.clear();
        lastDailyReset = System.currentTimeMillis();
    }

    // ---- Questy tygodniowe ----

    public int getWeeklyProgress(String questId) {
        return weeklyProgress.getOrDefault(questId, 0);
    }

    public void addWeeklyProgress(String questId, int amount) {
        weeklyProgress.merge(questId, amount, Integer::sum);
    }

    public boolean isWeeklyCompleted(String questId) {
        return completedWeekly.contains(questId);
    }

    public void setWeeklyCompleted(String questId) {
        completedWeekly.add(questId);
    }

    public void resetWeekly() {
        weeklyProgress.clear();
        completedWeekly.clear();
        lastWeeklyReset = System.currentTimeMillis();
    }

    // ---- Osiągnięcia ----

    public int getAchievementProgress(String achievementId) {
        return achievementProgress.getOrDefault(achievementId, 0);
    }

    public void addAchievementProgress(String achievementId, int amount) {
        achievementProgress.merge(achievementId, amount, Integer::sum);
    }

    public int getAchievementTargetProgress(String achievementId, String target) {
        Map<String, Integer> inner = achievementTargetProgress.get(achievementId);
        return inner == null ? 0 : inner.getOrDefault(target.toUpperCase(), 0);
    }

    public void addAchievementTargetProgress(String achievementId, String target, int amount) {
        achievementTargetProgress
                .computeIfAbsent(achievementId, k -> new HashMap<>())
                .merge(target.toUpperCase(), amount, Integer::sum);
    }

    public Map<String, Map<String, Integer>> getAchievementTargetProgressMap() {
        return achievementTargetProgress;
    }

    public int getCompletedTiers(String achievementId) {
        return achievementTiers.getOrDefault(achievementId, 0);
    }

    public void incrementCompletedTiers(String achievementId) {
        achievementTiers.merge(achievementId, 1, Integer::sum);
    }

    // ---- Timestamps ----

    public long getLastDailyReset()          { return lastDailyReset; }
    public void setLastDailyReset(long t)    { this.lastDailyReset = t; }

    public long getLastWeeklyReset()         { return lastWeeklyReset; }
    public void setLastWeeklyReset(long t)   { this.lastWeeklyReset = t; }

    // ---- Raw access dla QuestStorage ----

    public Map<String, Integer> getDailyProgressMap()        { return dailyProgress; }
    public Map<String, Integer> getWeeklyProgressMap()       { return weeklyProgress; }
    public Set<String>          getCompletedDailySet()       { return completedDaily; }
    public Set<String>          getCompletedWeeklySet()      { return completedWeekly; }
    public Map<String, Integer> getAchievementProgressMap()  { return achievementProgress; }
    public Map<String, Integer> getAchievementTiersMap()     { return achievementTiers; }
}
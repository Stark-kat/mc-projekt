package stark.skyBlockTest2.crate;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.database.DatabaseManager;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.menu.MenuHolder;

import java.util.*;

public class CrateAnimationGui {

    private static final int[] DRUM_SLOTS    = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int   CENTER_SLOT   = 13;
    private static final int   INDICATOR_TOP = 4;
    private static final int   INDICATOR_BOT = 22;

    private static final int[] PHASE_DELAYS   = {1, 1, 1, 2, 2, 3, 4, 5, 6, 8};
    private static final int   SPINS_PER_PHASE = 4;

    private final SkyBlockTest2   plugin;
    private final DatabaseManager db;

    private final Set<UUID>              animating      = new HashSet<>();
    private final Map<UUID, Inventory>   activeGuis     = new HashMap<>();
    private final Map<UUID, CrateReward> pendingRewards = new HashMap<>();

    public CrateAnimationGui(SkyBlockTest2 plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    // =========================================================================
    // API publiczne
    // =========================================================================

    public boolean isAnimating(UUID uuid) {
        return animating.contains(uuid);
    }

    public Inventory getActiveGui(UUID uuid) {
        return activeGuis.get(uuid);
    }

    /**
     * Wywoływane przy śmierci lub quicie gracza — natychmiast daje nagrodę
     * do ekwipunku i czyści stan. Rekord DB kasowany jest w cleanup().
     */
    public void forceFinish(UUID uuid) {
        CrateReward reward = pendingRewards.get(uuid);
        Player player = Bukkit.getPlayer(uuid);

        if (reward != null && player != null) {
            player.getInventory().addItem(reward.getItem())
                    .forEach((slot, overflow) ->
                            player.getWorld().dropItemNaturally(
                                    player.getLocation(), overflow));
        }

        cleanup(uuid);
    }

    /**
     * Wywoływane przy logowaniu gracza — sprawdza czy przy poprzedniej sesji
     * pozostała nieoddana nagroda (np. restart serwera w trakcie animacji)
     * i od razu ją dostarcza.
     */
    public void deliverPendingRewardOnJoin(Player player) {
        // Nie rób nic jeśli animacja właśnie trwa (normalny przypadek)
        if (animating.contains(player.getUniqueId())) return;

        CrateReward reward = db.loadPendingReward(player.getUniqueId());
        if (reward == null) return;

        // Daj nagrodę z opóźnieniem żeby gracz zdążył się w pełni załadować
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.getInventory().addItem(reward.getItem())
                    .forEach((slot, overflow) ->
                            player.getWorld().dropItemNaturally(
                                    player.getLocation(), overflow));
            player.sendMessage("§6Odebrano nagrodę ze skrzynki z poprzedniej sesji: "
                    + reward.getRarity().color + getItemName(reward.getDisplayItem()));
            db.deletePendingReward(player.getUniqueId());
        }, 20L);
    }

    // =========================================================================
    // Otwieranie animacji
    // =========================================================================

    public void open(Player player, CrateDefinition crate) {
        if (animating.contains(player.getUniqueId())) {
            player.sendMessage("§cPoczekaj az poprzednia skrzynka zostanie otwarta!");
            return;
        }

        CrateReward result = crate.rollReward();
        if (result == null) {
            player.sendMessage("§cBlad: skrzynka nie ma nagrod!");
            return;
        }

        // Zabieramy skrzynkę z ręki na samym początku, zanim gracz zdąży ją zmienić
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);

        int totalSpins   = PHASE_DELAYS.length * SPINS_PER_PHASE;
        int centerOffset = 4;
        int poolSize     = totalSpins + DRUM_SLOTS.length + 10;

        List<CrateReward> pool = crate.getShuffledRewardsForAnimation(poolSize);
        int resultIndex = totalSpins + centerOffset - 1;
        pool.set(resultIndex, result);

        Inventory gui = Bukkit.createInventory(new MenuHolder(), 27,
                "§8Otwieranie: §r" + crate.getRarity().color + crate.getDisplayName());
        buildStaticElements(gui);

        player.openInventory(gui);

        UUID uuid = player.getUniqueId();
        animating.add(uuid);
        activeGuis.put(uuid, gui);
        pendingRewards.put(uuid, result);

        // Zapisujemy nagrodę do DB — zabezpieczenie przed restartem serwera
        db.savePendingReward(uuid, result);

        new SpinAnimation(player, gui, pool, result).runTaskTimer(plugin, 0L, 1L);
    }

    // =========================================================================
    // Statyczne elementy GUI
    // =========================================================================

    private void buildStaticElements(Inventory gui) {
        ItemStack bg = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++)   gui.setItem(i, bg);
        for (int i = 18; i < 27; i++) gui.setItem(i, bg);

        // Stałe wskaźniki — nie zmieniają się w trakcie animacji
        ItemStack indicator = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name("§a★").build();
        gui.setItem(INDICATOR_TOP, indicator);
        gui.setItem(INDICATOR_BOT, indicator);
    }

    // =========================================================================
    // Sprzątanie
    // =========================================================================

    private void cleanup(UUID uuid) {
        animating.remove(uuid);
        activeGuis.remove(uuid);
        pendingRewards.remove(uuid);
        db.deletePendingReward(uuid);
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    private String getItemName(ItemStack item) {
        if (item == null) return "?";
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        String name = item.getType().name().replace("_", " ").toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    // =========================================================================
    // Animacja
    // =========================================================================

    private class SpinAnimation extends BukkitRunnable {

        private final Player            player;
        private final Inventory         gui;
        private final List<CrateReward> pool;
        private final CrateReward       result;

        private int  poolIndex    = 0;
        private int  phaseIndex   = 0;
        private int  spinsInPhase = 0;
        private int  tickCounter  = 0;
        private boolean finished  = false;

        SpinAnimation(Player player, Inventory gui, List<CrateReward> pool, CrateReward result) {
            this.player = player;
            this.gui    = gui;
            this.pool   = pool;
            this.result = result;
        }

        @Override
        public void run() {
            // Gracz offline — forceFinish/onPlayerQuit obsługuje ten przypadek
            if (!player.isOnline()) {
                cleanup(player.getUniqueId());
                cancel();
                return;
            }

            if (finished) return;

            int currentDelay = PHASE_DELAYS[Math.min(phaseIndex, PHASE_DELAYS.length - 1)];
            tickCounter++;
            if (tickCounter < currentDelay) return;
            tickCounter = 0;

            scrollDrum();
            poolIndex++;

            // Dźwięk kliknięcia przez całą animację, pitch rośnie wraz z fazą
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                    1.0f + (phaseIndex * 0.1f));

            spinsInPhase++;
            if (spinsInPhase >= SPINS_PER_PHASE) {
                spinsInPhase = 0;
                phaseIndex++;
            }

            if (phaseIndex >= PHASE_DELAYS.length) {
                finishAnimation();
            }
        }

        private void scrollDrum() {
            for (int i = 0; i < DRUM_SLOTS.length; i++) {
                int rewardIdx = (poolIndex + i) % pool.size();
                gui.setItem(DRUM_SLOTS[i], pool.get(rewardIdx).getDisplayItem());
            }
        }

        private void finishAnimation() {
            finished = true;
            cancel();

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.sendMessage(" ");
            player.sendMessage("§6§lSKRZYNKA OTWARTA!");
            player.sendMessage("§7Wylosowales: "
                    + result.getRarity().color + getItemName(result.getDisplayItem()));
            player.sendMessage("§7Ranga: " + result.getRarity().displayName);
            player.sendMessage(" ");

            plugin.getQuestManager().addProgress(player, stark.skyBlockTest2.quest.QuestTrigger.OPEN_CRATE, "", 1);

            // Daj nagrodę po 2s i zamknij GUI jeśli gracz je jeszcze ma otwarte
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.getInventory().addItem(result.getItem())
                            .forEach((slot, overflow) ->
                                    player.getWorld().dropItemNaturally(
                                            player.getLocation(), overflow));
                }

                // cleanup() kasuje też rekord w DB
                cleanup(player.getUniqueId());

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()
                            && player.getOpenInventory().getTopInventory() == gui) {
                        player.closeInventory();
                    }
                }, 40L);

            }, 40L);
        }
    }
}
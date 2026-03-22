package stark.skyBlockTest2.island.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandAction;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.IslandType;

import java.util.Objects;

public class IslandProtectionListener implements Listener {

    private final IslandManager islandManager;

    public IslandProtectionListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    // -------------------------------------------------------------------------
    // Pomocnicze
    // -------------------------------------------------------------------------

    /**
     * Zwraca wyspę OVERWORLD właściciela tej lokalizacji — niezależnie od świata.
     * Stąd ustawienia gości są zawsze z wyspy głównej.
     */
    private Island getEffectiveIsland(Location loc) {
        Island island = islandManager.getIslandAt(loc);
        if (island == null) return null;
        if (loc.getWorld() != null && IslandType.OVERWORLD.worldName.equals(loc.getWorld().getName())) {
            return island;
        }
        // Na wyspach dodatkowych — pobierz ustawienia z wyspy OVERWORLD właściciela
        Island main = islandManager.getIsland(island.getOwner());
        return main != null ? main : island;
    }

    /**
     * Sprawdza czy gracz może swobodnie działać (właściciel/członek).
     * Na wyspach dodatkowych tylko właściciel ma pełne prawa.
     */
    private boolean isEffectiveMember(Player player, Location loc) {
        Island island = islandManager.getIslandAt(loc);
        if (island == null) return false;
        if (loc.getWorld() != null && IslandType.OVERWORLD.worldName.equals(loc.getWorld().getName())) {
            return island.isMember(player.getUniqueId());
        }
        // Wyspa dodatkowa (Nether itp.) — prawa członków wynikają z głównej wyspy OVERWORLD,
        // bo Nether to rozszerzenie głównej wyspy, nie osobny byt
        Island main = islandManager.getIsland(island.getOwner());
        if (main != null) return main.isMember(player.getUniqueId());
        return island.getOwner().equals(player.getUniqueId());
    }

    private boolean isIslandWorld(org.bukkit.World world) {
        if (world == null) return false;
        for (IslandType type : IslandType.values()) {
            if (type.worldName.equals(world.getName())) return true;
        }
        return false;
    }

    private boolean shouldCancel(Player player, Block block, IslandAction type) {
        if (block == null) return false;
        Location loc = block.getLocation();
        if (isEffectiveMember(player, loc)) return false;
        Island island = getEffectiveIsland(loc);
        if (island == null) {
            // Blok poza granicą wyspy w świecie wyspowym — blokuj
            return isIslandWorld(loc.getWorld());
        }
        if (island.canVisitorDo(type)) return false;
        player.sendMessage("§cNie masz uprawnień do: " + type.getDisplayName());
        return true;
    }

    private boolean isChest(Material mat) {
        String name = mat.name();
        return (name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER"))
                && mat != Material.ENDER_CHEST;
    }

    // -------------------------------------------------------------------------
    // Granice wyspy — blokujemy wyjście poza wyspę niezależnie od borderu
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIslandBoundary(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        Location from = event.getFrom();
        Location to   = event.getTo();
        if ((from.getBlockX() >> 4) == (to.getBlockX() >> 4)
                && (from.getBlockZ() >> 4) == (to.getBlockZ() >> 4)) return;

        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) return;

        Island fromIsland = islandManager.getIslandAt(from);
        Island toIsland   = islandManager.getIslandAt(to);

        if (fromIsland == null && toIsland == null) return;
        if (Objects.equals(fromIsland, toIsland)) return;

        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Zbanowani — blokujemy wejście na wyspę
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onBannedMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        Location from = event.getFrom();
        Location to   = event.getTo();
        if ((from.getBlockX() >> 4) == (to.getBlockX() >> 4)
                && (from.getBlockZ() >> 4) == (to.getBlockZ() >> 4)) return;

        Player player = event.getPlayer();
        Island island = getEffectiveIsland(to);
        if (island == null) return;
        if (!island.isBanned(player.getUniqueId())) return;
        event.setCancelled(true);
        player.sendMessage("§cJesteś zbanowany na tej wyspie!");
    }

    // -------------------------------------------------------------------------
    // Bloki
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.BREAK_BLOCKS))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.PLACE_BLOCKS))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onFireSpread(BlockIgniteEvent event) {
        if (event.getPlayer() != null)
            if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.FIRE_SPREAD))
                event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Wiadra
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.USE_BUCKETS))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.USE_BUCKETS))
            event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Przedmioty — pickup i drop
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (isEffectiveMember(player, loc)) return;
        Island island = getEffectiveIsland(loc);
        if (island == null) return;
        if (!island.canVisitorDo(IslandAction.PICKUP_ITEMS))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (isEffectiveMember(player, loc)) return;
        Island island = getEffectiveIsland(loc);
        if (island == null) return;
        if (!island.canVisitorDo(IslandAction.DROP_ITEMS)) {
            event.setCancelled(true);
            player.sendMessage("§cNie możesz wyrzucać przedmiotów na tej wyspie!");
        }
    }

    // -------------------------------------------------------------------------
    // Portale
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (isEffectiveMember(player, loc)) return;
        Island island = getEffectiveIsland(loc);
        if (island == null) return;
        if (!island.canVisitorDo(IslandAction.USE_PORTALS)) {
            event.setCancelled(true);
            player.sendMessage("§cNie możesz używać portali na tej wyspie!");
        }
    }

    // -------------------------------------------------------------------------
    // Interakcje z blokami
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        Material mat = block.getType();

        if (event.getAction() == Action.PHYSICAL) {
            if (Tag.PRESSURE_PLATES.isTagged(mat))
                if (shouldCancel(player, block, IslandAction.USE_PRESSURE_PLATES))
                    event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isChest(mat)) {
                if (shouldCancel(player, block, IslandAction.OPEN_CONTAINERS)) event.setCancelled(true);
            } else if (mat.name().contains("ANVIL")) {
                if (shouldCancel(player, block, IslandAction.USE_ANVIL)) event.setCancelled(true);
            } else if (mat == Material.ENCHANTING_TABLE) {
                if (shouldCancel(player, block, IslandAction.USE_ENCHANTING)) event.setCancelled(true);
            } else if (mat == Material.BREWING_STAND) {
                if (shouldCancel(player, block, IslandAction.USE_BREWING)) event.setCancelled(true);
            } else if (mat == Material.CRAFTING_TABLE) {
                if (shouldCancel(player, block, IslandAction.USE_CRAFTING)) event.setCancelled(true);
            } else if (Tag.DOORS.isTagged(mat) || Tag.FENCE_GATES.isTagged(mat) || Tag.TRAPDOORS.isTagged(mat)) {
                if (shouldCancel(player, block, IslandAction.USE_DOORS)) event.setCancelled(true);
            } else if (Tag.BUTTONS.isTagged(mat) || mat == Material.LEVER) {
                if (shouldCancel(player, block, IslandAction.USE_BUTTONS)) event.setCancelled(true);
            } else if (mat == Material.FLOWER_POT) {
                if (shouldCancel(player, block, IslandAction.INTERACT_DECORATIONS)) event.setCancelled(true);
            } else if (mat == Material.LECTERN) {
                if (shouldCancel(player, block, IslandAction.INTERACT_UTILITY)) event.setCancelled(true);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Interakcje z bytami
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof org.bukkit.entity.ArmorStand || entity instanceof org.bukkit.entity.ItemFrame)
            if (shouldCancel(event.getPlayer(), entity.getLocation().getBlock(), IslandAction.ARMOR_STAND_INTERACT))
                event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player  = event.getPlayer();
        Entity entity  = event.getRightClicked();
        Block locBlock = entity.getLocation().getBlock();

        if (entity instanceof Villager) {
            if (shouldCancel(player, locBlock, IslandAction.VILLAGER_TRADE)) event.setCancelled(true);
        } else if (entity instanceof Animals) {
            ItemStack item = player.getInventory().getItemInMainHand();
            Material hand  = item.getType();

            if (hand == Material.BUCKET) {
                if (shouldCancel(player, locBlock, IslandAction.MILK_COWS)) event.setCancelled(true);
            } else if (hand == Material.SHEARS) {
                if (shouldCancel(player, locBlock, IslandAction.SHEAR_SHEEP)) event.setCancelled(true);
            } else {
                // Karmienie i hodowanie — wspólna akcja INTERACT_ANIMALS
                if (shouldCancel(player, locBlock, IslandAction.INTERACT_ANIMALS)) event.setCancelled(true);
            }
        }
    }

    // -------------------------------------------------------------------------
    // PVP — zawsze wyłączone na wyspach
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player)) return;
        Island island = islandManager.getIslandAt(event.getEntity().getLocation());
        if (island == null) return;
        event.setCancelled(true);
        attacker.sendMessage("§cPVP jest wyłączone na wyspach!");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        Entity entity = event.getEntity();
        if (event.getEntity() instanceof Animals || event.getEntity() instanceof Villager)
            if (shouldCancel(player, event.getEntity().getLocation().getBlock(), IslandAction.KILL_ANIMALS))
                event.setCancelled(true);
        if (entity instanceof org.bukkit.entity.ArmorStand)
            if (shouldCancel(player, entity.getLocation().getBlock(), IslandAction.ARMOR_STAND_INTERACT))
                event.setCancelled(true);

        if (entity instanceof org.bukkit.entity.ItemFrame)
            if (shouldCancel(player, entity.getLocation().getBlock(), IslandAction.ITEM_FRAME_INTERACT))
                event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Uprawy
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onCropTrample(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.FARMLAND) return;
        if (shouldCancel(event.getPlayer(), block, IslandAction.CROP_TRAMPLE))
            event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Smycz — zawsze wyłączona dla gości
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onLeash(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.LeashHitch)) return;
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (isEffectiveMember(player, loc)) return;
        if (getEffectiveIsland(loc) == null) return;
        event.setCancelled(true);
        player.sendMessage("§cNie możesz używać smyczy na tej wyspie!");
    }

    // -------------------------------------------------------------------------
    // Wybuchy — ochrona świata
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onExplosion(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHangingDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof org.bukkit.entity.Hanging || entity instanceof org.bukkit.entity.ArmorStand) {
            var cause = event.getCause();
            if (cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                    cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)
                event.setCancelled(true);
        }
    }
// RAMKI
    @EventHandler(priority = EventPriority.LOW)
    public void onHangingBreak(org.bukkit.event.hanging.HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.ItemFrame)) return;
        if (shouldCancel(player, event.getEntity().getLocation().getBlock(), IslandAction.ITEM_FRAME_INTERACT))
            event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
// Teleportacje — ender perła i chorus fruit
// -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        var cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) return;

        Island fromIsland = islandManager.getIslandAt(event.getFrom());
        Island toIsland   = islandManager.getIslandAt(event.getTo());

        if (fromIsland == null && toIsland == null) return;
        if (Objects.equals(fromIsland, toIsland)) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage("§cNie możesz teleportować się poza granicę wyspy!");
    }

// -------------------------------------------------------------------------
// Projektyle — strzały, trójząb przez granicę
// -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        if (event.getHitEntity() == null) return;

        Island shooterIsland = islandManager.getIslandAt(shooter.getLocation());
        Island targetIsland  = islandManager.getIslandAt(event.getHitEntity().getLocation());

        if (Objects.equals(shooterIsland, targetIsland)) return;

        event.setCancelled(true);
    }

// -------------------------------------------------------------------------
// Wędka — hookowanie bytów przez granicę
// -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!(event.getCaught() instanceof Entity caught)) return;

        Island playerIsland = islandManager.getIslandAt(event.getPlayer().getLocation());
        Island caughtIsland = islandManager.getIslandAt(caught.getLocation());

        if (Objects.equals(playerIsland, caughtIsland)) return;

        event.setCancelled(true);
    }

// -------------------------------------------------------------------------
// Tłoki — blokujemy przepychanie bloków między wyspami
// -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Island pistonIsland = islandManager.getIslandAt(event.getBlock().getLocation());
        for (Block block : event.getBlocks()) {
            Block dest = block.getRelative(event.getDirection());
            if (!Objects.equals(pistonIsland, islandManager.getIslandAt(dest.getLocation()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Island pistonIsland = islandManager.getIslandAt(event.getBlock().getLocation());
        for (Block block : event.getBlocks()) {
            Block dest = block.getRelative(event.getDirection());
            if (!Objects.equals(pistonIsland, islandManager.getIslandAt(dest.getLocation()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

// -------------------------------------------------------------------------
// Przepływ cieczy — woda/lawa nie może zalać sąsiedniej wyspy
// -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        if (!isIslandWorld(event.getBlock().getWorld())) return;
        Island fromIsland = islandManager.getIslandAt(event.getBlock().getLocation());
        Island toIsland   = islandManager.getIslandAt(event.getToBlock().getLocation());

        if (Objects.equals(fromIsland, toIsland)) return;

        event.setCancelled(true);
    }

// -------------------------------------------------------------------------
// Łóżko — blokujemy efekty snu na wyspach (spawn, robienie dnia)
// -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!isIslandWorld(event.getPlayer().getWorld())) return;
        event.setUseBed(Event.Result.DENY);
    }
}
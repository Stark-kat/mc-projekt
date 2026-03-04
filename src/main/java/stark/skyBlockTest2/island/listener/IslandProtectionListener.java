package stark.skyBlockTest2.island.listener;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;

public class IslandProtectionListener implements Listener {

    private final IslandManager islandManager;

    public IslandProtectionListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    private boolean shouldCancel(Player player, Block block, IslandAction type) {
        if (block == null) return false;
        Island islandAt = islandManager.getIslandAt(block.getLocation());
        if (islandAt == null) return false;

        if (islandAt.getOwner().equals(player.getUniqueId()) || islandAt.getMembers().contains(player.getUniqueId())) {
            return false;
        }

        if (islandAt.canVisitorDo(type)) return false;

        player.sendMessage("§cNie masz uprawnień do: " + type.getDisplayName());
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.BREAK_BLOCKS)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.PLACE_BLOCKS)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onFireSpread(BlockIgniteEvent event) {
        if (event.getPlayer() != null) {
            if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.FIRE_SPREAD)) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.USE_BUCKETS)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock(), IslandAction.USE_BUCKETS)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        Material mat = block.getType();

        if (event.getAction() == Action.PHYSICAL) {
            if (Tag.PRESSURE_PLATES.isTagged(mat)) {
                if (shouldCancel(player, block, IslandAction.USE_PRESSURE_PLATES)) event.setCancelled(true);
            }
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // 1. POJEMNIKI (Skrzynie, Beczki, Shulkery)
            if (isChest(mat)) {
                if (shouldCancel(player, block, IslandAction.OPEN_CONTAINERS)) event.setCancelled(true);
            }
            // 2. KOWADŁA
            else if (mat.name().contains("ANVIL")) {
                if (shouldCancel(player, block, IslandAction.USE_ANVIL)) event.setCancelled(true);
            }
            // 3. STÓŁ DO ENCHANTU
            else if (mat == Material.ENCHANTING_TABLE) {
                if (shouldCancel(player, block, IslandAction.USE_ENCHANTING)) event.setCancelled(true);
            }
            // 4. STÓŁ ALCHEMICZNY
            else if (mat == Material.BREWING_STAND) {
                if (shouldCancel(player, block, IslandAction.USE_BREWING)) event.setCancelled(true);
            }
            // 5. STÓŁ RZEMIEŚLNICZY (Crafting)
            else if (mat == Material.CRAFTING_TABLE) {
                if (shouldCancel(player, block, IslandAction.USE_CRAFTING)) event.setCancelled(true);
            }
            // 6. DRZWI I FURTKI
            else if (Tag.DOORS.isTagged(mat) || Tag.FENCE_GATES.isTagged(mat) || Tag.TRAPDOORS.isTagged(mat)) {
                if (shouldCancel(player, block, IslandAction.USE_DOORS)) event.setCancelled(true);
            }
            // 7. MECHANIZMY
            else if (Tag.BUTTONS.isTagged(mat) || mat == Material.LEVER) {
                if (shouldCancel(player, block, IslandAction.USE_BUTTONS)) event.setCancelled(true);
            }
            else if (mat == Material.FLOWER_POT) {
                if (shouldCancel(player, block, IslandAction.INTERACT_DECORATIONS)) event.setCancelled(true);
            }
            else if (mat == Material.LECTERN) {
                if (shouldCancel(player, block, IslandAction.INTERACT_UTILITY)) event.setCancelled(true);
            }
        }
    }

    private boolean isChest(Material mat) {
        String name = mat.name();
        return (name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER"))
                && mat != Material.ENDER_CHEST;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof org.bukkit.entity.ArmorStand || entity instanceof org.bukkit.entity.ItemFrame) {
            if (shouldCancel(event.getPlayer(), entity.getLocation().getBlock(), IslandAction.ARMOR_STAND_INTERACT)) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Block locBlock = entity.getLocation().getBlock();

        if (entity instanceof Villager) {
            if (shouldCancel(player, locBlock, IslandAction.VILLAGER_TRADE)) event.setCancelled(true);
        } else if (entity instanceof Animals) {
            ItemStack item = player.getInventory().getItemInMainHand();
            Material hand = item.getType();

            if (hand == Material.BUCKET) {
                if (shouldCancel(player, locBlock, IslandAction.MILK_COWS)) event.setCancelled(true);
            } else if (hand == Material.SHEARS) {
                if (shouldCancel(player, locBlock, IslandAction.SHEAR_SHEEP)) event.setCancelled(true);
            } else if (item.getType().isEdible() || hand == Material.WHEAT) {
                if (shouldCancel(player, locBlock, IslandAction.FEED_ANIMALS)) event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (event.getEntity() instanceof Animals || event.getEntity() instanceof Villager) {
                if (shouldCancel(player, event.getEntity().getLocation().getBlock(), IslandAction.KILL_ANIMALS)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCropTrample(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == Material.FARMLAND) {
            if (shouldCancel(event.getPlayer(), event.getClickedBlock(), IslandAction.CROP_TRAMPLE)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onExplosion(org.bukkit.event.entity.EntityExplodeEvent event) {
        // Czyścimy listę bloków do zniszczenia.
        // Wybuch nadal się odbywa i zadaje obrażenia, ale świat pozostaje nienaruszony.
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        // To samo dla wybuchów bloków (np. łóżko/kotwica odrodzenia)
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHangingDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        org.bukkit.entity.Entity entity = event.getEntity();

        // Sprawdzamy, czy to ramka, obraz lub stojak na zbroje
        if (entity instanceof org.bukkit.entity.Hanging || entity instanceof org.bukkit.entity.ArmorStand) {

            // Jeśli powodem obrażeń jest wybuch (blokowy lub jednostki)
            if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                    event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {

                event.setCancelled(true);
            }
        }
    }

    // --- ENUMY ---

    public enum ActionCategory {
        GENERAL("§6Ogólne", Material.GRASS_BLOCK),
        UTILITY("§eBloki Użytkowe", Material.CRAFTING_TABLE),
        REDSTONE("§cMechanizmy", Material.REDSTONE),
        MOBS("§aIstoty", Material.COW_SPAWN_EGG);

        private final String displayName;
        private final Material icon;

        ActionCategory(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
    }

    public enum IslandAction {
        // Ogólne
        BREAK_BLOCKS("Niszczenie", false, ActionCategory.GENERAL),
        PLACE_BLOCKS("Budowanie", false, ActionCategory.GENERAL),
        USE_BUCKETS("Używanie wiader", false, ActionCategory.GENERAL),
        FIRE_SPREAD("Podpalanie", false, ActionCategory.GENERAL),
        CROP_TRAMPLE("niszczenie upraw przez skok", false, ActionCategory.GENERAL),
        TELEPORT_VISIT("Odwiedziny", true, ActionCategory.GENERAL),

        // Bloki Użytkowe
        OPEN_CONTAINERS("Otwieranie skrzyń", false, ActionCategory.UTILITY),
        USE_CRAFTING("Stół rzemieślniczy", true, ActionCategory.UTILITY),
        USE_ANVIL("Używanie kowadeł", true, ActionCategory.UTILITY),
        USE_ENCHANTING("Stół do zaklęć", true, ActionCategory.UTILITY),
        USE_BREWING("Stół alchemiczny", true, ActionCategory.UTILITY),
        INTERACT_DECORATIONS("dekoracje", false, ActionCategory.UTILITY),
        INTERACT_UTILITY("ramki", false, ActionCategory.UTILITY),


        // Mechanizmy
        USE_DOORS("Drzwi i furtki", true, ActionCategory.REDSTONE),
        USE_BUTTONS("Dźwignie i przyciski", true, ActionCategory.REDSTONE),
        USE_PRESSURE_PLATES("Płytki naciskowe", true, ActionCategory.REDSTONE),

        // Istoty
        FEED_ANIMALS("Karmienie zwierząt", true, ActionCategory.MOBS),
        MILK_COWS("Dojenie krów", true, ActionCategory.MOBS),
        SHEAR_SHEEP("Strzyżenie owiec", true, ActionCategory.MOBS),
        KILL_ANIMALS("Zabijanie istot", false, ActionCategory.MOBS),
        ARMOR_STAND_INTERACT("Interakcja ze stojakiem", false, ActionCategory.UTILITY),
        VILLAGER_TRADE("Handel z Villagerami", true, ActionCategory.MOBS);

        private final String displayName;
        private final boolean editable;
        private final ActionCategory category;

        IslandAction(String displayName, boolean editable, ActionCategory category) {
            this.displayName = displayName;
            this.editable = editable;
            this.category = category;
        }

        public String getDisplayName() { return displayName; }
        public boolean isEditable() { return editable; }
        public ActionCategory getCategory() { return category; }
    }
}
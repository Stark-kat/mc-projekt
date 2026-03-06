package stark.skyBlockTest2.gui.item;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.island.IslandRole;

public class GuiItems {

    public static ItemStack closeButton() {
        return new ItemBuilder(Material.BARRIER)
                .name("§cZamknij")
                .setString("action", "CloseGui")
                .build();
    }

    public static ItemStack background() {
        return new ItemBuilder(Material.GLASS)
                .name(" ")
                .build();
    }

    public static ItemStack greenBackground() {
        return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name(" ")
                .build();
    }

    public static ItemStack grayBackground() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();
    }

    public static ItemStack spawn() {
        return new ItemBuilder(Material.ENDER_PEARL)
                .name("§bSpawn")
                .lore("§7Teleportuj się na spawn")
                .setString("action", "TeleportSpawn")
                .build();
    }

    public static ItemStack clock() {
        return new ItemBuilder(Material.CLOCK)
                .name("§eUstaw dzień")
                .lore("§7W dzień jest bezpieczniej!")
                .setString("action", "SetDay")
                .build();
    }

    public static ItemStack createIsland() {
        return new ItemBuilder(Material.COMPASS)
                .name("§aStwórz wyspę")
                .setString("action", "CreateIsland")
                .build();
    }

    public static ItemStack book() {
        return new ItemBuilder(Material.BOOK)
                .name("§eSkyBlock")
                .lore(
                        "§7Aby zagrać, stwórz wyspę.",
                        "§7Graj z przyjaciółmi,",
                        "§7pokonuj wyzwania",
                        "§7i wspinaj się na szczyty",
                        "§7rankingu! §aZapraszamy!"
                )
                .build();
    }

    public static ItemStack home() {
        return new ItemBuilder(Material.DIRT)
                .name("§aWyspa")
                .lore("§7Teleportuj się na swoją wyspę")
                .setString("action", "TeleportHome")
                .build();
    }

    public static ItemStack lvl1() {
        return new ItemBuilder(Material.DIRT)
                .name("setlvl 1")
                .setString("action", "lvl1")
                .build();
    }

    public static ItemStack lvl2() {
        return new ItemBuilder(Material.GRASS_BLOCK)
                .name("setlvl 2")
                .setString("action", "lvl2")
                .build();
    }

    public static ItemStack lvl3() {
        return new ItemBuilder(Material.GOLD_BLOCK)
                .name("setlvl 3")
                .setString("action", "lvl3")
                .build();
    }

    public static ItemStack lvl4() {
        return new ItemBuilder(Material.EMERALD_BLOCK)
                .name("setlvl 4")
                .setString("action", "lvl4")
                .build();
    }

    public static ItemStack lvl5() {
        return new ItemBuilder(Material.DIAMOND_BLOCK)
                .name("setlvl 5")
                .setString("action", "lvl5")
                .build();
    }

    public static ItemStack upgradeSizeLvl() {
        return new ItemBuilder(Material.DIAMOND_PICKAXE)
                .name("§bRozmiar wyspy")
                .lore("§7Kliknij, aby ulepszyć rozmiar")
                .setString("action", "menuSizeLvl")
                .build();
    }

    public static ItemStack islandSettings() {
        return new ItemBuilder(Material.COMPARATOR)
                .name("§eUstawienia wyspy")
                .lore("§7Zarządzaj uprawnieniami gości")
                .setString("action", "OpenSettings")
                .build();
    }

    public static ItemStack members() {
        return new ItemBuilder(Material.PLAYER_HEAD)
                .name("§aCzłonkowie")
                .lore("§7Zarządzaj członkami wyspy")
                .setString("action", "members")
                .build();
    }

    public static ItemStack islandHub() {
        return new ItemBuilder(Material.NETHERRACK)
                .name("§dMoje Wyspy")
                .lore("§7Zarządzaj wszystkimi swoimi wyspami")
                .setString("action", "OpenIslandHub")
                .build();
    }

    /**
     * Głowa gracza z podglądem roli.
     * Używana np. w MembersGui — kliknięcie otwiera MemberSettingsGui.
     */
    public static ItemStack playerHead(OfflinePlayer player, IslandRole role) {
        String roleLabel = role != null ? role.getDisplayName() : IslandRole.MEMBER.getDisplayName();
        String status    = player.isOnline() ? "§aOnline" : "§7Offline";

        return new ItemBuilder(Material.PLAYER_HEAD)
                .skull(player)
                .name(roleLabel + " §f" + (player.getName() != null ? player.getName() : "?"))
                .lore(
                        "§7Status: " + status,
                        "§8UUID: " + player.getUniqueId().toString().substring(0, 8)
                )
                .setString("target_uuid", player.getUniqueId().toString())
                .setString("action", "MemberInfo")
                .build();
    }

    /**
     * Stara wersja zachowana dla kompatybilności — deleguje do nowej.
     */
    public static ItemStack playerHead(OfflinePlayer player, boolean isOwner) {
        IslandRole role = isOwner ? IslandRole.OWNER : IslandRole.MEMBER;
        return playerHead(player, role);
    }
}
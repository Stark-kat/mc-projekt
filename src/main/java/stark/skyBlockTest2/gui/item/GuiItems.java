package stark.skyBlockTest2.gui.item;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.gui.builder.ItemBuilder;

public class GuiItems {



    public static ItemStack closeButton() {
        return new ItemBuilder(Material.BARRIER)
                .name("Close")
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
                .name("Spawn")
                .lore("teleport to spawn")
                .setString("action", "TeleportSpawn")
                .build();
    }
    public static ItemStack clock() {
        return new ItemBuilder(Material.CLOCK)
                .name("Set Day")
                .lore("it's safer during the day")
                .setString("action", "SetDay")
                .build();
    }
    public static ItemStack createIsland() {
        return new ItemBuilder(Material.COMPASS)
                .name("Create Island")
                .setString("action", "CreateIsland")
                .build();
    }
    public static ItemStack book() {
        return new ItemBuilder(Material.BOOK)
                .name("żeby zagrać musisz stworzyć wyspe")
                .lore("stwórz wyspe",
                        "graj z przyjaciółmi",
                        "pokonuj przeszkody",
                        "i wspinaj sie na najwyższe ",
                        "miejsca podiebnych wysep",
                        "Zapraszamy!")
                .build();
    }
    public static ItemStack home() {
        return new ItemBuilder(Material.DIRT)
                .name("Island home")
                .setString("action", "TeleportHome")
                .build();
    }
    public static ItemStack lvl1() {
        return new ItemBuilder(Material.DIRT)
                .name("setlvl 1")
                .setString("action" ,"lvl1")
                .build();
    }
    public static ItemStack lvl2() {
        return new ItemBuilder(Material.GRASS_BLOCK)
                .name("setlvl 2")
                .setString("action" ,"lvl2")
                .build();
    }
    public static ItemStack lvl3() {
        return new ItemBuilder(Material.GOLD_BLOCK)
                .name("setlvl 3")
                .setString("action" ,"lvl3")
                .build();
    }
    public static ItemStack lvl4() {
        return new ItemBuilder(Material.EMERALD_BLOCK)
                .name("setlvl 4")
                .setString("action" ,"lvl4")
                .build();
    }
    public static ItemStack lvl5() {
        return new ItemBuilder(Material.DIAMOND_BLOCK)
                .name("setlvl 5")
                .setString("action" ,"lvl5")
                .build();
    }
    public static ItemStack upgradeSizeLvl() {
        return new ItemBuilder(Material.DIAMOND_PICKAXE)
                .name("Upgrade size lvl")
                .setString("action" , "menuSizeLvl")
                .build();
    }
    public static ItemStack islandSettings() {
        return new ItemBuilder(Material.DIAMOND_BLOCK)
                .name("Island Settings")
                .setString("action", "OpenSettings")
                .build();
    }
    public static ItemStack members() {
        return new ItemBuilder(Material.STONE)
                .name("Members")
                .setString("action", "members")
                .build();
    }
    public static ItemStack playerHead(OfflinePlayer player, boolean isOwner) {
        String role = isOwner ? "§6§lLIDER" : "§fCzłonek";
        String status = player.isOnline() ? "§aOnline" : "§cOffline";

        return new ItemBuilder(Material.PLAYER_HEAD)
                .skull(player) // Upewnij się, że masz metodę .skull() w ItemBuilder!
                .name(role + " §e" + player.getName())
                .lore(
                        "§7Status: " + status,
                        "",
                        "§8UUID: " + player.getUniqueId().toString().substring(0, 8)
                )
                // Możemy tu dodać akcję, jeśli będziesz chciał np. klikać by
                .setString("target_uuid", player.getUniqueId().toString())
                .setString("action", "MemberInfo")
                .build();
    }
}

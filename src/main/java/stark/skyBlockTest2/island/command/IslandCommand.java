package stark.skyBlockTest2.island.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.gui.menu.CreateIslandGui;
import stark.skyBlockTest2.gui.menu.MenuGui;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;

public class IslandCommand implements CommandExecutor {

    private final IslandManager islandManager;
    private final MenuGui menuGui;
    private final CreateIslandGui createIslandGui;

    public IslandCommand(IslandManager islandManager, MenuGui menuGui, CreateIslandGui createIslandGui) {
        this.islandManager = islandManager;
        this.menuGui = menuGui;
        this.createIslandGui = createIslandGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko gracz może użyć tej komendy.");
            return true;
        }

        if (args.length == 0) {
            if (!islandManager.hasIsland(player.getUniqueId())) {
                createIslandGui.open(player);
            }else {
                menuGui.open(player);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "create" -> islandManager.createIsland(player);
            case "home" -> islandManager.teleportHome(player);
            case "sethome" -> islandManager.setHome(player);
            case "repairhome" -> {
                Island island = islandManager.getIsland(player.getUniqueId());

                if (island == null) {
                    player.sendMessage("§cNie należysz do żadnej wyspy!");
                    return true;
                }
                if (!islandManager.isOwner(player.getUniqueId())) {
                    player.sendMessage("§cTylko lider może naprawić punkt domowy!");
                    return true;
                }
                islandManager.repairHomeToNearest(island);
                player.sendMessage("§aPunkt domowy został naprawiony!");
            }
            case "delete" -> islandManager.deleteIsland(player);
            case "upgrade" -> islandManager.upgradeIslandSize(player);
            case "setlvl" -> {
                // 1. Sprawdzamy czy jest args[1], bo args[0] to "setlvl"
                if (args.length < 2) {
                    player.sendMessage("§cPoprawne użycie: /is setlvl <1-5>");
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[1]);

                    if (level < 1 || level > 5) {
                        player.sendMessage("§cPoziom musi być z zakresu 1-5!");
                        return true;
                    }

                    if (!islandManager.hasIsland(player.getUniqueId())) {
                        player.sendMessage("§cNie masz wyspy!");
                        return true;
                    }

                    islandManager.setIslandLevel(player, level);
                    player.sendMessage("§aUstawiono poziom " + level + " dla Twojej wyspy.");

                } catch (NumberFormatException e) {
                    player.sendMessage("§cArgument musi być liczbą!");
                }
            }
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage("§cPoprawne użycie: /island invite <nick>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§cGracz jest offline.");
                    return true;
                }
                islandManager.sendInvite(player, target);
            }
            case "accept" -> islandManager.acceptInvite(player);
            case "decline", "reject" -> islandManager.declineInvite(player);
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUżycie: /island kick <nick>");
                    return true;
                }

                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);

                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    player.sendMessage("§cTen gracz nigdy nie grał na tym serwerze!");
                    return true;
                }
                islandManager.kickMember(player, target.getUniqueId());
            }

            case "leave" -> islandManager.leaveIsland(player);
            case "members", "list" -> islandManager.showMembers(player);
            case "setleader", "transfer" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUżycie: /island setleader <nick_członka>");
                    return true;
                }
                org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);

                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    player.sendMessage("§cTen gracz nigdy nie był na serwerze!");
                    return true;
                }

                islandManager.transferOwnership(player, target.getUniqueId());
            }
            case "visit" -> {
                if (args.length < 2) {
                    player.sendMessage("§cPoprawne użycie: /is visit <nick>");
                    return true;
                }
                islandManager.teleportToIsland(player, args[1]);
            }
            default -> player.sendMessage("§cNieznana podkomenda.");
        }

        return true;
    }
}
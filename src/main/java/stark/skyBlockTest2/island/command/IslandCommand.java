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
import stark.skyBlockTest2.island.IslandType;

public class IslandCommand implements CommandExecutor {

    private final SkyBlockTest2 plugin;
    private final IslandManager islandManager;
    private final MenuGui menuGui;
    private final CreateIslandGui createIslandGui;

    public IslandCommand(SkyBlockTest2 plugin, IslandManager islandManager, MenuGui menuGui, CreateIslandGui createIslandGui) {
        this.plugin          = plugin;
        this.islandManager   = islandManager;
        this.menuGui         = menuGui;
        this.createIslandGui = createIslandGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("skyblock.admin") && !sender.isOp()) {
                sender.sendMessage("§cNie masz uprawnień do tej komendy.");
                return true;
            }
            plugin.reloadConfig();
            plugin.getShopManager().reload();
            plugin.getCrateManager().reload();
            plugin.getQuestManager().reload();
            plugin.getGeneratorManager().reload();
            plugin.getEconomyManager().reload();
            plugin.getAuctionManager().reload();
            sender.sendMessage("§a[SkyBlock] Przeladowano config.yml, shop.yml, loot_crates.yml, questy, osiagniecia, generator.yml, ekonomie i aukcje.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko gracz może użyć tej komendy.");
            return true;
        }

        if (args.length == 0) {
            if (!islandManager.hasIsland(player.getUniqueId())) {
                createIslandGui.open(player);
            } else {
                menuGui.open(player);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "create"   -> islandManager.createIsland(player);
            case "home"     -> islandManager.teleportHome(player);
            case "nether"   -> islandManager.teleportHome(player, IslandType.NETHER);
            case "end"      -> islandManager.teleportHome(player, IslandType.END);
            case "cave" -> islandManager.teleportHome(player, IslandType.CAVE);

            case "sethome" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("nether")) {
                    islandManager.setHome(player, IslandType.NETHER);
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("end")) {
                    islandManager.setHome(player, IslandType.END);
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("cave")) {
                    islandManager.setHome(player, IslandType.CAVE);
                } else {
                    islandManager.setHome(player);
                }
            }

            case "delete" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                    islandManager.deleteIsland(player);
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("cancel")) {
                    islandManager.cancelDeleteIsland(player);
                } else {
                    islandManager.requestDeleteIsland(player);
                }
            }
            case "upgrade"  -> islandManager.upgradeIslandSize(player);

            case "setlvl" -> {
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

            case "accept"           -> islandManager.acceptInvite(player);
            case "decline", "reject" -> islandManager.declineInvite(player);

            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUżycie: /island kick <nick>");
                    return true;
                }
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
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
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
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

            // -----------------------------------------------------------------
            // Banowanie
            // -----------------------------------------------------------------

            case "ban" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUżycie: /island ban <nick>");
                    return true;
                }
                islandManager.banPlayer(player, args[1]);
            }

            case "unban", "pardon" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUżycie: /island unban <nick>");
                    return true;
                }
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    player.sendMessage("§cTen gracz nigdy nie grał na tym serwerze!");
                    return true;
                }
                islandManager.unbanPlayer(player, target.getUniqueId());
            }

            // -----------------------------------------------------------------
            // Role
            // -----------------------------------------------------------------

            case "setcoleader", "coleader" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUżycie: /island setcoleader <nick_członka>");
                    return true;
                }
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    player.sendMessage("§cTen gracz nigdy nie grał na tym serwerze!");
                    return true;
                }
                islandManager.setCoLeader(player, target.getUniqueId());
            }

            // -----------------------------------------------------------------
            // Komendy administracyjne / testowe
            // -----------------------------------------------------------------

            case "givexp" -> {
                if (!player.hasPermission("skyblock.admin") && !player.isOp()) {
                    player.sendMessage("§cNie masz uprawnień do tej komendy.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUżycie: /is givexp <ilość>");
                    return true;
                }
                Island island = islandManager.getIsland(player.getUniqueId());
                if (island == null) {
                    player.sendMessage("§cNie masz wyspy!");
                    return true;
                }
                try {
                    long amount = Long.parseLong(args[1]);
                    if (amount <= 0) {
                        player.sendMessage("§cIlość XP musi być większa od 0.");
                        return true;
                    }
                    plugin.getQuestManager().grantXp(island, amount);
                    player.sendMessage("§aDodano §f" + amount + " §aXP wyspy.");
                } catch (NumberFormatException e) {
                    player.sendMessage("§cArgument musi być liczbą.");
                }
            }

            case "setislandlvl" -> {
                if (!player.hasPermission("skyblock.admin") && !player.isOp()) {
                    player.sendMessage("§cNie masz uprawnień do tej komendy.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUżycie: /is setislandlvl <poziom>");
                    return true;
                }
                Island island = islandManager.getIsland(player.getUniqueId());
                if (island == null) {
                    player.sendMessage("§cNie masz wyspy!");
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[1]);
                    if (level < 1) {
                        player.sendMessage("§cPoziom musi być większy od 0.");
                        return true;
                    }
                    plugin.getQuestManager().setIslandXpLevel(island, level);
                    player.sendMessage("§aUstawiono XP wyspy na poziom §f" + level + "§a.");
                } catch (NumberFormatException e) {
                    player.sendMessage("§cArgument musi być liczbą.");
                }
            }

            default -> player.sendMessage("§cNieznana podkomenda. Użyj /island aby otworzyć menu.");
        }

        return true;
    }
}
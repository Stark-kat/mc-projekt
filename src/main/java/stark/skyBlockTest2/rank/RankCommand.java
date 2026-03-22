package stark.skyBlockTest2.rank;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * /rank set <nick> <ranga> [dni]
 * /rank remove <nick>
 * /rank info <nick>
 *
 * Wymagane uprawnienie: skyblock.rank.admin
 * (lub op jeśli nie używasz permission managera)
 */
public class RankCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "skyblock.rank.admin";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private final RankManager rankManager;

    public RankCommand(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {

        if (!sender.hasPermission(PERMISSION) && !sender.isOp()) {
            sender.sendMessage("§cBrak uprawnień.");
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "set"    -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "info"   -> handleInfo(sender, args);
            default       -> { sendHelp(sender); yield true; }
        };
    }

    // -------------------------------------------------------------------------
    // /rank set <nick> <ranga> [dni]
    // -------------------------------------------------------------------------

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUżycie: /rank set <nick> <ranga> [dni]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            sender.sendMessage("§cNie znaleziono gracza: §f" + args[1]);
            return true;
        }

        Rank rank = Rank.fromString(args[2]);
        if (rank == Rank.PLAYER && !args[2].equalsIgnoreCase("PLAYER")) {
            sender.sendMessage("§cNieznana ranga: §f" + args[2] + "§c. Dostępne: PLAYER, VIP, ADMIN");
            return true;
        }

        String grantedBy = sender instanceof Player p ? p.getName() : "CONSOLE";
        UUID uuid = target.getUniqueId();

        if (args.length >= 4) {
            // Tryb czasowy
            int days;
            try {
                days = Integer.parseInt(args[3]);
                if (days <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage("§cLiczba dni musi być dodatnią liczbą całkowitą.");
                return true;
            }
            rankManager.setRankForDays(uuid, rank, grantedBy, days);
            sender.sendMessage("§aUstawiono rangę §6" + rank.getDisplayName()
                    + "§a dla §f" + target.getName() + "§a na §f" + days + "§a dni.");
        } else {
            // Bezterminowo
            rankManager.setRank(uuid, rank, grantedBy);
            sender.sendMessage("§aUstawiono rangę §6" + rank.getDisplayName()
                    + "§a dla §f" + target.getName() + "§a bezterminowo.");
        }

        // Powiadom gracza jeśli online
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            online.sendMessage("§aOtrzymałeś rangę §6" + rank.getDisplayName() + "§a!");
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // /rank remove <nick>
    // -------------------------------------------------------------------------

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUżycie: /rank remove <nick>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            sender.sendMessage("§cNie znaleziono gracza: §f" + args[1]);
            return true;
        }

        rankManager.removeRank(target.getUniqueId());
        sender.sendMessage("§aUsunięto rangę gracza §f" + target.getName() + "§a.");

        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            online.sendMessage("§7Twoja ranga została usunięta.");
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // /rank info <nick>
    // -------------------------------------------------------------------------

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUżycie: /rank info <nick>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            sender.sendMessage("§cNie znaleziono gracza: §f" + args[1]);
            return true;
        }

        UUID uuid = target.getUniqueId();
        RankDatabase.RankEntry entry = rankManager.getRankEntry(uuid);

        sender.sendMessage("§7--- Ranga gracza §f" + target.getName() + " §7---");

        if (entry == null) {
            sender.sendMessage("§7Ranga: §fGracz (domyślna)");
        } else {
            sender.sendMessage("§7Ranga: §f" + entry.rank().getDisplayName());
            sender.sendMessage("§7Przyznał: §f" + (entry.grantedBy() != null ? entry.grantedBy() : "—"));
            sender.sendMessage("§7Przyznano: §f" + DATE_FORMAT.format(new Date(entry.grantedAt())));

            if (entry.expiresAt() != null) {
                if (entry.isExpired()) {
                    sender.sendMessage("§7Wygasła: §c" + DATE_FORMAT.format(new Date(entry.expiresAt())) + " (wygasła)");
                } else {
                    sender.sendMessage("§7Wygasa: §f" + DATE_FORMAT.format(new Date(entry.expiresAt())));
                }
            } else {
                sender.sendMessage("§7Wygasa: §fNigdy (bezterminowa)");
            }
        }

        return true;
    }

    // -------------------------------------------------------------------------

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6/rank set <nick> <ranga> [dni]§7 - ustaw rangę");
        sender.sendMessage("§6/rank remove <nick>§7 - usuń rangę");
        sender.sendMessage("§6/rank info <nick>§7 - sprawdź rangę");
        sender.sendMessage("§7Dostępne rangi: §fPLAYER, VIP, ADMIN");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("set", "remove", "info");
        if (args.length == 3 && args[0].equalsIgnoreCase("set"))
            return Arrays.asList("PLAYER", "VIP", "ADMIN");
        return List.of();
    }
}
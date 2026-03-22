package stark.skyBlockTest2.island.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class IslandTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "home", "sethome", "repairhome", "delete",
            "upgrade", "setlvl",
            "invite", "accept", "decline", "kick", "leave",
            "members", "list",
            "setleader", "transfer",
            "visit",
            "ban", "unban", "pardon",
            "setcoleader", "coleader",
            "givexp", "setislandlvl"
    );

    // Subkomendy wymagające nicku gracza jako args[1]
    private static final List<String> NEEDS_PLAYER = List.of(
            "invite", "kick", "setleader", "transfer", "visit",
            "ban", "unban", "pardon", "setcoleader", "coleader"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();

        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(typed))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && NEEDS_PLAYER.contains(args[0].toLowerCase())) {
            String typed = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(typed))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
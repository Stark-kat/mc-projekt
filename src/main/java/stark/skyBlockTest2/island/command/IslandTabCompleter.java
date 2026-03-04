package stark.skyBlockTest2.island.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IslandTabCompleter implements TabCompleter {

    private final List<String> subCommands = Arrays.asList(
            "create", "home", "sethome", "repairhome", "delete", "upgrade",
            "invite", "accept", "decline", "kick", "leave", "members", "setleader", "setlvl", "visit"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Podpowiadaj główne podkomendy
            return StringUtil.copyPartialMatches(args[0], subCommands, new ArrayList<>());
        } else if (args.length == 2) {
            // Podpowiadaj graczy online dla konkretnych komend
            if (args[0].equalsIgnoreCase("invite") ||
                    args[0].equalsIgnoreCase("kick") ||
                    args[0].equalsIgnoreCase("setleader")) {
                return null; // Zwrócenie null w Bukkit domyślnie podpowiada graczy online
            }
        }

        return Collections.emptyList();
    }
}

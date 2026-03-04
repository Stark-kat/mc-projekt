    package stark.skyBlockTest2.island.command;

    import org.bukkit.command.Command;
    import org.bukkit.command.CommandExecutor;
    import org.bukkit.command.CommandSender;
    import org.bukkit.entity.Player;
    import stark.skyBlockTest2.SkyBlockTest2;

    public class IslandLvlCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cTa komenda jest tylko dla graczy!");
                    return true;
                }
                if (args.length == 0) {
                    player.sendMessage("§cPoprawne użycie: /ustawpoziom <1-5>");
                    return true;
                }

                try {
                    int level = Integer.parseInt(args[0]);
                    if (level < 1 || level > 5) {
                        player.sendMessage("§cPoziom musi być z zakresu 1-5!");
                        return true;
                    }

                    SkyBlockTest2.getInstance().getIslandManager().setIslandLevel(player, level);
                    player.sendMessage("§aUstawiono poziom " + level + " dla Twojej wyspy.");

                } catch (NumberFormatException e) {
                    player.sendMessage("§cArgument musi być liczbą!");
                }
                return true;
            }
        }

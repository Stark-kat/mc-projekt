package stark.skyBlockTest2.util;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class JoinMessage implements Listener {

    private final CounterMenager counterMenager;

    public JoinMessage(CounterMenager counterMenager) {
        this.counterMenager = counterMenager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        String msg;

        if(player.hasPlayedBefore()){
            msg= "§aMiło Cię znowu widzieć, §f" + player.getName() + "§a!";
        } else {
            msg= "§6Witaj §f" + player.getName() + "§6! Jesteś tu pierwszy raz 🎉";
            counterMenager.add("UnikalniGracze");
        }
        player.sendMessage(msg);
    }
}

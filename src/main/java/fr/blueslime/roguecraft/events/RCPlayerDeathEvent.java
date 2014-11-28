package fr.blueslime.roguecraft.events;

import fr.blueslime.roguecraft.RogueCraft;
import fr.blueslime.roguecraft.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class RCPlayerDeathEvent implements Listener
{
    @EventHandler
    public void event(PlayerDeathEvent event)
    {
        Player deadPlayer = (Player) event.getEntity();
        Arena arena = RogueCraft.getPlugin().getArena();
        
        event.setDeathMessage(null);
        
        if(arena.isGameStarted())
        {
            if(arena.hasPlayer(deadPlayer.getUniqueId()))
            {
                if(deadPlayer.isDead())
                {
                    event.setKeepLevel(false);
                    event.setDroppedExp(0);

                    for (ItemStack i : deadPlayer.getInventory().getContents())
                    {
                        event.getDrops().remove(i);
                    }

                    for (ItemStack i : deadPlayer.getInventory().getArmorContents())
                    {
                        event.getDrops().remove(i);
                    }

                    event.getDrops().clear();
                    arena.lose(deadPlayer);
                }
            }
        }
    }
}

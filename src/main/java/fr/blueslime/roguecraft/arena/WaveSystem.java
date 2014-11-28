package fr.blueslime.roguecraft.arena;

import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import fr.blueslime.roguecraft.Messages;
import fr.blueslime.roguecraft.arena.Wave.WaveType;
import fr.blueslime.roguecraft.monsters.boss.BasicBoss;
import fr.blueslime.roguecraft.monsters.BasicMonster;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import net.zyuiop.MasterBundle.StarsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class WaveSystem
{
    private final Arena arena;
    private WaveTimer waveTimer;
    private EndWaveTimer endWaveTimer;
    
    public WaveSystem(Arena arena)
    {
        this.arena = arena;
    }
    
    public void next()
    {
        endCallback();
        this.arena.upWaveCount();
        
        if(this.endWaveTimer != null)
        {
            this.endWaveTimer.end();
            this.endWaveTimer = null;
        }
        
        for(Player player : Bukkit.getOnlinePlayers())
        {
            if(this.arena.hasPlayer(player.getUniqueId()))
                this.arena.getPlayer(player).repearStuff();
            
            player.teleport(new Location(this.arena.getWorld(), 0.0D, 250.0D, 0.0D));
        }
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Selecting random area...");
        this.arena.broadcastMessage(Messages.preparingArea);
        
        WaveType waveType = WaveType.NORMAL;
        
        if(this.arena.getWaveCount() % 10 == 0)
            waveType = WaveType.BOSS;
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Wave type is: " + waveType.name().toUpperCase());
        
        ArrayList<Area> areas = this.arena.getAreasByType(waveType);
        Collections.shuffle(areas, new Random(System.nanoTime()));
        Area area = areas.get(0);
        
        Wave wave = new Wave(this.arena, waveType, this.arena.getWaveCount(), area);
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Creating mob list...");
        
        Random rand = new Random();
        
        if(waveType == WaveType.NORMAL)
        {
            int mobsCount = (arena.getWaveCount() + (rand.nextInt(3) + 1)) * (arena.getActualPlayers() / (int) 2.42);
            ArrayList<BasicMonster> monsters = this.arena.getLogicRandomizer().prepareMobs(this.arena, mobsCount);

            for(BasicMonster monster : monsters)
            {
                wave.registerMob(monster);
            }
        }
        else
        {
            BasicBoss boss = this.arena.getLogicRandomizer().prepareBoss(this.arena);
            wave.registerBoss(boss);
        }
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Mob list created!");
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Teleporting players...");
        
        for(Player player : Bukkit.getOnlinePlayers())
        {
            player.getPlayer().getPlayer().teleport(area.getPlayersSpawn());
        }
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Starting countdown...");
        
        this.arena.setActualArea(area);
        this.arena.setWave(wave);
        
        for (ArenaPlayer player : this.arena.getArenaPlayers())
        {
            player.updateScoreboard();
        }
        
        this.waveTimer = new WaveTimer(this.arena);
        this.waveTimer.start();
        
        this.arena.broadcastMessage(Messages.waveStarting);
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Wave generated, end of work! Time to sleep :D");
    }
    
    public void start()
    {
        Wave wave = this.arena.getWave();
        
        if(this.waveTimer != null)
        {
            this.waveTimer.end();
            this.waveTimer = null;
        }
        
        if(wave.getWaveType() == WaveType.BOSS)
        {
            wave.getBoss().spawnMob(this.arena, wave.getWaveArea().getMobSpawns().get(0), this.arena.getWaveCount());
        }
        else
        {
            int partitionSize = IntMath.divide(wave.getMonsters().size(), wave.getWaveArea().getMobSpawns().size(), RoundingMode.UP);
            List<List<BasicMonster>> partitions = Lists.partition(wave.getMonsters(), partitionSize);
            
            for(int i = 0; i < partitions.size(); i++)
            {
                List<BasicMonster> monsterList = partitions.get(i);
                
                for(BasicMonster monster : monsterList)
                {
                    monster.spawnMob(this.arena, wave.getWaveArea().getMobSpawns().get(i), this.arena.getWaveCount());
                }
            }
        }
        
        if(wave.getWaveType() == WaveType.NORMAL)
        {
            for(BonusChest chestLocation : wave.getWaveArea().getBonusChestSpawns())
            {
                Block block = this.arena.getWorld().getBlockAt(chestLocation.getLocation());
                block.setType(Material.CHEST);
                Chest chest = (Chest) block.getState();
                
                org.bukkit.material.Chest materialChest = new org.bukkit.material.Chest(Material.CHEST);
                materialChest.setFacingDirection(chestLocation.getFace());
                chest.setData(materialChest);
                
                Inventory chestInv = chest.getBlockInventory();

                HashMap<Integer, ItemStack> items = this.arena.getChestLootsRandomizer().randomLootsInSlots(false);

                for(int slot : items.keySet())
                {
                    chestInv.setItem(slot, items.get(slot));
                }
            }
        }
        
        for (ArenaPlayer player : this.arena.getArenaPlayers())
        {
            player.updateScoreboard();
        }
        
        this.arena.broadcastMessage(Messages.waveStarted);
    }
    
    public void end()
    {
        this.arena.broadcastMessage(Messages.waveEnded);
        
        Wave wave = this.arena.getWave();
        
        if(wave.getWaveType() == WaveType.BOSS)
        {
            for(BonusChest chestLocation : wave.getWaveArea().getBonusChestSpawns())
            {
                Block block = this.arena.getWorld().getBlockAt(chestLocation.getLocation());
                block.setType(Material.CHEST);
                Chest chest = (Chest) block.getState();
                
                org.bukkit.material.Chest materialChest = new org.bukkit.material.Chest(Material.CHEST);
                materialChest.setFacingDirection(chestLocation.getFace());
                chest.setData(materialChest);
                
                Inventory chestInv = chest.getBlockInventory();
                
                HashMap<Integer, ItemStack> items = this.arena.getChestLootsRandomizer().randomLootsInSlots(true);

                for(int slot : items.keySet())
                {
                    chestInv.setItem(slot, items.get(slot));
                }
            }
        }
        
        for(ArenaPlayer player : this.arena.getArenaPlayers())
        {            
            if(this.arena.getWave().getWaveType() == WaveType.NORMAL)
            {
                player.addCoins(2);
                player.addXP(50);
            }
            else
            {
                StarsManager.creditJoueur(player.getPlayerID(), 1, "Mort d'un boss");
                player.addCoins(5);
                player.addXP(100);
            }
        }
        
        if(this.arena.getWave().getWaveType() == WaveType.NORMAL)
        {
            this.endWaveTimer = new EndWaveTimer(this.arena, 5L);
        }
        else
        {
            this.endWaveTimer = new EndWaveTimer(this.arena, 15L);
        }
        this.endWaveTimer.start();
    }
    
    public void endCallback()
    {
        if(this.arena.getWave() != null)
        {
            for(BonusChest chestLocation : this.arena.getWave().getWaveArea().getBonusChestSpawns())
            {
                Block block = this.arena.getWorld().getBlockAt(chestLocation.getLocation());
                Chest chest = (Chest) block.getState();
                Inventory chestInv = chest.getBlockInventory();
                chestInv.clear();
                block.setType(Material.AIR);
            }
        }
    }
    
    public boolean isFinished()
    {
        return this.arena.getWave().getMonstersLeft() == 0;
    }
}

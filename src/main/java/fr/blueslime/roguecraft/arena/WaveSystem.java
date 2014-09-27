package fr.blueslime.roguecraft.arena;

import fr.blueslime.roguecraft.RogueCraft;
import fr.blueslime.roguecraft.arena.Wave.WaveType;
import fr.blueslime.roguecraft.monsters.BasicBoss;
import fr.blueslime.roguecraft.monsters.BasicMonster;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.bukkit.Bukkit;

public class WaveSystem
{
    private final Arena arena;
    private WaveTimer waveTimer;
    
    public WaveSystem(Arena arena)
    {
        this.arena = arena;
    }
    
    public void next()
    {
        /**
         * 
         * TODO: Teleporter les joueurs dans une salle en bedrock d'attente pendant la génération 
         * 
         */
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Starting generating...");
        
        WaveType waveType = WaveType.NORMAL;
        
        if(arena.getWaveCount() % 10 == 0)
            waveType = WaveType.BOSS;
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Wave type is: " + waveType.name().toUpperCase());
        
        File schematicsFolder = new File(RogueCraft.getPlugin().getDataFolder() + "schematics" + File.separator + this.arena.getMapName());
        File[] schematics = schematicsFolder.listFiles();
        
        ArrayList<File> wantedSchematics = new ArrayList<>();
        
        for(File file : schematics)
        {
            if(waveType == WaveType.BOSS && file.getName().startsWith("boss_"))
                wantedSchematics.add(file);
            else if(waveType == WaveType.NORMAL && file.getName().startsWith("normal_"))
                wantedSchematics.add(file);
        }
        
        Collections.shuffle(wantedSchematics, new Random(System.nanoTime()));
        File schematic = wantedSchematics.get(0);
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Schematic selected is: " + schematic.getAbsolutePath());
        
        /**
         * 
         * TODO: Detruire la salle actuelle (si première alors détruire spawn)
         * 
         */
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Building area in world...");
        
        /**
         * 
         * TODO: Placer le schematic
         * 
         */
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Area builded in world!");
        
        Area area = null; // Faire l'object avec les blocs posés via le schematics;
        Wave wave = new Wave(waveType, arena.getWaveCount(), area);
        RandomizerLogic randLogin = new RandomizerLogic();
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Creating mob list...");
        
        if(waveType == WaveType.NORMAL)
        {
            ArrayList<BasicMonster> monsters = randLogin.prepareMobs(arena);

            for(BasicMonster monster : monsters)
            {
                wave.registerMob(monster);
            }
        }
        else
        {
            BasicBoss boss = randLogin.prepareBoss(arena);
            wave.registerBoss(boss);
        }
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Mob list created!");
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Teleporting players...");
        
        for(ArenaPlayer player : arena.getActualPlayersList())
        {
            player.getPlayer().getPlayer().teleport(area.getPlayersSpawn().add(0.0D, 1.0D, 1.0D));
        }
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Starting countdown...");
        
        arena.setWave(wave);
        this.waveTimer = new WaveTimer(arena);
        this.waveTimer.start();
        
        Bukkit.getLogger().info("[RogueCraft-WaveSystem] Wave generated, end of work! Time to sleep :D");
    }
    
    public void start()
    {
        Wave wave = arena.getWave();
        
        if(this.waveTimer != null)
        {
            this.waveTimer.end();
        }
        
        if(wave.getWaveType() == WaveType.BOSS)
        {
            wave.getBoss().spawnMob(wave.getWaveArea().getMobSpawns().get(0), arena.getWaveCount());
        }
        else
        {
            /**
             * 
             * TODO: Séparer la liste des monstres en parties (selon le nombre de points de spawns)
             * 
             */
        }
    }
    
    public boolean isFinished()
    {
        return arena.getWave().getMonstersLeft() == 0;
    }
}
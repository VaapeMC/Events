package me.vaape.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import net.md_5.bungee.api.ChatColor;

public class Events extends JavaPlugin implements Listener{
	
	public static Events plugin;
	static WorldGuardPlugin worldGuard = getWorldGuard();
	static WorldEditPlugin worldEdit = getWorldEdit();
	
	public static ArrayList<String> gw = new ArrayList<>();
	public static ArrayList<String> fish = new ArrayList<>();
	public static ArrayList<String> off = new ArrayList<>();
	
	public static boolean gwRunning = false;
	public static boolean fishRunning = false;
	
	public void onEnable() {
		plugin = this;
		loadConfiguration();
		getLogger().info(ChatColor.GREEN + "Events have been enabled!");
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(new GuildWars(this), this);
		getServer().getPluginManager().registerEvents(new CrateDrops(this), this);
		getServer().getPluginManager().registerEvents(new Fishing(this), this);
		
		plugin.getCommand("guildwars").setExecutor(new GuildWars(this));
		plugin.getCommand("gw").setExecutor(new GuildWars(this));
		plugin.getCommand("guildwarsstart").setExecutor(new GuildWars(this));
		plugin.getCommand("gwstart").setExecutor(new GuildWars(this));
		plugin.getCommand("guildwarsend").setExecutor(new GuildWars(this));
		plugin.getCommand("gwend").setExecutor(new GuildWars(this));
		plugin.getCommand("guildwarsrefill").setExecutor(new GuildWars(this));
		plugin.getCommand("gwrefill").setExecutor(new GuildWars(this));
		
		plugin.getCommand("fstart").setExecutor(new Fishing(this));
		plugin.getCommand("fishstart").setExecutor(new Fishing(this));
		plugin.getCommand("fishing").setExecutor(new Fishing(this));
		plugin.getCommand("fish").setExecutor(new Fishing(this));
		
		plugin.getCommand("dropcrate").setExecutor(new CrateDrops(this));
		
		CrateDrops.scheduleNextCrateDrop(60);
		
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { //The server will restart daily so this will occur daily
			
			@Override
			public void run() {
				GuildWars.canUpgrade = true;
				Bukkit.broadcastMessage(ChatColor.GOLD + "[Guild Wars] " + ChatColor.BLUE + "The castle can now be upgraded.");
				GuildWars.refillLoot();
			}
		},1 * 60 * 60 * 20); //1 hour
	}
	
	public void onDisable(){
		plugin = null;
	  }
	
	public static Events getInstance() {
		return plugin;
	}
	
	public void loadConfiguration() {
		final FileConfiguration config = this.getConfig();
		config.addDefault("defenders", "Myst");
		config.options().copyDefaults(true);
		saveConfig();
	}
	
	public static WorldGuardPlugin getWorldGuard() {
	    Plugin worldGuardPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
	 
	    if (worldGuardPlugin == null || !(worldGuardPlugin instanceof WorldGuardPlugin)) {
	        return null;
	    }
	 
	    return (WorldGuardPlugin) worldGuardPlugin;
	}
	
	public static WorldEditPlugin getWorldEdit() {
		Plugin worldEditPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		if (worldEditPlugin instanceof WorldEditPlugin) return (WorldEditPlugin) worldEditPlugin;
		else return null;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (sender instanceof Player) {
			
			Player player = (Player) sender;
			String UUID = player.getUniqueId().toString();
			
			if (cmd.getName().equalsIgnoreCase("scoreboard") || cmd.getName().equalsIgnoreCase("sb")) {
				if (args.length == 0) {
					player.sendMessage(ChatColor.RED + "Wrong usage, try /sb [guildwars/fish/off]");
				}
				else {
					//Guild Wars
					if (args[0].equalsIgnoreCase("guildwars") || args[0].equalsIgnoreCase("gw")) {
						if (gw.contains(UUID)) {
							gw.remove(UUID);
							off.add(UUID);
							player.sendMessage(ChatColor.BLUE + "Scoreboard disabled.");
							player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
						}
						else {
							gw.add(UUID);
							fish.remove(UUID);
							off.remove(UUID);
							player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Guild Wars " + ChatColor.BLUE + "scoreboard enabled.");
							player.setScoreboard(getGWBoard()); 
						}
					}
					//Fish
					else if (args[0].equalsIgnoreCase("fish") || args[0].equalsIgnoreCase("f")) {
						if (fish.contains(UUID)) {
							fish.remove(UUID);
							off.add(UUID);
							player.sendMessage(ChatColor.BLUE + "Scoreboard disabled.");
							player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
						}
						else {
							fish.add(UUID);
							gw.remove(UUID);
							off.remove(UUID);
							
							if (Fishing.fishRunning) {
								player.setScoreboard(Fishing.board);
							}
							else {
								player.setScoreboard(Fishing.getFishScoreboard());
							}
							
							player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Fishing " + ChatColor.BLUE + "scoreboard enabled.");
						}
					}
					
					//Off
					else if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("disable")) {
						if (off.contains(UUID)) {
							player.sendMessage(ChatColor.RED + "No scoreboard enabled.");
						}
						else {
							off.add(UUID);
							fish.remove(UUID);
							gw.remove(UUID);
							player.sendMessage(ChatColor.BLUE + "Scoreboard disabled.");
							player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
						}
					}
					else {
						player.sendMessage(ChatColor.RED + "Unknown scoreboard, try /sb [guildwars/fish/off]");
					}
				}
			}
		}
		else {
			
		}
		return true;
	}
	
	public Scoreboard getGWBoard() {
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective o = board.registerNewObjective("guildwars", "dummy");
		
		o.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Guild Wars");
		
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		Score defenders = o.getScore(ChatColor.GOLD + "Defenders: " + ChatColor.GRAY + plugin.getConfig().getString("defenders"));
		Score level = o.getScore(ChatColor.GOLD + "Level: " + ChatColor.GRAY + plugin.getConfig().getInt("level"));
		
		defenders.setScore(1);
		level.setScore(0);
		
		return board;
	}
	
	public Scoreboard getFishBoard() {
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective o = board.registerNewObjective("fish", "dummy");
		
		o.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Fish");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		Score guy1 = o.getScore(ChatColor.GOLD + "Guy 1");
		Score guy2 = o.getScore(ChatColor.GOLD + "Guy 2");
		
		guy1.setScore(1);
		guy2.setScore(0);
		
		return board;
	}
	
	@EventHandler
	public void onJoin (PlayerJoinEvent event) {
		off.add(event.getPlayer().getUniqueId().toString());
	}
	
	@EventHandler
	public void onLeave (PlayerQuitEvent event) {
		Player player = event.getPlayer();
		String UUID = player.getUniqueId().toString();
		gw.remove(UUID);
		fish.remove(UUID);
		off.remove(UUID);
	}
	
	@EventHandler
	public void onDeath (PlayerDeathEvent event) {
		Player player = event.getEntity();
		
		World world = Bukkit.getServer().getWorld("world");
		Location dLoc = player.getLocation();
		
		com.sk89q.worldedit.util.Location WElocation = new com.sk89q.worldedit.util.Location(BukkitAdapter.adapt(world), dLoc.getX(), dLoc.getY(), dLoc.getZ());
		
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		ApplicableRegionSet set = query.getApplicableRegions(WElocation);
		
		for (ProtectedRegion region : set) {
			if (region.getId().equalsIgnoreCase("castle")) {
				event.setKeepInventory(true);
			}
		}
	}
}

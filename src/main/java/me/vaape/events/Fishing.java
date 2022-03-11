package me.vaape.events;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import me.vaape.rewards.Rewards;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.spigotmc.event.entity.EntityDismountEvent;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import net.md_5.bungee.api.ChatColor;

public class Fishing implements CommandExecutor, Listener{
	
	static Events plugin;
	static WorldGuardPlugin worldGuard = Events.getWorldGuard();
	static WorldEditPlugin worldEdit = Events.getWorldEdit();
	
	public static ArrayList<String> fish = Events.fish;
	
	public static boolean fishRunning = Events.fishRunning;
	
	private static int minutes = 30;
	private static int seconds = 0;
	
	public static Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
	public static Objective o = board.registerNewObjective("fish", "dummy");
	public static Score time;
	public static HashMap<String, Score> scores = new HashMap<String, Score>();

	public Fishing(Events passedPlugin) {
		Fishing.plugin = passedPlugin;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (cmd.getName().equalsIgnoreCase("fishstart") || cmd.getName().equalsIgnoreCase("fstart")) {
			if (sender.isOp()) {
				startFishMessages();
				sender.sendMessage(ChatColor.GREEN + "Fishing messages started.");
			}
			else {
				sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
			}
		}
		if (cmd.getName().equalsIgnoreCase("fishstartnow") || cmd.getName().equalsIgnoreCase("fstartnow")) {
			if (sender.isOp()) {
				startFish();
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[Fishing] " + ChatColor.BLUE + ChatColor.BOLD + "Event has begun! /scoreboard fish");
				sender.sendMessage(ChatColor.GREEN + "Fishing event started.");
			}
			else {
				sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
			}
		}

		if (sender instanceof Player) {
			
			Player player = (Player) sender;
			
			if (cmd.getName().equalsIgnoreCase("fishing") || cmd.getName().equalsIgnoreCase("fish")) {
				
				ZoneId zone = ZoneId.of("UTC");
				
				LocalDateTime now = LocalDateTime.now(zone);
				LocalDateTime nextSunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.of(21, 0));
			
				long millisUntilEvent = now.until(nextSunday, ChronoUnit.SECONDS);
				
				player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Fish info:");
				
				if (fishRunning) {
					player.sendMessage(ChatColor.BLUE + "Next fishing event: " + ChatColor.GREEN + "Running now");
				}
				
				else {
					player.sendMessage(ChatColor.BLUE + "Next fishing event: " + ChatColor.GRAY + "Sunday 21:00 GMT");
					player.sendMessage(ChatColor.BLUE + "Time until next event: " +
										ChatColor.GRAY + String.format("%d hours %d minutes",
										TimeUnit.SECONDS.toHours(millisUntilEvent),
										TimeUnit.SECONDS.toMinutes(millisUntilEvent) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(millisUntilEvent))));
				}
			}
		}
		return false;
	}
	
	private static void startFishMessages() {
		Bukkit.getServer().broadcastMessage("");
		Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + "PvP Fishing Tournament starts in 1 hour at /warp Fish! Use /sb fish to open the scoreboard.");
		Bukkit.getServer().broadcastMessage("");
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage("");
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + "PvP Fishing Tournament starts in 30 minutes at /warp Fish! Use /sb fish to open the scoreboard.");
				Bukkit.getServer().broadcastMessage("");
			}
		}, 30 * 60 * 20);
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {

			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage("");
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + "PvP Fishing Tournament starts in 20 minutes at /warp Fish! Use /sb fish to open the scoreboard.");
				Bukkit.getServer().broadcastMessage("");
			}
		}, 40 * 60 * 20);
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage("");
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + "PvP Fishing Tournament starts in 10 minutes at /warp Fish! Use /sb fish to open the scoreboard.");
				Bukkit.getServer().broadcastMessage("");
			}
		}, 50 * 60 * 20);
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {

			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage("");
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + "PvP Fishing Tournament starts in 5 minutes at /warp Fish! Use /sb fish to open the scoreboard.");
				Bukkit.getServer().broadcastMessage("");
			}
		}, 55 * 60 * 20);
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {

			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage("");
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + "PvP Fishing Tournament starts in 1 minute at /warp Fish! Use /sb fish to open the scoreboard.");
				Bukkit.getServer().broadcastMessage("");
			}
		}, 59 * 60 * 20);
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage("");
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[Fishing] " + ChatColor.BLUE + ChatColor.BOLD + "Event has begun at /warp Fish!");
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[Fishing] " + ChatColor.BLUE + ChatColor.BOLD + "Event has begun at /warp Fish!");
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[Fishing] " + ChatColor.BLUE + ChatColor.BOLD + "Event has begun at /warp Fish!");
				Bukkit.getServer().broadcastMessage("");
				startFish();
			}
		}, 60 * 60 * 20);
	}
	
	private static void startFish() {
		
		fishRunning = true;
		
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			
			if (Events.fish.contains(player.getUniqueId().toString())) {
				player.setScoreboard(board);
			}
			
			
		}
		
		minutes = 30;
		seconds = 0;
		
		o.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Fishing");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		plugin.getConfig().set("fishing", null); //Reset scoreboard in config
		plugin.saveConfig();
				
		//Timer
		new Timer().schedule(new TimerTask() {

			@Override
			public void run() { //Every second
				
				Map<String, Integer> allScores = new HashMap<>();
				
				for (String name : board.getEntries()) {
					allScores.put(name, o.getScore(name).getScore());
				}
				
				int topScore = 0;
				String first = null;
								
				if (allScores.size() > 1) {
					for (Map.Entry<String, Integer> set : allScores.entrySet()) { //Iterate through all entries on the scoreboard
						int value = set.getValue();
						if (value != 999) { //This is the entry for the timer countdown
							if (value >= topScore) {
								first = set.getKey();
								topScore = value;
							}
							if (Bukkit.getPlayer(set.getKey()) != null) {
								Bukkit.getPlayer(set.getKey()).setGlowing(false); //Make every person on the scoreboard not glowing
							}
						}
					}

					if (Bukkit.getPlayer(first) != null) {
						Bukkit.getPlayer(first).setGlowing(true); //Set the person in first to glowing
					}
				}
				
				seconds--;
				
				scores.put(
						ChatColor.AQUA + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds),
						o.getScore(ChatColor.AQUA + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds)));
				
				scores.get(ChatColor.AQUA + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds)).setScore(999);
				
				if (seconds < 0) { //Every minute
					minutes--;
					seconds = 59;
					
					board.resetScores(ChatColor.AQUA + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes + 1, 0)); //Removes previous timer entry
					board.resetScores(ChatColor.AQUA + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes + 1, -1)); //TODO 59 disappears
					
					if (minutes < 0) {
						
						new BukkitRunnable() {
							
							@Override
							public void run() {
								endFish();
							}
						}.runTask(plugin);
						cancel();
						return;
					}
					else {
						if ((minutes + 1) % 5 == 0) { //Every 5 minutes
							for (Player player : Bukkit.getServer().getOnlinePlayers()) {
								if (inFish(player.getLocation())) {
									String UUID = player.getUniqueId().toString();
									plugin.getConfig().set("fishing." + UUID + ".multiplier", plugin.getConfig().getInt("fishing." + UUID + ".multiplier") + 1);
									plugin.saveConfig();
									player.sendMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + "Multiplier increased to " + ChatColor.BOLD + plugin.getConfig().getInt("fishing." + UUID + ".multiplier") + ChatColor.BLUE + ".");
								}
							}
						}
					}
				}
				else {
					board.resetScores(ChatColor.AQUA + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds + 1)); //Removes previous timer entry
				}
			}
		}, 1000, 1000);
	}
	
	private static void endFish() {
		
		fishRunning = false;
		
		purgeFish();
		
		//Remove glowing from all players
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			player.setGlowing(false);
		}
		
		HashMap<String, Integer> finalScores = new HashMap<>();
		
		for (String name : board.getEntries()) {
			finalScores.put(name, o.getScore(name).getScore());
		}
		
		// Create a list from elements of HashMap 
		List<Map.Entry<String, Integer> > list = new LinkedList<Map.Entry<String, Integer>>(finalScores.entrySet());
		
		//Sort list
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o1.getValue().compareTo(o2.getValue()));
			}
		});
		
		//Put data from sorted list to hashmap
		HashMap<String, Integer> sortedScores = new LinkedHashMap<String, Integer>();
		
		for (Map.Entry<String, Integer> aa : list) {
			sortedScores.put(aa.getKey(), aa.getValue());
		}
		
		List<String> s = plugin.getConfig().getStringList("fishing.scores");
		
		for (String name : sortedScores.keySet()) {
			s.add(name + ":" + sortedScores.get(name));
		}
		
		plugin.getConfig().set("fishing", null); //Reset the fishing section of config
		
		plugin.getConfig().set("fishing.scores", s);
		plugin.saveConfig();
		
		//Updated
		s = plugin.getConfig().getStringList("fishing.scores");
		
		List<String> fullList = new ArrayList<>();
		
		for (String string : s) {
			String[] words = string.split(":");
			fullList.add(words[0]);
			fullList.add(words[1]);
		}
		
		//Every other element in fullList is a name
		if(fullList.size() > 1) {
			OfflinePlayer first = Bukkit.getServer().getOfflinePlayer(fullList.get(fullList.size() - 2));
			plugin.getConfig().set("fishing.winner", first.getUniqueId().toString());
			Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + first.getName() + " has won the fishing event and recieved a Rare Fish!");
			Rewards.plugin.giveReward("fish_1st_place", first, true);
		}
		else {
			Bukkit.getServer().broadcastMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + "Fishing event ended with no prizes given!");
		}
		if (fullList.size() > 3) {
			OfflinePlayer second = Bukkit.getServer().getOfflinePlayer(fullList.get(fullList.size() - 4));
			Rewards.plugin.giveReward("fish_2nd_place", second, true);
		}
		if (fullList.size() > 5) {
			OfflinePlayer third = Bukkit.getServer().getOfflinePlayer(fullList.get(fullList.size() - 6));
			Rewards.plugin.giveReward("fish_3rd_place", third, true);
		}
		
		List<OfflinePlayer> placed = new ArrayList<>();

		for (int i = 8; i<=20; i+= 2) {
			if ((i >= 0) && (i < fullList.size())) {
				placed.add(Bukkit.getServer().getOfflinePlayer(fullList.get(fullList.size() - i)));
			}
		}

		for (OfflinePlayer player : placed) {
			Rewards.plugin.giveReward("degg", player, true);
		}
		plugin.saveConfig();
	}
	
	public static Scoreboard getFishScoreboard() {
		Scoreboard fishBoard = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective fishObjective = fishBoard.registerNewObjective("fishBoard", "dummy");
		HashMap<String, Score> fishScores = new HashMap<String, Score>();
		
		fishObjective.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Fishing");
		fishObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		List<String> s = plugin.getConfig().getStringList("fishing.scores");
		
		for (String string : s) {
			String[] words = string.split(":");
			fishScores.put(words[0], fishObjective.getScore(words[0]));
			fishScores.get(words[0]).setScore(Integer.parseInt(words[1]));
		}
		
		return fishBoard;
	}
	
	public static boolean inFish(Location location) {
		
		World world = location.getWorld();
		
		com.sk89q.worldedit.util.Location WElocation = new com.sk89q.worldedit.util.Location(BukkitAdapter.adapt(world), location.getX(), location.getY(), location.getZ());
		
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		ApplicableRegionSet set = query.getApplicableRegions(WElocation);
		
		for (ProtectedRegion region : set) {
			if (region.getId().equalsIgnoreCase("fish")) {
				return true;
			}
		}
		return false;
	}
	
	public static void purgeFish() {
		Location spawn = new Location(Bukkit.getServer().getWorld("world"), -26.5, 103, 193.5, 270, 0);
		
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (inFish(p.getLocation())) {
				p.teleport(spawn, TeleportCause.PLUGIN);
			}
		}
		spawn.getWorld().playSound(spawn, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 10f, 1.5f);
	}
	
	public void resetMultiplier(Player player) {
		plugin.getConfig().set("fishing." + player.getUniqueId().toString() + ".multiplier", 1);
		plugin.saveConfig();
		player.sendMessage(ChatColor.AQUA + "[Fishing] " + ChatColor.BLUE + "Multiplier reset.");
	}
	
	public static boolean isWinner(OfflinePlayer player) {
		if (player.getUniqueId().toString().equals(plugin.getConfig().getString("fishing.winner"))) {
			return true;
		}
		else {
			return false;
		}
	}
	
	@EventHandler
	public void onTeleport (PlayerTeleportEvent event) {
		if (event.getCause() == TeleportCause.COMMAND) {
			if (inFish(event.getFrom())) {
				if (fishRunning) {
					resetMultiplier(event.getPlayer());
				}
			}
		}
		if (inFish(event.getTo())) {
			if (fishRunning) {
				plugin.getConfig().set("fishing." + event.getPlayer().getUniqueId().toString() + ".multiplier", 1);
				plugin.saveConfig();
			}
		}
	}
	
	@EventHandler
	public void onDeath (PlayerDeathEvent event) {
		Player defender = event.getEntity();
		if (inFish(event.getEntity().getLocation()) && fishRunning) {
			resetMultiplier(event.getEntity());
			if (defender.getKiller() != null) {
				if (defender.getKiller() instanceof Player) {
					
					Player attacker = defender.getKiller();
					
					if (scores.containsKey(defender.getName())) {
						
						int defendersFish = (int) Math.floor(scores.get(defender.getName()).getScore()/2) + 1;
						scores.get(defender.getName()).setScore(scores.get(defender.getName()).getScore() - defendersFish);
						
						if (scores.containsKey(attacker.getName())) {
							scores.get(attacker.getName()).setScore(scores.get(attacker.getName()).getScore() + defendersFish);
						}
						else {
							scores.put(attacker.getName(), o.getScore(attacker.getName()));
							scores.get(attacker.getName()).setScore(defendersFish);
						}
						Bukkit.getServer().broadcastMessage(defender.getName() + " has been slain by " + attacker.getName() + " for " + ChatColor.AQUA + defendersFish + ChatColor.RESET + " fish.");
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerJoin (PlayerJoinEvent event) {
		event.getPlayer().setGlowing(false);
		if (inFish(event.getPlayer().getLocation())) {
			Location spawn = new Location(Bukkit.getServer().getWorld("world"), -26.5, 103, 193.5, 270, 0);
			event.getPlayer().teleport(spawn, TeleportCause.PLUGIN);
		}
	}
	
	@EventHandler
	public void onCatchFish (PlayerFishEvent event) {
		if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
			Player player = event.getPlayer();
			if (inFish(event.getPlayer().getLocation()) && fishRunning) {
				if (scores.get(player.getName()) == null) { //If not already in scoreboard add initial fish
					scores.put(player.getName(), o.getScore(player.getName()));
					scores.get(player.getName()).setScore(plugin.getConfig().getInt("fishing." + player.getUniqueId().toString() + ".multiplier"));
				}
				else { //If already in scoreboard add to number of fish
					scores.get(player.getName()).setScore(scores.get(player.getName()).getScore() + plugin.getConfig().getInt("fishing." + player.getUniqueId().toString() + ".multiplier"));
				}
			}
		}
	}
}

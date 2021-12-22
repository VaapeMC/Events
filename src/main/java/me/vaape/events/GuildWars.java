package me.vaape.events;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.vaape.guilds.GuildManager;
import net.md_5.bungee.api.ChatColor;
import net.raidstone.wgevents.events.RegionLeftEvent;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
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

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GuildWars implements CommandExecutor, Listener{
	
	static Events plugin;
	static WorldGuardPlugin worldGuard = Events.getWorldGuard();
	static WorldEditPlugin worldEdit = Events.getWorldEdit();
	
	public static ArrayList<String> gw = Events.gw;
	
	public static boolean canUpgrade = false;
	
	public static List<String> holders = new ArrayList<String>();
	
	public static boolean gwRunning = Events.gwRunning;

	public GuildWars(Events passedPlugin) {
		GuildWars.plugin = passedPlugin;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (cmd.getName().equalsIgnoreCase("guildwarsstart") || cmd.getName().equalsIgnoreCase("gwstart")) {
			if (sender.isOp()) {
				sender.sendMessage(ChatColor.GREEN + "Guild Wars started.");
				startGuildWarsMessages();
			}
			else {
				sender.sendMessage("Unknown command. Type \"/help\" for help.");
			}
		}
		
		if (cmd.getName().equalsIgnoreCase("guildwarsend") || cmd.getName().equalsIgnoreCase("gwend")) {
			if (sender.isOp()) {
				sender.sendMessage(ChatColor.GREEN + "Guild Wars ended.");
				gwEnd();
			}
			else {
				sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
			}
		}
		
		if (cmd.getName().equalsIgnoreCase("guildwarsrefill") || cmd.getName().equalsIgnoreCase("gwrefill")) {
			if (sender.isOp()) {
				refillLoot();
				Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Guild Wars] " + ChatColor.BLUE + "Loot has been refilled.");
			}
			else {
				Bukkit.getServer().broadcastMessage(ChatColor.RED + "You do not have permission to do this.");
			}
		}

		if (sender instanceof Player) {
			
			Player player = (Player) sender;
			
			if (cmd.getName().equalsIgnoreCase("guildwars") || cmd.getName().equalsIgnoreCase("gw")) {
				
				ZoneId zone = ZoneId.of("-05:00");
				
				LocalDateTime now = LocalDateTime.now(zone);
				LocalDateTime nextWednesday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY)).with(LocalTime.of(16, 0));
				LocalDateTime nextSaturday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).with(LocalTime.of(16, 0));
			
				long millisUntilWednesday = now.until(nextWednesday, ChronoUnit.SECONDS);
				long millisUntilSaturday = now.until(nextSaturday, ChronoUnit.SECONDS);
				
				player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Guild Wars info:");
				player.sendMessage(ChatColor.BLUE + "Current defenders: " + ChatColor.GRAY + plugin.getConfig().getString("defenders"));
				player.sendMessage(ChatColor.BLUE + "Castle level: " + ChatColor.GRAY + plugin.getConfig().getInt("level"));
				
				if (gwRunning) {
					player.sendMessage(ChatColor.BLUE + "Next attack: " + ChatColor.GREEN + "Attacking now");
				}
				
				else if (millisUntilWednesday < millisUntilSaturday) {
					player.sendMessage(ChatColor.BLUE + "Next attack: " + ChatColor.GRAY + "Wednesday 16:00 EST");
					player.sendMessage(ChatColor.BLUE + "Time until next attack: " +
										ChatColor.GRAY + String.format("%d hours %d minutes",
										TimeUnit.SECONDS.toHours(millisUntilWednesday),
										TimeUnit.SECONDS.toMinutes(millisUntilWednesday) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(millisUntilWednesday))));
				}
				else {
					player.sendMessage(ChatColor.BLUE + "Next attack: " + ChatColor.GRAY + "Saturday 16:00 EST");
					player.sendMessage(ChatColor.BLUE + "Time until next attack: " +
										ChatColor.GRAY + String.format("%d hours %d minutes",
										TimeUnit.SECONDS.toHours(millisUntilSaturday),
										TimeUnit.SECONDS.toMinutes(millisUntilSaturday) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(millisUntilSaturday))));
				}
			}
		}
		return false;
	}
	
	private static void startGuildWarsMessages() {
		Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Guild Wars] " + ChatColor.BLUE + "Next attack in 1 hour at /warp gw");
		
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Guild Wars] " + ChatColor.BLUE + "Next attack in 30 minutes at /warp gw");
			}
		}, 30 * 60 * 20); //30 minute warning
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Guild Wars] " + ChatColor.BLUE + "Next attack in 10 minutes at /warp gw");
			}
		}, 50 * 60 * 20); //10 minute warning
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Guild Wars] " + ChatColor.BLUE + "Next attack in 5 minutes at /warp gw");
			}
		}, 55 * 60 * 20); //5 minute warning
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[Guild Wars] " + ChatColor.BLUE + ChatColor.BOLD + "Attack has begun at /warp gw! Use /sb gw for scoreboard!");
				startGuildWars();
			}
		}, 60 * 60 * 20); //Start
	}
	
	private static void startGuildWars() {
		gwRunning = true;
		
		//Get initial holders
		updateHolders();
		
		//Purge enemies
		purgeCastle();
		
		//Create Queen
		spawnQueen(200, true);
		
		//Timer
		new Timer().schedule(new TimerTask() {
			
			int minutes = 30;
			int seconds = 0;
			
			@Override
			public void run() {
				seconds--;
				
				//If castle level is between 2 and 3 refresh effects every second
				if (plugin.getConfig().getInt("level") > 1 && plugin.getConfig().getInt("level") < 4) {
					new BukkitRunnable() {
						
						@Override
						public void run() {
							refreshEffects(1);
							
						}
					}.runTask(plugin);
				}
				//If castle level is 4 or above refresh strengthened effects every second
				else if (plugin.getConfig().getInt("level") > 3){
					new BukkitRunnable() {
						
						@Override
						public void run() {
							refreshEffects(2);
							
						}
					}.runTask(plugin);
				}
				
				
				if (seconds < 0) { //Every minute
					minutes--;
					seconds = 59;
					
					if (minutes < 0) { //When time reaches 0
						this.cancel();
						
						new BukkitRunnable() {
							
							@Override
							public void run() {
						
							gwEnd();
							Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
							Objective o = board.registerNewObjective("guildwars", "dummy");
						
							o.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Guild Wars");
							o.setDisplaySlot(DisplaySlot.SIDEBAR);
						
							Score defenders = o.getScore(ChatColor.GOLD + "Defenders: " + ChatColor.GRAY + plugin.getConfig().getString("defenders"));
							Score level = o.getScore(ChatColor.GOLD + "Level: " + ChatColor.GRAY + plugin.getConfig().getInt("level"));
						
							defenders.setScore(1);
							level.setScore(0);
						
							for (Player player : Bukkit.getServer().getOnlinePlayers()) {
								if (gw.contains(player.getUniqueId().toString())) {
									player.setScoreboard(board);
								}
							}
						}
					}.runTask(plugin);
						return;
					}
					
					//If castle level is between 3 and 5 run mercenaries 1
					if (plugin.getConfig().getInt("level") > 2 && plugin.getConfig().getInt("level") < 6) {
						new BukkitRunnable() {
							
							@Override
							public void run() {
								castleMercenaries1();
								
							}
						}.runTask(plugin);
					}
					//If castle level is above level 5 run mercenaries 2
					else if (plugin.getConfig().getInt("level") > 4){
						new BukkitRunnable() {
							
							@Override
							public void run() {
								castleMercenaries2();
								
							}
						}.runTask(plugin);
					}
				}
				
				//Update scoreboard every second
				new BukkitRunnable() {
					
					@Override
					public void run() {
						Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
						Objective o = board.registerNewObjective("guildwars", "dummy");
						
						o.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Guild Wars");
						o.setDisplaySlot(DisplaySlot.SIDEBAR);
						
						Score defenders = o.getScore(ChatColor.GOLD + "Defenders: " + ChatColor.GRAY + plugin.getConfig().getString("defenders"));
						Score level = o.getScore(ChatColor.GOLD + "Level: " + ChatColor.GRAY + plugin.getConfig().getInt("level"));
						Score time = o.getScore(ChatColor.GOLD + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds));
						
						defenders.setScore(2);
						level.setScore(1);
						time.setScore(0);
						
						for (Player player : Bukkit.getServer().getOnlinePlayers()) {
							if (gw.contains(player.getUniqueId().toString())) {
								player.setScoreboard(board);
							}
						}
					}
				}.runTask(plugin);
			}
		}, 1000, 1000);
	}
	
	@SuppressWarnings("deprecation")
	private void takeCastle(Player player, boolean firstTake) {
		
		//Remove armor stands
		for (Entity entity : Bukkit.getServer().getWorld("world").getEntities()) {
			if (entity instanceof ArmorStand || entity instanceof Villager) {
				if (inCastle(entity.getLocation())) {
					entity.setInvulnerable(false);
					entity.remove();
				}
			}
		}
		
		//Adding king
		player.setMetadata("royal", new FixedMetadataValue(plugin, "gw"));
		player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 99999, 3, false, false), true);
		
		Location location = new Location(Bukkit.getWorld("world"), 91.5, 148.25, 72.5);
		ArmorStand stand = (ArmorStand) Bukkit.getServer().getWorld("world").spawnEntity(location, EntityType.ARMOR_STAND);
		stand.setVisible(false);
		stand.setInvulnerable(true);
		stand.setCollidable(false);
		stand.setGravity(false);
		Bukkit.getServer().broadcastMessage("adding passenger: " + player.toString());
		stand.addPassenger(player);

		
		//If this is the first time the castle has been taken in this guild war, stop code here
		//This is when the player right clicks their own queen to swap out
		if (firstTake) { 
			return;
		}
		
		//Update holders
		String tag = GuildManager.getPlayerGuildTag(player.getUniqueId().toString());
		
		plugin.getConfig().set("defenders", tag);
		updateHolders();
		
		Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Guild Wars] " + ChatColor.BLUE + player.getName() + " has killed the Queen. " + ChatColor.BOLD + tag + ChatColor.BLUE + " is now defending.");
		
		//Purge enemies
		purgeCastle();
		
		//Upgrades
		plugin.getConfig().set("level", 0);
		plugin.saveConfig();
		
		Block signBlock = Bukkit.getWorld("world").getBlockAt(91, 141, 74);
		BlockState signState = signBlock.getState();
		Sign sign = (Sign) signState;
		sign.setLine(1, ChatColor.BOLD + "Castle level: " + 0);
		sign.setGlowingText(true);
		sign.update();
		
		Bukkit.getWorld("world").getBlockAt(90, 139, 73).setType(Material.AIR);
		Bukkit.getWorld("world").getBlockAt(90, 140, 73).setType(Material.AIR);
		Bukkit.getWorld("world").getBlockAt(90, 141, 73).setType(Material.AIR);
		Bukkit.getWorld("world").getBlockAt(92, 139, 73).setType(Material.AIR);
		Bukkit.getWorld("world").getBlockAt(92, 140, 73).setType(Material.AIR);
		Bukkit.getWorld("world").getBlockAt(92, 141, 73).setType(Material.AIR);
	}
	
	private static void gwEnd() {
		
		gwRunning = false;
		Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Guild Wars] " + ChatColor.BLUE + ChatColor.BOLD + plugin.getConfig().getString("defenders") + ChatColor.BLUE + " have been victorious and now control the city.");
		
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.hasMetadata("royal")) {
				Location location = new Location(Bukkit.getWorld("world"), 91.5, 150, 71.5);
				player.teleport(location, TeleportCause.PLUGIN);
				player.removeMetadata("royal", plugin);
				player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
			}
		}
		
		//Remove armor stands and villagers
		for (Entity entity : Bukkit.getServer().getWorld("world").getEntities()) {
			if (entity instanceof ArmorStand || entity instanceof Villager) {
				if (inCastle(entity.getLocation())) {
					entity.setInvulnerable(false);
					entity.remove();
				}
			}
		}
		
		purgeCastle();
		clearMercenaries();
	}
	
	private static void spawnQueen(double health, boolean firstQueen) {
		
		//Remove armor stands
		for (Entity entity : Bukkit.getServer().getWorld("world").getEntities()) {
			if (entity instanceof ArmorStand) {
				if (inCastle(entity.getLocation())) {
					entity.setInvulnerable(false);
					entity.remove();
				}
			}
		}
		
		//Create Queen
		Location location = new Location(Bukkit.getWorld("world"), 91.5, 148.25, 72.5);
		Villager queen = (Villager) Bukkit.getServer().getWorld("world").spawnEntity(location, EntityType.VILLAGER);
		queen.setProfession(Villager.Profession.CLERIC);
		queen.setVillagerType(org.bukkit.entity.Villager.Type.SAVANNA);
		queen.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Queen of " + plugin.getConfig().getString("defenders"));
		queen.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 99999, 0, false, false), true);
		queen.setMaxHealth(health);
		queen.setHealth(health);
		queen.setMetadata("royal", new FixedMetadataValue(plugin, "gw"));
		queen.setSilent(true);
		queen.setCollidable(false);
		
		if (firstQueen) {
			queen.setMetadata("first", new FixedMetadataValue(plugin, "gw"));
		}
				
		ArmorStand stand = (ArmorStand) Bukkit.getServer().getWorld("world").spawnEntity(location, EntityType.ARMOR_STAND);
		stand.setVisible(false);
		stand.setInvulnerable(true);
		stand.setCollidable(false);
		stand.setGravity(false);
		stand.addPassenger(queen);
		
	}
	
	private static void purgeCastle() {
		clearMercenaries();
		
		Location castleSpawn = new Location(Bukkit.getServer().getWorld("world"), 60.5, 132, 149.5, -135, 0);
		
		castleSpawn.getWorld().playSound(castleSpawn, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 10f, 1.5f);
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			
			String pTag = GuildManager.getPlayerGuildTag(p.getUniqueId().toString());
			
			if (pTag != null) {
				if (pTag.equals(plugin.getConfig().getString("defenders")) ||
					GuildManager.getAllies(plugin.getConfig().getString("defenders").toLowerCase()).contains(pTag)) {
					}
					else {
						if (inCastle(p.getLocation())) {
							p.teleport(castleSpawn, TeleportCause.PLUGIN);
						}
					}
			}
			else {
				p.teleport(castleSpawn, TeleportCause.PLUGIN);
			}
		}
	}
	
	private static void castleMercenaries1() {
		clearMercenaries();
		
		Location spawnPoint1 = new Location(Bukkit.getServer().getWorld("world"), 78.5, 144, 107.5);
		org.bukkit.util.Vector vectorEast = new org.bukkit.util.Vector(2.5, 0, 0);
		//Spawn skeletons
		Skeleton skeleton1 = (Skeleton) spawnPoint1.getWorld().spawnEntity(spawnPoint1, EntityType.SKELETON);
		Skeleton skeleton2 = (Skeleton) spawnPoint1.getWorld().spawnEntity(spawnPoint1.add(0, 0, -10), EntityType.SKELETON);
		Skeleton skeleton3 = (Skeleton) spawnPoint1.getWorld().spawnEntity(spawnPoint1.add(0, 0, -10), EntityType.SKELETON);
		//Set velocity
		skeleton1.setVelocity(vectorEast);
		skeleton2.setVelocity(vectorEast);
		skeleton3.setVelocity(vectorEast);
		
		//Right side
		Location spawnPoint2 = spawnPoint1.add(26, 0, 20); //-22 because previously added 11 two times
		org.bukkit.util.Vector vectorWest = new org.bukkit.util.Vector(-2.5, 0, 0);
		//Spawn skeletons
		Skeleton skeleton4 = (Skeleton) spawnPoint2.getWorld().spawnEntity(spawnPoint2, EntityType.SKELETON);
		Skeleton skeleton5 = (Skeleton) spawnPoint2.getWorld().spawnEntity(spawnPoint2.add(0, 0, -10), EntityType.SKELETON);
		Skeleton skeleton6 = (Skeleton) spawnPoint2.getWorld().spawnEntity(spawnPoint2.add(0, 0, -10), EntityType.SKELETON);
		//Set velocity
		skeleton4.setVelocity(vectorWest);
		skeleton5.setVelocity(vectorWest);
		skeleton6.setVelocity(vectorWest);
		
		ArrayList<Skeleton> skeletons = new ArrayList<Skeleton>();
		skeletons.add(skeleton1);
		skeletons.add(skeleton2);
		skeletons.add(skeleton3);
		skeletons.add(skeleton4);
		skeletons.add(skeleton5);
		skeletons.add(skeleton6);
		
		spawnPoint1.getWorld().playSound(spawnPoint1.add(-10, 0, 10), Sound.ENTITY_ZOMBIE_INFECT, 1f, 1.5f);
		
		for (Skeleton skeleton : skeletons) {
			skeleton.setHealth(1);
			skeleton.setMaxHealth(2);
			skeleton.setMetadata("mercenary", new FixedMetadataValue(plugin, "gw"));
		}
	}
	
	private static void castleMercenaries2() {
		clearMercenaries();
		
		Location spawnPoint1 = new Location(Bukkit.getServer().getWorld("world"), 78.5, 144, 107.5);
		org.bukkit.util.Vector vectorEast = new org.bukkit.util.Vector(2.5, 0, 0);
		//Spawn blazes
		Blaze blaze1 = (Blaze) spawnPoint1.getWorld().spawnEntity(spawnPoint1, EntityType.BLAZE);
		Blaze blaze2 = (Blaze) spawnPoint1.getWorld().spawnEntity(spawnPoint1.add(0, 0, -10), EntityType.BLAZE);
		Blaze blaze3 = (Blaze) spawnPoint1.getWorld().spawnEntity(spawnPoint1.add(0, 0, -10), EntityType.BLAZE);
		//Set velocity
		blaze1.setVelocity(vectorEast);
		blaze2.setVelocity(vectorEast);
		blaze3.setVelocity(vectorEast);
		
		//Right side
		Location spawnPoint2 = spawnPoint1.add(26, 0, 20); //-22 because previously added 11 two times
		org.bukkit.util.Vector vectorWest = new org.bukkit.util.Vector(-2.5, 0, 0);
		//Spawn blazes
		Blaze blaze4 = (Blaze) spawnPoint2.getWorld().spawnEntity(spawnPoint2, EntityType.BLAZE);
		Blaze blaze5 = (Blaze) spawnPoint2.getWorld().spawnEntity(spawnPoint2.add(0, 0, -10), EntityType.BLAZE);
		Blaze blaze6 = (Blaze) spawnPoint2.getWorld().spawnEntity(spawnPoint2.add(0, 0, -10), EntityType.BLAZE);
		//Set velocity
		blaze4.setVelocity(vectorWest);
		blaze5.setVelocity(vectorWest);
		blaze6.setVelocity(vectorWest);
		
		ArrayList<Blaze> blazes = new ArrayList<Blaze>();
		blazes.add(blaze1);
		blazes.add(blaze2);
		blazes.add(blaze3);
		blazes.add(blaze4);
		blazes.add(blaze5);
		blazes.add(blaze6);
		
		spawnPoint1.getWorld().playSound(spawnPoint1.add(-10, 0, 10), Sound.ENTITY_ZOMBIE_INFECT, 1f, 1.5f);
		
		for (Blaze blaze : blazes) {
			blaze.setHealth(1);
			blaze.setMaxHealth(2);
			blaze.setMetadata("mercenary", new FixedMetadataValue(plugin, "gw"));
		}
	}
	
	private static void clearMercenaries() {
		for (Entity entity : Bukkit.getServer().getWorld("world").getEntities()) {
			if (entity instanceof Skeleton || entity instanceof Blaze || entity instanceof Arrow) {
				if (inCastle(entity.getLocation())) {
					entity.remove();
				}
			}
		}
	}
	
	private void upgradeCastle(String name) {
		int level = plugin.getConfig().getInt("level");
		int newLevel = level + 1;
		plugin.getConfig().set("level", newLevel);
		plugin.saveConfig();
		
		Block signBlock = Bukkit.getWorld("world").getBlockAt(91, 141, 74);
		signBlock.setType(Material.BIRCH_WALL_SIGN);
		Directional signData = (Directional) signBlock.getBlockData();
		signData.setFacing(BlockFace.SOUTH);
		signBlock.setBlockData(signData);
		
		BlockState signState = signBlock.getState();
		Sign sign = (Sign) signState;
		sign.setLine(1, ChatColor.BOLD + "Castle level: " + newLevel);
		sign.setGlowingText(true);
		sign.update();
		
		switch (newLevel) {
		case 1:
			Bukkit.getWorld("world").getBlockAt(90, 139, 73).setType(Material.GOLD_BLOCK);
			break;
		case 2:
			Bukkit.getWorld("world").getBlockAt(90, 140, 73).setType(Material.BEACON);
			break;
		case 3:
			Block skullBlock = Bukkit.getWorld("world").getBlockAt(90, 141, 73);
			skullBlock.setType(Material.SKELETON_WALL_SKULL);
			Directional skullData = (Directional) skullBlock.getBlockData();
			skullData.setFacing(BlockFace.SOUTH);
			skullBlock.setBlockData(skullData);
			break;
		case 4:
			Bukkit.getWorld("world").getBlockAt(92, 139, 73).setType(Material.BEACON);
			break;
		case 5:
			Block headBlock = Bukkit.getWorld("world").getBlockAt(92, 140, 73);
			headBlock.setType(Material.MAGMA_BLOCK);
			
//			//Set skin
//			Skull head = (Skull) headBlock.getState();
//			UUID uuid = UUID.fromString("4c38ed11-596a-4fd4-ab1d-26f386c1cbac"); //MHF_Blaze
//			head.setOwningPlayer(Bukkit.getServer().getOfflinePlayer(uuid));
//			head.update();
//			
//			//Set direction
//			Directional headData = (Directional) headBlock.getBlockData();
//			headData.setFacing(BlockFace.SOUTH);
//			headBlock.setBlockData(headData);
			break;
		case 6:
			Bukkit.getWorld("world").getBlockAt(92, 141, 73).setType(Material.GOLD_BLOCK);
			break;
		}
		
		canUpgrade = false;
		
		Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Guild Wars] " + ChatColor.BLUE + name + " has upgraded the castle to level " + newLevel + ".");
	}
	
	public static boolean inCastle(Location location) {
		World world = location.getWorld();
		
		com.sk89q.worldedit.util.Location WElocation = new com.sk89q.worldedit.util.Location(BukkitAdapter.adapt(world), location.getX(), location.getY(), location.getZ());
		
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		ApplicableRegionSet set = query.getApplicableRegions(WElocation);
		
		for (ProtectedRegion region : set) {
			if (region.getId().equalsIgnoreCase("castle")) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean inSpawn(Location location) {
		World world = location.getWorld();
		
		com.sk89q.worldedit.util.Location WElocation = new com.sk89q.worldedit.util.Location(BukkitAdapter.adapt(world), location.getX(), location.getY(), location.getZ());
		
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		ApplicableRegionSet set = query.getApplicableRegions(WElocation);
		
		for (ProtectedRegion region : set) {
			if (region.getId().equalsIgnoreCase("innerspawn")) {
				return true;
			}
		}
		return false;
	}
	
	private static void updateHolders() {
		holders = GuildManager.getGuildPlayers(plugin.getConfig().getString("defenders"));
	}
	
	private static void refreshEffects(int buffLevel) {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (holders.contains(player.getUniqueId().toString())) {
				if (inCastle(player.getLocation())) {
					
					//Check if player has higher level buff
					//Strength
					if (player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
						if (player.getPotionEffect(PotionEffectType.INCREASE_DAMAGE).getAmplifier() < buffLevel) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 99999, buffLevel - 1, false, false), false);
						}
					}
					else {
						player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 99999, buffLevel - 1, false, false), false);
					}
					//Speed
					if (player.hasPotionEffect(PotionEffectType.SPEED)) {
						if (player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() < buffLevel) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, buffLevel - 1, false, false), false);
						}
					}
					else {
						player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, buffLevel - 1, false, false), false);
					}
					//Regen
					if (player.hasPotionEffect(PotionEffectType.REGENERATION)) {
						if (player.getPotionEffect(PotionEffectType.REGENERATION).getAmplifier() < buffLevel) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 99999, buffLevel - 2, false, false), false);
						}
					}
					else {
						player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 99999, buffLevel - 2, false, false), false);
					}
				}
			}
		}
	}
	
	public static void refillLoot() {
		int level = plugin.getConfig().getInt("level");
		
		World world = Bukkit.getWorld("world");
		Chest chest1 = (Chest) world.getBlockAt(86, 141, 73).getState();
		Chest chest2 = (Chest) world.getBlockAt(87, 141, 73).getState();
		Chest chest3 = (Chest) world.getBlockAt(86, 140, 73).getState();
		Chest chest4 = (Chest) world.getBlockAt(87, 140, 73).getState();
		Chest chest5 = (Chest) world.getBlockAt(95, 141, 73).getState();
		Chest chest6 = (Chest) world.getBlockAt(96, 141, 73).getState();
		Chest chest7 = (Chest) world.getBlockAt(95, 140, 73).getState();
		Chest chest8 = (Chest) world.getBlockAt(96, 140, 73).getState();
		
		List<Chest> chests = Arrays.asList(chest1, chest2, chest3, chest4, chest5, chest6, chest7, chest8);
		
		for (Chest chest : chests) {
			Inventory inventory = chest.getBlockInventory();
			for (int i = 0; i < inventory.getSize(); i++) { //Loop through each item slot in inventory
				ItemStack item = getLootItem();
				if (item != null) {
					if (item.getType() != Material.AIR) {
						Bukkit.getPlayer("Vaape").getInventory().addItem(item);
					}
				}
				inventory.setItem(i, getLootItem());
			}
		}
		Bukkit.getWorld("world").playSound(new Location(Bukkit.getWorld("world"), 91, 140, 73), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.1f);
	}
	
	private static ItemStack getLootItem() {
		int level = plugin.getConfig().getInt("level");
		int lootLevel = 0;
		if (level == 0) { //Loot level 0
			lootLevel = 0;
		}
		else if (level > 0 && level < 6) { //Loot level 1
			lootLevel = 1;
		}
		else if (level == 6) { //Loot level 2
			lootLevel = 2;
		}
		
		Set<String> itemNames = plugin.getConfig().getConfigurationSection("loot.probabilities level " + lootLevel).getKeys(false);
		double total = 0; //Total probability pool
		for (String itemName : itemNames) {
			double probability = 1 / plugin.getConfig().getDouble("loot.probabilities level " + lootLevel + "." + itemName);
			total += probability;
		}
		
		//Count up from 0 with increment = each individual probability, when random < counter choose that item
		double random = Math.random() * total; //Random number between 0 and total
		double counter = 0;
		ItemStack generatedItem = null;
		for (String itemName : itemNames) {
			double probability = 1 / plugin.getConfig().getDouble("loot.probabilities level " + lootLevel + "." + itemName);
			counter += probability;
			if (random <= counter) {
				generatedItem = plugin.getConfig().getItemStack("loot.items." + itemName);
				break;
			}
		}
		return generatedItem;
	}
	
	@EventHandler
	public void onRightClickEntity (PlayerInteractEntityEvent event) {
		if (gwRunning) {
			if (event.getRightClicked() instanceof Villager) {
				if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.NAME_TAG || event.getPlayer().getInventory().getItemInOffHand().getType() == Material.NAME_TAG) {
					event.setCancelled(true);
				}
				if (inCastle(event.getRightClicked().getLocation())) {
					event.setCancelled(true);
					if (event.getRightClicked().hasMetadata("first")) {
						if (holders.contains(event.getPlayer().getUniqueId().toString())) {
							takeCastle(event.getPlayer(), true);
							event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1f, 2f);
						}
						else {
							event.getPlayer().sendMessage(ChatColor.RED + "Only a member of the holding guild can become King.");
						}
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onRegionLeave(RegionLeftEvent event) {
		
		if (gwRunning) {
			if (holders.contains(event.getPlayer().getUniqueId().toString())) {
				if (event.getRegion().getId().equalsIgnoreCase("castle")) {
					if (plugin.getConfig().getInt("level") > 1) {
						
						event.getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
						event.getPlayer().removePotionEffect(PotionEffectType.SPEED);
						event.getPlayer().removePotionEffect(PotionEffectType.REGENERATION);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onBlockIgnite (BlockIgniteEvent event) {
		if (event.getCause() == IgniteCause.FIREBALL) {
			if (event.getIgnitingEntity() instanceof Fireball) {
				Fireball fireball = (Fireball) event.getIgnitingEntity();
				if (fireball.getShooter() instanceof Blaze) {
					Blaze blaze = (Blaze) fireball.getShooter();
					if (blaze.hasMetadata("mercenary")) {
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onTarget (EntityTargetLivingEntityEvent event) {
		
		if (event.getEntity().hasMetadata("mercenary")) {
			LivingEntity target = event.getTarget();
			
			if (target instanceof Blaze || target instanceof Skeleton || target instanceof Villager) {
				event.setCancelled(true);
			}
			else if (target instanceof Player) {
				
				String pTag = GuildManager.getPlayerGuildTag(event.getTarget().getUniqueId().toString());
				
				if (pTag != null) {
					
					if (pTag.equals(plugin.getConfig().getString("defenders")) ||
						GuildManager.getAllies(plugin.getConfig().getString("defenders").toLowerCase()).contains(pTag)) {
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	//Add skeleton mercenary arrow meta data
	@EventHandler
	public void onBowShoot(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Skeleton) {
			if (event.getEntity().hasMetadata("mercenary")) {
				event.getProjectile().setMetadata("mercenary", new FixedMetadataValue(plugin, "gw"));
			}
		}
	}
	
	//Apply effects from mercenary projectiles
	@EventHandler
	public void onDamage (EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Arrow) {
			if (event.getDamager().hasMetadata("mercenary")) {
				if (event.getEntity() instanceof Player) {
					Player player = (Player) event.getEntity();
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 3, false, true), true);
				}
			}
		}
		else if (event.getDamager() instanceof SmallFireball) {
			SmallFireball fireball = (SmallFireball) event.getDamager();
			if (fireball.getShooter() instanceof Blaze) {
				Blaze blaze = (Blaze) fireball.getShooter();
				if (blaze.hasMetadata("mercenary")) {
					if (event.getEntity() instanceof Player) {
						Player player = (Player) event.getEntity();
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 3, false, true), true);
					}
				}
			}
		}
	}
	
	//Upgrade
	@EventHandler
	public void buttonPress (PlayerInteractEvent event) {
		
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
			event.getClickedBlock().getType() == Material.STONE_BUTTON &&
			event.getClickedBlock().getX() == 91 && event.getClickedBlock().getY() == 140 && event.getClickedBlock().getZ() == 74) {
			Player player = event.getPlayer();
			
			ItemStack hand = player.getInventory().getItemInMainHand();
			int level = plugin.getConfig().getInt("level");
			int newLevel = level + 1;
			
			if (level == 6) {
				player.sendMessage(ChatColor.RED + "You can not upgrade the castle beyond level 6.");
				return;
			}
			
			if (hand != null && hand.getType() == Material.DRAGON_EGG && hand.getAmount() >= (newLevel * 5)) {
				
				if (canUpgrade) {
					hand.setAmount(hand.getAmount() - (newLevel * 5));
					player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
					upgradeCastle(player.getName());
				}
				else {
					player.sendMessage(ChatColor.RED + "The castle can not be upgraded right now.");
				}
			}
			else {
				player.sendMessage(ChatColor.RED + "You need " + (newLevel * 5) + " dragon eggs to upgrade the castle to level " + newLevel + ".");
			}
		}
	}
	
	//Remove player from castle on login
	@EventHandler
	public void onPlayerJoin (PlayerJoinEvent event) {
		if (gwRunning) {
			if (inCastle(event.getPlayer().getLocation())) {
				event.getPlayer().teleport(new Location(Bukkit.getServer().getWorld("world"), 60.5, 132, 149.5, -135, 0), TeleportCause.PLUGIN);
			}
		}
	}
	
	//Stop players teleporting into castle
	@EventHandler
	public void onTeleport (PlayerTeleportEvent event) {
		if (gwRunning && event.getCause() == TeleportCause.COMMAND) {
			if (inCastle(event.getTo())) {
				event.setTo(new Location(Bukkit.getServer().getWorld("world"), 60.5, 132, 149.5, -135, 0));
			}
		}
		if (event.getPlayer().hasMetadata("royal")) {
			if (event.getCause() == TeleportCause.COMMAND) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + "You can not teleport while sitting on the throne.");
			}
		}
	}
	
	//Remove royal attributes if king and replace with low health queen
	@EventHandler
	public void onPlayerQuit (PlayerQuitEvent event) {
		if (event.getPlayer().hasMetadata("royal")) {
			event.getPlayer().removeMetadata("royal", plugin);
			event.getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
			spawnQueen(50, false);
		}
	}
	
	//Handle takeCastle if king dies
	@EventHandler
	public void onPlayerDeath (PlayerDeathEvent event) {
		Player king = event.getEntity();
		if (king.hasMetadata("royal")) {
			king.removeMetadata("royal", plugin);
			king.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
			takeCastle(king.getKiller(), false);
		}
	}
	
	//Stop king exiting throne
	@EventHandler
	public void onDismountStand (EntityDismountEvent event) {
		if (!gwRunning) {
			return;
		}
		
		if (event.getEntity().hasMetadata("royal")) {
			Bukkit.broadcastMessage("exited stand");
			event.setCancelled(true);
		}
	}
	
	
	@EventHandler
	public void onHit (EntityDamageByEntityEvent event) {
		if (!gwRunning) {
			return;
		}
		Entity entity = event.getEntity();
		Entity attacker = event.getDamager();
		
		//For handling Queen damage
		if (entity instanceof Villager) {
		
			if (entity.getCustomName() == null) {
				return;
			}
			
			if (entity.getCustomName().contains(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Queen of")) {
				
				event.setCancelled(true);
				
				if (attacker instanceof Arrow) {
					return;
				}
				
				if (attacker instanceof Player) {
					Player player = (Player) attacker;
					String tag = GuildManager.getPlayerGuildTag(player.getUniqueId().toString());
					if (tag == null) {
						player.sendMessage(ChatColor.RED + "You must be in a guild to attack the Queen.");
					}
					else if (plugin.getConfig().getString("defenders").equals(tag)) {
						player.sendMessage(ChatColor.RED + "You can not hurt your Queen.");
					}
					else {
						event.setCancelled(false);
					}
				}
			}
		}
		
		//For handling King damage
		else if (entity instanceof Player) {
			if (entity.hasMetadata("royal")) {
				
				event.setCancelled(true);
				
				if (attacker instanceof Arrow) {
					return;
				}
				if (attacker instanceof Player) {
					Player player = (Player) attacker;
					String tag = GuildManager.getPlayerGuildTag(player.getUniqueId().toString());
					if (tag == null) {
						player.sendMessage(ChatColor.RED + "You must be in a guild to attack the King.");
					}
					else if (plugin.getConfig().getString("defenders").equals(tag)) {
						player.sendMessage(ChatColor.RED + "You can not hurt your King.");
					}
					else {
						event.setCancelled(false);
					}
				}
			}
		}
	}
	
	//Handle Queen death
	@EventHandler
	public void onEntityDeath (EntityDeathEvent event) {
		if (!gwRunning) {
			return;
		}
		if (event.getEntity() instanceof Villager) {
			if (event.getEntity().getCustomName().contains(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Queen of")) {
				event.getEntity().removeMetadata("royal", plugin);
				event.getEntity().getLocation().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_WOLF_HOWL, 1f, 2f);
				takeCastle(event.getEntity().getKiller(), false);
			}
		}
	}
	
	//Castle owners can open doors
	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		
		if (event.getClickedBlock() == null) {
			return;
		}
		
		if (!inCastle(event.getClickedBlock().getLocation())) {
			return;
		}
		
		Player player = event.getPlayer();
		
		if (player.isOp()) {
			return;
		}
		
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			
			if (event.getClickedBlock().getType() == Material.SPRUCE_DOOR) {
				
				event.setCancelled(true);
				
				String tag = GuildManager.getPlayerGuildTag(player.getUniqueId().toString());
				
				if (tag == null) {
					player.sendMessage(ChatColor.RED + "Only castle holders can open this door.");
				}
				else if (plugin.getConfig().getString("defenders").equals(tag)) {
					event.setCancelled(false);
				}
				else {
					player.sendMessage(ChatColor.RED + "Only castle holders can open this door.");
				}
			}
		}
	}
	
	//King doesn't take fire damage
	@EventHandler
	public void onDamage (EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			if (event.getEntity().hasMetadata("royal")) {
				if (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) {
					event.setCancelled(true);
				}
			}
		}
	}
	
	//King can't pearl
	@EventHandler
	public void onPearl (PlayerLaunchProjectileEvent event) {
		if (event.getPlayer().hasMetadata("royal")) {
			if (event.getProjectile() instanceof EnderPearl) {
				event.setCancelled(true);
			}
		}
	}
}

package me.vaape.events;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;

import net.md_5.bungee.api.ChatColor;

@SuppressWarnings("deprecation")
public class CrateDrops implements CommandExecutor, Listener{
	
	static Events plugin;

	public static boolean landed; //True after ANY crate has landed since last restart (unless falling blocks are in the air or waiting for backup timer)
	//public static Location crateCoords = null; //Will be null until first crate drop
	
	static Calendar calendar = Calendar.getInstance();
	static int day = calendar.get(Calendar.DAY_OF_WEEK);

	public CrateDrops(Events passedPlugin) {
		CrateDrops.plugin = passedPlugin;
	}

	//TODO Set exact values for location, random loot in chest
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (cmd.getName().equalsIgnoreCase("scheduledropcrate")) {

			if (sender.hasPermission("events.scheduledropcrate")) {

				startCrateDropMessages(getRandomLocation().clone().add(0.5, 0, 0.5));
				sender.sendMessage(ChatColor.GREEN + "Crate drop started.");
			}
			else {
				sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
			}
		}

		if (cmd.getName().equalsIgnoreCase("scheduledropcratenow")) {

			if (sender.hasPermission("events.scheduledropcrate")) {
				dropCrate(getRandomLocation().clone().add(0.5, 0, 0.5));
				sender.sendMessage(ChatColor.GREEN + "Crate dropped.");
			}
		}

		return true;
	}
	
	private static void startCrateDropMessages(Location location) {
		
		Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "[Crate Drop] " + ChatColor.BLUE + "A crate will drop at [" + location.getBlockX() + ", " + location.getBlockZ() + "] in 30 minutes.");

		//20 minute warning
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "[Crate Drop] " + ChatColor.BLUE + "A crate will drop at [" + location.getBlockX()  + ", " + location.getBlockZ() + "] in 20 minutes.");
			}
		}.runTaskLater(plugin, 10 * 60 * 20);

		//10 minute warning
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "[Crate Drop] " + ChatColor.BLUE + "A crate will drop at [" + location.getBlockX()  + ", " + location.getBlockZ() + "] in 10 minutes.");
			}
		}.runTaskLater(plugin, 20 * 60 * 20);

		//5 minute warning
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "[Crate Drop] " + ChatColor.BLUE + "A crate will drop at [" + location.getBlockX()  + ", " + location.getBlockZ() + "] in 5 minutes.");
			}
		}.runTaskLater(plugin, 25 * 60 * 20);

		//1 minute warning
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "[Crate Drop] " + ChatColor.BLUE + "A crate will drop at [" + location.getBlockX()  + ", " + location.getBlockZ() + "] in 1 minute.");
			}
		}.runTaskLater(plugin, 29 * 60 * 20);

		//30 second warning
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "[Crate Drop] " + ChatColor.BLUE + "A crate will drop at [" + location.getBlockX()  + ", " + location.getBlockZ() + "] in 30 seconds.");
			}
		}.runTaskLater(plugin, 1770 * 20); //29.5 * 60 = 1770

		//Drop
		new BukkitRunnable() {
			@Override
			public void run() {
				dropCrate(location);
			}
		}.runTaskLater(plugin, 30 * 60 * 20);
	}
	
	public static void dropCrate(Location location) {
		
		for (Chunk chunk : getSurroundingChunks(location)) {
			chunk.load();
			chunk.setForceLoaded(true);
		}
		
		landed = false;
		createLanding(location);
		
		Block b1 = Bukkit.getServer().getWorld("world").getHighestBlockAt(location);
		Block b2 = Bukkit.getServer().getWorld("world").getHighestBlockAt(location.clone().add(1, 0, 0));
		Block b3 = Bukkit.getServer().getWorld("world").getHighestBlockAt(location.clone().add(1, 0, 1));
		Block b4 = Bukkit.getServer().getWorld("world").getHighestBlockAt(location.clone().add(1, 0, 2));
		Block b5 = Bukkit.getServer().getWorld("world").getHighestBlockAt(location.clone().add(2, 0, 0));
		Block b6 = Bukkit.getServer().getWorld("world").getHighestBlockAt(location.clone().add(2, 0, 1));
		Block b7 = Bukkit.getServer().getWorld("world").getHighestBlockAt(location.clone().add(2, 0, 2));
		Block b8 = Bukkit.getServer().getWorld("world").getHighestBlockAt(location.clone().add(0, 0, 1));
		Block b9 = Bukkit.getServer().getWorld("world").getHighestBlockAt(location.clone().add(0, 0, 2));
		
		ArrayList<Block> highestBlocks = new ArrayList<Block>();
		highestBlocks.addAll(Arrays.asList(b1, b2, b3, b4, b5, b6 ,b7, b8, b9));
		
		double highestY = b1.getLocation().getY();
		
		//Get highest block out of all the highest blocks
		for (Block b : highestBlocks) {
			if (b.getY() > highestY) {
				highestY = b.getLocation().getY();
			}
		}
		
		Location highestPoint = location.clone();
		highestPoint.setY(highestY);
		startBackupTimer(highestPoint);
		
		FallingBlock fallingWood1 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location, Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingWood2 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(0, 1, 0), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingWood3 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(0, 2, 0), Material.SPRUCE_PLANKS, (byte) 1);
		
		//
		FallingBlock fallingWood4 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(1, 0, 0), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingPiston1 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(1, 1, 0), Material.OAK_LOG, (byte) 2);
		FallingBlock fallingWood5 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(1, 2, 0), Material.SPRUCE_STAIRS, (byte) 6);
		
		FallingBlock fallingWood6 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(2, 0, 0), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingWood7 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(2, 1, 0), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingWood8 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(2, 2, 0), Material.SPRUCE_PLANKS, (byte) 1);
		
		//
		FallingBlock fallingWood9 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(0, 0, 1), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingPiston2 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(0, 1, 1), Material.OAK_LOG, (byte) 4);
		FallingBlock fallingWood10 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(0, 2, 1), Material.SPRUCE_STAIRS, (byte) 4);
		
		FallingBlock fallingWood11 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(0, 0, 2), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingWood12 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(0, 1, 2), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingWood13 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(0, 2, 2), Material.SPRUCE_PLANKS, (byte) 1);
		
		//
		FallingBlock fallingWood14 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(1, 0, 2), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingPiston3 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(1, 1, 2), Material.OAK_LOG, (byte) 3);
		FallingBlock fallingWood15 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(1, 2, 2), Material.SPRUCE_STAIRS, (byte) 7);
		
		FallingBlock fallingWood16 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(2, 0, 2), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingWood17 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(2, 1, 2), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingWood18 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(2, 2, 2), Material.SPRUCE_PLANKS, (byte) 1);
		
		//
		FallingBlock fallingWood19 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(2, 0, 1), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingPiston4 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(2, 1, 1), Material.OAK_LOG, (byte) 5);
		FallingBlock fallingWood20 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(2, 2, 1), Material.SPRUCE_STAIRS, (byte) 5);
		
		//Middle
		FallingBlock fallingPiston5 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(1, 0, 1), Material.SPRUCE_PLANKS, (byte) 1);
		FallingBlock fallingChest = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(1, 1, 1), Material.CHEST, (byte) 0);
		FallingBlock fallingPiston6 = (FallingBlock) Bukkit.getServer().getWorld("world").spawnFallingBlock(location.clone().add(1, 2, 1), Material.SPRUCE_PLANKS, (byte) 1);
		
		ArrayList<FallingBlock> fallingBlocks = new ArrayList<FallingBlock>();
		fallingBlocks.addAll(Arrays.asList(fallingWood1, fallingWood2,  fallingWood3, fallingWood4, fallingWood5, fallingWood6, fallingWood7, fallingWood8, fallingWood9, fallingWood10,
				 					fallingWood11, fallingWood12, fallingWood13, fallingWood14, fallingWood15, fallingWood16, fallingWood17, fallingWood18, fallingWood19, fallingWood20,
				 					fallingPiston1, fallingPiston2, fallingPiston3, fallingPiston4, fallingPiston5, fallingPiston6, fallingChest));
		for (FallingBlock block : fallingBlocks) {
			block.setMetadata("crate", new FixedMetadataValue(plugin, "cratedrops"));
			block.setDropItem(false);
			block.setGlowing(true);
		}
		
		fallingWood1.setMetadata("reference", new FixedMetadataValue(plugin, "cratedrops"));

		Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "[Crate Drop] " + ChatColor.BLUE + "" + ChatColor.BOLD + "A crate has dropped at [" + location.getBlockX()  + ", " + location.getBlockZ() + "]");
	}
	
	private static List<Chunk> getSurroundingChunks(Location location) { //Gets 3x3 chunk grid around location
		List<Chunk> chunks = new ArrayList<>();
		for (int x = -16; x <= 16; x = x + 16) {
			for (int z = -16; z <= 16; z = z + 16) {
				Chunk chunk = location.getWorld().getChunkAt((location.getBlockX() + x) >> 4, (location.getBlockZ() + z) >> 4);
				chunks.add(chunk);
			}
		}
		return chunks;
	}
	
	public static void createLanding(Location crateLocation) {
		
		//Square
		Block b1 = Bukkit.getServer().getWorld("world").getHighestBlockAt(crateLocation);
		Block b2 = Bukkit.getServer().getWorld("world").getHighestBlockAt(crateLocation.clone().add(1, 0, 0));
		Block b3 = Bukkit.getServer().getWorld("world").getHighestBlockAt(crateLocation.clone().add(1, 0, 1));
		Block b4 = Bukkit.getServer().getWorld("world").getHighestBlockAt(crateLocation.clone().add(1, 0, 2));
		Block b5 = Bukkit.getServer().getWorld("world").getHighestBlockAt(crateLocation.clone().add(2, 0, 0));
		Block b6 = Bukkit.getServer().getWorld("world").getHighestBlockAt(crateLocation.clone().add(2, 0, 1));
		Block b7 = Bukkit.getServer().getWorld("world").getHighestBlockAt(crateLocation.clone().add(2, 0, 2));
		Block b8 = Bukkit.getServer().getWorld("world").getHighestBlockAt(crateLocation.clone().add(0, 0, 1));
		Block b9 = Bukkit.getServer().getWorld("world").getHighestBlockAt(crateLocation.clone().add(0, 0, 2));
		
		ArrayList<Block> highestBlocks = new ArrayList<Block>();
		highestBlocks.addAll(Arrays.asList(b1, b2, b3, b4, b5, b6 ,b7, b8, b9));
		
		double highestY = b1.getLocation().getY();
		
		//Get the highest block out of all the highest blocks
		for (Block b : highestBlocks) {
			if (b.getY() > highestY) {
				highestY = b.getLocation().getY();
			}
		}
		
		Location base = crateLocation.clone();
		base.setY(highestY);
		
		base.getBlock().setType(Material.COBBLESTONE);
		base.clone().add(1, 0, 0).getBlock().setType(Material.COBBLESTONE);
		base.clone().add(1, 0, 1).getBlock().setType(Material.COBBLESTONE);
		base.clone().add(1, 0, 2).getBlock().setType(Material.COBBLESTONE);
		base.clone().add(2, 0, 0).getBlock().setType(Material.COBBLESTONE);
		base.clone().add(2, 0, 1).getBlock().setType(Material.COBBLESTONE);
		base.clone().add(2, 0, 2).getBlock().setType(Material.COBBLESTONE);
		base.clone().add(0, 0, 1).getBlock().setType(Material.COBBLESTONE);
		base.clone().add(0, 0, 2).getBlock().setType(Material.COBBLESTONE);
	}
	
	//If the falling blocks do not land for whatever reason, the crate creation is not triggered
	//This back up timer waits 15 seconds, if the falling blocks have not landed, manually creates the crate
	public static void startBackupTimer(Location location) {
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				if (!landed) {
					createCrate(location);
					landed = true;
				}
				for (Chunk chunk : getSurroundingChunks(location)) {
					chunk.setForceLoaded(false);
					chunk.unload();
				}
				
			}
		}.runTaskLater(plugin, 15 * 20);
	}
	
	public static void createCrate(Location location) {
		
		for (Entity entity : location.getWorld().getNearbyEntities(location, 10, 256, 10)) {
			if (entity instanceof FallingBlock) {
				entity.remove();
			}
		}
		
		WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
		File crate = new File(worldEditPlugin.getDataFolder() + File.separator + "/schematics/Crate.schem");
		EditSession session = worldEditPlugin.getWorldEdit().getEditSessionFactory().getEditSession(new BukkitWorld(location.getWorld()), 1000);
		
		//Must paste crate before outer crate or else the signs will break
		
		//paste crate
		ClipboardFormat format = ClipboardFormats.findByFile(crate);
		try {
			ClipboardReader reader = format.getReader(new FileInputStream(crate));
			Clipboard clipboard = reader.read();
			
			Operation operation = new ClipboardHolder(clipboard).createPaste(session)
					.to(BlockVector3.at(location.getX(), location.getY(), location.getZ())).ignoreAirBlocks(true).build();
			try {
			Operations.complete(operation);
			session.flushSession();
			}
			catch (WorldEditException e) {
				e.printStackTrace();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		location.getWorld().playSound(location, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 10f, 0.8f);
		fillChest(location);
		
		//Remove crate and scoreboards, schedule next crate drop after 10 minutes
		new Timer().schedule(new TimerTask() { //3 minute timer
			
			@Override
			public void run() {
				new BukkitRunnable() {
					
					@Override
					public void run() {
						
						WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
						File airCube = new File(worldEditPlugin.getDataFolder() + File.separator + "/schematics/AirCube.schematic");
						EditSession session = worldEditPlugin.getWorldEdit().getEditSessionFactory().getEditSession(new BukkitWorld(location.getWorld()), 1000);
						
						ClipboardFormat airCubeFormat = ClipboardFormats.findByFile(airCube);
						try {
							ClipboardReader airCubeReader = airCubeFormat.getReader(new FileInputStream(airCube));
							Clipboard airCubeClipboard = airCubeReader.read();
							
							Operation airCubeOperation = new ClipboardHolder(airCubeClipboard).createPaste(session)
									.to(BlockVector3.at(location.getX(), location.getY(), location.getZ())).ignoreAirBlocks(false).build();
							try {
							Operations.complete(airCubeOperation);
							session.flushSession();
							}
							catch (WorldEditException e) {
								e.printStackTrace();
							}
						}
						catch (IOException e) {
							e.printStackTrace();
						}

					}
				}.runTask(plugin);
			}
		}, 180 * 1000); 
	}
	
	public static void fillChest(Location location) {
		location.clone().add(1, 1, 1).getBlock().setType(Material.CHEST);
		Chest chest = (Chest) location.clone().add(1, 1, 1).getBlock().getState();
		
		Inventory inventory = chest.getInventory();
		for (int i = 0; i < inventory.getSize(); i++) { //Loop through each item slot in inventory
			inventory.setItem(i, getCrateItem());
		}
	}
	
	public static Location getRandomLocation() {
		
		Random randomX = new Random();
		Random randomZ = new Random();
		
		int x = randomX.nextInt(1200) - 500;
		int z = randomZ.nextInt(1200) - 500;
		if (z > -115 && z < 485 && x > -245 && x < 320) {
			return getRandomLocation();
		}
		else {
			Location location = new Location(Bukkit.getServer().getWorld("world"), x, 256, z);
			return location;
		}
	}
	
	@EventHandler
	public void onBlockLand(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof FallingBlock) {
			if (event.getEntity().hasMetadata("crate")) {
				event.setCancelled(true);
				if (event.getEntity().hasMetadata("reference")) {
					createCrate(event.getEntity().getLocation());
					landed = true;
				}
			}
		}
	}
	
	private static ItemStack getCrateItem() {
		
		Set<String> itemNames = plugin.getConfig().getConfigurationSection("loot.crate drop").getKeys(false);
		double total = 0; //Total probability pool
		for (String itemName : itemNames) {
			double probability = 1 / plugin.getConfig().getDouble("loot.crate drop." + itemName);
			total += probability;
		}
		
		//Count up from 0 with increment = each individual probability, when random < counter choose that item
		double random = Math.random() * total; //Random number between 0 and total
		double counter = 0;
		ItemStack generatedItem = null;
		for (String itemName : itemNames) {
			double probability = 1 / plugin.getConfig().getDouble("loot.crate drop." + itemName);
			counter += probability;
			if (random <= counter) {
				generatedItem = plugin.getConfig().getItemStack("loot.items." + itemName);
				break;
			}
		}
		return generatedItem;
	}
}

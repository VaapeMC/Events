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
import me.vaape.rewards.Rewards;
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
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.boss.api.BossAPI;
import org.mineacademy.boss.api.event.BossDeathEvent;
import org.mineacademy.boss.model.Boss;
import org.mineacademy.boss.model.BossSpawnReason;
import org.mineacademy.boss.model.SpawnedBoss;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.sql.Array;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class Invasion implements CommandExecutor, Listener {

    static Events plugin;
    static WorldGuardPlugin worldGuard = Events.getWorldGuard();
    static WorldEditPlugin worldEdit = Events.getWorldEdit();

    public static ArrayList<String> invasion = Events.invasion;

    public static boolean invasionRunning = Events.invasionRunning;

    public boolean invasionWasDefended;

    public BukkitTask timerTask;

    public Invasion(Events passedPlugin) {
        plugin = passedPlugin;
        invasionWasDefended = plugin.getConfig().getBoolean("invasion.was defended");
    }

    private static int minutes = 30;
    private static int seconds = 0;
    public static Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
    public static Objective o = board.registerNewObjective("fish", "dummy");
    public static Score time;
    public static HashMap<String, Score> scores = new HashMap<String, Score>();


    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("invasionstart")) {
            if (sender.isOp()) {
                sender.sendMessage(ChatColor.GREEN + "Invasion started.");
                startInvasionMessages();
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            }
        }

        if (cmd.getName().equalsIgnoreCase("invasionstartnow")) {
            if (sender.isOp()) {
                sender.sendMessage(ChatColor.GREEN + "Invasion started.");
                startInvasion();
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            }
        }

        if (cmd.getName().equalsIgnoreCase("invasionend")) {
            if (sender.isOp()) {
                sender.sendMessage(ChatColor.GREEN + "Invasion ended.");
                endInvasion();
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            }
        }

        if (sender instanceof Player) {

            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("invasion")) {

                final int ninePM = 21;

                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                OffsetDateTime next9PM;
                if (now.getHour() >= ninePM) {
                    next9PM = now.plus(1, ChronoUnit.DAYS)
                            .withHour(ninePM)
                            .truncatedTo(ChronoUnit.HOURS);
                } else {
                    next9PM = now.withHour(ninePM)
                            .truncatedTo(ChronoUnit.HOURS);
                }

                long timeUntilInvasionInMillis = Duration.between(now, next9PM).toMillis();

                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Invasion info:");

                if (invasionRunning) {
                    player.sendMessage(ChatColor.BLUE + "Next invasion: " + ChatColor.GREEN + "Attacking now");
                } else {
                    player.sendMessage(ChatColor.BLUE + "Next invasion: " + ChatColor.GRAY + "Today 21:00 GMT");
                    player.sendMessage(ChatColor.BLUE + "Time until next invasion: " +
                            ChatColor.GRAY + String.format("%d hours %d minutes",
                            TimeUnit.SECONDS.toHours(timeUntilInvasionInMillis),
                            TimeUnit.SECONDS.toMinutes(timeUntilInvasionInMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(timeUntilInvasionInMillis))));
                }
            }
        }
        return false;
    }

    private void startInvasionMessages() {
        Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Invasion] " + ChatColor.BLUE + "Next attack in 1 hour at /warp Windrunner");

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {

            @Override
            public void run() {
                Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Invasion] " + ChatColor.BLUE + "Next attack in 30 minutes at /warp Windrunner");
            }
        }, 30 * 60 * 20); //30 minute warning
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {

            @Override
            public void run() {
                Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Invasion] " + ChatColor.BLUE + "Next attack in 10 minutes at /warp Windrunner");
            }
        }, 50 * 60 * 20); //10 minute warning
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {

            @Override
            public void run() {
                Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Invasion] " + ChatColor.BLUE + "Next attack in 5 minutes at /warp Windrunner");
            }
        }, 55 * 60 * 20); //5 minute warning
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Events.getInstance(), new Runnable() {

            @Override
            public void run() {
                Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[Invasion] " + ChatColor.BLUE + ChatColor.BOLD + "Attack has begun at /warp Windrunner! Use /sb invasion for scoreboard!");
                //startGuildWars();
            }
        }, 60 * 60 * 20); //Start
    }

    private void startInvasion() {

        invasionRunning = true;

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {

            if (Events.invasion.contains(player.getUniqueId().toString())) {
                player.setScoreboard(board);
            }

        }

        minutes = 30;
        seconds = 0;

        o.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Invasion");
        o.setDisplaySlot(DisplaySlot.SIDEBAR);

        plugin.getConfig().set("invasion", null); //Reset scoreboard in config
        plugin.saveConfig();

        //Timer
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                Map<String, Integer> allScores = new HashMap<>();

                for (String name : board.getEntries()) {
                    allScores.put(name, o.getScore(name).getScore());
                }

                if (allScores.size() > 1) {
                    for (Map.Entry<String, Integer> set : allScores.entrySet()) { //Iterate through all entries on the scoreboard
                        int value = set.getValue();
                        if (value != 999) { //This is the entry for the timer countdown

                        }
                    }
                }

                seconds--;

                scores.put(
                        ChatColor.BLUE + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds),
                        o.getScore(ChatColor.BLUE + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds)));

                scores.get(ChatColor.BLUE + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds)).setScore(999);

                if (seconds < 0) { //Every minute
                    minutes--;
                    seconds = 59;

                    spawnRandomBoss();

                    board.resetScores(ChatColor.BLUE + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes + 1, 0)); //Removes previous timer entry
                    board.resetScores(ChatColor.BLUE + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes + 1, -1));

                    if (minutes < 0) {

                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                endInvasion();
                            }
                        }.runTask(plugin);
                        cancel();
                        return;
                    }
                    else {
                        if ((minutes + 1) % 5 == 0) { //Every 5 minutes
                            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                                if (inInvasion(player.getLocation())) {
                                    empowerPlayer(player, 20);
                                }
                            }
                        }
                    }
                }
                else {
                    board.resetScores(ChatColor.BLUE + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds + 1)); //Removes previous timer entry
                }
            }
        }.runTaskTimer(plugin, 20, 5);
    }

    private void endInvasion() {

        timerTask.cancel();

        invasionRunning = false;

        //If time remaining still there, remove it
        for (String string : board.getEntries()) {
            if (!string.contains("Time remaining")) continue;
            board.resetScores(string);
        }

        //Check all alive bosses, if any are in invasion territory then not successfully defended
        for (Entity entity : Bukkit.getServer().getWorld("world").getEntities()) {
            Bukkit.getServer().broadcastMessage(ChatColor.GREEN + "entity: " + entity.getName() + " " + entity.getLocation());
            if (!BossAPI.isBoss(entity)) continue;
            if (!inInvasion(entity.getLocation())) continue;
            invasionWasDefended = false;
            Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "was boss in invasion");

        }

        if (invasionWasDefended) {
            Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Invasion] " + ChatColor.BLUE + "The heroes of the city have successfuly vanquished all invaders. The city is safe for now...");
            plugin.getConfig().set("invasion.was defended", true);
        }
        else {
            Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "[Invasion] " + ChatColor.BLUE + "The invaders have successfully overthrown Captain Windrunner and his defenses. The city feels colder...");
            plugin.getConfig().set("invasion.was defended", false);
        }
        plugin.saveConfig();

        purgeInvasion();
        clearBosses();


        //Get final placements
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

        //List<String> s = plugin.getConfig().getStringList("invasion.scores");
        List<String> s = new ArrayList<String>();

        for (String name : sortedScores.keySet()) {
            s.add(name + ":" + sortedScores.get(name));
        }

        plugin.getConfig().set("invasion.scores", s);
        plugin.saveConfig();

        //Updated
        s = plugin.getConfig().getStringList("invasion.scores");

        List<String> listOfNames = new ArrayList<>();

        //Each string = Name: score
        for (String string : s) {
            String[] words = string.split(":");
            listOfNames.add(words[0]);
        }

        Collections.reverse(listOfNames);

        List<OfflinePlayer> placed = new ArrayList<>();

        int count = 0;
        for (String name : listOfNames) {
            count = count + 1;
            placed.add(Bukkit.getServer().getOfflinePlayer(name));
            Bukkit.getServer().broadcastMessage(name);
        }

        for (OfflinePlayer player : placed) {
            Rewards.plugin.giveReward("degg", player, true);
        }
    }

    private static void purgeInvasion() {

        Location spawn = new Location(Bukkit.getServer().getWorld("world"), -26.5, 103, 193.5, -90, 0);

        spawn.getWorld().playSound(spawn, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 10f, 1.5f);
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {

            if (inSpawn(p.getLocation())) {
                p.teleport(spawn, TeleportCause.PLUGIN);
            }
        }
    }

    private static void clearBosses() {
        for (SpawnedBoss boss : BossAPI.getBosses(Bukkit.getServer().getWorld("world"))) {
            if (inInvasion(boss.getEntity().getLocation())) {
                boss.getEntity().remove();
            }
        }
    }

    public static void spawnRandomBoss() {
        List<String> bossNames = new ArrayList<>();
        bossNames.add("Salamander");
        Random randomizer = new Random();
        String name = bossNames.get(randomizer.nextInt(bossNames.size()));
        Boss boss = BossAPI.getBoss(name);
        Location loc = getRandomSpawnLocation();
        boss.spawn(loc, BossSpawnReason.CUSTOM);
    }

    public static Location getRandomSpawnLocation() {
        int xLowerBound = -27;
        int xUpperBound = 62;
        int zLowerBound = 321;
        int zUpperBound = 380;

        int xRange = xUpperBound - xLowerBound;
        int zRange = zUpperBound - zLowerBound;

        double randomX = (Math.random() * xRange) + xLowerBound;
        double randomZ = (Math.random() * zRange) + zLowerBound;

        return new Location(Bukkit.getServer().getWorld("world"), randomX, 95, randomZ);
    }

    public static boolean inInvasion(Location location) {
        World world = location.getWorld();

        com.sk89q.worldedit.util.Location WElocation = new com.sk89q.worldedit.util.Location(BukkitAdapter.adapt(world), location.getX(), location.getY(), location.getZ());

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(WElocation);

        for (ProtectedRegion region : set) {
            if (region.getId().equalsIgnoreCase("invasion")) {
                return true;
            }
        }
        return false;
    }

    public static Scoreboard getInvasionScoreboard() {
        Scoreboard invasionBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective invasionObjective = invasionBoard.registerNewObjective("invasionBoard", "dummy");
        HashMap<String, Score> invasionScores = new HashMap<String, Score>();

        invasionObjective.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Invasion");
        invasionObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> s = plugin.getConfig().getStringList("invasion.scores");

        for (String string : s) {
            String[] words = string.split(":");
            invasionScores.put(words[0], invasionObjective.getScore(words[0]));
            invasionScores.get(words[0]).setScore(Integer.parseInt(words[1]));
        }

        return invasionBoard;
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

    private static void empowerPlayer(Player player, int seconds) {
        player.sendMessage(ChatColor.BLUE + "You feel a burst of energy as Captain Windrunner empowers you...");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, seconds * 20, 4, false, true), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, seconds * 20, 4, false, true), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, seconds * 20, 1, false, true), true);

    }

    @EventHandler
    public void onRightClickEntity(PlayerInteractEntityEvent event) {
        if (invasionRunning) {
            if (event.getRightClicked() instanceof Villager) {
                if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.NAME_TAG || event.getPlayer().getInventory().getItemInOffHand().getType() == Material.NAME_TAG) {
                    event.setCancelled(true);
                }
                if (inInvasion(event.getRightClicked().getLocation())) {
                    event.setCancelled(true);
                    if (event.getRightClicked().getCustomName() == null) return;
                    if (!event.getRightClicked().getCustomName().contains("Captain Windrunner")) return;
                    empowerPlayer(event.getPlayer(), 10);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath (EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        if (!invasionRunning) return;
        Player player = event.getEntity().getKiller();
        if (!inInvasion(event.getEntity().getLocation())) return;
        if (BossAPI.isBoss(event.getEntity())) return;
        addKillsToScoreboard(player, 1);
    }

    @EventHandler
    public void onBossDeath (BossDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        if (!invasionRunning) return;
        Player player = event.getEntity().getKiller();
        if (!inInvasion(event.getEntity().getLocation())) return;
        addKillsToScoreboard(player, 5);

        celebratePlayerKillBoss(player);
    }

    //Remove player from castle on login
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (invasionRunning) {
            if (inInvasion(event.getPlayer().getLocation())) {
                event.getPlayer().teleport(new Location(Bukkit.getServer().getWorld("world"), -26.5, 103, 193, -90, 0), TeleportCause.PLUGIN);
            }
        }
    }

    public void addKillsToScoreboard(Player player, int number) {
        if (scores.get(player.getName()) == null) { //If not already in scoreboard add initial fish
            scores.put(player.getName(), o.getScore(player.getName()));
            scores.get(player.getName()).setScore(plugin.getConfig().getInt("invasion." + number));
            Bukkit.getServer().broadcastMessage("player not on scoreboard");

        }
        else { //If already in scoreboard add to number of fish
            scores.get(player.getName()).setScore(scores.get(player.getName()).getScore() + number);
            Bukkit.getServer().broadcastMessage("player on scoreboard, adding kills");

        }
    }

    public void celebratePlayerKillBoss(Player player) {
        player.getWorld().playSound(((Player) player).getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.5f);
        player.sendMessage(ChatColor.BLUE + "You feel the wind run through you...");
        spawnFirework(player.getLocation(), true);
        player.setVelocity(new Vector(0, 1, 0));
    }

    public static void spawnFirework(Location location, boolean doInitialExplosion){

        if (doInitialExplosion) {
            Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
            FireworkMeta fwm = fw.getFireworkMeta();

            fwm.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(Color.fromRGB(0xFF5C5C)).withFade(Color.WHITE, Color.fromRGB(0xFFADAD)).build());
            fw.setFireworkMeta(fwm);
            fw.detonate();
        }

        Firework fw2 = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta fwm2 = fw2.getFireworkMeta();

        fwm2.setPower(2);
        fwm2.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(Color.fromRGB(0xFF5C5C)).withFade(Color.WHITE, Color.fromRGB(0xFFADAD)).build());
        fwm2.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(Color.fromRGB(0xFF5C5C)).withFade(Color.WHITE).build());
        fwm2.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BURST).withColor(Color.fromRGB(0xFF5C5C)).withFade(Color.WHITE).flicker(true).build());

        fw2.setFireworkMeta(fwm2);
    }
}

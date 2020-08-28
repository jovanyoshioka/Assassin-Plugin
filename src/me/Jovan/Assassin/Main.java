package me.Jovan.Assassin;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.Jovan.Assassin.Files.DataManager;
import net.ess3.api.Economy;
import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener
{
	
	public DataManager m_data;
	
	private static Economy m_essentialsEconomy;
	private Hire m_hire;
	private Bounty m_bounty;
	
	// Runs on startup, reloads, or plugin reloads.
	@Override
	public void onEnable()
	{	
		// Allows access to data config file.
		this.m_data = new DataManager(this);
		
		RegisteredServiceProvider<Economy> eProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (eProvider != null)
		{
			m_essentialsEconomy = eProvider.getProvider();
		}
		
		m_hire = new Hire(this.m_data);
		m_bounty = new Bounty(this.m_data);
		
		this.getServer().getPluginManager().registerEvents(this, this);
		
		System.out.println("Assassin plugin loaded!");
	}
	
	// Runs on shutdown, reloads, and plugin reloads.
	@Override
	public void onDisable()
	{	
		
	}
	
	// Runs when command is entered.
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		// Verify that the sender is a player.
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			
			switch (label.toLowerCase())
			{
				case "assassin":
					if (player.hasPermission("assassin.assassin"))
					{
						return this.runAssassinCmd(player, args);
					} else
					{
						player.sendMessage(ChatColor.RED + "You do not have permission to that command!");
						return true;
					}
				case "hire":
					if (player.hasPermission("assassin.hire"))
					{
						return m_hire.runHireCmd(player, args);
					} else
					{
						player.sendMessage(ChatColor.RED + "You do not have permission to that command!");
						return true;
					}
				case "hireaccept":
					if (player.hasPermission("assassin.hireaccept"))
					{
						return m_hire.runHireAcceptCmd(player, args);
					} else
					{
						player.sendMessage(ChatColor.RED + "You do not have permission to that command!");
						return true;
					}
				case "hiredeny":
					if (player.hasPermission("assassin.hiredeny"))
					{
						return m_hire.runHireDenyCmd(player, args);
					} else
					{
						player.sendMessage(ChatColor.RED + "You do not have permission to that command!");
						return true;
					}
				case "hirereveal":
					if (player.hasPermission("assassin.hirereveal"))
					{
						return m_hire.runRevealCmd(player, args);
					} else
					{
						player.sendMessage(ChatColor.RED + "You do not have permission to that command!");
						return true;
					}
				case "hirecancel":
					if (player.hasPermission("assassin.hirecancel"))
					{
						return m_hire.runCancelContractCmd(player, args);
					} else
					{
						player.sendMessage(ChatColor.RED + "You do not have permission to that command!");
						return true;
					}
				case "hirelist":
					if (player.hasPermission("assassin.hirelist"))
					{
						return m_hire.runListCmd(player);
					} else
					{
						player.sendMessage(ChatColor.RED + "You do not have permission to that command!");
						return true;
					}
				case "bounty":
					if (player.hasPermission("assassin.bounty"))
					{
						return m_bounty.runBountyCmd(player, args);
					} else
					{
						player.sendMessage(ChatColor.RED + "You do not have permission to that command!");
						return true;
					}
				case "bountylist":
					if (player.hasPermission("assassin.bountylist"))
					{
						return m_bounty.runListCmd(player);
					} else
					{
						player.sendMessage(ChatColor.RED + "You do not have permission to that command!");
						return true;
					}
				default:
					return true;
			}
		} else
		{
			sender.sendMessage("Console cannnot run this command.");
			return true;
		}
	}
	
	@EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
		// Create runnable and schedule as SyncDelayedTask to display notifications after join message.
		// Create a long 2 delay (3rd argument) to display after essentials plugin join messages.
		Runnable runnable = () -> sendJoinNotification(e);
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, runnable, 3);
    }
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e)
	{
		// Get the target and assassin.
		String playerKilled = e.getEntity().getName();
		String playerKiller = null;
		if (e.getEntity().getKiller() != null)
		{
			playerKiller = e.getEntity().getKiller().getName();
		}
		
		
		// If contracts are active, fulfill them and send custom death message. Otherwise, do nothing.
		// Return 0: No contract found, do nothing, return 1: contract fulfilled by assassin, return 
		// 2: target defended themself, gets reward instead. 3: playerKiller was both an assassin and target in relation to playerKilled.
		int contractResult = 0;
		if (playerKiller != null)
		{
			contractResult = m_hire.fulfillContract(playerKiller, playerKilled);
		}
		if (contractResult == 1)
		{
			// Assassin fulfilled contract.
			// Play victory sound and send message
			e.getEntity().getKiller().playSound(e.getEntity().getKiller().getLocation(), "ui.toast.challenge_complete", 1, 1);
			e.setDeathMessage("§4" + ChatColor.BOLD + playerKiller + "§c" + ChatColor.BOLD + " has successfully executed §4" 
					+ ChatColor.BOLD + playerKilled + "§c" + ChatColor.BOLD + "!");
		} else if (contractResult == 2)
		{
			// Target defended themself.
			// Play victory sound and send message
			e.getEntity().getKiller().playSound(e.getEntity().getKiller().getLocation(), "ui.toast.challenge_complete", 1, 1);
			e.setDeathMessage("§4" + ChatColor.BOLD + playerKiller + "§c" + ChatColor.BOLD + " has defended themself against §4" 
					+ ChatColor.BOLD + playerKilled + "§c" + ChatColor.BOLD + "!");
		} else if (contractResult == 3)
		{
			// playerKiller was both an assassin and target in relation to playerKilled.
			e.getEntity().getKiller().playSound(e.getEntity().getKiller().getLocation(), "ui.toast.challenge_complete", 1, 1);
			e.setDeathMessage("§4" + ChatColor.BOLD + playerKiller + "§c" + ChatColor.BOLD + " has successfully executed and defended "
					+ "themself against §4" + ChatColor.BOLD + playerKilled + "§c" + ChatColor.BOLD + "!");
		}
		
		boolean bountyObtained = false;
		if (playerKiller != null && (!(playerKiller.equalsIgnoreCase(playerKilled))))
		{
			bountyObtained = m_bounty.obtainBounty(playerKiller, playerKilled);
		}
		// If bounty is obtained, message entire server.
		if (bountyObtained == true)
		{
			// Bounty has been obtained.
			Bukkit.broadcastMessage("§d§l" + playerKiller + " has claimed the bounty on " + playerKilled + "!");
		}
		
		// If assassination took place, put items of the individual who died in a chest.
		if (contractResult > 0 || bountyObtained == true)
		{
			// Get inventory and place them inside two chests.
			// Require two chests because size of inventory.
			
			// Get player inventory before death.
			Inventory playerInventory = e.getEntity().getInventory();
			
			// Get block where player died, and the block above it.
			// Shouldn't have trouble with overwriting another block because the player
			// model can't go inside blocks -- the two death blocks will mimic the player model.
			Block blockBottom = e.getEntity().getLocation().getBlock();
			Block blockTop = e.getEntity().getLocation().add(0, 1, 0).getBlock();
			
			// Set death blocks to chests.
			blockBottom.setType(Material.CHEST);
			blockTop.setType(Material.CHEST);
			
			// Create chest data objects to place inventory into.
			Chest chestBottom = (Chest) blockBottom.getState();
			Chest chestTop = (Chest) blockTop.getState();
			
			// Get inventory of chests to place items in.
			Inventory chestBottomInv = chestBottom.getInventory();
			Inventory chestTopInv = chestTop.getInventory();
			
			// Loop through chests' indexes and place items inside relative to inventory items' indexes.
			for (int i = 0; i < chestBottomInv.getSize(); i++)
			{
				chestBottomInv.setItem(i, playerInventory.getItem(i));
			}
			int chestTopSize = playerInventory.getSize() - chestBottomInv.getSize();
			for (int i = 0; i < chestTopSize; i++)
			{
				chestTopInv.setItem(i, playerInventory.getItem(chestBottomInv.getSize() + i));
			}
			
			// Clear drops because they are placed in chest already.
			// Not clearing will duplicate the items.
			e.getDrops().clear();
		}
	}
	
	// Get essentials economy api for other classes
	public static Economy getEconomyAPI()
	{
		return m_essentialsEconomy;
	}
	
	// Get player balance
	public static BigDecimal getPlayerBalance(Player playerInstance)
	{
		BigDecimal currentMoney;
		try
		{
			currentMoney = m_essentialsEconomy.getMoneyExact(playerInstance.getName());
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
		return currentMoney;
	}
	
	// Check if player has any pending contracts requiring action on player join.
	// In other words, check if player is an assassin in any pending contracts.
	public void sendJoinNotification(PlayerJoinEvent event)
	{
		// Integer used to accumulate the number of pending contracts to display for user.
		int numOfPending = 0;
		
		// Loop through all pending contracts.
		List<String> pendingAssassins = m_data.getConfig().getStringList("pendingcontracts.assassin");
		for (int i = 0; i < pendingAssassins.size(); i++)
		{
			if (pendingAssassins.get(i).equalsIgnoreCase(event.getPlayer().getName()))
			{
				// A pending contract was found, add to numOfPending integer to be displayed at the end of looping.
				numOfPending++;
			}
		}
		
		// If there are pending contracts, send notification.
		if (numOfPending != 0)
		{
			event.getPlayer().sendMessage("§4You have §l" + numOfPending + " §4pending assassination contract(s) awaiting your approval.");
			event.getPlayer().sendMessage("§4Use §l/hirelist§4 to view them and §l/hireaccept§4 or §l/hiredeny§4 to take action.");
		}
	}
	
	// Used to sort players by successful assassinations for leaderboard command.
	public static HashMap<String, Integer> sortLeaderboard(HashMap<String, Integer> hm) 
    { 
        // Create a list from elements of HashMap.
        List<Map.Entry<String, Integer> > list = 
               new LinkedList<Map.Entry<String, Integer> >(hm.entrySet()); 
  
        // Sort the list.
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() { 
            public int compare(Map.Entry<String, Integer> o1,  
                               Map.Entry<String, Integer> o2) 
            { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        });
        
        // Reverse the order of the list.
        Collections.reverse(list);
          
        // Put data from sorted list to HashMap.
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>(); 
        for (Map.Entry<String, Integer> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        }
        
        return temp; 
    } 
	
	// Runs leaderboard or stats command based on argument entered.
	public boolean runAssassinCmd(Player playerInstance, String[] arguments)
	{
		// Verify leaderboard or stats was entered.
		if (arguments.length == 1)
		{
			// Verify config formatted correctly.
			if (m_data.getConfig().contains("stats.player") && m_data.getConfig().contains("stats.assassinations")
					&& m_data.getConfig().contains("stats.counters") && m_data.getConfig().contains("leaderboard"))
			{
				// Check if player already has a stat count, if not, create entry for them.
				List<String> statsPlayers = m_data.getConfig().getStringList("stats.player");
				List<String> statsAssassinations = m_data.getConfig().getStringList("stats.assassinations");
				List<String> statsCounters = m_data.getConfig().getStringList("stats.counters");
				boolean foundPlayerStats = false;
				for (int i = 0; i < statsPlayers.size(); i++)
				{
					// Check if current player in config matches playerInstance.
					if (statsPlayers.get(i).equalsIgnoreCase(playerInstance.getName()))
					{
						foundPlayerStats = true;
					}
				}
				if (foundPlayerStats == false)
				{
					// Stats for playerInstance was not found, so create an entry for them.
					statsPlayers.add(playerInstance.getName());
					statsAssassinations.add("0");
					statsCounters.add("0");
					
					// Save added entry to config.
					m_data.getConfig().set("stats.player", statsPlayers);
					m_data.saveConfig();
					m_data.getConfig().set("stats.assassinations", statsAssassinations);
					m_data.saveConfig();
					m_data.getConfig().set("stats.counters", statsCounters);
					m_data.saveConfig();
				}
				
				// Check for which /assassin command to run, leaderboard or stats, and run it.
				if (arguments[0].equalsIgnoreCase("leaderboard")) // /assassin leaderboard, display leaderboard of assassins.
				{
					statsPlayers = m_data.getConfig().getStringList("stats.player");
					statsAssassinations = m_data.getConfig().getStringList("stats.assassinations");
					
					// Create HashMap to sort the players based on the contract completion count.
					HashMap<String, Integer> unsortedLeaderboard = new HashMap<String, Integer>();
					for (int i = 0; i < statsPlayers.size(); i++)
					{
						if (!(statsPlayers.get(i).equalsIgnoreCase("null") && statsAssassinations.get(i).equalsIgnoreCase("-1")))
						{
							unsortedLeaderboard.put(statsPlayers.get(i), Integer.parseInt(statsAssassinations.get(i)));
						}
					}
					
					// Sort players on leaderboard.
					Map<String, Integer> sortedLeaderboard = sortLeaderboard(unsortedLeaderboard);
					
					// Send playerInstance the leaderboard via sendMessage.
					playerInstance.sendMessage("§f§l--- §4§lAssassin §c§lLeaderboard §f§l---");
					int i = 0;
					for (Map.Entry<String, Integer> en : sortedLeaderboard.entrySet()) { 
			            if (i == m_data.getConfig().getInt("leaderboard"))
			            {
			            	break;
			            } else
			            {
			            	playerInstance.sendMessage("§f" + (i+1) + ". §4" + en.getKey() + "§f, §c" + en.getValue() + " Contracts/Bounties");
			            	i++;
			            }
			        }
					
					return true;
				} else if (arguments[0].equalsIgnoreCase("stats")) // /assassin stats, display stats (assassinations, counters, etc.)
				{
					statsPlayers = m_data.getConfig().getStringList("stats.player");
					statsAssassinations = m_data.getConfig().getStringList("stats.assassinations");
					statsCounters = m_data.getConfig().getStringList("stats.counters");
					
					// Loop through and find player.
					for (int i = 0; i < statsPlayers.size(); i++)
					{
						// Check if player in statsPlayers matches the playerInstance name, if so, display stats.
						if (statsPlayers.get(i).equalsIgnoreCase(playerInstance.getName()))
						{
							playerInstance.sendMessage("§f§l--- §4§lAssassin §c§lStats §f§l---");
							playerInstance.sendMessage("§4Contracts Fulfilled: §c" + statsAssassinations.get(i));
							playerInstance.sendMessage("§4Contracts Countered: §c" + statsCounters.get(i));
						}
					}
					
					return true;
				} else
				{
					// Incorrect syntax error.
					playerInstance.sendMessage(ChatColor.RED + "Incorrect syntax. Usage: /assassin <leaderboard/stats>.");
					return true;
				}
			} else
			{
				// Config not formatted correctly.
				playerInstance.sendMessage(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
				return true;
			}
		} else
		{
			// Incorrect syntax error.
			playerInstance.sendMessage(ChatColor.RED + "Incorrect syntax. Usage: /assassin <leaderboard/stats>.");
			return true;
		}
	}
}
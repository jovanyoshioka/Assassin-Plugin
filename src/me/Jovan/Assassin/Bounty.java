package me.Jovan.Assassin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Jovan.Assassin.Files.DataManager;
import net.ess3.api.Economy;
import net.md_5.bungee.api.ChatColor;

public class Bounty {

  private static DataManager m_data;
  Economy m_essentialsEconomy;
  
  public Bounty(DataManager data)
  {
    m_data = data;
    
    m_essentialsEconomy = Main.getEconomyAPI();
  }
  
  // Verify config is formatted correctly.
  public static boolean verifyConfig()
  {
    return (m_data.getConfig().contains("bounties.target") && m_data.getConfig().contains("bounties.reward")
        && m_data.getConfig().contains("minimumbounty") && m_data.getConfig().contains("stats.player") 
        && m_data.getConfig().contains("stats.assassinations") && m_data.getConfig().contains("stats.counters"));
  }
  
  public int getTotalBountyReward(String targetName)
  {
    // Get all data to search through.
    List<String> targets = m_data.getConfig().getStringList("bounties.target");
    List<String> rewards = m_data.getConfig().getStringList("bounties.reward");
    
    // Loop through all targets to find matching name.
    for (int i = 0; i < targets.size(); i++)
    {
      // Check if config target matches entered target.
      if (targets.get(i).equalsIgnoreCase(targetName))
      {
        return Integer.parseInt(rewards.get(i));
      }
    }
    
    // Target not found.
    return -1;
  }
  
  public boolean runBountyCmd(Player playerInstance, String[] arguments)
  {
    // Verify syntax is correct.
    if (arguments.length == 2)
    {
      // Verify target player exists.
      Player target = Bukkit.getPlayer(arguments[0]);
      if (target != null) // Player for target does exist.
      {
        // Verify reward is a numeric value and will not throw a NumberFormatException.
        int verifiedReward = 0;
        try
        {
          verifiedReward = Integer.parseInt(arguments[1]);
        } catch (NumberFormatException e)
        {
          playerInstance.sendMessage(ChatColor.RED + "Your monetary reward must be a numeric value; only use digits.");
          return true;
        }
        
        // Verify config has been formatted correctly.
        if (this.verifyConfig() == true)
        {
          if (verifiedReward >= m_data.getConfig().getInt("minimumbounty"))
          {	
            // Verify player has enough money to place a bounty.
            if (Main.getPlayerBalance(playerInstance).intValue() >= verifiedReward)
            {	
              // Place bounty on target.
              try
              {
                // Take funds from employer's balance.
                BigDecimal reward = new BigDecimal(Integer.parseInt(arguments[1]));
                m_essentialsEconomy.substract(playerInstance.getName(), reward);
                
                // Get data from bounties.
                List<String> targets = m_data.getConfig().getStringList("bounties.target");
                List<String> rewards = m_data.getConfig().getStringList("bounties.reward");
                
                // Add to reward of existing target.
                boolean foundTarget = false;
                for (int i = 0; i < targets.size(); i++)
                {
                  // Check if target name matches. If so, add to the reward.
                  if (targets.get(i).equalsIgnoreCase(arguments[0]))
                  {
                    // Found target
                    foundTarget = true;
                    
                    // Add to existing total reward.
                    rewards.set(i, "" + (Integer.parseInt(rewards.get(i)) + Integer.parseInt(arguments[1])));
                    m_data.getConfig().set("bounties.reward", rewards);
                    m_data.saveConfig();
                  }
                }
                
                // If foundTarget is false after for loop, target is not on bounty list yet.
                // Insert target and reward into bounty list in config.
                if (foundTarget == false)
                {
                  // Insert data into bounties in config file.
                  targets.add(Bukkit.getPlayer(arguments[0]).getName());
                  m_data.getConfig().set("bounties.target", targets);
                  m_data.saveConfig();
                  
                  rewards.add(reward.toString());
                  m_data.getConfig().set("bounties.reward", rewards);
                  m_data.saveConfig();
                }
                
                // Notify employer of successful bounty placement.
                playerInstance.sendMessage(ChatColor.GREEN + "$" + reward + " has been taken "
                    + "from your account and a bounty has been placed on " + arguments[0] + ".");
                
                // Notify entire server.
                Bukkit.broadcastMessage("�d�lA bounty has been placed on �f�l" + Bukkit.getPlayer(arguments[0]).getName() + " �d�lwith a "
                    + "reward of �a�l$" + reward + "�d�l. This increases the total bounty reward on �f�l" + Bukkit.getPlayer(arguments[0]).getName()
                    + " �d�lto �a�l$" + this.getTotalBountyReward(Bukkit.getPlayer(arguments[0]).getName()) + "�d�l.");
                
                return true;
              } catch (Exception e)
              {
                e.printStackTrace();
                return true;
              }
            } else
            {
              // Not enough money.
              playerInstance.sendMessage(ChatColor.RED + "You do not have sufficient funds.");
              return true;
            }
          } else
          {
            // Did not meet minimum bounty value.
            playerInstance.sendMessage(ChatColor.RED + "A bounty reward must be at least $" + m_data.getConfig().getInt("minimumbounty"));
            return true;
          }
        } else
        {
          playerInstance.sendMessage(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
          return true;
        }
      } else
      {
        // Target player does not exist.
        playerInstance.sendMessage(ChatColor.RED + "Player assigned as the target does not exist.");
        return true;
      }
    } else
    {
      // Incorrect syntax error.
      playerInstance.sendMessage(ChatColor.RED + "Incorrect syntax. Usage: /bounty <target> <reward>.");
      return true;
    }
  }
  
  // Lists all bounties.
  public boolean runListCmd(Player playerInstance)
  {
    // Verify config is formatted correctly.
    if (this.verifyConfig())
    {
      // Master array list that will be looped through and printed to the player.
      ArrayList<List<String>> bountiesList = new ArrayList<List<String>>();
      
      // Get config lists.
      List<String> targets = m_data.getConfig().getStringList("bounties.target");
      List<String> rewards = m_data.getConfig().getStringList("bounties.reward");
      
      // Loop through config and add to contractLists.
      for (int i = 0; i < targets.size(); i++)
      {
        bountiesList.add(Arrays.asList(targets.get(i), rewards.get(i)));
      }
      
      if (bountiesList.size() > 1)
      {
        playerInstance.sendMessage("�4�lBounties�f�l: | TARGET | REWARD |");
        for (int i = 0; i < bountiesList.size(); i++)
        {
          if (!(bountiesList.get(i).get(0).equalsIgnoreCase("null") && bountiesList.get(i).get(1).equalsIgnoreCase("-1")))
          {
            playerInstance.sendMessage("�4" + (i) + "�r: | " + bountiesList.get(i).get(0) + " | $" + bountiesList.get(i).get(1) + " |");
          }
        }
        return true;
      } else
      {
        playerInstance.sendMessage(ChatColor.RED + "There are no bounties currently.");
        return true;
      }
    } else
    {
      // Config not formatted correctly.
      playerInstance.sendMessage(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
      return true;
    }
  }
  
  // Checks if bounty is on someone's head and rewards killer if so.
  public boolean obtainBounty(String killerPlayer, String killedPlayer)
  {
    // Verify config is formatted correctly.
    if (this.verifyConfig())
    {
      // Get all targets and rewards to compare with killed player.
      List<String> targets = m_data.getConfig().getStringList("bounties.target");
      List<String> rewards = m_data.getConfig().getStringList("bounties.reward");
      
      // Loop through targets and check if any match killed player.
      for (int i = 0; i < targets.size(); i++)
      {
        if (targets.get(i).equalsIgnoreCase(killedPlayer))
        {
          // Player killed had a bounty on them, reward killer.
          
          // Store reward, will be used later.
          String rewardString = rewards.get(i);
          
          // Remove target from bounty list.
          targets.remove(i);
          m_data.getConfig().set("bounties.target", targets);
          m_data.saveConfig();
          rewards.remove(i);
          m_data.getConfig().set("bounties.reward", rewards);
          m_data.saveConfig();
          
          // Add one to player's assassinations stats count.
          List<String> statsPlayers = m_data.getConfig().getStringList("stats.player");
          List<String> statsAssassinations = m_data.getConfig().getStringList("stats.assassinations");
          List<String> statsCounters = m_data.getConfig().getStringList("stats.counters");
          
          // Loop through to find player in stats.
          boolean foundPlayer = false;
          for (int j = 0; j < statsPlayers.size(); j++)
          {
            if (statsPlayers.get(j).equalsIgnoreCase(killerPlayer))
            {
              // Set foundPlayer to true so code does not create another entry for player in stats.
              foundPlayer = true;
              
              // Add one to assassinations count.
              statsAssassinations.set(j, "" + (Integer.parseInt(statsAssassinations.get(j))+1));
              m_data.getConfig().set("stats.assassinations", statsAssassinations);
              m_data.saveConfig();
            }
          }
          
          // If entry was not found, make one for player.
          if (foundPlayer == false)
          {
            statsPlayers.add(killerPlayer);
            m_data.getConfig().set("stats.player", statsPlayers);
            m_data.saveConfig();
            
            statsAssassinations.add("1");
            m_data.getConfig().set("stats.assassinations", statsAssassinations);
            m_data.saveConfig();
            
            statsCounters.add("0");
            m_data.getConfig().set("stats.counters", statsCounters);
            m_data.saveConfig();
          }
          
          // Add reward amount to killer player's balance.
          BigDecimal reward = new BigDecimal(Integer.parseInt(rewardString));
          try
          {
            m_essentialsEconomy.add(killerPlayer, reward);
          } catch (Exception e)
          {
            e.printStackTrace();
            return false;
          }
          
          // Message player that they got the bounty.
          Bukkit.getPlayer(killerPlayer).playSound(Bukkit.getPlayer(killerPlayer).getLocation(), "ui.toast.challenge_complete", 1, 1);
          Bukkit.getPlayer(killerPlayer).sendMessage("�a�lYou have successfully claimed the bounty on �f�l" + killedPlayer + " �a�lfor $" + reward + ".");
          
          return true;
        }
      }
      
      return false;
    } else
    {
      System.out.println(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
      return false;
    }
  }
}

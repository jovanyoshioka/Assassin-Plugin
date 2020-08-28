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
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Hire
{
  
  private static DataManager m_data;
  Economy m_essentialsEconomy;
  
  // Constructor
  public Hire(DataManager data)
  {
    m_data = data;
    
    m_essentialsEconomy = Main.getEconomyAPI();
  }
  
  // Verify config is formatted correctly.
  public static boolean verifyConfig()
  {
    return (m_data.getConfig().contains("pendingcontracts.employer") && m_data.getConfig().contains("pendingcontracts.assassin") 
        && m_data.getConfig().contains("pendingcontracts.target") && m_data.getConfig().contains("pendingcontracts.reward")
        && m_data.getConfig().contains("activecontracts.employer") && m_data.getConfig().contains("activecontracts.assassin")
        && m_data.getConfig().contains("activecontracts.target") && m_data.getConfig().contains("activecontracts.reward")
        && m_data.getConfig().contains("activecontracts.revealed") && m_data.getConfig().contains("completecontracts.employer")
        && m_data.getConfig().contains("completecontracts.assassin") && m_data.getConfig().contains("completecontracts.target")
        && m_data.getConfig().contains("completecontracts.reward") && m_data.getConfig().contains("completecontracts.lost")
        && m_data.getConfig().contains("listrestriction") && m_data.getConfig().contains("stats.player") 
        && m_data.getConfig().contains("stats.assassinations") && m_data.getConfig().contains("stats.counters") 
        && m_data.getConfig().contains("leaderboard"));
  }
  
  // Creates contract proposal.
  public boolean runHireCmd(Player playerInstance, String[] arguments)
  {
    // Verify syntax is correct.
    if (arguments.length == 3)
    {
      // Verify player is not hiring themself
      if (!(playerInstance.getName().equalsIgnoreCase(arguments[0])))
      {
        // Verify assassin player exists.
        Player assassin = Bukkit.getPlayer(arguments[0]);
        if (assassin != null) // Player for assassin exists.
        {
          // Verify target player exists.
          Player target = Bukkit.getPlayer(arguments[1]);
          if (target != null) // Player for target does exist.
          {
            // Verify reward is a numeric value and will not throw a NumberFormatException.
            int verifiedReward = 0;
            try
            {
              verifiedReward = Integer.parseInt(arguments[2]);
            } catch (NumberFormatException e)
            {
              playerInstance.sendMessage(ChatColor.RED + "Your monetary reward must be a numeric value; only use digits.");
              return true;
            }
            
            // Verify player has enough money to create the contract.
            if (verifiedReward > 0 && Main.getPlayerBalance(playerInstance).intValue() >= verifiedReward)
            {	
              // Verify config has been formatted correctly.
              if (this.verifyConfig() == true)
              {
                // Verify contract with the same employer and target is not already made
                // Verify pending contracts.
                List<String> employers = m_data.getConfig().getStringList("pendingcontracts.employer");
                List<String> assassins = m_data.getConfig().getStringList("pendingcontracts.assassin");
                List<String> targets = m_data.getConfig().getStringList("pendingcontracts.target");
                for (int i = 0; i < employers.size(); i++)
                {
                  if (employers.get(i).equalsIgnoreCase(playerInstance.getName()) && targets.get(i).equalsIgnoreCase(arguments[1]))
                  {
                    playerInstance.sendMessage(ChatColor.RED + "You have already hired someone to assassinate that target!");
                    return true;
                  }
                }
                
                // Verify active contracts.
                List<String> activeEmployers = m_data.getConfig().getStringList("activecontracts.employer");
                List<String> activeAssassins = m_data.getConfig().getStringList("activecontracts.assassin");
                List<String> activeTargets = m_data.getConfig().getStringList("activecontracts.target");
                for (int i = 0; i < activeEmployers.size(); i++)
                {
                  if (activeEmployers.get(i).equalsIgnoreCase(playerInstance.getName()) && activeTargets.get(i).equalsIgnoreCase(arguments[1]))
                  {
                    playerInstance.sendMessage(ChatColor.RED + "You have already hired someone to assassinate that target!");
                    return true;
                  }
                }
                
                // Create proposal contract
                try
                {
                  // Take funds from employer's balance.
                  BigDecimal reward = new BigDecimal(Integer.parseInt(arguments[2]));
                  m_essentialsEconomy.substract(playerInstance.getName(), reward);
                  
                  // Insert data into pending contracts.
                  
                  employers.add(playerInstance.getName());
                  m_data.getConfig().set("pendingcontracts.employer", employers);
                  m_data.saveConfig();
                  
                  assassins.add(arguments[0]);
                  m_data.getConfig().set("pendingcontracts.assassin", assassins);
                  m_data.saveConfig();
                  
                  targets.add(arguments[1]);
                  m_data.getConfig().set("pendingcontracts.target", targets);
                  m_data.saveConfig();
                  
                  List<String> rewards = m_data.getConfig().getStringList("pendingcontracts.reward");
                  rewards.add(reward.toString());
                  m_data.getConfig().set("pendingcontracts.reward", rewards);
                  m_data.saveConfig();
                  
                  // Notify employer of successful contract creation.
                  playerInstance.sendMessage(ChatColor.GREEN + "$" + reward + " has been taken "
                      + "from your account and the contract has been proposed to " + assassin.getName() + ".");
                  
                  // Send message to the assassin regarding potential contract if assassin is online.
                  if (assassin.isOnline())
                  {
                    // Syntax for accepting/denying: /hireaccept <employer> <target> or /hiredeny <employer> <target>
                    TextComponent acceptBtn = new TextComponent("�a" + ChatColor.BOLD + "Accept ");
                    acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hireaccept " + playerInstance.getName() + " " + target.getName()));
                    
                    TextComponent denyBtn = new TextComponent("�c" + ChatColor.BOLD + "Deny");
                    denyBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hiredeny " + playerInstance.getName() + " " + target.getName()));
                    
                    TextComponent proposalMsg = new TextComponent(ChatColor.BOLD + playerInstance.getName() + ChatColor.RESET + " wants you to assassinate " + 
                        ChatColor.BOLD + target.getName() + ChatColor.RESET + " for �a" + ChatColor.BOLD + "$" + reward + ChatColor.RESET + ". ");
                    proposalMsg.addExtra(acceptBtn);
                    proposalMsg.addExtra(denyBtn);
                    
                    assassin.spigot().sendMessage(proposalMsg);
                  }
                  
                  return true;
                } catch (Exception e)
                {
                  e.printStackTrace();
                  return true;
                }
              } else
              {
                playerInstance.sendMessage(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
                return true;
              }
              
            } else
            {
              // Not enough money.
              playerInstance.sendMessage(ChatColor.RED + "You do not have sufficient funds.");
              return true;
            }
          } else // Player for target does not exist.
          {
            playerInstance.sendMessage(ChatColor.RED + "Player assigned as the target does not exist.");
            return true;
          }
        } else // Player for assassin does not exist.
        {
          playerInstance.sendMessage(ChatColor.RED + "Player assigned as the assassin does not exist.");
          return true;
        }
      } else
      {
        // Trying to hire themself as the assassin, you can't do that.
        playerInstance.sendMessage(ChatColor.RED + "You cannot hire yourself as the assassin, naughty.");
        return true;
      }
    } else
    {
      // Incorrect syntax error.
      playerInstance.sendMessage(ChatColor.RED + "Incorrect syntax. Usage: /hire <assassin> <target> <reward>.");
      return true;
    }
  }
  
  // Accepts proposal contract.
  public boolean runHireAcceptCmd(Player playerInstance, String[] arguments)
  {
    // Verify syntax is correct.
    if (arguments.length == 2)
    {
      // Verify config is formatted correctly
      if (this.verifyConfig() == true)
      {
        // Verify inputted contract proposal exists.
        List<String> employers = m_data.getConfig().getStringList("pendingcontracts.employer");
        List<String> assassins = m_data.getConfig().getStringList("pendingcontracts.assassin");
        List<String> targets = m_data.getConfig().getStringList("pendingcontracts.target");
        List<String> rewards = m_data.getConfig().getStringList("pendingcontracts.reward");
        
        for (int i = 0; i < employers.size(); i++)
        {
          if (employers.get(i).equalsIgnoreCase(arguments[0]) && assassins.get(i).equalsIgnoreCase(playerInstance.getName()) && targets.get(i).equalsIgnoreCase(arguments[1]))
          {	
            // Accept pending contract
            // In contract proposal creation, we verify these players exist -- use these names instead of the arguments (for possible case-sensitivity issues).
            String employer = Bukkit.getPlayer(employers.get(i)).getName();
            String assassin = Bukkit.getPlayer(assassins.get(i)).getName();
            String target = Bukkit.getPlayer(targets.get(i)).getName();
            String reward = rewards.get(i);
            
            // Add contract to activecontracts in the m_data.getConfig().
            List<String> activeEmployers = m_data.getConfig().getStringList("activecontracts.employer");
            activeEmployers.add(employer);
            m_data.getConfig().set("activecontracts.employer", activeEmployers);
            m_data.saveConfig();
            
            List<String> activeAssassins = m_data.getConfig().getStringList("activecontracts.assassin");
            activeAssassins.add(assassin);
            m_data.getConfig().set("activecontracts.assassin", activeAssassins);
            m_data.saveConfig();
            
            List<String> activeTargets = m_data.getConfig().getStringList("activecontracts.target");
            activeTargets.add(target);
            m_data.getConfig().set("activecontracts.target", activeTargets);
            m_data.saveConfig();
            
            List<String> activeRewards = m_data.getConfig().getStringList("activecontracts.reward");
            activeRewards.add(reward);
            m_data.getConfig().set("activecontracts.reward", activeRewards);
            m_data.saveConfig();
            
            List<String> activeRevealedContracts = m_data.getConfig().getStringList("activecontracts.revealed");
            activeRevealedContracts.add("false");
            m_data.getConfig().set("activecontracts.revealed", activeRevealedContracts);
            m_data.saveConfig();
            
            // Remove contract from pendingcontracts in the m_data.getConfig().
            employers.remove(i);
            m_data.getConfig().set("pendingcontracts.employer", employers);
            m_data.saveConfig();
            
            assassins.remove(i);
            m_data.getConfig().set("pendingcontracts.assassin", assassins);
            m_data.saveConfig();
            
            targets.remove(i);
            m_data.getConfig().set("pendingcontracts.target", targets);
            m_data.saveConfig();
            
            rewards.remove(i);
            m_data.getConfig().set("pendingcontracts.reward", rewards);
            m_data.saveConfig();
            
            // Message assassin that contract was successfully signed.
            playerInstance.sendMessage("�a" + ChatColor.BOLD + "Your assassination contract to kill �f" + ChatColor.BOLD + target + "�a" + ChatColor.BOLD + 
                " on behalf of �f" + ChatColor.BOLD + employer + "�a" + ChatColor.BOLD + " for �f" + ChatColor.BOLD + "$" + reward + "�a" + ChatColor.BOLD +
                " has been successfully activated. Good luck!");
            
            // Message employer, if online, that the contract was successfully signed.
            Player employerPlayerInstance = Bukkit.getPlayer(employer);
            if (employerPlayerInstance.isOnline() == true)
            {
              employerPlayerInstance.sendMessage("�a" + ChatColor.BOLD + "Your assassination contract to hire �f" + ChatColor.BOLD + assassin + "�a" + 
                  ChatColor.BOLD + " to kill �f" + ChatColor.BOLD + target + "�a" + ChatColor.BOLD + " for �f" + ChatColor.BOLD + "$" + 
                  reward + "�a" + ChatColor.BOLD + " has been successfully activated.");
            }
            
            // Message entire server (with a sound) that a new assassination contract was signed -- make everyone super paranoid.
            Player[] onlinePlayers = Bukkit.getOnlinePlayers().toArray(new Player[Bukkit.getOnlinePlayers().size()]);
            for (int j = 0; j < onlinePlayers.length; j++)
            {
              onlinePlayers[j].playSound(onlinePlayers[j].getLocation(), "minecraft:entity.wither.spawn", 1, 1);
            }
            Bukkit.broadcastMessage("�d" + ChatColor.BOLD + "A new assassination contract has been activated! Watch out folks.");
            
            return true;
          }
        }
        
        // If contract was not found in for-loop iterations, nothing would be returned previously, so the method continues to here.
        // Notify the assassin player that the contract was not found.
        playerInstance.sendMessage(ChatColor.RED + "A pending contract with the players you entered does not exist!");
        return true;
      } else
      {
        playerInstance.sendMessage(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
        return true;
      }
    } else
    {
      // Incorrect syntax error.
      playerInstance.sendMessage(ChatColor.RED + "Incorrect syntax. Usage: /hireaccept <employer> <target>.");
      return true;
    }
  }
  
  // Denies proposal contract.
  public boolean runHireDenyCmd(Player playerInstance, String[] arguments)
  {
    if (arguments.length == 2)
    {
      // Verify config is formatted correctly
      if (this.verifyConfig() == true)
      {
        // Verify contract proposal with parameters exists.
        List<String> employers = m_data.getConfig().getStringList("pendingcontracts.employer");
        List<String> assassins = m_data.getConfig().getStringList("pendingcontracts.assassin");
        List<String> targets = m_data.getConfig().getStringList("pendingcontracts.target");
        List<String> rewards = m_data.getConfig().getStringList("pendingcontracts.reward");
        for (int i = 0; i < employers.size(); i++)
        {
          if (employers.get(i).equalsIgnoreCase(arguments[0]) && assassins.get(i).equalsIgnoreCase(playerInstance.getName()) && 
              targets.get(i).equalsIgnoreCase(arguments[1]))
          {
            // Contract exists, deny it.
            // Remove contract from pendingcontracts in m_data.getConfig().
            String employer = employers.get(i);
            employers.remove(i);
            m_data.getConfig().set("pendingcontracts.employer", employers);
            m_data.saveConfig();
            
            String assassin = assassins.get(i);
            assassins.remove(i);
            m_data.getConfig().set("pendingcontracts.assassin", assassins);
            m_data.saveConfig();
            
            String target = targets.get(i);
            targets.remove(i);
            m_data.getConfig().set("pendingcontracts.target", targets);
            m_data.saveConfig();
            
            String reward = rewards.get(i);
            rewards.remove(i);
            m_data.getConfig().set("pendingcontracts.reward", rewards);
            m_data.saveConfig();
            
            // Refund money to employer's balance.
            BigDecimal rewardAmount = new BigDecimal(Integer.parseInt(reward));
            try
            {
              m_essentialsEconomy.add(employer, rewardAmount);
            } catch (Exception e)
            {
              e.printStackTrace();
              return true;
            }
            
            // Message assassin player the contract was successfully denied.
            playerInstance.sendMessage(ChatColor.GREEN + "You have successfully denied the contract.");
            
            // Message employer, if online, that the contract was denied.
            Player employerPlayerInstance = Bukkit.getPlayer(employer);
            if (employerPlayerInstance.isOnline() == true)
            {
              employerPlayerInstance.sendMessage("�c" + ChatColor.BOLD + "Your assassination contract to hire �f" + ChatColor.BOLD + assassin + "�c" + 
                  ChatColor.BOLD + " to kill �f" + ChatColor.BOLD + target + "�c" + ChatColor.BOLD + " for �a" + ChatColor.BOLD + "$" + 
                  reward + "�c" + ChatColor.BOLD + " has been denied.");
            }
            
            return true;
          }
        }
        
        // If contract was not found in for-loop iterations, nothing would be returned previously, so the method continues to here.
        // Notify the assassin player that the contract was not found.
        playerInstance.sendMessage(ChatColor.RED + "A pending contract with the players you entered does not exist!");
        return true;
      } else
      {
        playerInstance.sendMessage(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
        return true;
      }
    } else
    {
      // Incorrect syntax error.
      playerInstance.sendMessage(ChatColor.RED + "Incorrect syntax. Usage: /hiredeny <employer> <target>.");
      return true;
    }
  }
  
  // Checks if contract is active between a player killed and a player killer.
  public static int fulfillContract(String killerPlayer, String killedPlayer)
  {
    // Verify config is formatted correctly.
    if (verifyConfig() == true)
    {
      // Fulfill any contracts that have both the same assassin and target.
      int totalRewardAmount = 0;
      int totalStolenAmount = 0;
      boolean assassinIsVictorious = false;
      boolean targetIsVictorious = false;
      
      // Getting activecontracts to loop through and remove those that have been fulfilled.
      List<String> employers = m_data.getConfig().getStringList("activecontracts.employer");
      List<String> assassins = m_data.getConfig().getStringList("activecontracts.assassin");
      List<String> targets = m_data.getConfig().getStringList("activecontracts.target");
      List<String> rewards = m_data.getConfig().getStringList("activecontracts.reward");
      List<String> revealedContracts = m_data.getConfig().getStringList("activecontracts.revealed");
      
      // Getting completecontracts to add fulfilled contracts to it.
      List<String> completeEmployers = m_data.getConfig().getStringList("completecontracts.employer");
      List<String> completeAssassins = m_data.getConfig().getStringList("completecontracts.assassin");
      List<String> completeTargets = m_data.getConfig().getStringList("completecontracts.target");
      List<String> completeRewards = m_data.getConfig().getStringList("completecontracts.reward");
      List<String> completeLosses = m_data.getConfig().getStringList("completecontracts.lost");
      
      for (int i = employers.size()-1; i >= 0; i--)
      {
        if ((assassins.get(i).equalsIgnoreCase(killerPlayer) && targets.get(i).equalsIgnoreCase(killedPlayer)) || 
            (assassins.get(i).equalsIgnoreCase(killedPlayer) && targets.get(i).equalsIgnoreCase(killerPlayer)))
        {
          // Contract found, reward player the money, notify employer (if online), remove contract 
          // from activecontracts, and add to completecontracts.
          // Store employer and reward variables -- needed later.
          String employer = employers.get(i);
          String assassin = assassins.get(i);
          String target = targets.get(i);
          String reward = rewards.get(i);
          
          // Remove contract from activecontracts.
          // Employer
          completeEmployers.add(employers.get(i));
          employers.remove(i);
          m_data.getConfig().set("activecontracts.employer", employers);
          m_data.saveConfig();
          
          // Assassin
          completeAssassins.add(assassins.get(i));
          assassins.remove(i);
          m_data.getConfig().set("activecontracts.assassin", assassins);
          m_data.saveConfig();
          
          // Target
          completeTargets.add(targets.get(i));
          targets.remove(i);
          m_data.getConfig().set("activecontracts.target", targets);
          m_data.saveConfig();
          
          // Reward
          completeRewards.add(rewards.get(i));
          rewards.remove(i);
          m_data.getConfig().set("activecontracts.reward", rewards);
          m_data.saveConfig();
          
          // Revealed Status
          revealedContracts.remove(i);
          m_data.getConfig().set("activecontracts.revealed", revealedContracts);
          m_data.saveConfig();
          
          // Lost Status
          if (assassin.equalsIgnoreCase(killerPlayer) && target.equalsIgnoreCase(killedPlayer))
          {
            // Assassin killed target.
            assassinIsVictorious = true;
            completeLosses.add("false");
          } else if (assassin.equalsIgnoreCase(killedPlayer) && target.equalsIgnoreCase(killerPlayer))
          {
            // Target killed assassin.
            targetIsVictorious = true;
            completeLosses.add("true");
          }
          
          // Set config completecontracts to the modified lists.
          m_data.getConfig().set("completecontracts.employer", completeEmployers);
          m_data.saveConfig();
          m_data.getConfig().set("completecontracts.assassin", completeAssassins);
          m_data.saveConfig();
          m_data.getConfig().set("completecontracts.target", completeTargets);
          m_data.saveConfig();
          m_data.getConfig().set("completecontracts.reward", completeRewards);
          m_data.saveConfig();
          m_data.getConfig().set("completecontracts.lost", completeLosses);
          m_data.saveConfig();
          
          BigDecimal rewardAmount = new BigDecimal(Integer.parseInt(reward));
          try
          {
            // Reward player the money.
            Main.getEconomyAPI().add(killerPlayer, rewardAmount);
            if (assassin.equalsIgnoreCase(killerPlayer) && target.equalsIgnoreCase(killedPlayer))
            {
              // Assassin killed target.
              totalRewardAmount += Integer.parseInt(reward);
            } else if (assassin.equalsIgnoreCase(killedPlayer) && target.equalsIgnoreCase(killerPlayer))
            {
              // Target killed assassin.
              totalStolenAmount += Integer.parseInt(reward);
            }
          } catch (Exception e)
          {
            e.printStackTrace();
            return 0;
          }
          
          // Notify employer, if online, that contract was fulfilled/a failure.
          Player employerPlayerInstance = Bukkit.getPlayer(employer);
          if (employerPlayerInstance != null && employerPlayerInstance.isOnline() == true)
          {
            if (assassin.equalsIgnoreCase(killerPlayer) && target.equalsIgnoreCase(killedPlayer))
            {
              // Assassin killed target.
              employerPlayerInstance.sendMessage("�f" + ChatColor.BOLD + killedPlayer + "�a" + ChatColor.BOLD + " was "
                  + "successfully removed by �f" + ChatColor.BOLD + killerPlayer + "�a.");
            } else if (assassin.equalsIgnoreCase(killedPlayer) && target.equalsIgnoreCase(killerPlayer))
            {
              // Target killed assassin.
              employerPlayerInstance.sendMessage("�f" + ChatColor.BOLD + killedPlayer + "�c" + ChatColor.BOLD + " failed at "
                  + "assassinating �f" + ChatColor.BOLD + killerPlayer + "�c. �f" + ChatColor.BOLD + killerPlayer 
                  + " �creaped the reward!");
            }
          }
          
          // Increment number of contracts complete for assassin player in m_data.getConfig().
          // Increment number of contracts countered for target player, if killed assassin.
          List<String> statsPlayers = m_data.getConfig().getStringList("stats.player");
          List<String> statsAssassinations = m_data.getConfig().getStringList("stats.assassinations");
          List<String> statsCounters = m_data.getConfig().getStringList("stats.counters");
          boolean foundPlayerStats = false;
          for (int j = 0; j < statsPlayers.size(); j++)
          {
            if (statsPlayers.get(j).equalsIgnoreCase(killerPlayer))
            {
              if (assassin.equalsIgnoreCase(killerPlayer) && target.equalsIgnoreCase(killedPlayer))
              {
                // Assassin killed target.
                statsAssassinations.set(j, "" + (Integer.parseInt(statsAssassinations.get(j))+1));
                
              } else if (assassin.equalsIgnoreCase(killedPlayer) && target.equalsIgnoreCase(killerPlayer))
              {
                // Target killed assassin.
                statsCounters.set(j, "" + (Integer.parseInt(statsCounters.get(j))+1));
              }
              
              foundPlayerStats = true;
            }
          }
          // Create entry for player in stats if one was not found.
          if (foundPlayerStats == false)
          {
            statsPlayers.add(Bukkit.getPlayer(killerPlayer).getName());
            if (assassin.equalsIgnoreCase(killerPlayer) && target.equalsIgnoreCase(killedPlayer))
            {
              // Assassin killed target.
              statsAssassinations.add("1");
              statsCounters.add("0");
              
            } else if (assassin.equalsIgnoreCase(killedPlayer) && target.equalsIgnoreCase(killerPlayer))
            {
              // Target killed assassin.
              statsAssassinations.add("0");
              statsCounters.add("1");
            }
          }
          
          // Save stats info to data config.
          m_data.getConfig().set("stats.player", statsPlayers);
          m_data.saveConfig();
          m_data.getConfig().set("stats.assassinations", statsAssassinations);
          m_data.saveConfig();
          m_data.getConfig().set("stats.counters", statsCounters);
          m_data.saveConfig();
        }
      }
      
      // Output results.
      if (assassinIsVictorious == true && targetIsVictorious == true)
      {
        // Notify playerKilled of failure.
        Bukkit.getPlayer(killedPlayer).sendMessage("�c" + ChatColor.BOLD + "You failed at fulfilling your "
            + "active contract(s) to assassinate �f" + ChatColor.BOLD + killerPlayer + "�c" + ChatColor.BOLD 
            + ". Your assassination contract has been lost.");
        
        // Notify assassin of both reward and stolen amount.
        Bukkit.getPlayer(killerPlayer).sendMessage("�a" + ChatColor.BOLD + "You have successfully fulfilled your "
            + "active contract(s) to assassinate �f" + ChatColor.BOLD + killedPlayer + "�a" + ChatColor.BOLD 
            + ". You also defended yourself against them as they had a contract on your head. You have "
            + "been rewarded with $" + totalRewardAmount + " and stole an additional $" + totalStolenAmount + " from"
            + "their contract. *clap clap*");
        
        // 3: Death message includes executing and defending.
        return 3;
      } else if (assassinIsVictorious == true)
      {
        // Notify assassin of completion of contract.
        Bukkit.getPlayer(killerPlayer).sendMessage("�a" + ChatColor.BOLD + "You have successfully fulfilled your "
            + "active contract(s) to assassinate �f" + ChatColor.BOLD + killedPlayer + "�a" + ChatColor.BOLD 
            + ". You have been rewarded with $" + totalRewardAmount + ".");
        
        // 1: Death message includes executing.
        return 1;
      } else if (targetIsVictorious == true)
      {
        // Notify assassin of failure.
        Bukkit.getPlayer(killedPlayer).sendMessage("�c" + ChatColor.BOLD + "You failed at fulfilling your "
            + "active contract(s) to assassinate �f" + ChatColor.BOLD + killerPlayer + "�c" + ChatColor.BOLD 
            + ". Your assassination contract has been lost.");
        
        // Notify target of stolen amount.
        Bukkit.getPlayer(killerPlayer).sendMessage("�a" + ChatColor.BOLD + "You successfully defended yourself against �f" 
            + ChatColor.BOLD + killedPlayer + "�a" + ChatColor.BOLD + " who was trying to assassinate you. You have stolen $"
            + totalStolenAmount + " from their contract.");
        
        // 2: Death message includes defending.
        return 2;
      } else
      {
        // Contract not found, do nothing -- just normal PvP.
        return 0;
      }
    } else
    {
      // Config is not formatted correctly.
      return 0;
    }
  }
  
  // Reveals assassin and target by wish of assassin.
  public boolean runRevealCmd(Player playerInstance, String[] arguments)
  {
    // Verify correct syntax.
    if (arguments.length == 1)
    {
      // Verify config is formatted correctly.
      if (this.verifyConfig() == true)
      {
        // Verify there is a contract with the assassin and target.
        boolean wasDisplayed = false;
        boolean alreadyDisplayed = false;
        
        List<String> assassins = m_data.getConfig().getStringList("activecontracts.assassin");
        List<String> targets = m_data.getConfig().getStringList("activecontracts.target");
        List<String> revealedContracts = m_data.getConfig().getStringList("activecontracts.revealed");
        for (int i = 0; i < assassins.size(); i++)
        {
          if (assassins.get(i).equalsIgnoreCase(playerInstance.getName()) && targets.get(i).equalsIgnoreCase(arguments[0])
              && revealedContracts.get(i).equals("false"))
          {
            // Contract exists.
            if (wasDisplayed == false)
            {			
              // Has not been revealed yet in any previous iterations, reveal and set revealedContracts at i to true.
              playerInstance.playSound(playerInstance.getLocation(), "minecraft:entity.ender_dragon.growl", 1, 1);
              Bukkit.getPlayer(targets.get(i)).playSound(playerInstance.getLocation(), "minecraft:entity.ender_dragon.growl", 1, 1);
              
              Bukkit.broadcastMessage("�4" + ChatColor.BOLD + "Watch out " + targets.get(i) + "! " 
                  + playerInstance.getName() + " is coming after you!");
              
              wasDisplayed = true;
            }
            
            revealedContracts.set(i, "true");
          } else if (assassins.get(i).equalsIgnoreCase(playerInstance.getName()) && targets.get(i).equalsIgnoreCase(arguments[0])
              && revealedContracts.get(i).equals("true"))
          {
            alreadyDisplayed = true;
          }
        }
        
        if (alreadyDisplayed == true)
        {
          // Player has already revealed themselves as the assassin.
          playerInstance.sendMessage(ChatColor.RED + "You already revealed that you are trying to assassinate " + Bukkit.getPlayer(arguments[0]).getName() + "!");
        } else if (wasDisplayed == false)
        {
          // No contract exists.
          playerInstance.sendMessage(ChatColor.RED + "An active contract with the player you entered does not exist!");
        } else
        {
          // Revealed, so save revealed = true to config.
          m_data.getConfig().set("activecontracts.revealed", revealedContracts);
          m_data.saveConfig();
        }
        
        return true;
      } else
      {
        playerInstance.sendMessage(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
        return true;
      }
    } else
    {
      // Incorrect syntax error.
      playerInstance.sendMessage(ChatColor.RED + "Incorrect syntax. Usage: /hirereveal <target>.");
      return true;
    }
  }
  
  // Cancel contract. If employer, can only cancel pending. If assassin, can only cancel active.
  public boolean runCancelContractCmd(Player playerInstance, String[] arguments)
  {
    // Verify correct syntax.
    if (arguments.length == 2)
    {
      // Verify config is formatted correctly.
      if (this.verifyConfig())
      {
        // Distinguish whether playerInstance is employer or assassin.
        if (playerInstance.getName().equalsIgnoreCase(arguments[0]))
        {
          // playerInstance is the employer, cancel only pending contracts.
          List<String> employers = m_data.getConfig().getStringList("pendingcontracts.employer");
          List<String> assassins = m_data.getConfig().getStringList("pendingcontracts.assassin");
          List<String> targets = m_data.getConfig().getStringList("pendingcontracts.target");
          List<String> rewards = m_data.getConfig().getStringList("pendingcontracts.reward");
          
          for (int i = 0; i < employers.size(); i++)
          {
            // Find contract to cancel where employer is playerInstance and target is argument[1].
            if (employers.get(i).equalsIgnoreCase(playerInstance.getName()) && targets.get(i).equalsIgnoreCase(arguments[1]))
            {
              String assassin = Bukkit.getPlayer(assassins.get(i)).getName();
              String target = Bukkit.getPlayer(targets.get(i)).getName();
              String reward = rewards.get(i);
              
              // Remove entries from string lists and modify m_data.getConfig().
              employers.remove(i);
              m_data.getConfig().set("pendingcontracts.employer", employers);
              m_data.saveConfig();
              
              assassins.remove(i);
              m_data.getConfig().set("pendingcontracts.assassin", assassins);
              m_data.saveConfig();
              
              targets.remove(i);
              m_data.getConfig().set("pendingcontracts.target", targets);
              m_data.saveConfig();
              
              rewards.remove(i);
              m_data.getConfig().set("pendingcontracts.reward", rewards);
              m_data.saveConfig();
              
              // Refund money to the employer, the playerInstance.
              BigDecimal refundAmount = new BigDecimal(Integer.parseInt(reward));
              try
              {
                m_essentialsEconomy.add(playerInstance.getName(), refundAmount);	
              } catch (Exception e)
              {
                e.printStackTrace();
                return true;
              }
              
              // Message employer, the playerInstance, that the contract was successfully removed and money refunded.
              playerInstance.sendMessage(ChatColor.GREEN + "You successfully cancelled your pending contract of hiring"
                  + " �f" + ChatColor.BOLD + assassin + " �ato assassinate �f" + ChatColor.BOLD + target + "�a. "
                  + "$" + refundAmount + " has been refunded to your balance.");
              
              // Message assassin, if online, that the pending contract was cancelled.
              Player assassinPlayerInstance = Bukkit.getPlayer(assassin);
              if (assassinPlayerInstance.isOnline())
              {
                assassinPlayerInstance.sendMessage(ChatColor.RED + "A contract proposed by "
                    + "�f" + ChatColor.BOLD + playerInstance.getName() + ChatColor.RED + " for you to assassinate "
                    + "�f" + ChatColor.BOLD + target + ChatColor.RED + " has been cancelled by the employer.");
              }
              
              return true;
            }
          }
          
          // If contract was not found in for-loop iterations, nothing would be returned previously, so the method continues to here.
          // Notify the employer player that the contract was not found.
          playerInstance.sendMessage(ChatColor.RED + "A pending contract with the players you entered does not exist!");
          return true;
        } else
        {
          // playerInstance is the employer, cancel only pending contracts.
          List<String> employers = m_data.getConfig().getStringList("activecontracts.employer");
          List<String> assassins = m_data.getConfig().getStringList("activecontracts.assassin");
          List<String> targets = m_data.getConfig().getStringList("activecontracts.target");
          List<String> rewards = m_data.getConfig().getStringList("activecontracts.reward");
          List<String> revealedContracts = m_data.getConfig().getStringList("activecontracts.revealed");
          
          for (int i = 0; i < employers.size(); i++)
          {
            // Find contract to cancel where employer matches argument[0] and target matches argument[1], and
            // verify playerInstance is the assassin.
            if (employers.get(i).equalsIgnoreCase(arguments[0]) && assassins.get(i).equalsIgnoreCase(playerInstance.getName()) 
                && targets.get(i).equalsIgnoreCase(arguments[1]))
            {
              String employer = Bukkit.getPlayer(employers.get(i)).getName();
              String target = Bukkit.getPlayer(targets.get(i)).getName();
              String reward = rewards.get(i);
              
              // Contract found, remove from activecontracts and refund money to employer.
              employers.remove(i);
              m_data.getConfig().set("activecontracts.employer", employers);
              m_data.saveConfig();
              
              assassins.remove(i);
              m_data.getConfig().set("activecontracts.assassin", assassins);
              m_data.saveConfig();
              
              targets.remove(i);
              m_data.getConfig().set("activecontracts.target", targets);
              m_data.saveConfig();
              
              rewards.remove(i);
              m_data.getConfig().set("activecontracts.reward", rewards);
              m_data.saveConfig();
              
              revealedContracts.remove(i);
              m_data.getConfig().set("activecontracts.revealed", revealedContracts);
              m_data.saveConfig();
              
              // Refund money to the employer.
              BigDecimal refundAmount = new BigDecimal(Integer.parseInt(reward));
              try
              {
                m_essentialsEconomy.add(employer, refundAmount);
              } catch (Exception e)
              {
                e.printStackTrace();
                return true;
              }
              
              // Message assassin, the playerInstance, that the contract was successfully cancelled.
              playerInstance.sendMessage(ChatColor.GREEN + "You successfully cancelled your active contract to assassinate "
                  + "�f" + ChatColor.BOLD + target + "�a on behalf of �f" + ChatColor.BOLD + employer + ChatColor.GREEN + ".");
              
              // Message employer, if online, that the active contract was cancelled.
              Player employerPlayerInstance = Bukkit.getPlayer(employer);
              if (employerPlayerInstance.isOnline())
              {
                employerPlayerInstance.sendMessage(ChatColor.RED + "Your active contract for �f" + ChatColor.BOLD
                    + playerInstance.getName() + ChatColor.RED + " to assassinate " + "�f" + ChatColor.BOLD
                    + target + ChatColor.RED + " has been cancelled by the assassin. �a" + "$" + refundAmount
                    + " has been refunded to your balance.");
              }
              
              return true;
            }
          }
          
          // If contract was not found in for-loop iterations, nothing would be returned previously, so the method continues to here.
          // Notify the assassin player that the contract was not found.
          playerInstance.sendMessage(ChatColor.RED + "An active contract with the players you entered does not exist!");
          return true;
        }
      } else
      {
        playerInstance.sendMessage(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
        return true;
      }
    } else
    {
      // Incorrect syntax error.
      playerInstance.sendMessage(ChatColor.RED + "Incorrect syntax. Usage: /hirecancel <employer> <target>.");
      return true;
    }
  }
  
  // Loops through all types of contracts and creates master array list.
  // contractType will be used to display the type of contract: PENDING (ACTION NEEDED), PENDING, ACTIVE, COMPLETE, LOST
  public ArrayList<List<String>> addToContractsList(ArrayList<List<String>> list, String configContractPath, 
      String contractType, Player player)
  {
    // Instantiate the different string lists that will be looped through.
    List<String> employers = m_data.getConfig().getStringList(configContractPath + ".employer");;
    List<String> assassins = m_data.getConfig().getStringList(configContractPath + ".assassin");
    List<String> targets = m_data.getConfig().getStringList(configContractPath + ".target");
    List<String> rewards = m_data.getConfig().getStringList(configContractPath + ".reward");
    List<String> losses = null;
    if (configContractPath.equals("completecontracts"))
    {
      losses = m_data.getConfig().getStringList(configContractPath + ".lost");
    }
    
    // Add any pending contracts with player as the employer or assassin to the master array list.
    int displayedCounter = 0;
    for (int i = employers.size()-1; i >= 0; i--)
    {
      // Check if player matches employer or assassin.
      if (employers.get(i).equalsIgnoreCase(player.getName()) || assassins.get(i).equalsIgnoreCase(player.getName()))
      {
        // Decipher whether contract needs approval by player or not -- makes life easier for player.
        if (assassins.get(i).equalsIgnoreCase(player.getName()) && contractType.equals("�cPENDING�r"))
        {
          // Player is an assassin for a pending contract.
          list.add(Arrays.asList("�cPENDING �l(ACTION NEEDED)�r", employers.get(i), assassins.get(i), targets.get(i), rewards.get(i)));
        } else if (contractType.equals("�6COMPLETE�r") && losses != null && losses.get(i).equals("true"))
        {
          // Player lost this contract by dying to the target.
          list.add(Arrays.asList("�4LOST�r", employers.get(i), assassins.get(i), targets.get(i), rewards.get(i)));
        } else
        {
          // Add to list as usual: PENDING, ACTIVE, or COMPLETE
          list.add(Arrays.asList(contractType, employers.get(i), assassins.get(i), targets.get(i), rewards.get(i)));
        }
        displayedCounter++;
      }
      
      // Only display top five most recent "completed" contracts.
      if (configContractPath.equals("completecontracts") && displayedCounter == m_data.getConfig().getInt("listrestriction"))
      {
        break;
      }
    }
    
    return list;
  }
  
  // Lists all pending and active contracts.
  public boolean runListCmd(Player playerInstance)
  {
    // Verify config is formatted correctly.
    if (this.verifyConfig())
    {
      // Master array list that will be looped through and printed to the player.
      ArrayList<List<String>> contractsList = new ArrayList<List<String>>();
      
      // Loop through config and add to contractLists.
      contractsList = this.addToContractsList(contractsList, "pendingcontracts", "�cPENDING�r", playerInstance);
      contractsList = this.addToContractsList(contractsList, "activecontracts", "�aACTIVE�r", playerInstance);
      contractsList = this.addToContractsList(contractsList, "completecontracts", "�6COMPLETE�r", playerInstance);
      
      // Check if there are any contracts. If so, print the master array out. If not, send message that no contracts were found.
      if (contractsList.size() > 0)
      {
        playerInstance.sendMessage("�4�lContracts�f�l: | STATUS | EMPLOYER | ASSASSIN | TARGET | REWARD |");
        for (int i = 0; i < contractsList.size(); i++)
        {
          playerInstance.sendMessage("�4" + (i+1) + "�r: | " + contractsList.get(i).get(0) + " | " + contractsList.get(i).get(1)
              + " | " + contractsList.get(i).get(2) + " | " + contractsList.get(i).get(3) + " | $" + contractsList.get(i).get(4) + " |");
        }
      } else
      {
        playerInstance.sendMessage(ChatColor.RED + "You do not have any contracts currently.");
      }
      
      return true;
    } else
    {
      // Config not formatted correctly.
      playerInstance.sendMessage(ChatColor.RED + "An error occurred: Config file is not formatted correctly!");
      return true;
    }
  }
  
}

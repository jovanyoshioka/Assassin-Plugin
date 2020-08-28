package me.Jovan.Assassin.Files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Jovan.Assassin.Main;

public class DataManager
{

  private Main m_plugin;
  private FileConfiguration m_dataConfig = null;
  private File m_configFile = null;
  
  public DataManager(Main plugin)
  {
    this.m_plugin = plugin;
    
    // Saves and/or initializes the config.
    this.saveDefaultConfig();
  }
  
  public void reloadConfig()
  {
    if (this.m_configFile == null)
    {
      this.m_configFile = new File(this.m_plugin.getDataFolder(), "data.yml");
    }
    
    this.m_dataConfig = YamlConfiguration.loadConfiguration(this.m_configFile);
    
    InputStream defaultStream = this.m_plugin.getResource("data.yml");
    if (defaultStream != null)
    {
      YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
      this.m_dataConfig.setDefaults(defaultConfig);
    }
  }
  
  public FileConfiguration getConfig()
  {
    if (this.m_dataConfig == null)
    {
      this.reloadConfig();
    }
    
    return this.m_dataConfig;
  }
  
  public void saveConfig()
  {
    if (this.m_dataConfig == null || this.m_configFile == null)
    {
      return;
    }
    
    try {
      this.getConfig().save(this.m_configFile);
    } catch (IOException e) {
      this.m_plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.m_configFile, e);
    }
  }
  
  public void saveDefaultConfig()
  {
    if (this.m_configFile == null)
    {
      this.m_configFile = new File(this.m_plugin.getDataFolder(), "data.yml");
    }
    
    if (!this.m_configFile.exists())
    {
      this.m_plugin.saveResource("data.yml", false);
    }
  }
}

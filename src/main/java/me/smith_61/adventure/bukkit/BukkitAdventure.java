package me.smith_61.adventure.bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import me.smith_61.adventure.bukkit.tasks.BukkitExecutor;
import me.smith_61.adventure.bukkit.tasks.ExtractWorld;
import me.smith_61.adventure.bukkit.tasks.LoadWorld;
import me.smith_61.adventure.common.Adventure;
import me.smith_61.adventure.common.AdventureInstance;
import me.smith_61.adventure.common.AdventureTeam;

public class BukkitAdventure extends Adventure {
	
	private static final String WORLD_NAME_FORMAT = "%s_%s_%s";
	
	private final Environment entryEnvironment;
	private final Map<Environment, BukkitAdventureWorld> worlds;
	
	private BukkitAdventure(String name, Environment entryEnvironment, Map<Environment, BukkitAdventureWorld> worlds) {
		super(name);
		
		this.entryEnvironment = entryEnvironment;
		this.worlds = worlds;
	}

	@Override
	protected ListenableFuture<AdventureInstance> createInstance(final AdventureTeam team) {
		List<ListenableFuture<World>> futures = new ArrayList<ListenableFuture<World>>();
		
		for(Entry<Environment, BukkitAdventureWorld> entry : this.worlds.entrySet()) {
			String worldName = String.format(WORLD_NAME_FORMAT, team.getName(), this.getName(), entry.getValue().getName());
			
			File worldSaveDir = BukkitPlugin.getInstance().getWorldSaveDir();
			File worldDir = null;
			
			while((worldDir = new File(worldSaveDir, worldName)).exists()) {
				worldName += "_";
			}
			worldDir.mkdirs();
			
			WorldCreator options = WorldCreator.name(worldName).environment(entry.getKey());
			
			ListenableFutureTask<String> extractWorld = ListenableFutureTask.create(new ExtractWorld(worldDir, entry.getValue()));
			BukkitExecutor.ASYNC.execute(extractWorld);
			
			futures.add(Futures.transform(extractWorld, new LoadWorld(options), BukkitExecutor.SYNC));
		}
		
		return Futures.transform(Futures.allAsList(futures), new CreateInstance());
	}
	
	public static BukkitAdventure loadAdventure(File file) throws AdventureLoadException {
		try {
			ZipFile zipFile = new ZipFile(file);
			
			ZipEntry adventureDescFile = zipFile.getEntry("adventure.yaml");
			if(adventureDescFile == null || adventureDescFile.isDirectory()) {
				adventureDescFile = zipFile.getEntry("./adventure.yaml");
				if(adventureDescFile == null){
					throw new AdventureLoadException(file.getName() + " is missing required 'adventure.yaml' file");
				}
			}
			
			InputStream in = null;
			try {
				in = zipFile.getInputStream(adventureDescFile);
				Configuration adventureDesc = YamlConfiguration.loadConfiguration(in);
				
				String name = adventureDesc.getString("name");
				if(name == null || name.isEmpty()) {
					throw new AdventureLoadException("Missing required field: 'name'");
				}
				
				// Load worlds
				ConfigurationSection worldsDesc = adventureDesc.getConfigurationSection("worlds");
				if(worldsDesc == null) {
					throw new AdventureLoadException("Missing required field: 'worlds'");
				}
				

				Map<Environment, BukkitAdventureWorld> worlds = new HashMap<Environment, BukkitAdventureWorld>();
				for(String key : worldsDesc.getKeys(false)) {
					if(key.equals("entry")) {
						continue;
					}
					
					Environment env = BukkitAdventure.getEnvironment(key);
					if(env == null) {
						throw new AdventureLoadException("Invalid environment type: '" + key + "'");
					}
					
					Object val = worldsDesc.get(key);
					if(!(val instanceof ConfigurationSection)) {
						throw new AdventureLoadException("Invalid yaml datatype for key: '" + key + "'. Expected: '" + ConfigurationSection.class + "'. Got: '" + val.getClass() + "'");
					}
					
					if(worlds.containsKey(env)) {
						throw new AdventureLoadException("World already specified for environment: '" + env.name() + "'");
					}
					else {
						worlds.put(env, BukkitAdventureWorld.fromDescription((ConfigurationSection)val, zipFile));
					}
				}
				if(worlds.size() == 0) {
					throw new AdventureLoadException("No world descriptions found in worlds section.");
				}
				
				String entryEnvName = worldsDesc.getString("entry", Environment.NORMAL.name());
				Environment entryEnv = BukkitAdventure.getEnvironment(entryEnvName);
				if(entryEnv == null) {
					throw new AdventureLoadException("Invalid entry environment type: '" + entryEnvName + "'");
				}
				if(!worlds.containsKey(entryEnv)) {
					throw new AdventureLoadException("Missing world for entry environment: '" + entryEnv.name() + "'");
				}
				
				return new BukkitAdventure(name, entryEnv, worlds);
			}
			finally {
				if(in != null) {
					in.close();
				}
			}
		}
		catch(IOException ioe) {
			throw new AdventureLoadException("Error loading adventure from file: " + file.getName(), ioe);
		}
	}
	
	private static Environment getEnvironment(String name) {
		try {
			return Environment.valueOf(name.toUpperCase());
		}
		catch(IllegalArgumentException iae) {
			return null;
		}
	}
	
	private class CreateInstance implements Function<List<World>, AdventureInstance> {
		
		private CreateInstance() {
		}
		
		@Override
		public AdventureInstance apply(@Nullable List<World> worlds) {
			Map<Environment, World> worldsMap = new HashMap<Environment, World>();
			
			for(World world : worlds) {
				worldsMap.put(world.getEnvironment(), world);
			}
			
			return new BukkitAdventureInstance(worldsMap.get(BukkitAdventure.this.entryEnvironment), worldsMap);
		}
		
	}
	
}

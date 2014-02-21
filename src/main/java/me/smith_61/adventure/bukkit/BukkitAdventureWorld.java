package me.smith_61.adventure.bukkit;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.bukkit.configuration.ConfigurationSection;

public class BukkitAdventureWorld {

	private final String name;
	
	private final ZipFile zipFile;
	private final ZipEntry entry;
	
	private BukkitAdventureWorld(String name, ZipFile zipFile, ZipEntry entry) {
		this.name = name;
		
		this.zipFile = zipFile;
		this.entry = entry;
	}
	
	public String getName() {
		return this.name;
	}
	
	public ZipInputStream getInputStream() throws IOException {
		return new ZipInputStream(this.zipFile.getInputStream(this.entry));
	}
	
	public static BukkitAdventureWorld fromDescription(ConfigurationSection section, ZipFile zipFile) throws AdventureLoadException {
		String name = section.getString("name");
		if(name == null || name.isEmpty()) {
			throw new AdventureLoadException("Missing required field: 'name' in world description for environment: '" + section.getName() + "'");
		}
		
		ZipEntry entry = zipFile.getEntry("maps/" + name + ".zip");
		if(entry == null) {
			entry = zipFile.getEntry("/maps/" + name + ".zip");
			if(entry == null) {
				throw new AdventureLoadException("Missing world zip file: 'maps/" + name + ".zip'");
			}
		}
		
		return new BukkitAdventureWorld(name, zipFile, entry);
	}
}

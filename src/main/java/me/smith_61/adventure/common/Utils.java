package me.smith_61.adventure.common;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.io.Files;

public class Utils {
	
	public static void deleteFileTree(File root, boolean onExit) {
		if(onExit) {
			root.deleteOnExit();
		}
		
		if(root.isDirectory()) {
			for(File child : root.listFiles()) {
				Utils.deleteFileTree(child, onExit);
			}
		}
		
		if(!onExit) {
			root.delete();
		}
	}
	
	public static void copyTo(File from, File to) throws IOException {
		if(from.isFile()) {
			Files.copy(from, to);
		}
		else {
			to.mkdirs();
			for(File child : from.listFiles()) {
				File toChild = new File(to, child.getName());
				
				Utils.copyTo(child, toChild);
			}
		}
	}
	
	public static void dumpZipContents(ZipFile file) {
		AdventureLogger.logf(Level.FINEST, "Dumping contents of ZipFile: %s.", file.getName());
		
		Enumeration<? extends ZipEntry> contents = file.entries();
		while(contents.hasMoreElements()) {
			ZipEntry entry = contents.nextElement();
			
			AdventureLogger.logf(Level.FINEST, "\tFound file: %s.", entry);
		}
	}
}

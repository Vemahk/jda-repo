package me.vem.jdab.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ExtFileManager {

	private static final String configDir = "config/";
	
	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	public static Gson getGson() { return gson; }
	
	public static String readFileAsString(File file) {
		if(file == null) return null;
		
		try (FileInputStream fis = new FileInputStream(file);){
			StringBuilder buf = new StringBuilder();
			for(int i = fis.read(); i != -1; i = fis.read())
				buf.append((char)i);
			fis.close();
			return buf.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static File getConfigFile(String fileName) {
		File dir = new File(configDir);
		if(!dir.exists()) dir.mkdirs();
		
		File target = new File(dir, fileName);
		if(!target.exists()) return null;
		return target;
	}
	
	public static PrintWriter getConfigOutput(String fileName) throws IOException{
		File dir = new File(configDir);
		if(!dir.exists()) dir.mkdirs();
		
		File file = new File(dir, fileName);
		if(file.exists()) file.delete();
		file.createNewFile();
		
		return new PrintWriter(file);
	}
}
package me.vem.jdab.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class ExtFileManager {

	private static final String configDir = "config/";
	
	private static Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
	public static Gson getGsonPretty() { return gsonPretty; }
	
	private static Gson gson = new GsonBuilder().create();
	public static Gson getGson() { return gson; }
	
	/**
	 * @param file
	 * @return The stream form, including return carriages, of the given file. 
	 */
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
	
	/**
	 * @param file
	 * @return The FileReader for the given file.
	 * @throws IOException
	 */
	public static FileReader getFileReader(File file){
		if(file == null) return null;
		if(!file.exists()) return null;
		
		try {
			return new FileReader(file);
		} catch (FileNotFoundException e) {}
		
		return null;
	}
	
	/**
	 * @param file
	 * @return The JsonReader for the given file. Assumes that the file is in .json format.
	 * @throws IOException
	 */
	public static JsonReader getJsonReader(File file){
		if(file == null) return null;
		return new JsonReader(getFileReader(file));
	}
	
	/**
	 * @param fileName
	 * @return The file in the config/ directory (if it exists, null otherwise) specified by 'fileName'.
	 */
	public static File getConfigFile(String fileName) {
		File dir = new File(configDir);
		if(!dir.exists()) dir.mkdirs();
		
		File target = new File(dir, fileName);
		if(!target.exists()) return null;
		return target;
	}
	
	/**
	 * Creates the PrintWriter for the given fileName in the config/ directory. 
	 * 
	 * @param fileName 
	 * @return the PrintWriter of the specified file. Creates the file if one does not exist; null under no circumstance.
	 * @throws IOException
	 */
	public static PrintWriter getConfigOutput(String fileName) throws IOException{
		File dir = new File(configDir);
		if(!dir.exists()) dir.mkdirs();
		
		File file = new File(dir, fileName);
		if(file.exists()) file.delete();
		file.createNewFile();
		
		return new PrintWriter(file);
	}
	
	/**
	 * Creates the PrintWriter for the given fileName in the config/subdir/ directory.
	 * 
	 * @param subdir
	 * @param fileName
	 * @return The PrintWriter of the specified file. Creates the subdir/file if they do not already exist; null under no circumstance.
	 * @throws IOException
	 */
	public static PrintWriter getConfigOutput(String subdir, String fileName) throws IOException{
		File dir = new File(configDir, subdir);
		if(!dir.exists()) dir.mkdirs();
		
		File file = new File(dir, fileName);
		if(file.exists()) file.delete();
		file.createNewFile();
		
		return new PrintWriter(file);
	}
}
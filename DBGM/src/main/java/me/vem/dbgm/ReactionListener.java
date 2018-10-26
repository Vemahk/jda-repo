package me.vem.dbgm;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class ReactionListener implements EventListener, Configurable{
	
	private static ReactionListener instance;
	public static ReactionListener getInstance() {
		if(instance == null)
			instance = new ReactionListener();
		return instance;
	}
	
	/**
	 * String 1: trigger
	 * String 2: reaction
	 */
	private Map<String, String> database;
	
	private ReactionListener() { load(); }
	
	@Override public void onEvent(Event event) {
		if (event instanceof MessageReceivedEvent)
            onMessageReceived((MessageReceivedEvent) event);
	}
	
	public void onMessageReceived(MessageReceivedEvent event) {
		String content = event.getMessage().getContentRaw();
		String resp = database.get(content);
		if(resp != null)
			Respond.async(event, resp);
	}
	
	public boolean addReaction(String trigger, String response) {
		if(database.containsKey(trigger))
			return false;
		database.put(trigger, response);
		return true;
	}
	
	public boolean removeReaction(String trigger) {
		return database.remove(trigger) != null;
	}

	@Override
	public void save() {
		try {
			PrintWriter writer = ExtFileManager.getConfigOutput("reactions.json");
			writer.print(ExtFileManager.getGsonPretty().toJson(database));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(database.size() == 0)
			ExtFileManager.getConfigFile("reactions.json").delete();
	}

	@Override
	public void load() {
		database = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("reactions.json");
		if(configFile == null) return;
		
		String fileContent = ExtFileManager.readFileAsString(configFile);
		if(fileContent == null || fileContent.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		database = gson.fromJson(fileContent, new TypeToken<HashMap<String, String>>(){}.getType());
	}
}
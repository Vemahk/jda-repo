package me.vem.dbgm.cmd.reaction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class ReactionListener implements EventListener, Configurable{
	
	private static ReactionListener instance;
	public static ReactionListener getInstance() {
		if(instance == null)
			instance = new ReactionListener();
		return instance;
	}
	
	/**
	 * Long: GuildID
	 * String 1: trigger
	 * String 2: reaction
	 */
	private Map<Long, Map<String, String>> database;
	
	private ReactionListener() {
		load();
		
		//Reaction commands are initialized here because ReactionListener and them all work together.
		AddCustomReaction.initialize();
		DeleteCustomReaction.initialize();
		ListCustomReactions.initialize();
	}
	
	@Override public void onEvent(Event event) {
		if (event instanceof GuildMessageReceivedEvent)
            onGuildMessageReceived((GuildMessageReceivedEvent) event);
	}
	
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		String content = event.getMessage().getContentRaw();
		Map<String, String> data = database.get(event.getGuild().getIdLong());
		if(data == null) return;
		
		String resp = data.get(content);
		if(resp != null)
			Respond.async(event.getChannel(), resp);
	}
	
	public boolean addReaction(Guild guild, String trigger, String response) {
		Map<String, String> guildData = database.get(guild.getIdLong());
		if(guildData == null)
			database.put(guild.getIdLong(), guildData = new LinkedHashMap<String, String>());
		
		if(guildData.containsKey(trigger))
			return false;
		
		guildData.put(trigger, response);
		return true;
	}
	
	public boolean removeReaction(Guild guild, String trigger) {
		Map<String, String> data = database.get(guild.getIdLong());
		if(data == null) return false;
		
		return data.remove(trigger) != null;
	}

	public String[] triggerList(Guild guild) {
		Map<String, String> data = database.get(guild.getIdLong());
		if(data == null) return new String[0];
		
		return data.keySet().toArray(new String[data.size()]);
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
		
		Logger.info("Reaction database saved...");
	}

	@Override
	public void load() {
		database = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("reactions.json");
		if(configFile == null) return;
		
		String fileContent = ExtFileManager.readFileAsString(configFile);
		if(fileContent == null || fileContent.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		database = gson.fromJson(fileContent, new TypeToken<HashMap<Long, LinkedHashMap<String, String>>>(){}.getType());
	}
}
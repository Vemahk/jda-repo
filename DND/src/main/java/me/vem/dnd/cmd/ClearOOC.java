package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.dnd.utils.ExtFileManager;
import me.vem.dnd.utils.Logger;
import me.vem.dnd.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ClearOOC extends Command implements Configurable{
	
	private static ClearOOC instance;
	public static ClearOOC getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new ClearOOC();
	}
	
	private static Map<Long, Set<Long>> allowedRooms;
	
	private ClearOOC() {
		super("clearooc");
		load();
	}
	
	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		Set<Long> guildSet = allowedRooms.get(event.getGuild().getIdLong());
		if(guildSet == null) 
			allowedRooms.put(event.getGuild().getIdLong(), guildSet = new HashSet<>());
		
		int check = 50;
		if(args.length > 0) {
			if(args[0].equals("allow")) {
				if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					Respond.timeout(event, 5000, "Only an administrator can allow/disallow text channels.");
					return false;
				}
				if(!guildSet.add(event.getTextChannel().getIdLong())) {
					Respond.timeout(event, 5000, "Chatroom was already allowed to begin with.");
					return false;
				}
				Respond.timeout(event, 5000, "Chatroom allowed");
				return true;
			}else if(args[0].equals("disallow")) {
				if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					Respond.timeout(event, 5000, "Only an administrator can can allow/disallow text channels.");
					return false;
				}
				if(!guildSet.remove(event.getTextChannel().getIdLong())) {
					Respond.timeout(event, 5000, "Could not remove room. Reason: already not allowed.");
					return false;
				}
				Respond.timeout(event, 5000, "Chatroom disallowed.");
				return true;
			}else{
				try {
					check = Integer.parseInt(args[0]);
				}catch(Exception e) { check = 50; } //If parsing fails, default 50
			}
		}
		
		if(!guildSet.contains(event.getTextChannel().getIdLong())) {
			Respond.timeout(event, 5000, "ClearOOC is not allowed in this chatroom. Ask an admin for details.");
			return false;
		}
		
		Respond.timeout(event, 5000, "Checking past "+check+" messages for ooc...");
		
		HashSet<Message> set = new HashSet<>();
		for(Message x : event.getTextChannel().getHistory().retrievePast(check).complete())
			if(x.getContentRaw().matches("^\\s*\\(.*\\)\\s*$")) // <3 regex
				set.add(x);
		
		HashSet<Message> delSet = new HashSet<>();
		for(Message m : set) 
			if(m.getCreationTime().isAfter(OffsetDateTime.now().minusDays(14)))
				delSet.add(m);
		
		
		if(delSet.size() >= 2)
			event.getTextChannel().deleteMessages(delSet).complete();
		else if(!delSet.isEmpty())
			for(Message m : delSet) m.delete().complete();
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		Member mem = event.getMember();
		
		if(mem.hasPermission(Permission.ADMINISTRATOR) || mem.hasPermission(Permission.MESSAGE_MANAGE)) return true;
		return false;
	}
	
	public boolean roomEnabled(Guild g, TextChannel tc) {
		Set<Long> set = allowedRooms.get(g.getIdLong());
		if(set == null) return false;
		
		return set.contains(tc.getIdLong());
	}
	
	@Override public void save() {
		try {
			PrintWriter out = ExtFileManager.getConfigOutput("clearooc.json");
			out.print(ExtFileManager.getGsonPretty().toJson(allowedRooms));
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Logger.infof("ClearOOC Database Saved...");
	}
	
	@Override public void load() {
		allowedRooms = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("clearooc.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		allowedRooms = gson.fromJson(content, new TypeToken<HashMap<Long, HashSet<Long>>>(){}.getType());
	}
	
	@Override protected String help() {
		return "Usage; clearooc [amount=50]";
	}
	@Override
	protected void unload() {
		save();
		instance = null;
	}
	
}

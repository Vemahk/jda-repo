package me.vem.role.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.role.Bot;
import me.vem.role.utils.ExtFileManager;
import me.vem.role.utils.Logger;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Rankup extends Command implements Configurable{
	
	private static Rankup instance;
	public static Rankup getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new Rankup();
	}
	
	private HashMap<Long, HashMap<String, Long>> info;
	
	private Rankup() {
		super("rankup");
		load();
	}
	
	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		Guild g = event.getGuild();
		long gid = g.getIdLong();
		HashMap<String, Long> ri = info.get(gid);
		if(ri == null)
			info.put(gid, ri = new HashMap<>());
		
		if(args.length == 0) {
			StringBuffer roleList = new StringBuffer();
			
			for(String s : ri.keySet())
				roleList.append(s).append('\n');
			
			Logger.debugf("%s", ri);
			
			if(roleList.length() == 0)
				roleList.append("This guild currently has no assignable roles.\n"
							  + "Ask an admin to 'rankup allow' to add roles.");
			
			Bot.respondAsyncf(event, "%s%nList of assignable roles: ```%n%s```", help(), roleList.toString());
			return true;
		}
		
		
		if(args[0].equals("allow")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Bot.respondAsync(event, "You do not have the permissions to allow roles.");
				return false;
			}
			
			if(args.length != 3) {
				Bot.respondAsync(event, "Usage: rankup allow <@role> <rolereference>");
				return false;
			}
			
			if(ri.containsKey(args[2])) {
				Bot.respondAsyncf(event, "The reference '%s' is already used. Try another.", args[2]);
				return true;
			}
			
			List<Role> roleList = event.getMessage().getMentionedRoles();
			if(roleList.size() != 1) {
				Bot.respondAsync(event, "Specific role mention not found. Please mention exactly one role.");
				return false;
			}
			
			Role r = roleList.get(0);
			ri.put(args[2], r.getIdLong());
			
			Bot.respondAsyncf(event, "Role '%s' added with alias '%s'", r.getName(), args[2]);
			
		}else if(args[0].equals("disallow")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Bot.respondAsync(event, "You do not have the permissions to disallow roles.");
				return false;
			}
			
			if(args.length != 2) {
				Bot.respondAsync(event, "Usage: rankup disallow <rolereference>");
				return false;
			}
			
			if(!ri.containsKey(args[1])) {
				Bot.respondAsyncf(event, "Role with alias '%s' does not exist.", args[1]);
				return false;
			}
			
			ri.remove(args[1]);
			
			Bot.respondAsync(event, "Role alias '"+ args[1] +"' removed.");
		}else {
			if(ri.size() == 0) {
				Bot.respondAsync(event, "This guild currently has no assignable roles.\nAsk an admin to 'rankup allow' to add roles.");
				return true;
			}
			
			Member mem = event.getMember();
			StringBuffer response = new StringBuffer();
			for(String ref : args) {
				
				if(ri.containsKey(ref)) {
					long rid = ri.get(ref);
					Role r = event.getGuild().getRoleById(rid);
					if(mem.getRoles().contains(r)) {
						g.getController().removeSingleRoleFromMember(mem, r).queue();
						response.append(String.format("Role '%s' removed.\n", ref));
					} else {
						g.getController().addSingleRoleToMember(mem, r).queue();
						response.append(String.format("Role '%s' given.\n", ref));
					}
				}else
					response.append(String.format("Role '%s' not found.%n", ref));
			}
			
			Bot.respondAsync(event, response.toString());
		}
		return true;
	}

	@Override public boolean hasPermissions(MessageReceivedEvent event) { return true; }

	@Override
	protected String help() {
		return "Usage: rankup <role> [role2] [role3] [role...etc]";
	}
	
	@Override
	public void save() {
		try {
			PrintWriter out = ExtFileManager.getConfigOutput("rankup.json");
			out.print(ExtFileManager.getGsonPretty().toJson(info));
			out.flush();
			out.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void load() {
		info = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("rankup.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		info = gson.fromJson(content, new TypeToken<HashMap<Long, HashMap<String, Long>>>(){}.getType());
	}
}
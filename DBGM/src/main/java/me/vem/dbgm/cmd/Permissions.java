package me.vem.dbgm.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Permissions extends Command implements Configurable{

	private static Permissions instance;
	public static Permissions getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) 
			instance = new Permissions();
	}
	
	private Permissions() {
		super("permissions");
		load();
	}
	
	/**
	 * Long: the id of the guild.
	 * PermissionData: the the storage of all the roles.
	 */
	private HashMap<Long, Data> database;
	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) 
			return getHelp(event.getChannel());
		
		Data data = database.get(event.getGuild().getIdLong());
		if(data == null)
			database.put(event.getGuild().getIdLong(), data = new Data(event.getGuild().getIdLong()));
		
		if("set".equals(args[0])) {
			if(args.length != 3) 
				return getHelp(event.getChannel());
			
			int lvl = 0;
			
			try {
				lvl = Integer.parseInt(args[2]);
			}catch(NumberFormatException e) {
				Respond.async(event.getChannel(), "Could not parse 3rd argument to int.");
				return false;
			}
			
			if("default".equals(args[1])) {
				data.setDefaultPermissionLevel(lvl);
				Respond.asyncf(event.getChannel(), "Default permission set to %d.", lvl);
				return true;
			}
			
			Member m = Utilities.getMemberFromMention(event.getGuild(), args[1]);
			if(m != null) {
				data.setMemberPermission(m, lvl);
				Respond.asyncf(event.getChannel(), "%s's permission level set to %d", m.getEffectiveName(), lvl);
				return true;
			}
			
			Role r = Utilities.getRoleFromMention(event.getGuild(), args[1]);
			if(r != null) {
				data.setRolePermission(r, lvl);
				Respond.asyncf(event.getChannel(), "Role %s's permission level set to %d", r.getName(), lvl);
				return true;
			}
			
			return getHelp(event.getChannel());
		}else if("keys".equals(args[0])) {
			if(args.length != 2)
				return getHelp(event.getChannel());
			
			Command c = Command.getCommand(args[1]);
			
			if(c == null) {
				Respond.asyncf(event.getChannel(), "`%s` is not a valid command.", args[1]);
				return false;
			}
			
			if(!(c instanceof SecureCommand)) {
				Respond.asyncf(event.getChannel(), "`%s` is not a SecureCommand. It doesn't have permission keys.", args[1]);
				return false;
			}
			
			SecureCommand sc = (SecureCommand) c;
			StringBuilder resp = new StringBuilder("Valid key set for '").append(args[1]).append("':\n```\n");
			for(String s : sc.getValidKeySet())
				resp.append(s).append('\n');
			
			Respond.async(event.getChannel(), resp.append("```").toString());
			//end keys
		}else if("require".equals(args[0])) {
			if(args.length != 3)
				return getHelp(event.getChannel());
			
			int lvl = 0;
			try {
				lvl = Integer.parseInt(args[2]);
			}catch(NumberFormatException e) {
				Respond.async(event.getChannel(), "Could not parse 3rd argument to int.");
				return false;
			}
			
			data.setRequirement(args[1], lvl);
			Respond.asyncf(event.getChannel(), "Key `%s` now requires a permission level of %d", args[1], lvl);
		}else if("unrequire".equals(args[0])) {
			if(args.length != 2)
				return getHelp(event.getChannel());
			
			if(data.removeRequirement(args[1]))
				Respond.asyncf(event.getChannel(), "Key `%s` no longer requires special permissions.", args[1]);
			else Respond.asyncf(event.getChannel(), "Key `%s` was not required already.", args[1]);
		}else if("check".equals(args[0])) {
			if(args.length != 2)
				return getHelp(event.getChannel());
			
			Respond.asyncf(event.getChannel(), "Requirement for `%s`: %d", args[1], data.getKeyRequirement(args[1]));
		}else{
			
			if("default".equals(args[0])) {
				Respond.asyncf(event.getChannel(), "Default permission level: %d", data.getDefaultPermissionLevel());
				return true;
			}
			
			Member m = Utilities.getMemberFromMention(event.getGuild(), args[0]);
			if(m != null) {
				Respond.asyncf(event.getChannel(), "%s's permission level: %d", m.getEffectiveName(), data.getMemberPermission(m));
				return true;
			}
			
			Role r = Utilities.getRoleFromMention(event.getGuild(), args[0]);
			if(r != null) {
				Respond.asyncf(event.getChannel(), "%s's permission level: %d", r.getName(), data.getRolePermission(r));
				return true;
			}
			
			return getHelp(event.getChannel());
		}
		
		return true;
	}

	public boolean hasPermissionsFor(Member m, String key) {
		Data data = database.get(m.getGuild().getIdLong());
		if(data == null) return true;
		
		int lvl = data.getMemberPermission(m);
		int req = data.getKeyRequirement(key);
		
		//Logger.debugf("Key: %s, %d required, %s has %d. %s", key, req, m.getEffectiveName(), lvl, lvl >= req);
		
		return lvl >= req;
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help() {
		return "Usage:\n```\n"
			 + "permissions -- shows the user's current permission level\n"
			 + "permissions <@user> -- returns the user's permission level.\n"
			 + "permissions <@role> -- returns the role's permission level.\n"
			 + "permissions default -- returns the default permission levle.\n"
			 + "permissions set <@user> <level> -- sets the user's permission level.\n"
			 + "permissions set <@role> <level> -- sets a role's default permission level.\n"
			 + "permissions set default <level> -- sets the default permission level for everybody.\n"
			 + "permissions keys <cmdname> -- gets a list of valid keys for the command.\n"
			 + "permissions require <cmdkey> <level> -- sets the required permission level to run a command.\n"
			 + "permissions unrequire <cmdkey> -- removes the need for any permission level.\n"
			 + "permissions check <cmdkey> -- returns the current requirement for a command key.\n"
			 + "```";
	}
	
	@Override public void save() {
		try {
			PrintWriter out = ExtFileManager.getConfigOutput("permissions.json");
			
			Gson gson = ExtFileManager.getGsonPretty();

			out.print(gson.toJson(database));
			
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Logger.infof("Permissions database saved...");
	}
	
	@Override public void load() {
		database = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("permissions.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		database = gson.fromJson(content, new TypeToken<HashMap<Long, Data>>(){}.getType());
	}
	
	@Override
	protected void unload() {
		save();
		instance = null;
	}
	
	public static class Data{
		
		private long guildID;
		private int defLevel;
		
		private Map<Long, Integer> roleLevels;
		private Map<Long, Integer> memberLevels;
		
		private Map<String, Integer> keyReq;
		
		public Data(long gid) {
			this.guildID = gid;
			roleLevels = new HashMap<>();
			memberLevels = new HashMap<>();
			keyReq = new HashMap<>();
		}
		
		public long getGuildID() { return guildID; }
		
		public int getDefaultPermissionLevel() {
			return defLevel;
		}
		
		public Data setDefaultPermissionLevel(int lvl) {
			defLevel = lvl;
			return this;
		}
		
		public int getMemberPermission(Member m) {
			if(m.getGuild().getIdLong() != guildID) {
				Logger.warn("Attempting to get a member's permission level of a guild he/she is not apart of... Behavior unknown.");
				return 0;
			}
			
			User u = m.getUser();
			if(memberLevels.containsKey(u.getIdLong()))
				return memberLevels.get(u.getIdLong());
			
			int max = defLevel;
			for(Role r : m.getRoles()) {
				int rLvl = getRolePermission(r);
				if(rLvl > max) max = rLvl;
			}
			
			return max;
		}
		
		public void setMemberPermission(Member m, int lvl) {
			if(m.getGuild().getIdLong() != guildID) {
				Logger.warn("Attempting to add member permission level to a guild it is not apart of... Dropping.");
				return;
			}
			
			memberLevels.put(m.getUser().getIdLong(), lvl);
		}
		
		public int getRolePermission(Role r) {
			if(r.getGuild().getIdLong() != guildID) {
				Logger.warn("Attempting to get a role's permission level of a guild it is not apart of... Behavior unknown.");
				return 0;
			}
			
			if(roleLevels.containsKey(r.getIdLong()))
				return roleLevels.get(r.getIdLong());
			return defLevel;
		}
		
		public void setRolePermission(Role r, int lvl) {
			if(r.getGuild().getIdLong() != guildID) {
				Logger.warn("Attempting to add role permission level to a guild it is not apart of... Dropping.");
				return;
			}
			
			roleLevels.put(r.getIdLong(), lvl);
		}
		
		public int getKeyRequirement(String key) {
			if(keyReq.containsKey(key))
				return keyReq.get(key);
			return 1;
		}
		
		public void setRequirement(String key, int lvl) {
			keyReq.put(key, lvl);
		}
		
		public boolean removeRequirement(String key) {
			return keyReq.remove(key) != null;
		}
	}
}
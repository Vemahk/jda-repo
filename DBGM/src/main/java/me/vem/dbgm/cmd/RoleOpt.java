package me.vem.dbgm.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class RoleOpt extends SecureCommand implements Configurable{

	private static RoleOpt instance;
	public static RoleOpt getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new RoleOpt();
	}
	
	/**
	 * Long 1: Guild ID
	 * String 1: Alias
	 * Long 2: Role ID
	 */
	private Map<Long, Map<String, Long>> database;
	
	private RoleOpt() {
		super("iam");
		load();
	}

	@Override public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		if(args.length == 0)
			return sendHelp(event.getChannel(), true);

		Map<String, Long> guildDatabase = database.get(event.getGuild().getIdLong());
		if(guildDatabase == null)
			database.put(event.getGuild().getIdLong(), guildDatabase = new LinkedHashMap<String, Long>());
		
		if("list".equals(args[0])) {
			if(guildDatabase.isEmpty()) {
				Respond.async(event.getChannel(), "Your guild does not have any role aliases you can opt-in to.");
				return true;
			}
			
			StringBuilder response = new StringBuilder("List of aliases:\n```\n");
			for(String s : guildDatabase.keySet()) {
				Role r = event.getGuild().getRoleById(guildDatabase.get(s));
				response.append(s).append(" -- ").append(r.getName()).append('\n');
			}
			
			Respond.async(event.getChannel(), response.append("```").toString());
		}else if("not".equals(args[0])) {
			if(args.length < 2)
				return sendHelp(event.getChannel(), false);
			
			if(!guildDatabase.containsKey(args[1])) {
				Respond.async(event.getChannel(), "Your guild does not have that alias.");
				return false;
			}
			
			Role r = event.getGuild().getRoleById(guildDatabase.get(args[1]));
			event.getGuild().getController().removeSingleRoleFromMember(event.getMember(), r).queue(
					(success) -> Respond.asyncf(event.getChannel(), "Role `%s` removed.", r.getName()),
					(failure) -> Respond.asyncf(event.getChannel(), "You did not have the role `%s`", r.getName()));
		}else if("assign".equals(args[0])) {
			if(args.length < 3) 
				return sendHelp(event.getChannel(), false);
			
			if(guildDatabase.containsKey(args[2])) {
				Role roleForExistingAlias = event.getGuild().getRoleById(guildDatabase.get(args[2]));
				Respond.asyncf(event.getChannel(), "There already exists an alias called `%s` for the role `%s`", args[2], roleForExistingAlias.getName());
				return false;
			}
			
			Role r = Utilities.getRoleFromMention(event.getGuild(), args[1]);
			if(r == null) {
				Respond.async(event.getChannel(), "Could not identify the role... Be sure to @mention the role group.");
				return false;
			}
			
			guildDatabase.put(args[2], r.getIdLong());
			Respond.asyncf(event.getChannel(), "`%s` added as an alias for `%s`!", args[2], r.getName());
		}else if("unassign".equals(args[0])) {
			if(args.length < 2)
				return sendHelp(event.getChannel(), false);
			
			if(!guildDatabase.containsKey(args[1])) {
				Respond.asyncf(event.getChannel(), "`%s` is not a known role alias.", args[1]);
				return false;
			}
			
			guildDatabase.remove(args[1]);
			Respond.asyncf(event.getChannel(), "The alias `%s` was removed!", args[1]);
		}else{
			if(!guildDatabase.containsKey(args[0])) {
				Respond.asyncf(event.getChannel(), "`%s` is not a known role alias.", args[0]);
				return false;
			}
			
			Role r = event.getGuild().getRoleById(guildDatabase.get(args[0]));
			event.getGuild().getController().addSingleRoleToMember(event.getMember(), r).queue(
					(success) -> Respond.asyncf(event.getChannel(), "Role `%s` assigned!", r.getName()),
					(failure) -> Respond.asyncf(event.getChannel(), "Could not assign role `%s`. Perhaps you already had it?", r.getName()));
			
		}
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		if(args.length > 0 && ("assign".equals(args[0]) || "unassign".equals(args[0])))
			return Permissions.getInstance().hasPermissionsFor(event.getMember(), "iam.assign");
		return true;
	}

	@Override public String[] usages() {
		return new String[] {
			"`iam list` -- gives a list of roles with their alias.",
			"`iam <rolealias>` -- Opts you in to a role by its alias.",
			"`iam not <rolealias>` -- Opts you out of a role by its alias.",
			"`iam assign <@role> <alias>` -- assigns an alias to a role.",
			"`iam unassign <alias>` -- unassigns an alias from a role."
		};
	}

	@Override protected void unload() {
		instance = null;
		save();
	}

	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("iam.assign");
	}
	
	@Override public void save() {
		try {
			PrintWriter out = ExtFileManager.getConfigOutput("roles.json");
			out.print(ExtFileManager.getGsonPretty().toJson(database));
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Logger.infof("RoleOpt database saved...");
	}
	
	@Override public void load() {
		database = new LinkedHashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("roles.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		database = gson.fromJson(content, new TypeToken<LinkedHashMap<Long, LinkedHashMap<String, Long>>>(){}.getType());
	}
	@Override
	public String getDescription() {
		return "Allows users to opt-in to certain roles that admins designate.";
	}
}
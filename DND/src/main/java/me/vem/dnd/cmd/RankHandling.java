package me.vem.dnd.cmd;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.DiscordBot;
import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class RankHandling extends Command implements Configurable, EventListener{

	private static RankHandling instance;
	public static RankHandling getInstance() {
		if(instance == null)
			instance = new RankHandling();
		return instance;
	}
	
	/**
	 * Long 1: Guild ID
	 * Long 2: Default Rank ID
	 */
	private Map<Long, RoleInfo> database;
	private RoleInfo getRoleInfo(Guild guild) {
		RoleInfo ret = database.get(guild.getIdLong());
		if(ret == null)
			database.put(guild.getIdLong(), ret = new RoleInfo());
		return ret;
	}
	
	private RankHandling() {
		super("iam");
		load();
	}

	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		if(args.length == 0)
			return sendHelp(event.getChannel(), true);

		RoleInfo info = getRoleInfo(event.getGuild());
		
		if("list".equals(args[0])) {
			if(info.roles.isEmpty()) {
				Respond.async(event.getChannel(), "Your guild does not have any role aliases you can opt-in to.");
				return true;
			}
			
			StringBuilder response = new StringBuilder("List of aliases:\n```\n");
			for(String s : info.aliases()) {
				Role r = event.getGuild().getRoleById(info.getRoleID(s));
				response.append(s).append(" -- ").append(r.getName()).append('\n');
			}
			
			Respond.async(event.getChannel(), response.append("```").toString());
		}else if("not".equals(args[0])) {
			if(args.length < 2)
				return sendHelp(event.getChannel(), false);
			
			if(!info.isAlias(args[1])) {
				Respond.async(event.getChannel(), "Your guild does not have that alias.");
				return false;
			}
			
			Role r = event.getGuild().getRoleById(info.getRoleID(args[1]));
			event.getGuild().removeRoleFromMember(event.getMember(), r).queue(
					(success) -> Respond.asyncf(event.getChannel(), "Role `%s` removed.", r.getName()),
					(failure) -> Respond.asyncf(event.getChannel(), "You did not have the role `%s`", r.getName()));
		}else if("assign".equals(args[0])) {
			if(args.length < 3) 
				return sendHelp(event.getChannel(), false);
			
			if(info.isAlias(args[2])) {
				Role roleForExistingAlias = event.getGuild().getRoleById(info.getRoleID(args[2]));
				Respond.asyncf(event.getChannel(), "There already exists an alias called `%s` for the role `%s`", args[2], roleForExistingAlias.getName());
				return false;
			}
			
			Role r = Utilities.getRoleFromMention(event.getGuild(), args[1]);
			if(r == null) {
				Respond.async(event.getChannel(), "Could not identify the role... Be sure to @mention the role group.");
				return false;
			}
			
			info.addRole(args[2], r);
			Respond.asyncf(event.getChannel(), "`%s` added as an alias for `%s`!", args[2], r.getName());
		}else if("unassign".equals(args[0])) {
			if(args.length < 2)
				return sendHelp(event.getChannel(), false);
			
			if(info.removeRole(args[1]))
				Respond.asyncf(event.getChannel(), "The alias `%s` was removed!", args[1]);
			else {
				Respond.asyncf(event.getChannel(), "`%s` is not a known role alias.", args[1]);
				return false;
			}
		}else if("default".equals(args[0])) {
			if(args.length < 2)
				return sendHelp(event.getChannel(), false);
			
			Role role = Utilities.getRoleFromMention(event.getGuild(), args[1]);
			if(role == null) {
				Respond.asyncf(event.getChannel(), "Invalid role entered.");
				return false;
			}
			
			info.setDefaultRole(role);
			Respond.asyncf(event.getChannel(), "Default role set!");
		}else{
			if(!info.isAlias(args[0])) {
				Respond.asyncf(event.getChannel(), "`%s` is not a known role alias.", args[0]);
				return false;
			}
			
			Role r = event.getGuild().getRoleById(info.getRoleID(args[0]));
			event.getGuild().addRoleToMember(event.getMember(), r).queue(
					(success) -> Respond.asyncf(event.getChannel(), "Role `%s` assigned!", r.getName()),
					(failure) -> Respond.asyncf(event.getChannel(), "Could not assign role `%s`. Perhaps you already had it?", r.getName()));
			
		}
		
		return true;
	}
	
	@Override
	public String getDescription() {
		return "Allows for the setting of a default assigned role and optable roles.";
	}

	@Override
	public String[] usages() {
		return new String[] {
			"`iam list` -- Prints a list of aliases you can opt in to.",
			"`iam <alias>` -- Opts you in to a specific alias' role.",
			"`iam not <alias>` -- Opts you out of a specific alias' role.",
			"`iam assign <@role> <alias>` -- Assigns an alias to a specific role.",
			"`iam unassign <alias>` -- Unassigns an alias from a role.",
			"`iam default <@role>` -- Assigns the default guild role."
		};
	}

	@Override
	public boolean hasPermissions(Member member, String... args) {
	    if(args.length == 0)
	        return true;
	    
	    switch(args[0]) {
    	    case "assign":
    	    case "unassign":
    	    case "default":
    	        return member.hasPermission(Permission.ADMINISTRATOR);
	    }
	    
		return true;
	}

	@Override
	public void save() {
		if(Utilities.saveToJSONFile("rank.json", database))
			Logger.info("Ranks database saved.");
		else Logger.err("A problem occured during the RankHandling save.");
	}

	@Override
	public void load() {
		database = new LinkedHashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("rank.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		database = gson.fromJson(content, new TypeToken<LinkedHashMap<Long, RoleInfo>>(){}.getType());
	}

	@Override
	protected void unload() {
		save();
		
		DiscordBot.getInstance().getJDA().removeEventListener(this);
		instance = null;
	}

	@Override
	public void onEvent(GenericEvent event) {
		if(event instanceof GuildMemberJoinEvent)
			onGuildJoin((GuildMemberJoinEvent)event);
	}

	private void onGuildJoin(GuildMemberJoinEvent event) {
		RoleInfo info = getRoleInfo(event.getGuild());
		if(info.getDefaultRoleId() != 0) {
			//Add default role...
			event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(info.getDefaultRoleId())).queue(
				(success) -> {
					event.getMember().getUser().openPrivateChannel().queue(
						(pc) -> {
							pc.sendMessage("Hey! Welcome to " + event.getGuild().getName() + "!").queue();
						}, (fail) -> {}
					);
				}, (failure) -> {
					Logger.errf("Unable to add default role '%d' to '%s' in the '%s' guild.", info.getDefaultRoleId(), event.getMember().getEffectiveName(), event.getGuild().getName());
				}
			);
		}
	}
	
	private class RoleInfo{
		private long defRoleID;
		
		/**
		 * String: alias;
		 * Long: Role ID;
		 */
		private Map<String, Long> roles;
		
		public RoleInfo() {
			roles = new LinkedHashMap<>();
		}
		
		public boolean addRole(String alias, Role role) {
			return roles.put(alias, role.getIdLong()) == null;
		}
		
		/**
		 * @param alias
		 * @return Whether it found and removed the specified alias.
		 */
		public boolean removeRole(String alias) {
			return roles.remove(alias) != null;
		}
		
		public boolean isAlias(String alias) {
			return roles.containsKey(alias);
		}
		
		public Set<String> aliases(){
			return roles.keySet();
		}
		
		public long getRoleID(String alias) {
			return roles.get(alias);
		}
		
		public void setDefaultRole(Role role) {
			defRoleID = role.getIdLong();
		}
		
		public long getDefaultRoleId() {
			return defRoleID;
		}
	}
}
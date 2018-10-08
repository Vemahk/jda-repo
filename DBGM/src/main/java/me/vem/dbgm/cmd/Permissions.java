package me.vem.dbgm.cmd;

import java.util.HashMap;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) {
			getHelp(event);
			return true;
		}
		
		return true;
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR); //This is going to get pretty meta.
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
			 + "permissions require <cmdname> <level> -- sets the required permission level to run a command.\n"
			 + "permissions unrequire <cmdname> -- removes the need for any permission level.\n"
			 + "```";
	}
	
	@Override
	public void save() {
		//TODO Save/Load
	}
	
	@Override
	public void load() {
		database = new HashMap<>();
	}
	
	@Override
	protected void unload() {
		save();
		instance = null;
	}
	
	private static class Data{
		
	}
}
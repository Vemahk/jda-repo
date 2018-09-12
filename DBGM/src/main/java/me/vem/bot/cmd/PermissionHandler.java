package me.vem.bot.cmd;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class PermissionHandler extends Command{

	private static PermissionHandler instance;
	public static PermissionHandler getInstance() { return instance; }
	public static void initialize() {
		if(instance != null) return;
		instance = new PermissionHandler();
	}
	
	private PermissionHandler() {
		super("permissions");
	}
	
	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		return true;
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return false; //This is going to get pretty meta.
	}

	@Override
	public String help() {
		return "Hmm...";
	}
}
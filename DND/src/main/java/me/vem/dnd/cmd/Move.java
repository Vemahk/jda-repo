package me.vem.dnd.cmd;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Move extends Command{

	private static Move instance;
	public static Move getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new Move();
	}
	
	private Move() {
		super("move");
	}

	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		getHelp(event);
		return true;
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	protected String help() {
		return "Currently unimplemented.";
	}
	@Override
	protected void unload() {
		instance = null;
	}
}
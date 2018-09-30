package me.vem.role.cmd;

import java.util.LinkedHashMap;

import me.vem.role.Bot;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public abstract class Command {

	/* Mmm, block code. Noice. */
	private static LinkedHashMap<String, Command> commands = new LinkedHashMap<>();
	public static boolean isCommand(String cmdname) { return commands.containsKey(cmdname); }
	public static Command getCommand(String cmdname) { return commands.get(cmdname); }
	
	public static String[] getCommandLabels() {
		return commands.keySet().toArray(new String[0]);
	}
	 
	protected Command(String cmdname) { commands.put(cmdname, this); }
	
	public abstract boolean hasPermissions(MessageReceivedEvent event);
	protected abstract String help();

	/**
	 * The super implementation of this method only checks permissions. Override this method in all implementing classes.
	 * @param event
	 * @param args
	 */
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!hasPermissions(event)) {
			Bot.respondAsync(event, "You do not have the permissions to run this command.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Tells the bot to respond in the channel given in the event with the help for this command.
	 * @param event
	 */
	public void getHelp(MessageReceivedEvent event) {
		Bot.respondAsync(event, this.help());
	}
	
}

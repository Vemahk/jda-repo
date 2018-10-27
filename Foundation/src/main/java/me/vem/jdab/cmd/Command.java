package me.vem.jdab.cmd;

import java.util.LinkedHashMap;
import java.util.Map;

import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public abstract class Command {

	/* Mmm, block code. Noice. */
	private static Map<String, Command> commands = new LinkedHashMap<>();
	public static boolean isCommand(String cmdname) { return commands.containsKey(cmdname); }
	public static Command getCommand(String cmdname) { return commands.get(cmdname); }
	
	public static String[] getCommandLabels() {
		return commands.keySet().toArray(new String[0]);
	}
	
	/**
	 * Calls the unload method on all initialized commands and clears the commands map.
	 */
	public static void unloadAll() {
		for(Command cmd : commands.values())
			cmd.unload();
		commands.clear();
	}
	 
	protected Command(String cmdname) { commands.put(cmdname, this); }
	
	public abstract boolean hasPermissions(MessageReceivedEvent event, String... args);
	protected abstract String help();
	
	/**
	 * Required postcondition: The command can be reloaded after this method is called.
	 * NOTE: The command class will remove the command instance from the HashMap, you do not need to do that here.
	 * 
	 * In the case of my example commands, their static instance must be set back to null.
	 */
	protected abstract void unload();
	
	/**
	 * The super implementation of this method only checks permissions. Override this method in all implementing classes.
	 * @param event
	 * @param args
	 */
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!hasPermissions(event, args)) {
			Respond.async(event, "You do not have the permissions to run this command.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Tells the bot to respond in the channel given in the event with the help for this command.
	 * @param event
	 */
	public boolean getHelp(MessageReceivedEvent event) {
		Respond.async(event, this.help());
		return true;
	}
	
}

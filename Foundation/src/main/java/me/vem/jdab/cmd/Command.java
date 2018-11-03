package me.vem.jdab.cmd;

import java.util.LinkedList;
import java.util.List;

import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public abstract class Command {

	private static List<Command> commands = new LinkedList<>();
	
	/**
	 * O(n) b/c I am figuring that there won't be an insane amount of commands being registered, so screw efficiency.
	 * @param cmd
	 */
	private static void addCommand(Command cmd) {
		for(Command c : commands)
			if(c.name.equals(cmd.name)) {
				Logger.warnf("Cannot register command '%s' because another command with its name has already been registered.", cmd.getClass().getName());
				return;
			}
		commands.add(cmd);
	}
	
	public static Command getCommand(String cmdname) {
		if(cmdname == null || cmdname.length() == 0)
			return null;
		
		for(Command c : commands)
			if(c.name.equals(cmdname))
				return c;
		return null;
	}
	
	public static String[] getCommandLabels() {
		String[] out = new String[commands.size()];
		int i=0;
		for(Command c : commands)
			out[i++] = c.name;
		
		return out;
	}
	
	/**
	 * Calls the unload method on all initialized commands and clears the commands map.
	 */
	public static void unloadAll() {
		for(Command cmd : commands)
			cmd.unload();
		commands.clear();
	}
	
	private final String name;
	
	protected Command(String cmdname) {
		this.name = cmdname;
		addCommand(this);
	}
	
	public abstract boolean hasPermissions(GuildMessageReceivedEvent event, String... args);
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
	 * @return Generally is meant to return whether the program did what the user intended it to do.<br>
	 * For example: if the user calls the command correctly but lacks permissions, then it fails to do what the user intented, so it would return false.
	 */
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!hasPermissions(event, args)) {
			Respond.async(event.getChannel(), "You do not have the permissions to run this command.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Tells the bot to respond in the channel given in the event with the help for this command.
	 * @param event
	 * @return true, always. So you can return this statement in the run() method.
	 */
	public boolean getHelp(TextChannel channel) {
		Respond.async(channel, this.help());
		return true;
	}
	
}

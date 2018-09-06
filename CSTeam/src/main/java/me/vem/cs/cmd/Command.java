package me.vem.cs.cmd;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface Command {
	/**
	 * This is where 
	 * @param args
	 * @param event
	 */
	public void run(String[] args, MessageReceivedEvent event);
	
	/**
	 * Determines whether a given user can use this command.
	 * @param event
	 * @return True if the user is allowed to use this command. False otherwise.
	 */
	public boolean hasPermissions(MessageReceivedEvent event);
	
	/**
	 * @return The string that the bot will respond with documenting how to use this specific command.
	 * If the string is null, the command WILL NOT be listed upon doing ~!help.
	 */
	public String help();
}

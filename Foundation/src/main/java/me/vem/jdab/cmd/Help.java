package me.vem.jdab.cmd;

import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Help extends Command{

	private static Help instance;
	public static Help getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new Help();
	} 
	
	private Help() { super("help"); }

	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) {
			Respond.async(event, getFormattedCommandList());
			return true;
		}
		
		if(Command.isCommand(args[0]))
			Command.getCommand(args[0]).getHelp(event);
		else Respond.async(event, "Command not recognized.\n" + getFormattedCommandList());
		
		return true;
	}
	
	private String getFormattedCommandList() {
		StringBuilder list = new StringBuilder("List of known commands:\n```\n");
		for(String s : Command.getCommandLabels())
			list.append(s).append('\n');
		return list.append("```").toString();
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		return true; //Everyone can use this command.
	}
	
	@Override
	public String help() {
		return "Usage: `help [command]`"; //Do not list this command. 
	}
	@Override
	protected void unload() {
		instance = null;
	}
}

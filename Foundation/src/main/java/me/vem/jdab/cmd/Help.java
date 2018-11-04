package me.vem.jdab.cmd;

import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Help extends Command{

	private static Help instance;
	public static Help getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new Help();
	} 
	
	private Help() { super("help"); }

	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) {
			//WHAT'S THIS?! CALLBACK HELL?!
			event.getAuthor().openPrivateChannel().queue(
				(channel) -> channel.sendMessage(getFormattedCommandList()).queue(
					(msg) -> event.getMessage().delete().queue()),
				(error) -> Respond.async(event.getChannel(), getFormattedCommandList()));
			return true;
		}
		
		Command cmd = Command.getCommand(args[0]);
		if(cmd != null) cmd.getHelp(event.getChannel());
		else Respond.async(event.getChannel(), "Command not recognized.\n" + getFormattedCommandList());
		
		return true;
	}
	
	private String getFormattedCommandList() {
		StringBuilder list = new StringBuilder("List of known commands:\n```\n");
		for(String s : Command.getCommandLabels())
			list.append(s).append('\n');
		return list.append("```").toString();
	}

	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
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

package me.vem.cs.cmd;

import java.util.ArrayList;
import java.util.Collections;

import me.vem.cs.Main;
import me.vem.cs.Main.TextFormat;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Help implements Command{

	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		if(args.length == 0) {
			listCommands(event);
			return;
		}
		
		if(Main.isCommand(args[0]))
			Main.respond(Main.commands.get(args[0]).help(), event);
		else listCommands("Unknown Command.\n", event);
	}
	
	private void listCommands(MessageReceivedEvent event) {
		listCommands("", event);
	}
	
	/**
	 * Makes the bot respond with a list of known commands (that don't return null from their help function). It will append the append string to the beginning of the message.
	 * @param append
	 * @param event
	 */
	private void listCommands(String append, MessageReceivedEvent event) {
		ArrayList<String> cmds = new ArrayList<>();
		for(String s : Main.commands.keySet()) cmds.add(s);
		Collections.sort(cmds);
		
		String out = "";
		for(String s : cmds) 
			if(Main.commands.get(s).help() != null)
				out += s+"\n";
		
		Main.respond(append + "List of known commands:\n" + Main.format(out, TextFormat.CODE), event);
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return true; //Everyone can use this command.
	}
	
	@Override
	public String help() {
		return null; //Do not list this command. 
	}
}

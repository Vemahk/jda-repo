package me.vem.bot.cmd;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class PermissionHandler implements Command{

	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return false; //This is going to get pretty meta.
	}

	@Override
	public String help(MessageReceivedEvent event) {
		return "Hmm...";
	}
}
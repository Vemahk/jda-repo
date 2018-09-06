package me.vem.dnd.cmd;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Move implements Command{

	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}
}
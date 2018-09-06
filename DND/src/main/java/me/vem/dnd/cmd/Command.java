package me.vem.dnd.cmd;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface Command {
	public void run(String[] args, MessageReceivedEvent event);
	public boolean hasPermissions(MessageReceivedEvent event);
}

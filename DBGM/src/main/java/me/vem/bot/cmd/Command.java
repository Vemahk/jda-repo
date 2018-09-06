package me.vem.bot.cmd;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface Command {
	public void run(String[] args, MessageReceivedEvent event);
	public boolean hasPermissions(MessageReceivedEvent event);
	public String help(MessageReceivedEvent event);
}

package me.vem.bot.cmd;

import me.vem.bot.Bot;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class AntiPurge implements Command{

	public static final String cmd_name = "antipurge";
	
	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		if(!hasPermissions(event)) {
			Bot.respond("Only admins can post antipurge messages.", event);
			return;
		}
		
		if(args.length == 0) {
			Bot.respond("SassyBot >> You gotta have a message to mark as [AntiPurge], bud.", event);
			return;
		}
		
		String trueText = event.getMessage().getContentRaw();
		trueText = trueText.substring(trueText.indexOf(' ') + 1);
		
		StringBuffer out = new StringBuffer();
		
		out.append("[AntiPurge] >> " + event.getMember().getAsMention() + "\n");
		out.append(trueText);
		
		if(out.length() >= 2000) {
			Bot.respond("Cannot [AntiPurge] message. Too long (exceeds 2,000 characters). Sorry boss.", event);
			return;
		}
		
		Bot.respond(out.toString(), event);
		event.getTextChannel().deleteMessageById(event.getMessageId()).queue();
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help(MessageReceivedEvent event) {
		return "Valid Usage: "+cmd_name+" <message>";
	}
}

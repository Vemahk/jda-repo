package me.vem.dbgm.cmd;

import me.vem.dbgm.Bot;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class AntiPurge extends Command{

	private static AntiPurge instance;
	public static AntiPurge getInstance() { return instance; }
	public static void initialize() {
		if(instance != null) return;
		instance = new AntiPurge();
	}
	
	private AntiPurge() {
		super("antipurge");
	}
	
	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) {
			super.getHelp(event);
			return false;
		}
		
		StringBuilder out = new StringBuilder();
		
		out.append("[AntiPurge] >> " + event.getMember().getAsMention() + "\n");
		
		for(int i=0;i < args.length;i++)
			out.append(args[i]).append(i < args.length - 1 ? " " : "");
		
		if(out.length() >= 2000) {
			Bot.respondAsync(event, "Cannot [AntiPurge] message. Too long (exceeds 2,000 characters). Sorry boss.");
			return false;
		}
		
		Bot.respondAsync(event, out.toString());
		event.getTextChannel().deleteMessageById(event.getMessageId()).queue();

		return true;
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help() {
		return "Valid Usage: antipurge <message>";
	}
}

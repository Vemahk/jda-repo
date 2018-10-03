package me.vem.dbgm.cmd;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class AntiPurge extends Command{

	private static AntiPurge instance;
	public static AntiPurge getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
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
		
		out.append("[AntiPurge]").append('\n');
		
		for(int i=0;i < args.length;i++)
			out.append(args[i]).append(i < args.length - 1 ? " " : "");
		
		out.append("\n\n - ").append(event.getMember().getAsMention());
		
		if(out.length() >= 2000) {
			Respond.async(event, "Cannot [AntiPurge] message. Too long (exceeds 2,000 characters). Sorry boss.");
			return false;
		}
		
		Respond.async(event, out.toString());
		event.getTextChannel().deleteMessageById(event.getMessageId()).queue();

		return true;
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help() {
		return "Valid Usage: antipurge <message>";
	}

	@Override
	protected void unload() {
		instance = null;
	}
}

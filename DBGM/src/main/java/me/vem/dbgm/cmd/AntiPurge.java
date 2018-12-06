package me.vem.dbgm.cmd;

import java.util.Arrays;
import java.util.List;

import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class AntiPurge extends SecureCommand{

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
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) {
			super.sendHelp(event.getChannel());
			return false;
		}
		
		StringBuilder out = new StringBuilder();
		
		out.append("[AntiPurge]").append('\n');
		
		for(int i=0;i < args.length;i++)
			out.append(args[i]).append(i < args.length - 1 ? " " : "");
		
		out.append("\n\n - ").append(event.getMember().getAsMention());
		
		if(out.length() >= 2000) {
			Respond.async(event.getChannel(), "Cannot [AntiPurge] message: too long (exceeds 2,000 characters)");
			return false;
		}
		
		Respond.async(event.getChannel(), out.toString());
		event.getMessage().delete().queue();

		return true;
	}

	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), "antipurge");
	}

	@Override
	public String help() {
		return "Usage:\n```\n"
			 + "antipurge <message>\n"
			 + "```";
	}

	@Override
	protected void unload() {
		instance = null;
	}
	
	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("antipurge");
	}
	@Override
	public String getDescription() {
		return "Copies the user's message and responds with that message designed in a way so as to not be purged by the Purge command.";
	}
}

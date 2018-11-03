package me.vem.dbgm.cmd.reaction;

import java.util.Arrays;
import java.util.List;

import me.vem.dbgm.cmd.Permissions;
import me.vem.dbgm.cmd.SecureCommand;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class AddCustomReaction extends SecureCommand{

	private static AddCustomReaction instance;
	public static AddCustomReaction getInstance() {return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new AddCustomReaction();
	}
	
	private AddCustomReaction() { super("acr"); }

	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length != 2) {
			getHelp(event.getChannel());
			return false;
		}
		
		if(ReactionListener.getInstance().addReaction(event.getGuild(), args[0], args[1])) {
			Respond.async(event.getChannel(), "Reaction added.");
		}else {
			Respond.async(event.getChannel(), "Reaction was not added... Does it already exist?");
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), "acr");
	}

	@Override
	protected String help() {
		return "acr stands for Add Custom Reaction.\n"
			 + "A Custom Reaction is an automatic bot response to a specific message.\n"
			 + "Note: Custom Reactions are case-sensitive and per-guild.\n\n"
			 + "Usage:\n```\n"
			 + "acr `[trigger]` `[response]` -- Adds a custom reaction to a particular trigger\n"
			 + "```";
	}

	@Override
	protected void unload() {
		instance = null;
	}
	
	@Override public List<String> getValidKeySet() {
		return Arrays.asList("acr");
	}
}
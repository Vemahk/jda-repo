package me.vem.dbgm.cmd.reaction;

import java.util.Arrays;
import java.util.List;

import me.vem.dbgm.cmd.Permissions;
import me.vem.dbgm.cmd.SecureCommand;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
		
		if(args.length != 2)
			return sendHelp(event.getChannel(), false);
		
		if(ReactionListener.getInstance().addReaction(event.getGuild(), args[0], args[1])) {
			Respond.async(event.getChannel(), "Reaction added.");
		}else {
			Respond.async(event.getChannel(), "Reaction was not added... Does it already exist?");
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(Member member, String... args) {
		return Permissions.getInstance().hasPermissionsFor(member, "acr");
	}

	@Override
	public String[] usages() {
		return new String[] {
			"``acr `[trigger]` `[response]` `` -- Adds a custom reaction with a specific trigger."
		};
	}

	@Override
	protected void unload() {
		instance = null;
	}
	
	@Override public List<String> getValidKeySet() {
		return Arrays.asList("acr");
	}
	
	@Override
	public String getDescription() {
		return "'acr' is an initialization of Add Custom Reaction.\nA custom reaction is a particular phrase that the bot will reply to with a specific message.";
	}
}
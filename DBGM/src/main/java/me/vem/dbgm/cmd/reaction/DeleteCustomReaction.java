package me.vem.dbgm.cmd.reaction;

import java.util.Arrays;
import java.util.List;

import me.vem.dbgm.cmd.Permissions;
import me.vem.dbgm.cmd.SecureCommand;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class DeleteCustomReaction extends SecureCommand{

	private static DeleteCustomReaction instance;
	public static DeleteCustomReaction getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new DeleteCustomReaction();
	}
	
	private DeleteCustomReaction() { super("dcr"); }

	@Override public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length != 1)
			return sendHelp(event.getChannel(), false);
		
		if(ReactionListener.getInstance().removeReaction(event.getGuild(), args[0]))
			Respond.async(event.getChannel(), "Custom reaction removed!");
		else Respond.async(event.getChannel(), "Custom reaction could not be removed. Did it exist?");
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(Member member, String... args) {
		return Permissions.getInstance().hasPermissionsFor(member, "dcr");
	}
	
	@Override
	public String[] usages() {
		return new String[] {
			"``dcr `[trigger]` `` -- Removes a custom reaction based upon its trigger."
		};
	}

	@Override protected void unload() { instance = null; }

	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("dcr");
	}
	
	@Override
	public String getDescription() {
		return "'dcr' is an initialization of Delete Custom Reaction.\nA custom reaction is a particular phrase that the bot will reply to with a specific message.";
	}
}
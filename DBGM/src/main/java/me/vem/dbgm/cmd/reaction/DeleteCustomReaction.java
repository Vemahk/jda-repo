package me.vem.dbgm.cmd.reaction;

import java.util.Arrays;
import java.util.List;

import me.vem.dbgm.ReactionListener;
import me.vem.dbgm.cmd.Permissions;
import me.vem.dbgm.cmd.SecureCommand;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class DeleteCustomReaction extends SecureCommand{

	private static DeleteCustomReaction instance;
	public static DeleteCustomReaction getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new DeleteCustomReaction();
	}
	
	private DeleteCustomReaction() { super("dcr"); }

	@Override public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length != 1) {
			getHelp(event);
			return false;
		}
		
		if(ReactionListener.getInstance().removeReaction(event.getGuild(), args[0]))
			Respond.async(event, "Custom reaction removed!");
		else Respond.async(event, "Custom reaction could not be removed. Did it exist?");
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), "dcr");
	}

	@Override
	protected String help() {
		return "Usage:\n"
			 + "dcr `[trigger]` -- format with \\`the grave key\\`";
	}

	@Override protected void unload() { instance = null; }

	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("dcr");
	}
}
package me.vem.dbgm.cmd.reaction;

import java.util.Arrays;
import java.util.List;

import me.vem.dbgm.ReactionListener;
import me.vem.dbgm.cmd.Permissions;
import me.vem.dbgm.cmd.SecureCommand;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class AddCustomReaction extends SecureCommand{

	private static AddCustomReaction instance;
	public static AddCustomReaction getInstance() {return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new AddCustomReaction();
	}
	
	private AddCustomReaction() { super("acr"); }

	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length != 2) {
			getHelp(event);
			return false;
		}
		
		if(ReactionListener.getInstance().addReaction(event.getGuild(), args[0], args[1])) {
			Respond.async(event, "Reaction added.");
		}else {
			Respond.async(event, "Reaction was not added... Does it already exist?");
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), "acr");
	}

	@Override
	protected String help() {
		return "Usage:\n"
			 + "acr `[trigger]` `[response]` //Formatting with \\`the grave key\\`";
	}

	@Override
	protected void unload() {
		instance = null;
	}
	
	@Override public List<String> getValidKeySet() {
		return Arrays.asList("acr");
	}
}
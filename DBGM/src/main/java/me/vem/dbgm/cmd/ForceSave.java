package me.vem.dbgm.cmd;

import java.util.Arrays;
import java.util.List;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ForceSave extends SecureCommand{

	private static ForceSave instance;
	public static ForceSave getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new ForceSave();
	}
	
	private ForceSave() { super("forcesave"); }

	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		for(String cmdLabel : Command.getCommandLabels()) {
			Command cmd = Command.getCommand(cmdLabel);
			if(cmd == null) continue; //Shouldn't happen, but you know.			
			
			if(cmd instanceof Configurable) {
				Configurable conf = (Configurable) cmd;
				conf.save();
			}
		}
		
		event.getMessage().delete().queue();
		Respond.timeout(event.getTextChannel(), 3000, "Databases Saved");
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), "forcesave");
	}

	@Override
	protected String help() {
		return "Usage:\n```\n"
			 + "forcesave -- forces the save of all registered commands.\n"
			 + "```";
	}

	@Override
	protected void unload() {
		instance = null;
	}

	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("forcesave");
	}

	
	
}

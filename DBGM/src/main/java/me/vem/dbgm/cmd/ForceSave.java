package me.vem.dbgm.cmd;

import java.util.Arrays;
import java.util.List;

import me.vem.jdab.DiscordBot;
import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ForceSave extends SecureCommand{

	private static ForceSave instance;
	public static ForceSave getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new ForceSave();
	}
	
	private ForceSave() { super("forcesave"); }

	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		for(String cmdLabel : Command.getCommandLabels()) {
			Command cmd = Command.getCommand(cmdLabel);
			if(cmd == null) continue; //Shouldn't happen, but you know.			
			
			if(cmd instanceof Configurable) {
				Configurable conf = (Configurable) cmd;
				conf.save();
			}
		}
		
		for(Object o : DiscordBot.getInstance().getJDA().getRegisteredListeners())
			if(o instanceof Configurable)
				((Configurable)o).save();
		
		event.getMessage().delete().queue();
		Respond.timeout(event.getChannel(), 3000, "Databases Saved");
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), "forcesave");
	}
	
	@Override public String[] usages() {
		return new String[] {
			"`forcesave` -- Forces the save of all registed commands."
		};
	}

	@Override
	protected void unload() {
		instance = null;
	}

	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("forcesave");
	}
	@Override
	public String getDescription() {
		return "Forces the bot to save all of its databases.";
	}
}

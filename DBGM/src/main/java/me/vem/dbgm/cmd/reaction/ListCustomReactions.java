package me.vem.dbgm.cmd.reaction;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class ListCustomReactions extends Command{

	private static ListCustomReactions instance;
	public static ListCustomReactions getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new ListCustomReactions();
	}
	
	private ListCustomReactions() { super("lcr"); }

	@Override public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		int page = 1;
		if(args.length == 1) {
			try {
				page = Integer.parseInt(args[0]);
			}catch(NumberFormatException e) {
				return !sendHelp(event.getChannel());
			}
		}else if(args.length > 1)
			return !sendHelp(event.getChannel());
		
		final int fPage = page; //For the lambda.
		if(lastList == null) respondPage(event, page);
		else {
			long diff = System.currentTimeMillis() / 1000 - lastList.getCreationTime().toEpochSecond();
			if(diff <= 60)
				lastList.editMessage(getPage(event.getGuild(), page)).queue((msg_success) -> {/*We don't care.*/},
					(error) -> respondPage(event, fPage));
			
			else respondPage(event, page);
		}
		
		return true;
	}
	
	private Message lastList = null;
	private void respondPage(GuildMessageReceivedEvent event, int page) {
		if(lastList != null)
			lastList.delete().queue((msg) -> {}, (err) -> {});
		lastList = Respond.sync(event.getChannel(), getPage(event.getGuild(), page));
	}

	private String getPage(Guild guild, int page) {
		String[] triggers = ReactionListener.getInstance().triggerList(guild);
		if(triggers.length == 0) return "This guild has no Custom Reactions.";
		
		if(triggers.length - 1 < (page-1) * 10) return "The Custom Reaction list does not have " + page + " pages";
		
		StringBuilder rsp = new StringBuilder("[Custom Reaction List Page ").append(page).append("]```");
		
		for(int i=(page-1) * 10; i < triggers.length && i < page * 10;i++)
			rsp.append('\n').append(triggers[i]);
		
		return rsp.append("```").toString();
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return true;
	}

	@Override
	protected String help() {
		return "Usage:\n```\n"
			 + "lcr [pagenum=1] -- Lists a given page of custom reactions.\n"
			 + "```";
	}

	@Override
	protected void unload() {
		instance = null;
	}

	@Override
	public String getDescription() {
		return "'lcr' is an initialization of List Custom Reactions.\nA custom reaction is a particular phrase that the bot will reply to with a specific message.";
	}
}

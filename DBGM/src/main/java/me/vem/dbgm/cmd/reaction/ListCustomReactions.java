package me.vem.dbgm.cmd.reaction;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ListCustomReactions extends Command{

	private static ListCustomReactions instance;
	public static ListCustomReactions getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new ListCustomReactions();
	}
	
	private ListCustomReactions() { super("lcr"); }

	@Override public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		int page = 1;
		if(args.length == 1) {
			try {
				page = Integer.parseInt(args[0]);
			}catch(NumberFormatException e) {
				getHelp(event);
				return false;
			}
		}else if(args.length > 1) {
			getHelp(event);
			return false;
		}
		
		int fPage = page; //For the lambda.
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
	private void respondPage(MessageReceivedEvent event, int page) {
		if(lastList != null)
			lastList.delete().queue((msg) -> {}, (err) -> {});
		lastList = Respond.sync(event, getPage(event.getGuild(), page));
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
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
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

}

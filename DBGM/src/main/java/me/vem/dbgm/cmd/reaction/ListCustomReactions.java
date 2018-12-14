package me.vem.dbgm.cmd.reaction;

import java.awt.Color;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.struct.menu.EmbedMenu;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
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
		
		new CRMenu(Respond.sync(event.getChannel(), getPage(event.getGuild(), 1)));
		
		return true;
	}

	private EmbedBuilder getPage(Guild guild, int page) {
		EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED);

		String[] triggers = ReactionListener.getInstance().triggerList(guild);
		if(triggers.length == 0) 
			return builder.setTitle("This guild has no Custom Reactions.");
		
		builder.setTitle("Custom Reactions - Page " + page);
		
		if(triggers.length - 1 < (page-1) * 10)
			return builder.addField("No such page", "", false);
		
		for(int i=(page-1) * 10; i < triggers.length && i < page * 10;i++)
			builder.addField("CR #" + i, triggers[i], true);
		
		return builder.setColor(Color.GREEN);
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return true;
	}
	
	@Override
	public String[] usages() {
		return new String[] {
			"`lcr` -- Lists a given page of custom reactions."
		};
	}

	@Override
	protected void unload() {
		instance = null;
	}

	@Override
	public String getDescription() {
		return "'lcr' is an initialization of List Custom Reactions.\nA custom reaction is a particular phrase that the bot will reply to with a specific message.";
	}
	
	private class CRMenu extends EmbedMenu{
		public CRMenu(Message msg) {
			super(msg);
		}

		@Override
		public MessageEmbed getEmbed(int page) {
			return ListCustomReactions.this.getPage(msg.getGuild(), page).build();
		}
	}
}

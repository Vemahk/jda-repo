package me.vem.jdab.cmd;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import me.vem.jdab.utils.Emoji;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Task;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class Help extends Command implements EventListener{

	private static Help instance;
	public static Help getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new Help();
	} 

	private List<Menu> openMenues;
	private Timer destroyQueue;
	
	private Help() {
		super("help");
		openMenues = new LinkedList<>();
		destroyQueue = new Timer();
	}

	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) {
			//WHAT'S THIS?! CALLBACK HELL?!
			event.getAuthor().openPrivateChannel().queue((pc) -> {
				new Menu(pc.sendMessage(getCommandList(1).build()).complete());
			}, (fail) -> {
				new Menu(Respond.sync(event.getChannel(), getCommandList(1)));
			});
			return true;
		}
		
		Command cmd = Command.getCommand(args[0]);
		if(cmd != null) cmd.sendHelp(event.getChannel(), true);
		else new Menu(Respond.sync(event.getChannel(), getCommandList(1).appendDescription(" - Command not recognized.")));
		
		event.getMessage().delete().queue();
		
		return true;
	}
	
	private EmbedBuilder getCommandList(int page) {
		EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED).setTitle("Help - Page " + page);
		
		if(page < 1)
			return builder.addField("No such page", "", false);
		
		Iterator<Command> iter = Command.getIter();
		for(int i=0; i < (page - 1) * 5;i++, iter.next())
			if(!iter.hasNext())
				return builder.addField("No such page", "", false);
		
		for(int x=0;x<5;x++) {
			if(!iter.hasNext()) {
				if(x == 0)
					return builder.addField("No such page", "", false);
				break;
			}
			Command nxt = iter.next();
			builder.addField(nxt.getName(), nxt.getDescription(), false);
		}
		
		return builder.setColor(Color.GREEN);
	}
	
	@Override
	public String getDescription() {
		return "Prints a list of known commands, or tries and get the help for a specific command.";
	}
	
	@Override
	public String[] usages() {
		return new String[] {
			"`help [command]`",
			" - Prints the help for the given command, or",
			" - Prints a list of commands if no command is mentioned."
		};
	}

	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return true;
	}
	
	@Override
	protected void unload() {
		destroyQueue.cancel();
		while(!openMenues.isEmpty())
			openMenues.get(0).destroy();
		instance = null;
	}
	
	private class Menu{
		private final Message msg;
		private int page;
		
		public Menu(Message msg) {
			this(msg, 1);
		}
		
		public Menu(Message msg, int page) {
			this.msg = msg;
			this.page = page;
			
			msg.addReaction(Help.LEFT_ARROW.toString()).queue();
			msg.addReaction(Help.RIGHT_ARROW.toString()).queue();
			
			openMenues.add(this);
			destroyQueue.schedule(new Task(() -> destroy()), 300 * 1000);
		}
		
		public void nextPage() {
			msg.editMessage(getCommandList(++page).build()).queue();
		}
		
		public void prevPage() {
			if(page == 1) return;
			msg.editMessage(getCommandList(--page).build()).queue();
		}
		
		public void destroy() {
			msg.delete().queue();
			openMenues.remove(this);
		}
	}
	
	@Override
	public void onEvent(Event event) {
		if(event instanceof MessageReactionAddEvent)
			addReaction((MessageReactionAddEvent)event);
		else if(event instanceof MessageReactionRemoveEvent)
			remReaction((MessageReactionRemoveEvent)event);
	}
	
	private static final Emoji LEFT_ARROW = new Emoji("\u2B05");
	private static final Emoji RIGHT_ARROW = new Emoji("\u27A1");
	
	private void addReaction(MessageReactionAddEvent event) {
		if(event.getUser().equals(event.getJDA().getSelfUser()))
			return;
		
		Emoji reaction = new Emoji(event.getReactionEmote());
		if(!(reaction.equals(LEFT_ARROW) || reaction.equals(RIGHT_ARROW)))
			return;
		
		for(Menu m : openMenues)
			if(m.msg.getIdLong() == event.getMessageIdLong())
				if(reaction.equals(LEFT_ARROW))
					m.prevPage();
				else m.nextPage();
	}
	
	private void remReaction(MessageReactionRemoveEvent event) {
		if(event.getUser().equals(event.getJDA().getSelfUser()))
			return;
		
		Emoji reaction = new Emoji(event.getReactionEmote());
		if(!(reaction.equals(LEFT_ARROW) || reaction.equals(RIGHT_ARROW)))
			return;
		
		for(Menu m : openMenues)
			if(m.msg.getIdLong() == event.getMessageIdLong())
				if(reaction.equals(LEFT_ARROW))
					m.prevPage();
				else m.nextPage();
	}
}

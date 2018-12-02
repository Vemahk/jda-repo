package me.vem.dnd.cmd;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import com.vdurmont.emoji.EmojiManager;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Vote extends Command{

	private static Vote instance;
	public static Vote getInstance() {
		return instance;
	}
	
	public static void initialize() {
		if(instance == null)
			instance = new Vote();
	}
	
	private Timer timer;
	private List<Message> activeVotes;
	
	private Vote() {
		super("vote");
		timer = new Timer();
		activeVotes = new LinkedList<>();
	}
	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		if(args.length < 6)
			return getHelp(event.getChannel());
		
		if(!"create".equalsIgnoreCase(args[0]))
			return !getHelp(event.getChannel());
		
		int seconds;
		try {
			seconds = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			Logger.err("Vote: could not parse number.");
			return !getHelp(event.getChannel());
		}
		
		Map<Object, String> items = new LinkedHashMap<>();
		
		for(int i=2;i<args.length-1;i+=2) {
			Emote e = Utilities.getEmoteFromMention(event.getGuild(), args[i]);
			
			if(e != null) {
				if(items.containsKey(e)) {
					Respond.async(event.getChannel(), "Cannot create a vote with two of the same emotes.");
					return false;
				}
				
				items.put(e, args[i+1]);
			}else{
				if(EmojiManager.isEmoji(args[i])) { //f-ing utf-32
					if(items.containsKey(args[i])) {
						Respond.async(event.getChannel(), "Cannot create a vote with two of the same emotes.");
						return false;
					}
					
					items.put(args[i], args[i+1]);
				}else {
					Logger.errf("Vote: could not read emote '%s'.", args[i]);
					return !getHelp(event.getChannel());
				}
			}
		}
		
		EmbedBuilder builder = new EmbedBuilder().setColor(Color.GREEN);
		
		builder.setDescription("This vote will expire in " + seconds + " seconds.");
		
		for(Object emoji : items.keySet()) {
			String eText = (emoji instanceof Emote) ? ((Emote)emoji).getAsMention() : emoji.toString();
			
			builder.addField(items.get(emoji), eText, true);
		}
			
		
		Message m = Respond.sync(event.getChannel(), builder);
		for(Object e : items.keySet()) {
			if(e instanceof Emote)
				m.addReaction((Emote)e).queue();
			else m.addReaction(e.toString()).queue();
		}
		
		activeVotes.add(m);
		
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	protected String help() {
		return "Usage:\n```\n"
			 + "vote create <time> <:emote1:> <`desc1`> <:emote2:> <`desc2`> ... (etc) \n"
			 + "\t- 'time' is in units of seconds.\n"
			 + "\t- Creates a vote mapping emotes to certain items to vote for.\n"
			 + "\t- There must be at least two items to vote for."
			 + "```";
	}

	@Override
	protected void unload() {
		timer.cancel();
		for(Message m : activeVotes)
			m.delete().queue();
		activeVotes.clear();
		
		instance = null;
	}
	
	
	
}

package me.vem.dnd.cmd;

import java.awt.Color;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import com.vdurmont.emoji.EmojiManager;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.struct.Task;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import me.vem.jdab.utils.emoji.Emoji;
import me.vem.jdab.utils.emoji.Emojis;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class PollCMD extends Command implements EventListener{

	private static PollCMD instance;
	public static PollCMD getInstance() {
		return instance;
	}
	
	public static void initialize() {
		if(instance == null)
			instance = new PollCMD();
	}
	
	private Timer timer;
	private List<Poll> activePolls;
	
	private PollCMD() {
		super("poll");
		timer = new Timer();
		activePolls = new LinkedList<>();
	}
	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		if(args.length < 5)
			return sendHelp(event.getChannel(), true);
		
		int seconds = 0;
		if(!"manuend".equals(args[0])) {
			try {
				seconds = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				Logger.err("Vote: could not parse number.");
				return sendHelp(event.getChannel(), false);
			}			
		}
		
		Map<Emoji, String> items = new LinkedHashMap<>();
		
		for(int i=1;i<args.length-1;i+=2) {
			Emoji e = null;
			
			if(EmojiManager.isEmoji(args[i]))
				e = new Emoji(args[i]);
			else {
				Emote emote = Utilities.getEmoteFromMention(event.getGuild(), args[i]);
				if(emote != null)
					e = new Emoji(emote);
			}
			
			if(e == null) {
				Logger.errf("Vote: could not read emote '%s'.", args[i]);
				return sendHelp(event.getChannel(), false);
			}
			
			if(items.containsKey(e)) {
				Respond.async(event.getChannel(), "Cannot create a poll with two of the same emotes.");
				return false;
			}
			
			items.put(e, args[i+1]);
		}
		
		EmbedBuilder builder = new EmbedBuilder().setColor(Color.GREEN);
		
		if(seconds > 0)
			builder.setDescription("This poll will expire in " + seconds + " seconds.");
		else builder.setDescription("This poll is set to never expire.");
		
		for(Emoji emoji : items.keySet())
			builder.addField(items.get(emoji), emoji.toString(), true);
		
		builder.addBlankField(false);
		builder.addField("Please only vote once!", "Users who vote more than once will be disqualified.", false);
		builder.setAuthor(event.getAuthor().getName() + '#' + event.getAuthor().getDiscriminator(), null, event.getMember().getUser().getAvatarUrl());
		builder.setTimestamp(Instant.now());
		
		event.getMessage().delete().queue();
		Message m = Respond.sync(event.getChannel(), builder);
		for(Emoji e : items.keySet())
			if(e.isEmote()) m.addReaction(e.getEmote()).queue();
			else m.addReaction(e.toString()).queue();
		
		Poll poll = new Poll(m, items).setAuthor(event.getAuthor());
		activePolls.add(poll);

		if(seconds <= 0) {
			m.addReaction(Emojis.XMARK.toString()).queue();
			poll.setManual();
		} else {
			timer.schedule(new Task(() -> {
				poll.end();
				activePolls.remove(poll);
			}), seconds * 1000);
		}
		
		return true;
	}

	
	
	@Override
	public void onEvent(GenericEvent event) {
		if(event instanceof MessageReactionAddEvent)
			addReaction((MessageReactionAddEvent)event);
		else if(event instanceof MessageReactionRemoveEvent)
			remReaction((MessageReactionRemoveEvent)event);
	}
	
	public void addReaction(MessageReactionAddEvent event) {
		if(event.getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong())
			return;
		
		Poll target = null;
		for(Poll poll : activePolls)
			if(poll.msg.getIdLong() == event.getMessageIdLong()) {
				target = poll;
				break;
			}

		if(target == null)
			return;
		
		Emoji emoji = new Emoji(event.getReactionEmote());
		
		if(emoji.equals(Emojis.XMARK)) {
			if(!target.isManual()) return;
			
			if(!event.getUser().equals(target.getAuthor()) && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				
				event.getUser().openPrivateChannel().queue(
					(pc) -> pc.sendMessage("Only the author of this poll, or a guild administrator, can manually close this poll.").queue()
				);
				
				return;
			}
			
			target.end();
			activePolls.remove(target);
			
		}else
			target.add(event.getUser(), new Emoji(event.getReactionEmote()));
	}
	
	public void remReaction(MessageReactionRemoveEvent event) {
		if(event.getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong())
			return;
		
		for(Poll poll : activePolls)
			if(poll.msg.getIdLong() == event.getMessageIdLong())
				poll.remove(event.getUser(), new Emoji(event.getReactionEmote()));
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String[] usages() {
		return new String[] {
			"``poll <time> <:emote1:> <`desc1`> <:emote2:> <`desc2`> ... (etc)``",
			" - `time` is in units of seconds.",
			" - Creates a poll mapping emotes to certain items to vote for.",
			" - There must be at least two items to vote for.",
			"``poll manuend <:emote1:> <`desc1`> <:emote2:> <`desc2`> ...``",
			" - Creates a poll that will not time out, but must be manually stopped."
		};
	}

	@Override
	protected void unload() {
		timer.cancel();
		for(Poll poll : activePolls)
			poll.msg.editMessage(new EmbedBuilder().setColor(Color.RED).setTitle("This poll has timed out.").build()).queue((success) -> poll.msg.clearReactions().queue());
		activePolls.clear();
		
		instance = null;
	}
	
	private static class Poll{
		private Message msg;
		private Map<Emoji, String> items;
		private Map<User, List<Emoji>> votes;
		
		private User author;
		private boolean isManual;
		
		public Poll(Message msg, Map<Emoji, String> items) {
			this.msg = msg;
			this.items = items;
			votes = new LinkedHashMap<>();
		}
		
		public Poll setAuthor(User author) {
			this.author = author;
			return this;
		}
		
		public User getAuthor() {
			return author;
		}
		
		public void setManual() { isManual = true; }
		public boolean isManual() { return isManual; }
		
		public void add(User u, Emoji e) {
			if(!items.containsKey(e))
				return;
			
			List<Emoji> list = votes.get(u);
			if(!votes.containsKey(u))
				votes.put(u, list = new LinkedList<>());
			
			list.add(e);
		}
		
		public void remove(User u, Emoji e) {
			if(!items.containsKey(e))
				return;
			
			if(!votes.containsKey(u))
				return;
			
			votes.get(u).remove(e);
		}
		
		public void end() {
			Map<Emoji, List<User>> results = new LinkedHashMap<>();
			for(Emoji e : items.keySet())
				results.put(e, new LinkedList<>());
			
			List<User> dq = new LinkedList<>();
			
			for(User u : votes.keySet()) {
				List<Emoji> userVotes = votes.get(u);
				if(userVotes.size() > 1)
					dq.add(u);
				else if(userVotes.size() == 1) 
					results.get(userVotes.get(0)).add(u);
			}
			
			Emoji[] rank = new Emoji[results.size()];
			
			int x=0;
			for(Emoji e : results.keySet())
				rank[x++] = e;
			
			Arrays.sort(rank, (a, b) -> {
				return results.get(b).size() - results.get(a).size();
			});
			
			EmbedBuilder builder = new EmbedBuilder().setColor(Color.GREEN).setTitle("The poll has ended. The results are as follows...");
			
			for(Emoji e : rank) {
				List<User> voters = results.get(e);
				int numVotes = voters.size();
				String title = '`' + items.get(e) + "` - " + numVotes + " vote" + (numVotes != 1 ? "s" : "");
				
				StringBuilder desc = new StringBuilder();
				for(User u : voters)
					desc.append(u.getAsMention()).append('\n');
				
				builder.addField(title, desc.toString(), true);
			}
			
			if(dq.size() > 0) {
				String title = dq.size() + " Disqualified User" + (dq.size() != 1 ? "s" : "") + ':';
				StringBuilder desc = new StringBuilder();
				for(User u : dq)
					desc.append(u.getAsMention()).append('\n');
				builder.addField(title, desc.toString(), false);
			}
			
			msg.editMessage(builder.build()).queue((success) -> msg.clearReactions().queue());
		}
	}

	@Override
	public String getDescription() {
		return "Allows people to call for polls for topics using emoji's.";
	}
}
package me.vem.dnd.cmd.vote;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.jetbrains.annotations.NotNull;

import com.vdurmont.emoji.EmojiManager;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Task;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class VoteCMD extends Command implements EventListener{

	private static VoteCMD instance;
	public static VoteCMD getInstance() {
		return instance;
	}
	
	public static void initialize() {
		if(instance == null)
			instance = new VoteCMD();
	}
	
	private Timer timer;
	private List<Vote> activeVotes;
	
	private VoteCMD() {
		super("vote");
		timer = new Timer();
		activeVotes = new LinkedList<>();
	}
	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		if(args.length < 6)
			return sendHelp(event.getChannel());
		
		if(!"create".equalsIgnoreCase(args[0]))
			return !sendHelp(event.getChannel());
		
		int seconds;
		try {
			seconds = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			Logger.err("Vote: could not parse number.");
			return !sendHelp(event.getChannel());
		}
		
		Map<Emoji, String> items = new LinkedHashMap<>();
		
		for(int i=2;i<args.length-1;i+=2) {
			Emoji e = null;
			
			Emote emote = Utilities.getEmoteFromMention(event.getGuild(), args[i]);
			if(emote != null)
				e = new Emoji(emote);
			else if(EmojiManager.isEmoji(args[i]))
				e = new Emoji(args[i]);
			
			if(e == null) {
				Logger.errf("Vote: could not read emote '%s'.", args[i]);
				return !sendHelp(event.getChannel());
			}
			
			if(items.containsKey(e)) {
				Respond.async(event.getChannel(), "Cannot create a vote with two of the same emotes.");
				return false;
			}
			
			items.put(e, args[i+1]);
		}
		
		EmbedBuilder builder = new EmbedBuilder().setColor(Color.GREEN);
		
		builder.setDescription("This vote will expire in " + seconds + " seconds.");
		
		for(Emoji emoji : items.keySet())
			builder.addField(items.get(emoji), emoji.toString(), true);
		
		builder.addBlankField(false);
		builder.addField("Please only vote once!", "Users who vote more than once will be disqualified.", false);
		builder.setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getUser().getAvatarUrl());
		
		event.getMessage().delete().queue();
		Message m = Respond.sync(event.getChannel(), builder);
		for(Emoji e : items.keySet())
			if(e.isEmote()) m.addReaction(e.getEmote()).queue();
			else m.addReaction(e.toString()).queue();
		
		Vote v = new Vote(m, items);
		activeVotes.add(v);
		
		timer.schedule(new Task(() -> {
			v.end();
			activeVotes.remove(v);
		}), seconds * 1000);
		
		return true;
	}
	
	@Override
	public void onEvent(Event event) {
		if(event instanceof MessageReactionAddEvent)
			addReaction((MessageReactionAddEvent)event);
		else if(event instanceof MessageReactionRemoveEvent)
			remReaction((MessageReactionRemoveEvent)event);
	}
	
	public void addReaction(MessageReactionAddEvent event) {
		if(event.getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong())
			return;
		
		for(Vote vote : activeVotes)
			if(vote.msg.getIdLong() == event.getMessageIdLong())
				vote.add(event.getUser(), new Emoji(event.getReactionEmote()));
	}
	
	public void remReaction(MessageReactionRemoveEvent event) {
		if(event.getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong())
			return;
		
		for(Vote vote : activeVotes)
			if(vote.msg.getIdLong() == event.getMessageIdLong())
				vote.remove(event.getUser(), new Emoji(event.getReactionEmote()));
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	protected String help() {
		return "Usage:\n```\n"
			 + "vote create <time> <:emote1:> <`desc1`> <:emote2:> <`desc2`> ... (etc)\n"
			 + "\t- 'time' is in units of seconds.\n"
			 + "\t- Creates a vote mapping emotes to certain items to vote for.\n"
			 + "\t- There must be at least two items to vote for."
			 + "```";
	}

	@Override
	protected void unload() {
		timer.cancel();
		for(Vote vote : activeVotes)
			vote.msg.delete().queue();
		activeVotes.clear();
		
		instance = null;
	}
	
	private static class Emoji{
		private Emote emote;
		private String utf;
		
		/**
		 * @param e The custom emote
		 * @throws IllegalArgumentException if passed a null emote.
		 */
		public Emoji(@NotNull Emote e) {
			if(e == null)
				throw new IllegalArgumentException("Passed Emote is null.");
			emote = e;
		}
		
		/**
		 * @param utf The UTF-32 representation of an emoji
		 * @throws IllegalArgumentException if the passed string does not represent a UTF-32 Emoji.
		 */
		public Emoji(String utf) {
			if(!EmojiManager.isEmoji(utf))
				throw new IllegalArgumentException("Passed string must be a UTF-32 Emoji");
			
			this.utf = utf;
		}
		
		public Emoji(ReactionEmote re) {
			if(re.isEmote())
				emote = re.getEmote();
			else utf = re.getName();
		}
		
		public boolean isEmote() {
			return emote != null;
		}
		
		public Emote getEmote() {
			return emote;
		}
		
		@Override
		public String toString() {
			if(emote != null)
				return emote.getAsMention();
			return utf;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((emote == null) ? 0 : emote.hashCode());
			result = prime * result + ((utf == null) ? 0 : utf.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Emoji other = (Emoji) obj;
			if (emote == null) {
				if (other.emote != null)
					return false;
			} else if (!emote.equals(other.emote))
				return false;
			if (utf == null) {
				if (other.utf != null)
					return false;
			} else if (!utf.equals(other.utf))
				return false;
			return true;
		}
	}
	
	private static class Vote{
		private Message msg;
		private Map<Emoji, String> items;
		private Map<User, List<Emoji>> votes;
		
		public Vote(Message msg, Map<Emoji, String> items) {
			this.msg = msg;
			this.items = items;
			votes = new LinkedHashMap<>();
		}
		
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
			Map<Emoji, Integer> results = new LinkedHashMap<>();
			for(Emoji e : items.keySet())
				results.put(e, 0);
			
			List<User> dq = new LinkedList<>();
			
			for(User u : votes.keySet()) {
				List<Emoji> userVotes = votes.get(u);
				if(userVotes.size() > 1)
					dq.add(u);
				else if(userVotes.size() == 1) {
					Emoji e = userVotes.get(0);
					results.put(e, results.get(e) + 1);
				}
			}
			
			List<Emoji> mostVoted = new LinkedList<>();
			int max = 0;
			for(Emoji e : results.keySet()) {
				int score = results.get(e);
				if(score > max) {
					max = score;
					mostVoted.clear();
					mostVoted.add(e);
				}else if(score == max) {
					mostVoted.add(e);
				}
			}
			
			Object[] winners = new Object[mostVoted.size()];
			int x = 0;
			for(Emoji e : mostVoted)
				winners[x++] = '`' + items.get(e) + '`';
			
			Object[] dqNames = new String[dq.size()];
			x = 0;
			for(User u : dq)
				dqNames[x++] = msg.getGuild().getMember(u).getAsMention();
			
			EmbedBuilder builder = new EmbedBuilder()
										.setColor(Color.GREEN)
										.setDescription(listForm(winners));
			
			if(winners.length > 1)
				builder.setTitle("And the winners of the vote are...");
			else builder.setTitle("And the winner of the vote is...");
			
			if(dqNames.length > 0)
				builder.addField("Disqualified Users:", listForm(dqNames), false);
			
			Respond.async(msg.getTextChannel(), builder);
			msg.delete().queue();
		}
		
		private String listForm(Object... objs) {
			if(objs.length == 0)
				return "";
			
			StringBuilder builder = new StringBuilder();
			
			if(objs.length == 1)
				builder.append(objs[0]);
			else if(objs.length == 2)
				builder.append(objs[0]).append(" and ").append(objs[1]);
			else {
				for(int i=0;i<objs.length;i++) {
					if(i != 0)
						builder.append(", ");
					
					if(i == objs.length-1)
						builder.append("and ");
					
					builder.append(objs[i]);
				}
			}
			
			return builder.toString();
		}
	}
}
package me.vem.dbgm.cmd;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Purge extends SecureCommand{

	private static Purge instance;
	public static Purge getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new Purge();
	}
	
	private Purge() { super("purge"); }
	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) { //Purge 100 anyone.
			purge(event.getChannel(), 101); //101 includes the purge message.
			return true;
		}
		
		int n = 100;
		//Try the last arg for a valid number	
		try { n = Integer.parseInt(args[args.length-1]); } catch(NumberFormatException e) {}
		
		Logger.debugf("Deleting the past %d messages...", n);
		
		//Try the last two args for a user mention.
		Member mem = Utilities.getMemberFromMention(event.getGuild(), args[args.length-1]);
		if(mem == null && args.length >= 2)
			mem = Utilities.getMemberFromMention(event.getGuild(), args[args.length-2]);
		
		if(!"regex".equalsIgnoreCase(args[0]))
			if(mem == null)
				purge(event.getChannel(), n);
			else purge(event.getChannel(), n, mem);
		else {
			if(args.length == 1 || args.length > 4) 
				return !getHelp(event.getChannel());
			if(mem == null)
				purge(event.getChannel(), n, args[1]);
			else purge(event.getChannel(), n, args[1], mem);
		}
		return true;
	}

	private void purge(TextChannel channel, int lastn, Member... members) {
		purge(channel, lastn, "", members);
	}
	
	private void purge(TextChannel channel, int lastn, @NotNull String regex, Member... members) {
		SelfPurgeList rem = new SelfPurgeList(channel);
		
		Pattern pattern = regex.isEmpty() ? null : Pattern.compile(regex);
		for(Message msg : channel.getIterableHistory().cache(false)) {
			if(--lastn < 0) break;
			
			//Skip if it does not pass the regex.
			boolean regexPass = pattern == null || pattern.matcher(msg.getContentDisplay()).matches();
			if(!regexPass) continue;
			
			if(members.length > 0) {
				for(Member mem : members)
					if(msg.getMember() == mem) {
						rem.add(msg);
						break;
					}
			}else rem.add(msg);
		}
		
		rem.clear();
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), "purge");
	}

	@Override
	public String help() {
		return "Usage:```\n"
			 + "purge [@user] [num=100]\n\t- Purges the last [num] messages of any specified person, or of everyone if no person is metnioned.\n\n"
			 + "purge regex `<regex>` [@user] [num=100]\n"
			 + "\t- Purges messages matching the given regex of a specified (or any) person. Searches the last [num] messages. Regex follows JAVA's format.\n"
			 + "\t- Example: purge regex `.\\d{1,4}\\S+ blah` 50 --> Would scan the last 50 messages for something that matched that regex crap.\n"
			 + "\t- For regex information, refer here: https://regexr.com/\n```";
	}
	@Override
	protected void unload() {
		instance = null;
	}
	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("purge");
	}
}

class SelfPurgeList implements Collection<Message>, Iterable<Message>{

	private Message[] list;
	private int len;
	
	private TextChannel linkedChannel;
	
	public SelfPurgeList(TextChannel tc){
		list = new Message[100];
		len = 0;
		linkedChannel = tc;
	}

	private static final int msptw = 1000 * 60 * 60 * 24 * 7 * 2; //msptw -> milliseconds per two weeks.
	@Override public boolean add(Message msg) {
		
		/********************************************************************************\
		|* AntiPurge -- Hardcoded.														*|
		|* These messages cannot be purged, no matter what. Note that only messages		*|
		|* sent by the bot with [AntiPurge] will resist the purge. Users' [AntiPurge]	*|
		|* messages will still be purged. The bot will only send an [AntiPurge]			*|
		|* message if the command is called by a allowable user. See the AntiPurge		*|
		|* command for more details. 													*|
		\********************************************************************************/
		if(msg.getContentDisplay().startsWith("[AntiPurge]") && msg.getAuthor().getIdLong() == msg.getJDA().getSelfUser().getIdLong())
			return false;
		
		//This chunk is essentially to prevent deleting messages older than two weeks.
		long dt = System.currentTimeMillis() - msg.getCreationTime().toEpochSecond() * 1000;
		if(dt - msptw >= -1000)
			return false;
		
		if(len >= 100)
			clear();
		list[len++] = msg;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends Message> c) {
		for(Message m : c)
			add(m);
		return true;
	}

	@Override
	public void clear() {
		if(len > 1)
			linkedChannel.deleteMessages(this).queue();
		if(len == 1)
			linkedChannel.deleteMessageById(list[0].getIdLong()).queue();
		else {
			len = 0;
			return;
		}
		
		for(int i=0;i<len;i++)
			list[i] = null;
		len = 0;
	}

	@Override
	public boolean contains(Object o) {
		if(!(o instanceof Message))
			return false;
		
		for(int i=0;i<len;i++)
			if(list[i] == o)
				return true;
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object m : c)
			if(!contains(m))
				return false;
		return true;
	}

	@Override
	public boolean isEmpty() { return len == 0; }

	@Override
	public Iterator<Message> iterator() {
		Iterator<Message> out = new Iterator<Message>() {
			private int next = 0;
			@Override
			public boolean hasNext() { return next < len; }
			@Override
			public Message next() { return list[next++]; }
		};
		return out;
	}

	/**
	 * Unwritten as this is a purge list. Anything added is meant to be destroyed.
	 */
	@Override
	public boolean remove(Object o) { return false; }

	/**
	 * Unwritten as this is a purge list. Anything added is meant to be destroyed.
	 */
	@Override
	public boolean removeAll(Collection<?> c) { return false; }

	/**
	 * Unwritten as this is a purge list. Anything added is meant to be destroyed.
	 */
	@Override
	public boolean retainAll(Collection<?> c) { return false; }

	@Override
	public int size() { return len; }

	@Override
	public Object[] toArray() { return list; } 

	/**
	 * Left empty due to this list not being a generic list.
	 */
	@Override
	public <T> T[] toArray(T[] a) { return null; }
}

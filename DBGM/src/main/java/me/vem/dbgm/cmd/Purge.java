package me.vem.dbgm.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import me.vem.dbgm.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Purge extends Command{

	private static Purge instance;
	public static Purge getInstance() { return instance; }
	public static void initialize() {
		if(instance != null) return;
		instance = new Purge();
	}
	
	private Purge() { super("purge"); }

	public static final String cmd_name = "purge";
	
	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) { //Purge 100 anyone.
			purge(event.getTextChannel(), 101, null); //101 includes the purge message.
			return true;
		}
		
		Message msg = event.getMessage();
		
		if(!args[0].equalsIgnoreCase("regex")) { //NORMAL PURGE BEGIN
			if(args.length == 1) {
				if(args[0].equals("help")) {
					super.getHelp(event);
					return true;
				}
				
				if(msg.getMentionedMembers().size() == 1) { //Just in case some sneaky sob mentiones two people and deletes that space...
					Member mem = msg.getMentionedMembers().get(0);
					purge(event.getTextChannel(), 100, mem);
				}else {
					try {
						int i = Integer.parseInt(args[0]);
						purge(event.getTextChannel(), i+1, null); //i+1 to include purge message.
					}catch(NumberFormatException e) {
						Respond.async(event, "Error parsing expected number. Run 'purge help' for help.");
					}
				}
			}
			
			if(args.length == 2) {
				if(msg.getMentionedMembers().size() != 1) { 
					Respond.async(event, "Please mention (@) exactly one person whose messages you plan to purge.");
					return false;
				}
				
				Member mem = msg.getMentionedMembers().get(0);
				
				try {
					int i = Integer.parseInt(args[1]);
					purge(event.getTextChannel(), i, mem);
				}catch(NumberFormatException e) {
					Respond.async(event, "Error parsing expected number. Run 'purge help' for help.");
				}
			}

			return true;
		}else { //NORMAL PURGE END; REGEX BEGIN
			if(args.length < 2 || args.length > 4) {
				super.getHelp(event);
				return false;
			}
			
			int n = 100;
			Member mem = null;
			
			if(msg.getMentionedMembers().size() == 1) {
				mem = msg.getMentionedMembers().get(0);
				
				if(args.length == 4) {
					try {
						n = Integer.parseInt(args[3]);
					}catch(NumberFormatException e) {
						Respond.asyncf(event, "Cannot parse expected integer...%nValidate: %s", debugParsedStringArr(args));
						return false;
					}
				}
				
			}else if(msg.getMentionedChannels().size() > 1) {
				Respond.async(event, "You can only delete messages from one person at a time.");
				return false;
			}else { //No mentions
				if(args.length >= 3) {
					try {
						n = Integer.parseInt(args[2]);
					}catch(NumberFormatException e) {
						Respond.asyncf(event, "Cannot parse expected integer...%nValidate: %s", debugParsedStringArr(args));
						return false;
					}
				}
			}
			
			//Logger.debugf("%d | %s | %s | %s", n, mem, args[1], Arrays.toString(args));
			this.purgeRegex(event.getTextChannel(), n, mem, args[1]);
		}//REGEX END
		return true;
	}
	
	public String debugParsedStringArr(String[] args) {
		StringBuilder nargs = new StringBuilder();
		for(String s : args)
			nargs.append("`" + s + "` ");
		return nargs.toString();
	}

	private void purge(TextChannel tc, int lastn, Member mem) {
		List<Message> check = fullHistory(tc, lastn);
		
		SelfPurgeList rem = new SelfPurgeList(tc);
		
		for(Message mes : check)
			if(mes == null) break;
			else if(mem == null || mes.getMember() == mem)
				rem.add(mes);
		
		rem.clear();
	}
	
	private void purgeRegex(TextChannel tc, int lastn, Member mem, String reg) {
		List<Message> check = fullHistory(tc, lastn);
		
		SelfPurgeList rem = new SelfPurgeList(tc);
		
		for(Message mes : check)
			if(mes == null) break;
			else if(mem == null || mes.getMember() == mem)
				if(mes.getContentDisplay().matches(reg))
					rem.add(mes);
		
		rem.clear();
	}
	
	private List<Message> fullHistory(TextChannel tc, int n){
		if(n == 0) return new ArrayList<>();
		
		Message[] out = new Message[n];
		int i = 0;
		Message next = tc.getMessageById(tc.getLatestMessageIdLong()).complete();
		out[i++] = next;
		
		while(i < n) {
			MessageHistory mh = tc.getHistoryBefore(next, n-i > 100 ? 100 : n-i).complete();
			if(mh.size() == 0) break;
			
			Iterator<Message> iter = mh.getRetrievedHistory().iterator();
			
			while(iter.hasNext()) {
				Message m = iter.next();
				out[i++] = m;
				if(!iter.hasNext())
					next = m;
			}
		}
		
		return Arrays.asList(out);
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help() {
		
		StringBuffer help = new StringBuffer();
		help.append("Valid usages: ```\n");
		
		help.append("purge [@user] [num (def 100)]\n\t- Purges the last [num] messages of any specified person, or of everyone if no person is metnioned.\n\n");
		help.append("purge regex \"<regex>\" [@user] [num (def 100)]\n"
				+ "\t- Purges messages matching the given regex of a specified (or any) person. Searches the last [num] messages. Regex follows JAVA's format.\n"
				+ "\t- Example: purge regex \".\\d{1,4}\\S+ blah\" 50 --> Would scan the last 50 messages for something that matched that regex crap.\n"
				+ "\t- For regex information, refer here: https://regexr.com/");
		
		help.append("```");
		return help.toString();
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
	
	@Override
	public boolean add(Message e) {
		
		/*
		 * AntiPurge -- Hardcoded.
		 * These messages cannot be purged, no matter what.
		 * Note that only messages sent by the bot with [AntiPurge] will resist the purge. Users' [AntiPurge] messages will still be purged.
		 * The bot will only send an [AntiPurge] message if the command is called by a allowable user.
		 * See the AntiPurge command for more details.
		 */
		if(e.getContentDisplay().startsWith("[AntiPurge]") && e.getAuthor().getIdLong() == e.getJDA().getSelfUser().getIdLong())
			return false;
		
		if(len >= 100) 
			clear();
		list[len++] = e;
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
			linkedChannel.deleteMessages(this).complete();
		if(len == 1)
			linkedChannel.deleteMessageById(list[0].getIdLong()).complete();
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

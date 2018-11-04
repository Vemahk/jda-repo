package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class ClearOOC extends Command implements Configurable{
	
	private static ClearOOC instance;
	public static ClearOOC getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new ClearOOC();
	}
	
	private static Map<Long, Set<Long>> allowedRooms;
	
	private ClearOOC() {
		super("clearooc");
		load();
	}
	
	private static final Pattern OOCRegex = Pattern.compile("^\\s*\\(.*\\)\\s*$");
	@Override public boolean run(GuildMessageReceivedEvent event, String... args) {
		Set<Long> guildSet = allowedRooms.get(event.getGuild().getIdLong());
		if(guildSet == null) allowedRooms.put(event.getGuild().getIdLong(), guildSet = new HashSet<>());
		
		TextChannel channel = event.getChannel();
		Message userMsg = event.getMessage();
		
		int check = 50;
		if(args.length > 0) {
			if(args[0].equals("allow")) {
				if(!guildSet.add(channel.getIdLong())) {
					Respond.timeout(channel, userMsg, 5000, "Channel was already allowed to begin with.");
					return false;
				}
				Respond.timeout(channel, userMsg, 5000, "Channel allowed");
				return true;
			}else if(args[0].equals("disallow")) {
				if(!guildSet.remove(channel.getIdLong())) {
					Respond.timeout(channel, userMsg, 5000, "Channel was not allowed to begin with.");
					return false;
				}
				Respond.timeout(channel, userMsg, 5000, "Channel disallowed.");
				return true;
			}else{
				try {
					check = Integer.parseInt(args[0]);
				}catch(Exception e) {} //Parse failed: do nothing.
			}
		}
		
		if(!guildSet.contains(channel.getIdLong())) {
			Respond.timeout(channel, userMsg, 5000, "ClearOOC is not allowed in this chatroom. Ask an admin for details.");
			return false;
		}
		
		Respond.timeoutf(channel, userMsg, 5000, "Checking past %d messages for OOC...", check);
		
		SelfPurgeList list = new SelfPurgeList(event.getChannel());
		
		int i=0;
		for(Message m : channel.getIterableHistory().cache(false)) {
			if(check > 0 && i++ >= check) break;
			if(OOCRegex.matcher(m.getContentRaw()).matches())
				list.add(m);
		}
		
		list.clear();
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		Member mem = event.getMember();
		
		if(args.length > 0 && ("allow".equals(args[0]) || "disallow".equals(args[0])))
			return mem.hasPermission(Permission.ADMINISTRATOR);
		
		return mem.hasPermission(Permission.MESSAGE_MANAGE);
	}
	
	public boolean roomEnabled(Guild g, TextChannel tc) {
		Set<Long> set = allowedRooms.get(g.getIdLong());
		if(set == null) return false;
		
		return set.contains(tc.getIdLong());
	}
	
	@Override public void save() {
		try {
			PrintWriter out = ExtFileManager.getConfigOutput("clearooc.json");
			out.print(ExtFileManager.getGsonPretty().toJson(allowedRooms));
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Logger.infof("ClearOOC Database Saved...");
	}
	
	@Override public void load() {
		allowedRooms = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("clearooc.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		allowedRooms = gson.fromJson(content, new TypeToken<HashMap<Long, HashSet<Long>>>(){}.getType());
	}
	
	@Override protected String help() {
		return "Usage:\n```\n"
			 + "clearooc [amount=50] -- clears the last 'amount' entries that match OOC.\n"
			 + "\t(if 'amount' is 0, then it will check all the messages in the channel)\n"
			 + "```";
	}
	@Override
	protected void unload() {
		save();
		instance = null;
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
	@Override public boolean add(Message e) {
		
		long dt = System.currentTimeMillis() - e.getCreationTime().toEpochSecond() * 1000;
		if(dt - msptw >= -1000) //If it's two weeks or older (with a grace of a second, for paranoia reasons).
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
		return new Iterator<Message>() {
			private int next = 0;
			@Override public boolean hasNext() { return next < len; }
			@Override public Message next() { return list[next++]; }
		};
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

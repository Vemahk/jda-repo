package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.DiscordBot;
import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.struct.MessagePurge;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
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
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(() -> {
			Logger.debug("Auto-clearooc initiated.");
			for(long guildId : allowedRooms.keySet())
				for(long channelId : allowedRooms.get(guildId)) 
					clear(DiscordBot.getInstance().getJDA().getTextChannelById(channelId), 0);
		}, 10, 10, TimeUnit.MINUTES);
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
					Respond.timeout(channel, userMsg, 5000, "This channel is already enabled for ClearOOC.");
					return false;
				}
				Respond.timeout(channel, userMsg, 5000, "This channel is now enabled for ClearOOC");
				return true;
			}else if(args[0].equals("disallow")) {
				if(!guildSet.remove(channel.getIdLong())) {
					Respond.timeout(channel, userMsg, 5000, "This channel was not already enabled for ClearOOC");
					return false;
				}
				Respond.timeout(channel, userMsg, 5000, "This channel is now disabled for ClearOOC");
				return true;
			}else{
				try {
					check = Integer.parseInt(args[0]);
				}catch(Exception e) {check = 50;}
			}
		}
		
		if(!guildSet.contains(channel.getIdLong())) {
			Respond.timeout(channel, userMsg, 5000, "ClearOOC is not allowed in this chatroom.");
			return false;
		}
		
		userMsg.delete().queue();
		clear(event.getChannel(), check);
		
		return true;
	}
	
	private void clear(TextChannel channel, int num) {
		MessagePurge.purge(channel, num, (msg) -> {
			return msg.getAuthor().isBot() || OOCRegex.matcher(msg.getContentRaw()).matches();
		});
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
	
	@Override
	public String[] usages() {
		return new String[] {
			"`clearocc [num=50]` -- clears the last `num` entries that match OOC format.",
			" - If `num` is 0, then it will attempt to check all messages in the channel."
		};
	}
	
	@Override
	protected void unload() {
		save();
		instance = null;
	}
	
	@Override
	public String getDescription() {
		return "Clears messages surrounded by parentheses (i.e. out of character char).";
	}
}
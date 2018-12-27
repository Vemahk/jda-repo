package me.vem.jdab.cmd;

import java.awt.Color;
import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.DiscordBot;
import me.vem.jdab.struct.MessagePurge;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class Monitor extends Command implements EventListener, Configurable{

	private static Monitor instance;
	public static Monitor getInstance() {
		if(instance == null)
			instance = new Monitor();
		return instance;
	}

	private Map<Long, MonitorInfo> database;
	private MonitorInfo getInfo(Guild guild) {
		MonitorInfo info = database.get(guild.getIdLong());
		if(info == null)
			database.put(guild.getIdLong(), info = new MonitorInfo().setGuild(guild));
		return info;
	}
	
	private Monitor() {
		super("monitor");
		msgLookup = new TreeMap<>();
		load();
	}

	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		if(args.length == 0 || !"activate".equals(args[0]))
			return sendHelp(event.getChannel(), false);
		
		MonitorInfo info = getInfo(event.getGuild());

		if(info.channel != null) {
			Respond.async(event.getChannel(), "This bot is already monitoring in another channel.");
			return false;
		}
		
		info.setup(event.getChannel());
		
		return true;
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String[] usages() {
		return new String[] {
			"`monitor activate` -- Activates the bot monitoring in the current channel."
		};
	}

	@Override
	public String getDescription() {
		return "Allows for the bot to log new users, deleted messages, and edited messages.";
	}

	@Override
	protected void unload() {
		save();
		instance = null;
	}

	@Override
	public void save() {
		if(Utilities.saveToJSONFile("monitor.json", database))
			Logger.info("Monitor database saved...");
		else Logger.err("Failed to save Monitor database");
	}

	@Override
	public void load() {
		database = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("monitor.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		database = gson.fromJson(content, new TypeToken<HashMap<Long, MonitorInfo>>(){}.getType());
		
		//Hmm
		
		for(MonitorInfo info : database.values()) {
			info.setup(DiscordBot.getInstance().getJDA().getTextChannelById(info.channelId));
		}
	}

	@Override public void onEvent(Event event) {
		if(event instanceof GuildMessageReceivedEvent)
			messageReceived((GuildMessageReceivedEvent)event);
		else if(event instanceof GuildMessageDeleteEvent)
			messageDeleted((GuildMessageDeleteEvent) event);
		else if(event instanceof GuildMessageUpdateEvent)
			messageUpdated((GuildMessageUpdateEvent) event);
		else if(event instanceof GuildMemberJoinEvent)
			userJoined((GuildMemberJoinEvent) event);
		else if(event instanceof GuildMemberLeaveEvent)
			userLeft((GuildMemberLeaveEvent) event);
	}
	
	private TreeMap<Long, MessageInfo> msgLookup;
	
	private void messageReceived(GuildMessageReceivedEvent event) {
		if(event.getAuthor().equals(event.getJDA().getSelfUser()))
			return;
		
		MonitorInfo info = getInfo(event.getGuild());
		
		if(info.channel == null)
			return;
		
		if(event.getChannel().equals(info.channel))
			event.getMessage().delete().queue();
		else {
			msgLookup.put(event.getMessageIdLong(), new MessageInfo(event.getAuthor(), event.getMessage().getContentRaw()));
		}
	}
	
	private void messageDeleted(GuildMessageDeleteEvent event) {
		MonitorInfo info = getInfo(event.getGuild());
		if(info.channel == null || info.channel.equals(event.getChannel()))
			return;
		
		EmbedBuilder embed = new EmbedBuilder().setColor(Color.ORANGE);
		
		if(msgLookup.containsKey(event.getMessageIdLong())) {
			MessageInfo msgInfo = msgLookup.get(event.getMessageIdLong());
			embed.setDescription("**Message sent by** " + event.getJDA().getUserById(msgInfo.getAuthorId()).getAsMention() + " **deleted in** " + event.getChannel().getAsMention() + '\n' + msgInfo.getContent());
		}else {
			embed.setDescription("**Message deleted in** " + event.getChannel().getAsMention());
		}

		embed.setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl());
		embed.setFooter("Message ID: " + event.getMessageId(), null);
		embed.setTimestamp(Instant.now());
		
		Respond.async(info.channel, embed);
	}
	
	private void messageUpdated(GuildMessageUpdateEvent event) {
		MonitorInfo info = getInfo(event.getGuild());
		if(info.channel == null)
			return;
		
		EmbedBuilder embed = new EmbedBuilder().setColor(Color.BLUE).setDescription("**Message edited in** " + event.getChannel().getAsMention());
		
		if(msgLookup.containsKey(event.getMessageIdLong())) {
			MessageInfo msgInfo = msgLookup.get(event.getMessageIdLong());
			embed.addField("Before", msgInfo.getContent(), false);
			msgInfo.setContent(event.getMessage().getContentRaw());
		}else {
			msgLookup.put(event.getMessageIdLong(), new MessageInfo(event.getAuthor(), event.getMessage().getContentRaw()));
		}
		
		embed.addField("After", event.getMessage().getContentRaw(), false);
		
		embed.setAuthor(event.getAuthor().getName() + '#' + event.getAuthor().getDiscriminator(), null, event.getAuthor().getAvatarUrl());
		embed.setFooter("User ID: " + event.getAuthor().getId(), null);
		embed.setTimestamp(Instant.now());
		
		Respond.async(info.channel, embed);
	}
	
	private void userJoined(GuildMemberJoinEvent event) {
		MonitorInfo info = getInfo(event.getGuild());
		if(info == null)
			return;
		
		EmbedBuilder embed = new EmbedBuilder().setColor(Color.GREEN);
		embed.setAuthor("Member Joined", null, event.getUser().getAvatarUrl());
		embed.setDescription('\\' + event.getMember().getAsMention() + " " + event.getUser().getName() + '#' + event.getUser().getDiscriminator());
		embed.setFooter("User ID: " + event.getUser().getId(), null);
		embed.setTimestamp(Instant.now());
		
		Respond.async(info.channel, embed);
	}
	
	private void userLeft(GuildMemberLeaveEvent event) {
		MonitorInfo info = getInfo(event.getGuild());
		if(info == null)
			return;
		
		EmbedBuilder embed = new EmbedBuilder().setColor(Color.RED);
		embed.setAuthor("Member Joined", null, event.getUser().getAvatarUrl());
		embed.setDescription('\\' + event.getUser().getAsMention() + " " + event.getUser().getName() + '#' + event.getUser().getDiscriminator());
		embed.setFooter("User ID: " + event.getUser().getId(), null);
		embed.setTimestamp(Instant.now());
		
		Respond.async(info.channel, embed);
	}
	
	private class MonitorInfo{
		
		private transient Guild guild;
		private transient TextChannel channel;
		
		private long channelId;
		
		public MonitorInfo setGuild(Guild guild) {
			if(this.guild == null)
				this.guild = guild;
			return this;
		}
		
		public void setup(TextChannel channel) {
			MessagePurge.purge(channel, (msg) -> {
				return !msg.getAuthor().isBot();
			});
			
			this.channel = channel;
			this.channelId = channel.getIdLong();
			
			setGuild(channel.getGuild());
		}
	}
	
	private class MessageInfo{
		private final long authorId;
		private String content;
		
		public MessageInfo(User author, String content) {
			authorId = author.getIdLong();
			this.content = content;
		}
		
		public long getAuthorId() { return authorId; }
		public String getContent() { return content; }
		
		public void setContent(String newContent) { content = newContent; }
	}
}
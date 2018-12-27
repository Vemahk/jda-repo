package me.vem.jdab.cmd;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.DiscordBot;
import me.vem.jdab.struct.MessagePurge;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
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
			database.put(guild.getIdLong(), info = new MonitorInfo(guild));
		return info;
	}
	
	private Monitor() {
		super("monitor");
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
	}
	
	private void messageReceived(GuildMessageReceivedEvent event) {
		if(event.getAuthor().equals(event.getJDA().getSelfUser()))
			return;
		
		MonitorInfo info = getInfo(event.getGuild());
		
		if(info.channel != null && event.getChannel().equals(info.channel))
			event.getMessage().delete().queue();
	}
	
	private class MonitorInfo{
		
		private transient Guild guild;
		private transient TextChannel channel;
		
		private long channelId;
		
		public MonitorInfo() { }
		
		public MonitorInfo(Guild guild) {
			setGuild(guild);
		}
		
		public void setGuild(Guild guild) {
			if(this.guild == null)
				this.guild = guild;
		}
		
		public void setup(TextChannel channel) {
			this.channel = channel;
			this.channelId = channel.getIdLong();
			
			setGuild(channel.getGuild());
			
			MessagePurge.purge(channel, (msg) -> {
				return !msg.getAuthor().isBot();
			});
		}
	}
}
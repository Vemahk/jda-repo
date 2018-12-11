package me.vem.dbgm.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.DiscordBot;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Game.GameType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.user.update.UserUpdateGameEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class StreamTrack extends SecureCommand implements EventListener, Configurable{

	private static StreamTrack instance;
	public static StreamTrack getInstance() {
		return instance;
	}
	
	public static void initialize() {
		if(instance == null)
			instance = new StreamTrack();
	}

	private Map<Long, Data> database;

	private StreamTrack() {
		super("stream");
		load();
	}

	@Override public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0)
			return sendHelp(event.getChannel(), true);
		
		if("add".equals(args[0])) {
			if(args.length == 1)
				return sendHelp(event.getChannel(), false);
			
			Member mentioned = Utilities.getMemberFromMention(event.getGuild(), args[1]);
			if(mentioned == null)
				return sendHelp(event.getChannel(), false);
			
			if(getData(event.getGuild()).track(mentioned.getUser()))
				Respond.asyncf(event.getChannel(), "%s is now being tracked for twitch streaming.", mentioned.getEffectiveName());
			else Respond.asyncf(event.getChannel(), "%s was already being tracked for twitch streaming.", mentioned.getEffectiveName());
		}else if("remove".equals(args[0])) {
			if(args.length == 1)
				return sendHelp(event.getChannel(), false);
			
			Member mentioned = Utilities.getMemberFromMention(event.getGuild(), args[1]);
			if(mentioned == null)
				return sendHelp(event.getChannel(), false);
			
			if(getData(event.getGuild()).untrack(mentioned.getUser()))
				Respond.asyncf(event.getChannel(), "%s removed", mentioned.getEffectiveName());
			else Respond.asyncf(event.getChannel(), "%s was not being tracked.", mentioned.getEffectiveName());
		}else if("channel".equals(args[0])) {
			if(args.length == 1) {
				getData(event.getGuild()).setResponseChannel(event.getChannel());
				Respond.async(event.getChannel(), "Streamers will now be mentioned in this channel!");
			}else {
				TextChannel target = Utilities.getTextChannelFromMention(event.getGuild(), args[1]);
				if(target == null)
					return sendHelp(event.getChannel(), false);
				
				getData(event.getGuild()).setResponseChannel(target);
				Respond.asyncf(event.getChannel(), "Streamers will now be mentioned in the %s channel!", args[1]);
			}
		}else if("response".equals(args[0])) {
			if(args.length == 1) {
				Respond.asyncf(event.getChannel(), "The current response is set to: `%s`", getData(event.getGuild()).getResponse());
			}else {
				getData(event.getGuild()).setMessage(args[1]);
				Respond.asyncf(event.getChannel(), "Set the bot's response to: `%s`", args[1]);
			}
		}else return sendHelp(event.getChannel(), true);
		
		return true;
	}
	
	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("stream.setchannel", "stream.adduser", "stream.setresponse", "stream.removeuser");
	}

	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		if(args.length == 0) return true;
		
		String key = null;
		
		if("add".equals(args[0]))
			key = "stream.adduser";
		else if("remove".equals(args[0])) 
			key = "stream.removeuser";
		else if("channel".equals(args[0]))
			key =  "stream.setchannel";
		else if("response".equals(args[0]))
			key = "stream.setresponse";
		else return true;
		
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), key);
	}

	@Override
	public String[] usages() {
		return new String[] {
			"`stream add <@user>` -- Adds a mentioned user to the list of streamers.",
			"`stream remove <@user>` -- Removes a mentioned user from the list of streamers.",
			"`stream channel [#channel]` -- Sets this channel (or a mentioned one) to be the channel the bot mention the streamer in.",
			"`stream response \\`<response message>\\`` -- Sets the text the bot will send when a streamer starts to stream.",
			" - `%user%` will be replaced with the streamer's mention.",
			" - `%url%` will be replaced with the twitch url of the streamer."
		};
	}

	@Override protected void unload() {
		save();
		DiscordBot.getInstance().getJDA().removeEventListener(this);
		instance = null;
	}

	@Override
	public String getDescription() {
		return "Monitors specific users for if their Discord Presense switches to \"Streaming...\", under which case it will send a message to a particular channel.";
	}
	
	@Override
	public void onEvent(Event event) {
		if(event instanceof UserUpdateGameEvent)
			onGameUpdate((UserUpdateGameEvent) event);
	}

	public void onGameUpdate(UserUpdateGameEvent event) {
		Guild g = event.getGuild();
		if(g == null) return;
		
		Member m = event.getMember();
		if(m == null) return;
		
		Data data = getData(g);
		if(!data.isTrackedUser(m.getUser()))
			return;
		
		Game nGame = event.getNewGame();
		if(nGame == null) return;
		
		if(nGame.getType() == GameType.STREAMING) {
			TextChannel rspChannel = g.getTextChannelById(data.getResponseChannelID());
			String response = data.getResponse()
								.replaceAll("%user%", m.getAsMention())
								.replaceAll("%url%", nGame.getUrl());
			Respond.async(rspChannel, response);
		}
	}
	
	public Data getData(Guild guild) {
		Data data = database.get(guild.getIdLong());
		if(data == null)
			database.put(guild.getIdLong(), data = new Data(guild));
		return data;
	}
	
	@Override public void save() {
		try {
			PrintWriter out = ExtFileManager.getConfigOutput("streamers.json");
			
			Gson gson = ExtFileManager.getGsonPretty();

			out.print(gson.toJson(database));
			
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Logger.infof("Streamers database saved...");
	}
	
	@Override public void load() {
		DiscordBot.getInstance().addEventListener(this);
		
		database = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("streamers.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		database = gson.fromJson(content, new TypeToken<HashMap<Long, Data>>(){}.getType());
	}
	
	public static class Data {
		
		private long responseChannel;
		private String message;
		private Set<Long> uids;
		
		private Data() {
			uids = new LinkedHashSet<>();
			message = "%user% is now streaming! Check it out at %url%.";
		}
		
		public Data(TextChannel c) {
			this();
			responseChannel = c.getIdLong();
		}
		
		public Data(Guild g) {
			this();
			responseChannel = g.getDefaultChannel().getIdLong();
		}
		
		public long getResponseChannelID() {
			return responseChannel;
		}
		
		public Data setResponseChannel(TextChannel channel) {
			responseChannel = channel.getIdLong();
			return this;
		}
		
		public boolean isTrackedUser(User u) {
			return uids.contains(u.getIdLong());
		}
		
		public boolean isTrackedMember(Member m) {
			return isTrackedUser(m.getUser());
		}
		
		public boolean track(User u) {
			return uids.add(u.getIdLong());
		}
		
		public boolean untrack(User u) {
			return uids.remove(u.getIdLong());
		}
		
		/**
		 * %user% will be replaced with the streamer's mention tag.<br>
		 * %url% will be replaced with the twitch url.
		 * @param msg
		 * @return this object
		 */
		public Data setMessage(String msg) {
			this.message = msg;
			return this;
		}
		
		public String getResponse() { return message; }
	}
}
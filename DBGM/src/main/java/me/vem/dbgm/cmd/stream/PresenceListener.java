package me.vem.dbgm.cmd.stream;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Game.GameType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.user.update.UserUpdateGameEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class PresenceListener implements EventListener, Configurable{

	private static PresenceListener instance;
	public static PresenceListener getInstance() {
		if(instance == null)
			instance = new PresenceListener();
		return instance;
	}
	
	private Map<Long, Data> database;
	
	private PresenceListener() { load(); }
	
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
		StreamTrack.initialize();
		
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


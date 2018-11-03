package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Meme extends Command implements Configurable{

	private static Meme instance;
	public static Meme getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new Meme();
	}
	
	private Message lastList;
	private Map<String, String> memes;
	
	private Meme() {
		super("meme");
		load();
	}
	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		TextChannel channel = event.getChannel();
		Message userMsg = event.getMessage();
		
		if(args.length == 0) {
			Respond.timeout(channel, userMsg, 5000, help());
			return false;
		}
		
		String meme = args[0];
		if(meme.equals("list")) {
			
			int page = 1;
			
			if(args.length >= 2)
				try { page = Integer.parseInt(args[1]); }catch(NumberFormatException e) {}
			
			if(lastList == null) respondPage(event, page);
			else {
				long diff = System.currentTimeMillis() / 1000 - lastList.getCreationTime().toEpochSecond();
				if(diff <= 60) //It's been less than 60 seconds since the last list was posted.
					lastList.editMessage(getPage(page)).queue();
				else respondPage(event, page);
			}
			
			event.getMessage().delete().queue();
			
		}else if(meme.equals("add")) {
			if(args.length<3) {
				Respond.timeoutf(channel, userMsg, 5000, "Invalid usage.%n%s", help());
				return false;
			}
			
			memes.put(args[1], args[2]);
			Respond.timeout(channel, userMsg, 5000, "Meme added. OuO");
		}else if(memes.containsKey(meme)){
			event.getMessage().delete().complete();
			String out = memes.get(meme);
			if(ClearOOC.getInstance().roomEnabled(event.getGuild(), event.getChannel())) out = "("+out+")";
			Respond.async(channel, out);
		}else Respond.timeout(channel, userMsg, 5000, "Unknown Meme. Ask an admin to add it.");
		
		return true;
	}
	
	private void respondPage(GuildMessageReceivedEvent event, int page) {
		lastList = Respond.sync(event.getChannel(), getPage(page));
	}

	private String getPage(int page) {
		if(memes.size() < (page-1) * 10) return "The Meme list does not have " + page + " pages";
		
		StringBuilder rsp = new StringBuilder("[Meme List Page ").append(page).append("]```");
		
		int i=0;
		for(String s : memes.keySet()) {
			if(i++ < (page-1) * 10) continue;
			if(i >= page * 10) break;
			rsp.append('\n').append(s);
		}
		
		return rsp.append("```").toString();
	}
	
	@Override public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		if(args.length > 0 && "add".equals(args[0]))
			return event.getMember().hasPermission(Permission.ADMINISTRATOR);
		return true;
	}
	
	@Override public void save() {
		try {
			PrintWriter out = ExtFileManager.getConfigOutput("memes.json");
			out.print(ExtFileManager.getGsonPretty().toJson(memes));
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Logger.infof("Meme database saved...");
	}
	
	@Override public void load() {
		memes = new LinkedHashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("memes.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		memes = gson.fromJson(content, new TypeToken<LinkedHashMap<String, String>>(){}.getType());
	}

	@Override protected String help() {
		return "Usage: meme <memename> or meme list [pagenum]";
	}
	@Override
	protected void unload() {
		save();
		instance = null;
	}
}

package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.dnd.utils.ExtFileManager;
import me.vem.dnd.utils.Logger;
import me.vem.dnd.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Meme extends Command implements Configurable{

	private static Meme instance;
	public static Meme getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new Meme();
	}
	
	private Message lastList;
	private int lastListPage;
	private Map<String, String> memes;
	
	private Meme() {
		super("meme");
		load();
	}
	
	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) {
			Respond.timeout(event, 5000, help());
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
				Respond.timeoutf(event, 5000, "Invalid usage.%n%s", help());
				return false;
			}
			
			memes.put(args[1], args[2]);
			Respond.timeout(event, 5000, "Meme added. OuO");
		}else if(memes.containsKey(meme)){
			event.getMessage().delete().complete();
			String out = memes.get(meme);
			if(ClearOOC.getInstance().roomEnabled(event.getGuild(), event.getTextChannel())) out = "("+out+")";
			Respond.async(event, out);
		}else Respond.timeout(event, 5000, "Unknown Meme. Ask an admin to add it.");
		
		return true;
	}
	
	private void respondPage(MessageReceivedEvent event, int page) {
		lastList = Respond.sync(event, getPage(page));
		lastListPage = page;
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
	
	@Override public boolean hasPermissions(MessageReceivedEvent event, String... args) {
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

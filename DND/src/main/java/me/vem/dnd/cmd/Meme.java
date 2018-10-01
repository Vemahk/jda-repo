package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.dnd.utils.ExtFileManager;
import me.vem.dnd.utils.Logger;
import me.vem.dnd.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Meme extends Command implements Configurable{

	private static Meme instance;
	public static Meme getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new Meme();
	}
	
	private Map<String, String> memes;
	
	private Meme() {
		super("meme");
		load();
	}
	
	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(args.length == 0) {
			Respond.timeout(event, 5000, help());
			return false;
		}
		
		String meme = args[0];
		if(meme.equals("list")) {
			String rsp = "Memes:\n";
			for(String s : memes.keySet()) rsp+=s+"\n";
			
			int timeout = 10;
			if(args.length > 1) {
				try {
					timeout = Integer.parseInt(args[1]);
				}catch(Exception e) {
					timeout = 10;
				}
			}
			if(timeout > 0)
				Respond.timeout(event, timeout * 1000, rsp);
			else Respond.async(event, rsp);
		}else if(meme.equals("add")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Respond.timeout(event, 5000, "Aye, you can't do this. Sorry bud.");
				return false;
			}
			
			if(args.length<3) {
				Respond.timeout(event, 5000, "Aye, u dun goofed.");
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
	
	@Override public boolean hasPermissions(MessageReceivedEvent event, String... args) { return true; }
	
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
		memes = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("memes.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		memes = gson.fromJson(content, new TypeToken<HashMap<String, String>>(){}.getType());
	}

	@Override protected String help() {
		return "Usage: meme <memename> or ~meme list";
	}
	@Override
	protected void unload() {
		save();
		instance = null;
	}
}

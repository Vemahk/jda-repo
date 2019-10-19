package me.vem.dbgm.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class Meme extends SecureCommand implements Configurable{

	private static Meme instance;
	public static Meme getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new Meme();
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
		
		if(args.length == 0)
			return sendHelp(event.getChannel(), true);
		
		String meme = args[0];
		if(meme.equals("list")) {
			
			int page = 1;
			
			if(args.length >= 2)
				try { page = Integer.parseInt(args[1]); }catch(NumberFormatException e) {}
			
			final int fPage = page;
			if(lastList == null) respondPage(event, page);
			else {
				long diff = System.currentTimeMillis() / 1000 - lastList.getTimeCreated().toEpochSecond();
				if(diff <= 60) //It's been less than 60 seconds since the last list was posted.
					lastList.editMessage(getPage(page)).queue((msg) -> {},
						(error) -> respondPage(event, fPage));
				else respondPage(event, page);
			}
			
			event.getMessage().delete().queue();
			
		}else if(meme.equals("add")) {
			if(args.length<3)
				return sendHelp(event.getChannel(), false);
			
			memes.put(args[1], args[2]);
			Respond.asyncf(event.getChannel(), "Meme `%s` added", args[1]);
		}else if(memes.containsKey(meme)){
			event.getMessage().delete().complete();
			String out = memes.get(meme);
			Respond.async(event.getChannel(), out);
		}else Respond.async(event.getChannel(), "Unknown Meme. Ask an admin to add it.");
		
		return true;
	}
	
	private void respondPage(GuildMessageReceivedEvent event, int page) {
		if(lastList != null)
			lastList.delete().queue((msg) -> {}, (err) -> {});
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
			return Permissions.getInstance().hasPermissionsFor(event.getMember(), "meme.add");
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), "meme");
	}
	
	@Override public List<String> getValidKeySet() {
		return Arrays.asList("meme", "meme.add");
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
	
	@Override public String[] usages() {
		return new String[] {
			"`meme <memename>` -- Responds with the saved meme.",
			"`meme list [pagenum=1]` -- Lists a given page of memes."
		};
	}
	
	@Override
	protected void unload() {
		save();
		instance = null;
	}
	
	@Override
	public String getDescription() {
		return "Stores a list of links to good memes.";
	}
}

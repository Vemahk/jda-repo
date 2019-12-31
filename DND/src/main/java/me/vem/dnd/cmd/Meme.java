package me.vem.dnd.cmd;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.struct.menu.EmbedMenu;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class Meme extends Command implements Configurable{

	private static Meme instance;
	public static Meme getInstance() {
		return instance;
	}

	public static void initialize() {
		if (instance == null)
			instance = new Meme();
	}
	
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
		
		if(args.length == 0)
			return sendHelp(channel, true);
		
		if("list".equals(args[0])) {
			
			int page = 1;
			
			if(args.length >= 2)
				try { page = Integer.parseInt(args[1]); }catch(NumberFormatException e) {}
			
			new MemeMenu(Respond.sync(event.getChannel(), getPage(page)), page).setTimeout(120);
			event.getMessage().delete().queue();
			
		}else if("add".equals(args[0])) {
			if(args.length<3)
				return sendHelp(channel, false);
			
			if(memes.put(args[1], args[2]) == null)
				Respond.timeoutf(channel, userMsg, 5000, "Meme `%s` added", args[1]);
			else Respond.timeoutf(channel, userMsg, 5000, "Meme `%s` replaced", args[1]);
		}else if("remove".equals(args[0])) {
			if(args.length < 2)
				return sendHelp(channel, false);
			
			if(memes.remove(args[1]) != null)
				Respond.timeoutf(channel, userMsg, 5000, "Meme `%s` removed...", args[1]);
			else Respond.timeoutf(channel, userMsg, 5000, "Meme `%s` not registered.", args[1]);
		}else if(memes.containsKey(args[0])){
			event.getMessage().delete().complete();
			String out = memes.get(args[0]);
			Respond.async(channel, out);
		}else Respond.timeout(channel, userMsg, 5000, "Unknown Meme. Ask an admin to add it.");
		
		return true;
	}

	private EmbedBuilder getPage(int page) {
		EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED).setTitle("Memes - Page " + page);
		
		if(page < 1)
			return builder.addField("No such page", "", false);
		
		Iterator<String> iter = memes.keySet().iterator();
		for(int i=0; i < (page - 1) * 5;i++, iter.next())
			if(!iter.hasNext())
				return builder.addField("No such page", "", false);
		
		for(int x=0;x<5;x++) {
			if(!iter.hasNext()) {
				if(x == 0)
					return builder.addField("No such page", "", false);
				break;
			}
			String next = iter.next();
			builder.addField(next, '`' + memes.get(next) + "`", false);
		}
		
		return builder.setColor(Color.CYAN);
	}
	
	@Override public boolean hasPermissions(Member member, String... args) {
		if(args.length > 0 && "add".equals(args[0]))
			return member.hasPermission(Permission.ADMINISTRATOR);
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
	
	@Override public String[] usages() {
		return new String[] {
			"`meme <memename>` -- Responds with the saved meme.",
			"`meme list [pagenum=1]` -- Lists a given page of memes.",
			"``meme add <`name`> <`reaction`>`` -- Saves a certain reaction by a given name.",
			"``meme remove <`name`>`` -- Removes a meme by its name."
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
	
	private class MemeMenu extends EmbedMenu{
		public MemeMenu(Message msg) {
			super(msg);
		}
		
		public MemeMenu(Message msg, int page) {
			super(msg, page);
		}

		@Override
		public MessageEmbed getEmbed(int page) {
			return Meme.this.getPage(page).build();
		}
	}
}

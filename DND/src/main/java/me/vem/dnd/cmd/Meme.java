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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

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
		
		String meme = args[0];
		if(meme.equals("list")) {
			
			int page = 1;
			
			if(args.length >= 2)
				try { page = Integer.parseInt(args[1]); }catch(NumberFormatException e) {}
			
			new MemeMenu(Respond.sync(event.getChannel(), getPage(page)), page).setTimeout(60);
			event.getMessage().delete().queue();
			
		}else if(meme.equals("add")) {
			if(args.length<3)
				return sendHelp(channel, false);
			
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

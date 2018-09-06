package me.vem.bot.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

import me.vem.bot.Bot;
import me.vem.bot.DataHandler;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Prefix implements DataHandler, SecureCommand{

	public static final String cmd_name = "prefix";
	private static final String DEFAULT_PREFIX = "~";
	
	private HashMap<Guild, String> prefixes;
	private File dataFile;
	
	public Prefix(JDA jda) {
		try {
			loadData(jda);
		} catch (IOException e) {} //Should not throw; formality.
	}
	
	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		if(args.length != 1) {
			Bot.respond(help(event), event);
			return;
		}
		
		if(!hasPermissions(event)) {
			Bot.respond(help(event), event);
			return;
		}
		
		Guild g = event.getGuild();
		String p = args[0];
		
		prefixes.put(g, p);
		try {
			saveData();
		} catch (IOException e) {}
		
		Bot.respond(String.format("Prefix in this guild has been set to '%s'!", p), event);
	}

	public String getPrefix(Guild g) {
		if(prefixes.containsKey(g))
			return prefixes.get(g);
		return DEFAULT_PREFIX;
	}
	
	public String getPrefix(MessageReceivedEvent event) {
		return getPrefix(event.getGuild());
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help(MessageReceivedEvent event) {
		if(hasPermissions(event))
			return String.format("Valid Usage: %s <newprefix>", cmd_name);
		return "You do not have the permissions to run this command.";
	}

	@Override
	public void saveData() throws IOException {
		if(dataFile == null) return;
		
		PrintWriter pw = new PrintWriter(dataFile);
		
		for(Guild g : prefixes.keySet())
			pw.println(g.getIdLong() + " " + prefixes.get(g));
		
		pw.flush();
		pw.close();
	}

	@Override
	public void loadData(JDA jda) throws IOException {
		prefixes = new HashMap<>();
		dataFile = new File("prefixes.dat");
		
		if(!dataFile.exists()) {
			dataFile.createNewFile();
			return;
		}
		
		Scanner read = new Scanner(dataFile);
		while(read.hasNextLine()) {
			Scanner in = new Scanner(read.nextLine());
			Guild g = jda.getGuildById(in.nextLong());
			String p = in.next();
			
			prefixes.put(g, p);
			in.close();
		}
		read.close();
	}

	private long token;
	private int pl; //pl >> Permission Level
	
	@Override public int getPermissionLevel() { return pl; }
	
	public SecureCommand setPermissionLevel(int i) {
		this.pl = i;
		return this;
	}

	@Override public long getToken() { return token; }

	@Override
	public SecureCommand setToken(long l) {
		
		return this;
	}
}
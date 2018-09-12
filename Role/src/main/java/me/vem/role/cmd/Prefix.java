package me.vem.role.cmd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

import me.vem.role.Bot;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Prefix implements Command{
	
	private HashMap<Guild, String> prefixes;
	
	private File data;
	
	public Prefix(JDA jda) {
		loadPrefixes(jda);
	}
	
	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		if(!hasPermissions(event)) {
			Bot.respond("You do not have the permissions to run this command.", event);
			return;
		}
		
		if(args.length == 1) {

			Guild g = event.getGuild();
			String p = args[0];
			
			prefixes.put(g, p);
			savePrefixes();

			Bot.respond("Prefix set to '"+p+"'.", event);
			
			return;
		}
		
		Bot.respond(help(event), event);
	}
	
	public String getPrefix(Guild guild) {
		if(prefixes.containsKey(guild))
			return prefixes.get(guild);
		return "~"; //Default Prefix
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help(MessageReceivedEvent event) {
		return String.format("Usage: %sprefix [newprefix]", getPrefix(event.getGuild()));
	}
	
	private void savePrefixes() {
		try {
			PrintWriter pw = new PrintWriter(data);
			
			for(Guild g : prefixes.keySet())
				pw.println(g.getIdLong() + " " + prefixes.get(g));
			
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void loadPrefixes(JDA jda) {
		prefixes = new HashMap<>();
		
		data = new File("prefixes.dat");
		if(!data.exists()) {
			try { 
				data.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		try {
			Scanner read = new Scanner(data);
			while(read.hasNextLine()) {
				Scanner in = new Scanner(read.nextLine());
				
				Guild g = jda.getGuildById(in.nextLong());
				String p = in.next();
				prefixes.put(g, p);
				
				in.close();
			}
			read.close();
		} catch (IOException e) { } //File is ensured to exist.
	}
}
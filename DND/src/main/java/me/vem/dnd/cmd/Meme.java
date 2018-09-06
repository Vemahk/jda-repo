package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.TreeMap;

import me.vem.dnd.Main;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Meme implements Command{

	private TreeMap<String, String> memes;
	
	public Meme() {
		memes = new TreeMap<>();
		loadSettings();
	}
	
	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		if(args.length == 0) {
			Main.respondTimeout("Usage: ~meme <memename> or ~meme list", 5, event);
			return;
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
				Main.respondTimeout(rsp, timeout, event);
			else Main.respond(rsp, event);
		}else if(meme.equals("add")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Main.respondTimeout("Aye, you can't do this. Sorry bud.", 5, event);
				return;
			}
			
			if(args.length<3) {
				Main.respondTimeout("Aye, u dun goofed.", 5, event);
				return;
			}
			
			memes.put(args[1], args[2]);
			saveSettings();
			Main.respondTimeout("Meme added. OuO", 5, event);
		}else if(memes.containsKey(meme)){
			event.getMessage().delete().complete();
			String out = memes.get(meme);
			if(ClearOOC.roomEnabled(event.getTextChannel())) out = "("+out+")";
			event.getTextChannel().sendMessage(out).complete();
		}else Main.respondTimeout("Unknown Meme. Ask Vem to add it, pleb.", 5, event);
		
	}

	private void saveSettings() {
		File f = new File("memes.dat");
		
		try {
			if(f.exists()) f.delete();
			f.createNewFile();
			
			PrintWriter file = new PrintWriter(f);
			for(String s : memes.keySet()) file.println(s+"::"+memes.get(s));
			file.flush();
			file.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSettings() {
		File f = new File("memes.dat");
		if(!f.exists()) return;
		
		try {
			Scanner file = new Scanner(f);
			while(file.hasNextLine()) {
				String[] ln = file.nextLine().split("::");
				memes.put(ln[0], ln[1]);
			}
			file.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return true;
	}
}

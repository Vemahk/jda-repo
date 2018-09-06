package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import me.vem.dnd.Main;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ClearOOC implements Command{
	
	private static Set<String> allowedRoomIDs;
	
	public static boolean roomEnabled(TextChannel tc) {
		return allowedRoomIDs.contains(tc.getId());
	}
	
	public ClearOOC() {
		allowedRoomIDs = new HashSet<String>();
		loadSettings();
	}
	
	public void run(String[] args, MessageReceivedEvent event) {
		int check = 50;
		if(args.length > 0) {
			if(args[0].equals("allow")) {
				if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					Main.respondTimeout("Only an administrator can allow/disallow text channels.", 5, event);
					return;
				}
				if(!allowedRoomIDs.add(event.getTextChannel().getId())) {
					Main.respondTimeout("Chatroom was already allowed to begin with.", 5, event);
					return;
				}
				Main.respondTimeout("Chatroom allowed", 5, event);
				saveSettings();
				return;
			}else if(args[0].equals("disallow")) {
				if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					Main.respondTimeout("Only an administrator can can allow/disallow text channels.", 5, event);
					return;
				}
				if(!allowedRoomIDs.remove(event.getTextChannel().getId())) {
					Main.respondTimeout("Chatroom was not allowed to begin with.", 5, event);
					return;
				}
				Main.respondTimeout("Chatroom disallowed.", 5, event);
				saveSettings();
				return;
			}else{
				try {
					check = Integer.parseInt(args[0]);
				}catch(Exception e) {
					//e.printStackTrace();
					check = 50;
				}
			}
		}
		
		if(!allowedRoomIDs.contains(event.getTextChannel().getId())) {
			Main.respondTimeout("'~clearooc' is not allowed in this chatroom. Ask an admin for details.", 5, event);
			return;
		}
		
		Main.respondTimeout("Checking past "+check+" messages for ooc...", 5, event);
		
		HashSet<Message> set = new HashSet<>();
		for(Message x : event.getTextChannel().getHistory().retrievePast(check).complete())
			if(x.getContentRaw().matches("^\\s*\\(.*\\)\\s*$"))
				set.add(x);
		
		HashSet<Message> delSet = new HashSet<>();
		for(Message m : set) 
			if(m.getCreationTime().isAfter(OffsetDateTime.now().minusDays(14)))
				delSet.add(m);
		
		
		if(delSet.size() >= 2)
			event.getTextChannel().deleteMessages(delSet).complete();
		else if(!delSet.isEmpty())
			for(Message m : delSet) m.delete().complete();
	}
	
	public boolean hasPermissions(MessageReceivedEvent event) {
		Member mem = event.getMember();
		
		if(mem.hasPermission(Permission.ADMINISTRATOR) || mem.hasPermission(Permission.MESSAGE_MANAGE)) return true;
		return false;
	}
	
	public void saveSettings() {
		File outFile = new File("clearooc.dat");
		
		try {
			if(outFile.exists()) outFile.delete();
			outFile.createNewFile();
			
			PrintWriter file = new PrintWriter(outFile);
			for(String s : allowedRoomIDs) file.println(s);
			file.flush();
			file.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadSettings() {
		File f = new File("clearooc.dat");
		if(!f.exists()) return;
		
		try {
			Scanner file = new Scanner(f);
			while(file.hasNextLine()) allowedRoomIDs.add(file.nextLine());
			file.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
}

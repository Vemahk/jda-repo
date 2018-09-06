package me.vem.cs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;

import me.vem.cs.cmd.SwearLog;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Listener extends ListenerAdapter{

	private SwearLog swearlog;
	
	public Listener() {
		loadFilter();
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Message m = event.getMessage();
		if(m.getAuthor().getId() == event.getJDA().getSelfUser().getId()) return;
		
		String content = m.getContentRaw();
		//Classical Commands
		if(content.startsWith("~!")) 
			Main.handle(event.getMessage().getContentRaw().substring(2), event);
		
		if(words.isEmpty()) return;
		String tlc = content.toLowerCase();
		for(String badword : words)
			if(tlc.matches("^.*\\W"+badword+"\\W.*$") || tlc.matches("^"+badword+"$") || tlc.matches("^"+badword+"\\W.*$") || tlc.matches("^.*\\W"+badword+"$")) {
				m.delete().complete();
				Main.respond("https://i.imgur.com/Q5jVVPw.png", event);
				notifyBadword(badword, event);
				break;
			}
	}

	/**
	 * Originally intended to message a group of admins who cursed, when, and with what. [WIP]
	 * @param badword
	 * @param event
	 */
	private void notifyBadword(String badword, MessageReceivedEvent event) {
		if(swearlog==null)
			swearlog = (SwearLog)Main.commands.get("swearlog");
		swearlog.notifyAdmins(badword, event);
	}
	
	/**
	 * List of bad words.
	 */
	public HashSet<String> words;
	
	/**
	 * Loads bad words from the 'blockedwords.txt' file.
	 */
	public void loadFilter() {
		words = new HashSet<>();
		try {
			File f = new File("blockedwords.txt");
			if(!f.exists()){
				Main.info("Swear filter offline. Reason: could not locate blockwords.txt");
				return;
			}
			Scanner file = new Scanner(f);
			
			while(file.hasNextLine())
				words.add(file.nextLine().trim());
			
			file.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}

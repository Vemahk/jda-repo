package me.vem.cs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import me.vem.cs.cmd.SwearLog;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class FilteredMessageListener implements EventListener{

	private static FilteredMessageListener instance;
	public static FilteredMessageListener getInstance() {
		if(instance == null)
			instance = new FilteredMessageListener();
		return instance;
	}
	
	private FilteredMessageListener() {
		loadFilter();
	}
	
	@Override
	public void onEvent(Event event) {
		if(event instanceof GuildMessageReceivedEvent)
			onMessageReceived((GuildMessageReceivedEvent) event);
	}
	
	public void onMessageReceived(GuildMessageReceivedEvent event) {
		Message msg = event.getMessage();
		User self = event.getJDA().getSelfUser();
		if(msg.getAuthor().getIdLong() == self.getIdLong())
			return;
		
		String rawContent = msg.getContentRaw();
		
		if(screenedWords.isEmpty()) return;
		String tlc = rawContent.toLowerCase();
		for(String badword : screenedWords)
			if(tlc.matches("^.*\\W"+badword+"\\W.*$") || tlc.matches("^"+badword+"$") || tlc.matches("^"+badword+"\\W.*$") || tlc.matches("^.*\\W"+badword+"$")) {
				event.getMessage().delete().complete();
				Respond.async(event.getChannel(), "https://i.imgur.com/Q5jVVPw.png");
				notifyBadword(event, badword);
				break;
			}
	}

	/**
	 * Originally intended to message a group of admins who cursed, when, and with what. [WIP]
	 * @param badword
	 * @param event
	 */
	private void notifyBadword(GuildMessageReceivedEvent event, String badword) {
		SwearLog.getInstance().notifyAdmins(badword, event);
	}
	
	/**
	 * List of bad words.
	 */
	public HashSet<String> screenedWords;
	
	/**
	 * Loads bad words from the 'blockedwords.txt' file.
	 */
	public void loadFilter() {
		screenedWords = new HashSet<>();
		try {
			File f = ExtFileManager.getConfigFile("blockedwords.txt");
			if(f == null){
				Logger.info("Swear filter offline. Could not locate blockedwords.txt");
				return;
			}
			
			BufferedReader reader = new BufferedReader(new FileReader(f));
			for(String read = reader.readLine(); read != null; read = reader.readLine())
				screenedWords.add(read.trim());
			
			reader.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}
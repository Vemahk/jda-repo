package me.vem.cs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import me.vem.cs.cmd.Command;
import me.vem.cs.cmd.Prefix;
import me.vem.cs.cmd.SwearLog;
import me.vem.cs.utils.ExtFileManager;
import me.vem.cs.utils.Logger;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter{

	private static MessageListener instance;
	public static MessageListener getInstance() {
		if(instance == null)
			instance = new MessageListener();
		return instance;
	}
	
	private MessageListener() { loadFilter(); }
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Message msg = event.getMessage();
		User self = event.getJDA().getSelfUser();
		if(msg.getAuthor().getIdLong() == self.getIdLong())
			return;
		
		Guild guild = event.getGuild();
		
		String rawContent = msg.getContentRaw();
		
		if(rawContent.equals(self.getAsMention())) {
			StringBuilder resp = new StringBuilder("List of valid commands:\n```\n");
			for(String cmd : Command.getCommandLabels())
				resp.append(cmd).append('\n');
			resp.append("```");
			
			Bot.respondAsync(event, resp.toString());
			
			return;
		}
		
		//Classic Commands
		if(rawContent.startsWith(Prefix.get(guild))
				|| rawContent.startsWith(self.getAsMention())) {
			String cmdname = getCommandNameFromRaw(guild, rawContent);
			if(cmdname.length() == 0)
				return;
			
			Command cmd = Command.getCommand(cmdname);
			if(cmd == null) {
				Bot.respondAsyncf(event, "Command `%s` not recognized.", cmdname);
			}else{
				String[] args = parseArgs(guild, rawContent);
				Logger.debugf("%s attempted to call %s with arguments %s.", event.getAuthor().getName(), cmdname, Arrays.toString(args));
				cmd.run(event, args);
			}
		}
		
		if(screenedWords.isEmpty()) return;
		String tlc = rawContent.toLowerCase();
		for(String badword : screenedWords)
			if(tlc.matches("^.*\\W"+badword+"\\W.*$") || tlc.matches("^"+badword+"$") || tlc.matches("^"+badword+"\\W.*$") || tlc.matches("^.*\\W"+badword+"$")) {
				event.getMessage().delete().queue();
				Bot.respondAsync(event, "https://i.imgur.com/Q5jVVPw.png");
				notifyBadword(event, badword);
				break;
			}
	}
	
	private String getCommandNameFromRaw(Guild guild, String raw) {
		int start;
		if(raw.startsWith(Prefix.get(guild)))
			start = Prefix.get(guild).length();
		else start = raw.indexOf(' ') + 1;
		
		int end = raw.indexOf(' ', start);
		if(end < 0) end = raw.length();
		
		return raw.substring(start, end);
	}
	
	private String[] parseArgs(Guild guild, String raw) {
		LinkedList<String> argsTmp = new LinkedList<>();
		
		int head = raw.indexOf(' ') + 1;
		
		if(!raw.startsWith(Prefix.get(guild)))
			head = raw.indexOf(' ', head) + 1; 
		
		if(head == 0) //No significant space found, i.e. no arguments. 
			return new String[0];
		
		StringBuilder buf = new StringBuilder();
		while(head < raw.length()) {
			char h = raw.charAt(head++);
			if(h == ' ') {
				if(buf.length() == 0) continue;
				argsTmp.add(buf.toString());
				buf = new StringBuilder();
			}else if(h == '`') { //Special case for grouped args
				while(head < raw.length()) {
					char h2 = raw.charAt(head++);
					if(h2 == '\\' && head < raw.length() && raw.charAt(head) == '`') //For escaping quotation marks.
						buf.append(raw.charAt(head++));
					else if(h2 == '`') 
						break;
					else buf.append(h2);
				}
			}else buf.append(h);
		}
		
		if(buf.length() > 0) 
			argsTmp.add(buf.toString());
		
		return argsTmp.toArray(new String[0]);
	}

	/**
	 * Originally intended to message a group of admins who cursed, when, and with what. [WIP]
	 * @param badword
	 * @param event
	 */
	private void notifyBadword(MessageReceivedEvent event, String badword) {
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

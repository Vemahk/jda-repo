package me.vem.dbgm;

import java.util.Arrays;
import java.util.LinkedList;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Prefix;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter{

	// I <3 Singletons
	private static MessageListener instance;
	public static MessageListener getInstance() {
		if(instance == null)
			instance = new MessageListener();
		return instance;
	}
	
	private MessageListener() {}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Message msg = event.getMessage();
		User self = event.getJDA().getSelfUser();
		if(msg.getAuthor().getIdLong() == self.getIdLong())
			return;
		
		Guild guild = event.getGuild();
		
		String rawContent = msg.getContentRaw();
		if(rawContent.equals(self.getAsMention())) {
			StringBuilder resp = new StringBuilder("List of known commands:\n```\n");
			for(String cmd : Command.getCommandLabels())
				resp.append(cmd + '\n');
			resp.append("```");
			
			Respond.async(event, resp.toString());
			
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
				Respond.asyncf(event, "Command `%s` not recognized.", cmdname);
			}else{
				String[] args = parseArgs(guild, rawContent);
				cmd.run(event, args);
				Logger.debugf("%s attempted to call %s with arguments %s.", event.getAuthor().getName(), cmdname, Arrays.toString(args));
			}
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
			}else if(h == '`' && buf.length() == 0) { //Special case for grouped args
				while(head < raw.length()) {
					char h2 = raw.charAt(head++);
					if(h2 == '\\' && head < raw.length() && raw.charAt(head) == '`') //For escaping quotation marks.
						buf.append(raw.charAt(head++));
					else if(h2 == '`') {
						argsTmp.add(buf.toString());
						buf = new StringBuilder();
						break;
					}else buf.append(h2);
				}
				
				if(buf.length() > 0) {
					Logger.errf("Args parsing ended unexpectedly early...%nDump: (%s) -> %s, & <%s>", raw, argsTmp, buf);
					argsTmp.add(buf.toString());
					buf = new StringBuilder();
				}
			}else buf.append(h);
		}
		
		if(buf.length() > 0) 
			argsTmp.add(buf.toString());
		
		return argsTmp.toArray(new String[0]);
	}
	
}
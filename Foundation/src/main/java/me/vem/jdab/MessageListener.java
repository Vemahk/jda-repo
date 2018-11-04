package me.vem.jdab;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Prefix;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class MessageListener implements EventListener{

	// I <3 Singletons
	private static MessageListener instance;
	public static MessageListener getInstance() {
		if(instance == null)
			instance = new MessageListener();
		return instance;
	}
	
	private MessageListener() {}
	
	@Override
	public void onEvent(Event event) {
		if(event instanceof GuildMessageReceivedEvent)
			onGuildMessageReceived((GuildMessageReceivedEvent)event);
		if(event instanceof PrivateMessageReceivedEvent)
			onPrivateMessageReceived((PrivateMessageReceivedEvent) event);
	}
	
	private void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if(event.getMessage().getContentDisplay().equals("cleanup"))
			event.getChannel().getIterableHistory().cache(false).forEachAsync(msg -> {
				if(msg.getAuthor().equals(event.getJDA().getSelfUser()))
					msg.delete().queue();
				return true;
			});
	}
	
	private void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		Message msg = event.getMessage();
		User self = event.getJDA().getSelfUser();
		if(msg.getAuthor().getIdLong() == self.getIdLong())
			return;
		
		Guild guild = event.getGuild();
		
		String rawContent = msg.getContentRaw();
		
		boolean selfMention = rawContent.startsWith(self.getAsMention());
		boolean prefixPresent = rawContent.startsWith(Prefix.get(guild));
		
		if(!(selfMention || prefixPresent))
			return;
		
		//Classic Commands
		Queue<String> parsed = parse(rawContent);
		String cmdname = "help";
		
		if(parsed.peek().equals(self.getAsMention())) {
			parsed.poll();
			if(!parsed.isEmpty())
				cmdname = parsed.poll();
		}else cmdname = parsed.poll().substring(Prefix.get(guild).length());
		
		Command cmd = Command.getCommand(cmdname);
		if(cmd == null) {
			Respond.asyncf(event.getChannel(), "Command `%s` not recognized.", cmdname);
		}else{
			String[] args = new String[parsed.size()];
			for(int i=0; !parsed.isEmpty() ;i++)
				args[i] = parsed.poll();
			Logger.debugf("%s attempted to call %s with arguments %s.", event.getAuthor().getName(), cmdname, Arrays.toString(args));
			cmd.run(event, args);
		}
	}
	
	private Queue<String> parse(String raw) {
		LinkedList<String> argsTmp = new LinkedList<>();
		
		int head = 0;
		
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
		
		return argsTmp;
	}
}
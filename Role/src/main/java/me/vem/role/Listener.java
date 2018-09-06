package me.vem.role;

import java.util.Iterator;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.MentionType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Listener extends ListenerAdapter{
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(event.isFromType(ChannelType.PRIVATE)) {
			Bot.info(String.format("[PM > %s] %s", event.getAuthor().getName(),
                    event.getMessage().getContentDisplay()));
		}else {
			Message m = event.getMessage();
			if(m.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) return;
			
			JDA jda = event.getJDA();
			Guild guild = event.getGuild();
			String content = m.getContentRaw();
			//Classical Commands
			if(content.startsWith(Bot.prefix.getPrefix(guild)))
				Bot.handle(event.getMessage().getContentRaw(), event);
			
			if(event.getMessage().isMentioned(jda.getSelfUser(), MentionType.USER) && event.getMessage().getContentRaw().startsWith(jda.getSelfUser().getAsMention())) {
				
				String s = event.getMessage().getContentRaw().replaceFirst(jda.getSelfUser().getAsMention(), "").trim();
				if(s.equals("")) { //The only part of the message was the mention of the bot.
					StringBuffer cmds = new StringBuffer();
					
					Iterator<String> iter = Bot.commands.keySet().iterator();
					while(iter.hasNext()){
						cmds.append(iter.next());
						if(iter.hasNext())
							cmds.append("\n");
					}
					
					Bot.respond("This server's prefix is set to '"+Bot.prefix.getPrefix(event.getGuild())+"'\n"
							+ "A current list of registered commands:" + Bot.format(cmds.toString(), Bot.TextFormat.CODE), event);
				}else Bot.handle(s, event);
			}
		}
	}
}

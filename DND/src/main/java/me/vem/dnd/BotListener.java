package me.vem.dnd;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BotListener extends ListenerAdapter{

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Message m = event.getMessage();
		if(m.getAuthor().getId() == event.getJDA().getSelfUser().getId()) return;
		
		String content = m.getContentRaw();
		//Classical Commands
		if(content.startsWith("~")) 
			Main.handle(event.getMessage().getContentRaw().substring(1), event);
	}
	
	@Override
	public void onReady(ReadyEvent event) {
		//Main.log("status", event.getJDA().getSelfUser().getId());
	}
	
}

package me.vem.bot;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.MentionType;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Listener extends ListenerAdapter{

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		JDA jda = event.getJDA();
		SelfUser self = jda.getSelfUser();
		Guild guild = event.getGuild();
		
		Message m = event.getMessage();
		if(m.getAuthor().getIdLong() == self.getIdLong()) //Ignore the bot's own messages.
			return;

		//Classical Commands
		String content = m.getContentDisplay();
		if(content.startsWith(Bot.prefix.getPrefix(event))) { 
			Bot.handle(content, event);
			return;
		}
		
		//Mention Commands
		if(m.isMentioned(self, MentionType.USER)) {
			content = m.getContentRaw();
			
			Bot.info("Bot mentioned> "+ content);
			
			if(content.startsWith(self.getAsMention()))
				content = content.replaceFirst(self.getAsMention(), "").trim();
			
			if(content.length() == 0) {
				//Help
				StringBuffer help = new StringBuffer();
				guild.getMember(self).getNickname();
				help.append(String.format("This Guild's prefix is set to '%s'.%n", Bot.prefix.getPrefix(event)));
				help.append("You can run commands via the command prefix or by mentioning this bot. "
						+ "However, keep in mind that the two handle your message differently. "
						+ "Using the prefix, the bot will ignore all text formatting (e.g. italics). "
						+ "Using the mention, the bot will recognize all formatting (e.g. *italics* will appear as \\*italics\\*).\n");
				help.append("List of valid commands:```\n");
				for(String cmd : Bot.commandSet())
					help.append(cmd + "\n");
				help.append("```");
				Bot.respond(help.toString(), event);
			}else 
				Bot.handle(content, event);
		}
	}
}

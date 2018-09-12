package me.vem.bot;
import java.io.IOException;
import java.util.Arrays;

import me.vem.bot.cmd.AntiPurge;
import me.vem.bot.cmd.PermissionHandler;
import me.vem.bot.cmd.Prefix;
import me.vem.bot.cmd.Purge;
import me.vem.bot.utils.Console;
import me.vem.bot.utils.IgnoredReference;
import me.vem.bot.utils.Logger;
import me.vem.bot.utils.Version;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * @author Vemahk
 * @JDAVersion 
 * 3.7.1_421
 */
public class Bot {
	
	/** The Bot Itself */
	private static JDA jda;
	public static JDA getJDA() { return jda; }
	
	public static void shutdown() {
		Logger.infof("%s is shutting down...", Version.getVersion());
		
		//Perform any save operation that may have to occur here.
		Prefix.getInstance().save();
		
		jda.shutdown(); 

		if(Console.hasConsole())
			Console.getConsole().dispose();
		System.exit(0);
	}
	
	public static void main(String[] args) throws IOException {
		Logger.infof("Hello World! From %s", Version.getVersion());
		Console.buildConsole();
		
		try {
			jda = new JDABuilder(AccountType.BOT).addEventListener(MessageListener.getInstance()).setToken(IgnoredReference.botToken).build().awaitReady();
			jda.setAutoReconnect(true);	
		}catch (Exception e){
			e.printStackTrace();
		}
		
		Prefix.initialize();
		Purge.initialize();
		AntiPurge.initialize();
		PermissionHandler.initialize();
	}
	
	/* RESPONCES */
	public static enum TextFormat{
		LINEDCODE("`"), CODE("```\n"), ITALICS("*"), BOLD("**"), BOLDITALICS("***"), UNDERLINE("__"), UNDERLINEITALICS("__*"), ALL("__***"), STRIKETHROUGH("--");
		
		private String s;
		private TextFormat(String s) { this.s = s; }
		public String format(String x) { return s + x + new StringBuffer(s).reverse().toString(); }
	}
	
	/**
	 * Synchronous response.
	 * @param event
	 * @param msg
	 * 
	 * @return the message object retrieved. Only possible for respondSync.
	 */
	public static Message respondSync(MessageReceivedEvent event, String msg) {
		return event.getTextChannel().sendMessage(msg).complete();
	}
	
	/**
	 * Synchronous response. Deletes message (and user's command call) after 'timeout' milliseconds.
	 * @param event
	 * @param msg
	 * @param timeout
	 */
	public static void respondTimeout(MessageReceivedEvent event, String msg, int timeout) {
		Message m = event.getTextChannel().sendMessage(msg).complete();
		if(timeout <= 0) return;
		
		new Thread(() ->{
			try { Thread.sleep(timeout); } catch (InterruptedException e) { e.printStackTrace(); }
			event.getTextChannel().deleteMessages(Arrays.asList(m, event.getMessage()));
		}).start();
	}
	
	/**
	 * Asynchronous response.
	 * @param event
	 * @param msg
	 */
	public static void respondAsync(MessageReceivedEvent event, String msg) {
		event.getTextChannel().sendMessage(msg).queue();
	}
	
	/**
	 * Asynchronous, formatted response. Uses printf formatting.
	 * @param event
	 * @param format
	 * @param objs
	 */
	public static void respondAsyncf(MessageReceivedEvent event, String format, Object... objs) {
		respondAsync(event, String.format(format, objs));
	}
	
}

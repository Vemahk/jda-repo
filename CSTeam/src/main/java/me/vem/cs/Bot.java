package me.vem.cs;

import java.io.IOException;
import java.util.Arrays;

import javax.security.auth.login.LoginException;

import me.vem.cs.cmd.Contests;
import me.vem.cs.cmd.Help;
import me.vem.cs.cmd.NextContest;
import me.vem.cs.cmd.Prefix;
import me.vem.cs.cmd.SwearLog;
import me.vem.cs.utils.Console;
import me.vem.cs.utils.IgnoredReference;
import me.vem.cs.utils.Logger;
import me.vem.cs.utils.Version;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Bot {

	/** The Bot Itself */
	private static JDA jda;
	public static JDA getJDA() { return jda; }
	
	public static void shutdown() {
		Logger.infof("%s is shutting down...", Version.getVersion());
		
		//Perform any save operation that may have to occur here.
		Prefix.getInstance().save();
		SwearLog.getInstance().save();
		Contests.getInstance().save();

		if(Console.hasConsole())
			Console.getConsole().dispose();
		
		jda.shutdown();
		
		System.exit(0);
	}
	
	public static void main(String[] args) throws IOException {
		Console.buildConsole();
		
		try {
			(jda = new JDABuilder(AccountType.BOT)
					.addEventListener(MessageListener.getInstance())
					.setToken(IgnoredReference.botToken)
					.build().awaitReady())
					.setAutoReconnect(true);
		} catch (LoginException | IllegalArgumentException | InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		//Registers all the commands. This is required for each command, else the bot won't recognize the command.
		Prefix.initialize();
		Help.initialize();
		NextContest.initialize();
		Contests.initialize();
		SwearLog.initialize();
	}
	
	/**
	 * This is for Discord's text formatting.
	 * @author Vemahk
	 */
	public enum TextFormat{
		LINEDCODE("`"), CODE("```\n"), ITALICS("*"), BOLD("**"), BOLDITALICS("***"), UNDERLINE("__"), UNDERLINEITALICS("__*"), ALL("__***"), STRIKETHROUGH("--");
		
		private String s;
		private TextFormat(String s) { this.s = s; }
		public String apply(String x) { return s + x + new StringBuffer(s).reverse().toString(); }
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

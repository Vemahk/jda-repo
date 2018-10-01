package me.vem.dnd.utils;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Respond {

	private static Timer timer = new Timer();
	public static void timerShutdown() {
		timer.cancel();
	}
	
	/**
	 * Synchronous bot response.
	 * @param event
	 * @param msg
	 * 
	 * @return the message object retrieved.
	 */
	public static Message sync(TextChannel channel, String msg) {
		return channel.sendMessage(msg).complete();
	}
	
	/**
	 * Synchornous bot response.
	 * @param event
	 * @param msg
	 * @return the message object retrieved.
	 */
	public static Message sync(MessageReceivedEvent event, String msg) {
		return sync(event.getTextChannel(), msg);
	}
	
	/**
	 * Synchronous bot response with printf formatting.
	 * @param channel
	 * @param format
	 * @param args
	 * @return the message object retrieved.
	 */
	public static Message syncf(TextChannel channel, String format, Object... args) {
		return sync(channel, String.format(format, args));
	}
	
	/**
	 * Synchronous bot response with printf formatting.
	 * @param event
	 * @param format
	 * @param args
	 * @return the message object retrieved.
	 */
	public static Message syncf(MessageReceivedEvent event, String format, Object... args) {
		return syncf(event.getTextChannel(), format, args);
	}
	
	/**
	 * Asynchronous response.
	 * @param channel
	 * @param msg
	 */
	public static void async(TextChannel channel, String msg) {
		channel.sendMessage(msg).queue();
	}
	
	/**
	 * Asynchronous response.
	 * @param event
	 * @param msg
	 */
	public static void async(MessageReceivedEvent event, String msg) {
		async(event.getTextChannel(), msg);
	}
	
	/**
	 * Asynchronous, formatted response. Uses printf formatting.
	 * @param channel
	 * @param format
	 * @param args
	 */
	public static void asyncf(TextChannel channel, String format, Object... args) {
		async(channel, String.format(format, args));
	}
	
	/**
	 * Asynchronous, formatted response. Uses printf formatting.
	 * @param event
	 * @param format
	 * @param args
	 */
	public static void asyncf(MessageReceivedEvent event, String format, Object... args) {
		asyncf(event.getTextChannel(), format, args);
	}
	
	/**
	 * Synchronous response. Deletes the bot's message *AND* the user's command call.
	 * @param event
	 * @param timeout Time to delay in milliseconds.
	 * @param msg
	 */
	public static void timeout(MessageReceivedEvent event, int timeout, String msg) {
		Message m = sync(event, msg);
		if(timeout <= 0) return;
		timer.schedule(new TimerTask() {
			@Override public void run() {
				event.getTextChannel().deleteMessages(Arrays.asList(m, event.getMessage())).queue();			
			}
		}, timeout);
	}
	
	/**
	 * Synchronous response. Deletes *ONLY* the bot's message.
	 * @param channel
	 * @param timeout Time to delay in milliseconds.
	 * @param msg
	 */
	public static void timeout(TextChannel channel, int timeout, String msg) {
		Message m = sync(channel, msg);
		if(timeout <= 0) return;
		
		timer.schedule(new TimerTask() {
			@Override public void run() {
				channel.deleteMessageById(m.getIdLong()).queue();				
			}
		}, timeout);
	}
	
	/**
	 * Synchronous response using printf formatting. Deletes the bot's message *AND* the user's command call.
	 * @param event
	 * @param timeout Time to delay in milliseconds.
	 * @param format
	 * @param args
	 */
	public static void timeoutf(MessageReceivedEvent event, int timeout, String format, Object... args) {
		timeout(event, timeout, String.format(format, args));
	}
	
	/**
	 * Synchronous response using printf formatting. Deletes *ONLY* the bot's message.
	 * @param channel
	 * @param timeout Time to delay in milliseconds.
	 * @param format
	 * @param args
	 */
	public static void timeoutf(TextChannel channel, int timeout, String format, Object... args) {
		timeout(channel, timeout, String.format(format, args));
	}
}

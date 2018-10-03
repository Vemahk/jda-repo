package me.vem.dnd.utils;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Respond {

	private static Timer timer = new Timer();
	public static void timerShutdown() { timer.cancel(); }
	
	public static void deleteMessage(long milliDelay, Message message) {
		timer.schedule(new Task(() -> message.delete().queue()), milliDelay);
	}
	
	public static void deleteMessages(TextChannel channel, long milliDelay, Message... messages) {
		timer.schedule(new Task(() -> channel.deleteMessages(Arrays.asList(messages)).queue()), milliDelay);
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
	 * @param delay Time to delay in milliseconds.
	 * @param msg
	 */
	public static void timeout(MessageReceivedEvent event, long delay, String msg) {
		Message m = sync(event, msg);
		if(delay > 0)
			deleteMessages(event.getTextChannel(), delay, m, event.getMessage());
	}
	
	/**
	 * Synchronous response. Deletes *ONLY* the bot's message.
	 * @param channel
	 * @param delay Time to delay in milliseconds.
	 * @param msg
	 */
	public static void timeout(TextChannel channel, long delay, String msg) {
		Message m = sync(channel, msg);
		if(delay > 0) deleteMessage(delay, m);
	}
	
	/**
	 * Synchronous response using printf formatting. Deletes the bot's message *AND* the user's command call.
	 * @param event
	 * @param delay Time to delay in milliseconds.
	 * @param format
	 * @param args
	 */
	public static void timeoutf(MessageReceivedEvent event, long delay, String format, Object... args) {
		timeout(event, delay, String.format(format, args));
	}
	
	/**
	 * Synchronous response using printf formatting. Deletes *ONLY* the bot's message.
	 * @param channel
	 * @param delay Time to delay in milliseconds.
	 * @param format
	 * @param args
	 */
	public static void timeoutf(TextChannel channel, long delay, String format, Object... args) {
		timeout(channel, delay, String.format(format, args));
	}
	
	private static class Task extends TimerTask {
		private Runnable func;
		public Task(Runnable r) { this.func = r; }
		@Override public void run() { func.run(); }
	}
}
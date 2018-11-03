package me.vem.jdab.utils;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

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
	 * Asynchronous response.
	 * @param channel
	 * @param msg
	 */
	public static void async(TextChannel channel, String msg) {
		channel.sendMessage(msg).queue();
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
	 * Synchronous response. Deletes the bot's message and the passed {@code userMsg} after {@code delay} milliseconds.
	 * @param channel The text channel to respond within.
	 * @param userMsg The user's message to delete
	 * @param delay The delay, in milliseconds, the bot should wait before deleting the messages.
	 * @param msg The text the bot should respond with.
	 */
	public static void timeout(TextChannel channel, Message userMsg, long delay, String msg) {
		Message m = sync(channel, msg);
		if(delay > 0)
			deleteMessages(channel, delay, m, userMsg);
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
	
	/**
	 * Synchronous response with printf formatting. Deletes the bot's message and the passed {@code userMsg} after {@code delay} milliseconds.
	 * @param channel The text channel to respond within.
	 * @param userMsg The user's message to delete
	 * @param delay The delay, in milliseconds, the bot should wait before deleting the messages.
	 * @param format The printf format to display.
	 * @param args The objects to fill the printf formatting.
	 */
	public static void timeoutf(TextChannel channel, Message userMsg, long delay, String format, Object... args) {
		timeout(channel, userMsg, delay, String.format(format, args));
	}
	
	private static class Task extends TimerTask {
		private Runnable func;
		public Task(Runnable r) { this.func = r; }
		@Override public void run() { func.run(); }
	}
}
package me.vem.jdab;

import javax.security.auth.login.LoginException;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Help;
import me.vem.jdab.cmd.Prefix;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

/**
 * The bot object that will open the JDA instance upon initialization.
 * @author Vemahk
 */
public class DiscordBot {

	private static DiscordBot instance;
	public static DiscordBot getInstance() { return instance; }
	public static void initialize(String token) {
		if(instance == null)
			instance = new DiscordBot(token);
	}
	
	private JDA jda;
	
	private DiscordBot(String token) {
		try {
			//Look at this trash. Don't be like me.
			(jda = new JDABuilder(token)
					.addEventListener(MessageListener.getInstance())
					.build().awaitReady())
					.setAutoReconnect(true);
		}catch(LoginException | IllegalArgumentException | InterruptedException e) {
			e.printStackTrace();
		}
		
		registerCommands();
	}
	
	/**
	 * Preferred that you override this method using an anonymous subclass...
	 * Up to you: you can register your own commands through whatever means you want.
	 */
	public void registerCommands() {
		Help.initialize();
		Prefix.initialize();
	}
	
	public JDA getJDA() {
		return jda;
	}
	
	/**
	 * Shuts down the JDA instance safely.
	 * 
	 * Closes all related threads so that the program will shutdown safely.
	 */
	public void shutdown() {
		//Close the console and all related threads.
		Console.shutdown();
		
		Logger.info("Shutting down...");
		
		//Shutdown the timer thread for queuing responses.
		Respond.timerShutdown();
		
		//Call all registered commands' unload function.
		Command.unloadAll();
		
		//Safely shutdown the JDA
		jda.shutdown();
		
		//Set instance to null for potential reinitialization.
		instance = null;
	}
	
	/**
	 * For adding actions to the pre and post shutdown. pre.run() will be executed, then DiscordBot.shutdown(), then post.run().
	 * @param pre The pre-action to shutdown. If null, skipped.
	 * @param post The post-action to shutdown. If null, skipped.
	 */
	public void shutdown(Runnable pre, Runnable post) {
		if(pre != null) pre.run();
		shutdown();
		if(post != null) post.run();
	}
}
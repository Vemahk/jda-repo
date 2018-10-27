package me.vem.jdab;

import javax.security.auth.login.LoginException;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.cmd.Help;
import me.vem.jdab.cmd.Prefix;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.hooks.EventListener;

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

		Help.initialize();
		Prefix.initialize();
	}
	
	public void addEventListener(EventListener listener) {
		jda.addEventListener(listener);
	}
	
	public JDA getJDA() {
		return jda;
	}
	
	private Runnable preShutdown, postShutdown;
	public void setPreShutdown(Runnable r) { preShutdown = r; }
	public void setPostShutdown(Runnable r) { postShutdown = r; }
	
	/**
	 * Shuts down the JDA instance safely.
	 * 
	 * Closes all related threads so that the program will shutdown safely.
	 */
	public void shutdown() {
		if(preShutdown != null)
			preShutdown.run();
		
		//Close the console and all related threads.
		Console.shutdown();
		
		Logger.info("Shutting down...");
		
		//Shutdown the timer thread for queuing responses.
		Respond.timerShutdown();
		
		//Call all registered commands' unload function.
		Command.unloadAll();
		
		//Save all configurable event listeners
		for(Object o : jda.getRegisteredListeners())
			if(o instanceof Configurable)
				((Configurable)o).save();
		
		//Safely shutdown the JDA
		jda.shutdown();
		
		//Set instance to null for potential reinitialization.
		instance = null;
		
		if(postShutdown != null)
			postShutdown.run();
	}
}
package me.vem.jdab;

import javax.security.auth.login.LoginException;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.cmd.Help;
import me.vem.jdab.cmd.Prefix;
import me.vem.jdab.cmd.Uptime;
import me.vem.jdab.struct.menu.MenuListener;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.confirm.ConfirmationListener;
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
		if(instance == null) {
			if(token == null || token.isEmpty()) 
				throw new RuntimeException("Bot Token was null or empty. DiscordBot failed to load.");
			else instance = new DiscordBot(token);
		}
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

		jda.addEventListener(MenuListener.getInstance());
		jda.addEventListener(ConfirmationListener.getInstance());
		
		Help.initialize();
		Prefix.initialize();
		Uptime.initialize();
	}
	
	public void addEventListener(Object listener) {
		jda.addEventListener(listener);
	}
	
	public void removeEventListener(Object listener) {
		jda.removeEventListener(listener);
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
		
		Logger.info("Shutting down...");
		
		//Call all registered commands' unload function.
		Command.unloadAll();
		
		MenuListener.unload();
		ConfirmationListener.unload();
		
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
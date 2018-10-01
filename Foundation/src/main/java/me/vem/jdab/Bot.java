package me.vem.jdab;

import javax.security.auth.login.LoginException;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Help;
import me.vem.jdab.cmd.Prefix;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.IgnoredReference;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Version;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class Bot {

	/** The Bot Itself */
	private static JDA jda;
	public static JDA getJDA() { return jda; }
	
	public static void shutdown() {
		Console.shutdown();
		
		Logger.infof("%s is shutting down...", Version.getVersion());
		
		Respond.timerShutdown();
		
		Command.unloadAll();
		
		//jda.shutdown(); //TODO uncomment
	}
	
	public static void startup() {
		Console.buildConsole();
		
		try {
			//Look at this trash. Don't be like me.
			(jda = new JDABuilder(AccountType.BOT)
					.addEventListener(MessageListener.getInstance())
					.setToken(IgnoredReference.botToken)
					.build().awaitReady())
					.setAutoReconnect(true);
		}catch(LoginException | IllegalArgumentException | InterruptedException e) {
			e.printStackTrace();
		}
		
		//Command Registry
		Help.initialize();
		Prefix.initialize();
	}
	
	public static void restart() throws InterruptedException {
		shutdown();
		
		Thread.sleep(5000);
		
		startup();
	}
	
	public static void main(String... args) {
		Logger.info("Hello World!");
		
		startup();
	}
}

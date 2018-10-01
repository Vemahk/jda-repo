package me.vem.jdab;

import javax.security.auth.login.LoginException;

import me.vem.jdab.cmd.Help;
import me.vem.jdab.cmd.Prefix;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.IgnoredReference;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Version;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class Bot {

	/** The Bot Itself */
	private static JDA jda;
	public static JDA getJDA() { return jda; }
	
	public static void shutdown() {
		Logger.infof("%s is shutting down...", Version.getVersion());
		
		//Perform any save operation that may have to occur here.
		Prefix.getInstance().save();

		if(Console.hasConsole())
			Console.getConsole().dispose();		
		
		//jda.shutdown(); //TODO uncomment
		
		Console.destroyTray();
	}
	
	public static void main(String... args) {
		Logger.info("Hello World!");
		
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
}

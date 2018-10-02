package me.vem.dnd;
import java.io.IOException;

import me.vem.dnd.cmd.ClearOOC;
import me.vem.dnd.cmd.Command;
import me.vem.dnd.cmd.ExportChannel;
import me.vem.dnd.cmd.Help;
import me.vem.dnd.cmd.Jobs;
import me.vem.dnd.cmd.Meme;
import me.vem.dnd.cmd.Prefix;
import me.vem.dnd.utils.Console;
import me.vem.dnd.utils.IgnoredReference;
import me.vem.dnd.utils.Logger;
import me.vem.dnd.utils.Respond;
import me.vem.dnd.utils.Version;
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
		jda.shutdown();
	}
	
	public static void startup() {
		Console.buildConsole();
		
		try {
			(jda = new JDABuilder(AccountType.BOT)
					.addEventListener(MessageListener.getInstance())
					.setToken(IgnoredReference.botToken)
					.build().awaitReady())
					.setAutoReconnect(true);
		}catch (Exception e){
			e.printStackTrace();
		}
		
		Help.initialize();
		Prefix.initialize();
		
		ClearOOC.initialize();
		ExportChannel.initialize();
		Jobs.initialize();
		Meme.initialize();
	}
	
	public static void main(String[] args) throws IOException {
		Logger.info("Hello World!");
		startup();
	}
}

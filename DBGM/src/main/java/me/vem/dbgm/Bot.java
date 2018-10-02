package me.vem.dbgm;
import java.io.IOException;

import me.vem.dbgm.cmd.AntiPurge;
import me.vem.dbgm.cmd.Command;
import me.vem.dbgm.cmd.Help;
import me.vem.dbgm.cmd.PermissionHandler;
import me.vem.dbgm.cmd.Prefix;
import me.vem.dbgm.cmd.Purge;
import me.vem.dbgm.utils.Console;
import me.vem.dbgm.utils.IgnoredReference;
import me.vem.dbgm.utils.Logger;
import me.vem.dbgm.utils.Respond;
import me.vem.dbgm.utils.Version;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

/**
 * @author Vemahk
 * @JDAVersion 
 * 3.7.1_421
 */
public class Bot {
	
	/** The Bot Itself */
	private static JDA jda;
	public static JDA getJDA() { return jda; }
	
	//Close ALL the threads.
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
			jda = new JDABuilder(AccountType.BOT).addEventListener(MessageListener.getInstance()).setToken(IgnoredReference.botToken).build().awaitReady();
			jda.setAutoReconnect(true);	
		}catch (Exception e){
			e.printStackTrace();
		}
		
		Help.initialize();
		Prefix.initialize();
		Purge.initialize();
		AntiPurge.initialize();
		PermissionHandler.initialize();
	}
	
	public static void main(String[] args) throws IOException {
		Logger.infof("Hello World! From %s", Version.getVersion());
		startup();
	}
}

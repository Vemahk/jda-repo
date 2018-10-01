package me.vem.dnd;
import java.io.IOException;

import me.vem.dnd.cmd.ClearOOC;
import me.vem.dnd.cmd.ExportChannel;
import me.vem.dnd.cmd.Help;
import me.vem.dnd.cmd.Jobs;
import me.vem.dnd.cmd.Meme;
import me.vem.dnd.cmd.Move;
import me.vem.dnd.cmd.Prefix;
import me.vem.dnd.utils.Console;
import me.vem.dnd.utils.IgnoredReference;
import me.vem.dnd.utils.Logger;
import me.vem.dnd.utils.Version;
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
		ClearOOC.getInstance().save();
		Jobs.getInstance().save();
		Meme.getInstance().save();
		
		jda.shutdown();

		if(Console.hasConsole())
			Console.getConsole().dispose();		
		Console.destroyTray();
	}
	
	public static void start() {
		
	}
	
	public static void main(String[] args) throws IOException {
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
		Move.initialize();
	}
}

package me.vem.dbgm;
import java.io.IOException;

import me.vem.dbgm.cmd.AntiPurge;
import me.vem.dbgm.cmd.PermissionHandler;
import me.vem.dbgm.cmd.Purge;
import me.vem.jdab.DiscordBot;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Version;

/**
 * @author Vemahk
 * @JDAVersion 
 * 3.7.1_421
 */
public class DBGM {
	
	public static void main(String[] args) throws IOException {
		Version.initialize(0, 0, 1, 2, "DBGM Bot");
		Logger.infof("Hello World! From %s", Version.getVersion());
		Console.buildConsole();
		
		DiscordBot.initialize(IgnoredReference.botToken);
		
		Purge.initialize();
		AntiPurge.initialize();
		PermissionHandler.initialize();
	}
}

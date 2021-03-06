package me.vem.dbgm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import me.vem.dbgm.cmd.*;
import me.vem.dbgm.cmd.reaction.ReactionListener;
import me.vem.jdab.DiscordBot;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Version;

/**
 * Discord Bot General Manager
 * @author Samuel (a.k.a. Vemahk)
 * @JDAVersion 3.7.1_421
 */
public class DBGM {
	public static void main(String[] args) {
		Logger.setupFileLogging();
		Version.initialize(0, 0, 1, 9, "DBGM");
		Console.initialize();
		
		Logger.infof("Hello World! From %s", Version.getVersion());
		
		String tokenFile = args.length > 0 ? fetchToken(args[0]) : "token.txt";
		DiscordBot bot = DiscordBot.initialize(fetchToken(tokenFile));
		
		//Permissions is critical to the function of several other commands, so it must be initialized first.
		Permissions.initialize();

		//ReactionListener must be initialized after Permissions because it in turn inits its relevant commands, some of which require Permissions.
		bot.addEventListener(ReactionListener.getInstance());
		bot.addEventListener(Monitor.getInstance());
		
		//Normal Commands
		StreamTrack.initialize();
		Purge.initialize();
		AntiPurge.initialize();
		RoleOpt.initialize();
		Meme.initialize();
		ExportChannel.initialize();
		ForceSave.initialize();
		Stopwatch.initialize();
	}
	
	public static String fetchToken(String file) {
		FileReader fReader = ExtFileManager.getFileReader(new File(file));
		if(fReader == null) return null;
		try {
			BufferedReader reader = new BufferedReader(fReader);
			String out = reader.readLine();
			reader.close();
			return out;
		} catch (IOException e) {
			return null;
		}
	}
}

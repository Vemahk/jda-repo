package me.vem.dnd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import me.vem.dnd.cmd.ClearOOC;
import me.vem.dnd.cmd.DiceRoller;
import me.vem.dnd.cmd.ExportChannel;
import me.vem.dnd.cmd.Jobs;
import me.vem.dnd.cmd.Meme;
import me.vem.dnd.cmd.PollCMD;
import me.vem.dnd.cmd.RankHandling;
import me.vem.jdab.DiscordBot;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Version;

public class DNDBot {
	public static void main(String[] args) throws IOException {
		Logger.info("Hello World!");
		
		Version.initialize(0, 0, 0, 6, "DND Bot");
		Console.buildConsole();

		String tokenFile = args.length > 0 ? fetchToken(args[0]) : "token.txt";
		DiscordBot.initialize(fetchToken(tokenFile));
		
		ClearOOC.initialize();
		ExportChannel.initialize();
		Jobs.initialize();
		Meme.initialize();
		
		PollCMD.initialize();
		DiscordBot.getInstance().addEventListener(PollCMD.getInstance());
		DiscordBot.getInstance().addEventListener(RankHandling.getInstance());
		DiscordBot.getInstance().addEventListener(DiceRoller.getInstance());
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
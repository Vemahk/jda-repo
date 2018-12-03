package me.vem.dnd;

import static me.vem.dnd.IgnoredReference.botToken;

import java.io.IOException;

import me.vem.dnd.cmd.ClearOOC;
import me.vem.dnd.cmd.ExportChannel;
import me.vem.dnd.cmd.Jobs;
import me.vem.dnd.cmd.Meme;
import me.vem.dnd.cmd.vote.VoteCMD;
import me.vem.jdab.DiscordBot;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Version;

public class DNDBot {
	public static void main(String[] args) throws IOException {
		Logger.info("Hello World!");
		
		Version.initialize(0, 0, 0, 1, "DND Bot");
		Console.buildConsole();
		
		DiscordBot.initialize(botToken);
		
		ClearOOC.initialize();
		ExportChannel.initialize();
		Jobs.initialize();
		Meme.initialize();
		
		VoteCMD.initialize();
		DiscordBot.getInstance().addEventListener(VoteCMD.getInstance());
	}
}
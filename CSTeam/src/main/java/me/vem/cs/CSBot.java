package me.vem.cs;

import java.io.IOException;

import me.vem.cs.cmd.Contests;
import me.vem.cs.cmd.NextContest;
import me.vem.cs.cmd.SwearLog;
import me.vem.jdab.DiscordBot;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.Version;

public class CSBot {
	
	public static void main(String[] args) throws IOException {
		Version.initialize(0, 0, 0, 1, "CSTeam Bot");
		Console.buildConsole();
		
		DiscordBot.initialize(IgnoredReference.botToken);
		DiscordBot bot = DiscordBot.getInstance();
		
		bot.addEventListener(FilteredMessageListener.getInstance());

		Contests.initialize();
		NextContest.initialize();
		SwearLog.initialize();
	}
}

package me.vem.dbgm;

import static me.vem.dbgm.IgnoredReference.botToken;

import me.vem.dbgm.cmd.AntiPurge;
import me.vem.dbgm.cmd.ExportChannel;
import me.vem.dbgm.cmd.ForceSave;
import me.vem.dbgm.cmd.Meme;
import me.vem.dbgm.cmd.Permissions;
import me.vem.dbgm.cmd.Purge;
import me.vem.dbgm.cmd.RoleOpt;
import me.vem.dbgm.cmd.reaction.ReactionListener;
import me.vem.dbgm.cmd.stream.PresenceListener;
import me.vem.dbgm.requ.Request;
import me.vem.jdab.DiscordBot;
import me.vem.jdab.utils.Console;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Version;

/**
 * Discord Bot General Manager
 * @author Samuel (a.k.a. Vemahk)
 * @JDAVersion 3.7.1_421
 */
public class DBGM {
	public static void main(String[] args) {
		Version.initialize(0, 0, 1, 6, "DBGM Bot");
		Console.buildConsole();
		
		Logger.infof("Hello World! From %s", Version.getVersion());
		
		DiscordBot.initialize(botToken);
		DiscordBot.getInstance().setPreShutdown(() -> Request.shutdown());
		
		//Permissions is critical to the function of several other commands, so it must be initialized first.
		Permissions.initialize();

		//ReactionListener must be initialized after Permissions because it in turn inits its relevant commands, some of which require Permissions.
		DiscordBot.getInstance().addEventListener(ReactionListener.getInstance());
		
		//PresenseListener, like ReactionListener, must be initialized after Permissions because it inits StreamTrack, which is a SecureCommand.
		DiscordBot.getInstance().addEventListener(PresenceListener.getInstance());
		
		//Normal Commands
		Purge.initialize();
		AntiPurge.initialize();
		RoleOpt.initialize();
		Meme.initialize();
		ExportChannel.initialize();
		ForceSave.initialize();
	}
}

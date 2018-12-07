package me.vem.dbgm.cmd.stream;

import java.util.Arrays;
import java.util.List;

import me.vem.dbgm.cmd.Permissions;
import me.vem.dbgm.cmd.SecureCommand;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class StreamTrack extends SecureCommand{

	private static StreamTrack instance;
	public static StreamTrack getInstance() {
		return instance;
	}
	
	public static void initialize() {
		if(instance == null)
			instance = new StreamTrack();
	}
	
	private StreamTrack() { super("stream"); }

	@Override public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0)
			return sendHelp(event.getChannel(), true);
		
		PresenceListener listener = PresenceListener.getInstance();
		
		if("add".equals(args[0])) {
			if(args.length == 1)
				return sendHelp(event.getChannel(), false);
			
			Member mentioned = Utilities.getMemberFromMention(event.getGuild(), args[1]);
			if(mentioned == null)
				return sendHelp(event.getChannel(), false);
			
			if(listener.getData(event.getGuild()).track(mentioned.getUser()))
				Respond.asyncf(event.getChannel(), "%s is now being tracked for twitch streaming.", mentioned.getEffectiveName());
			else Respond.asyncf(event.getChannel(), "%s was already being tracked for twitch streaming.", mentioned.getEffectiveName());
		}else if("remove".equals(args[0])) {
			if(args.length == 1)
				return sendHelp(event.getChannel(), false);
			
			Member mentioned = Utilities.getMemberFromMention(event.getGuild(), args[1]);
			if(mentioned == null)
				return sendHelp(event.getChannel(), false);
			
			if(listener.getData(event.getGuild()).untrack(mentioned.getUser()))
				Respond.asyncf(event.getChannel(), "%s removed", mentioned.getEffectiveName());
			else Respond.asyncf(event.getChannel(), "%s was not being tracked.", mentioned.getEffectiveName());
		}else if("channel".equals(args[0])) {
			if(args.length == 1) {
				listener.getData(event.getGuild()).setResponseChannel(event.getChannel());
				Respond.async(event.getChannel(), "Streamers will now be mentioned in this channel!");
			}else {
				TextChannel target = Utilities.getTextChannelFromMention(event.getGuild(), args[1]);
				if(target == null)
					return sendHelp(event.getChannel(), false);
				
				listener.getData(event.getGuild()).setResponseChannel(target);
				Respond.asyncf(event.getChannel(), "Streamers will now be mentioned in the %s channel!", args[1]);
			}
		}else if("response".equals(args[0])) {
			if(args.length == 1) {
				Respond.asyncf(event.getChannel(), "The current response is set to: `%s`", listener.getData(event.getGuild()).getResponse());
			}else {
				listener.getData(event.getGuild()).setMessage(args[1]);
				Respond.asyncf(event.getChannel(), "Set the bot's response to: `%s`", args[1]);
			}
		}else return sendHelp(event.getChannel(), true);
		
		return true;
	}
	
	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("stream.setchannel", "stream.adduser", "stream.setresponse", "stream.removeuser");
	}

	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		if(args.length == 0) return true;
		
		String key = null;
		
		if("add".equals(args[0]))
			key = "stream.adduser";
		else if("remove".equals(args[0])) 
			key = "stream.removeuser";
		else if("channel".equals(args[0]))
			key =  "stream.setchannel";
		else if("response".equals(args[0]))
			key = "stream.setresponse";
		else return true;
		
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), key);
	}

	@Override
	public String[] usages() {
		return new String[] {
			"`stream add <@user>` -- Adds a mentioned user to the list of streamers.",
			"`stream remove <@user>` -- Removes a mentioned user from the list of streamers.",
			"`stream channel [#channel]` -- Sets this channel (or a mentioned one) to be the channel the bot mention the streamer in.",
			"`stream response \\`<response message>\\`` -- Sets the text the bot will send when a streamer starts to stream.",
			" - `%user%` will be replaced with the streamer's mention.",
			" - `%url%` will be replaced with the twitch url of the streamer."
		};
	}

	@Override protected void unload() {
		instance = null;
	}

	@Override
	public String getDescription() {
		return "Monitors specific users for if their Discord Presense switches to \"Streaming...\", under which case it will send a message to a particular channel.";
	}
}
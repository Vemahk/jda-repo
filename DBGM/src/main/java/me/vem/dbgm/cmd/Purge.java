package me.vem.dbgm.cmd;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import me.vem.jdab.struct.MessagePurge;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Purge extends SecureCommand{

	private static Purge instance;
	public static Purge getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new Purge();
	}
	
	private Purge() { super("purge"); }
	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) { //Purge 100 anyone.
			purge(event.getChannel(), 101); //101 includes the purge message.
			return true;
		}
		
		int n = 100;
		//Try the last arg for a valid number	
		try { n = Integer.parseInt(args[args.length-1]); } catch(NumberFormatException e) {}
		
		//Try the last two args for a user mention.
		Member mem = Utilities.getMemberFromMention(event.getGuild(), args[args.length-1]);
		if(mem == null && args.length >= 2)
			mem = Utilities.getMemberFromMention(event.getGuild(), args[args.length-2]);
		
		if(!"regex".equalsIgnoreCase(args[0]))
			if(mem == null)
				purge(event.getChannel(), n);
			else purge(event.getChannel(), n, mem);
		else {
			if(args.length == 1 || args.length > 4) 
				return sendHelp(event.getChannel(), false);
			if(mem == null)
				purge(event.getChannel(), n, args[1]);
			else purge(event.getChannel(), n, args[1], mem);
		}
		return true;
	}

	private void purge(TextChannel channel, int lastn, Member... members) {
		purge(channel, lastn, "", members);
	}
	
	private void purge(TextChannel channel, int lastn, @NotNull String regex, Member... members) {
		Pattern pattern = regex.isEmpty() ? null : Pattern.compile(regex);
		
		MessagePurge.purge(channel, lastn, (msg) -> {
			/********************************************************************************\
			|* AntiPurge -- Hardcoded.														*|
			|* These messages cannot be purged, no matter what. Note that only messages		*|
			|* sent by the bot with [AntiPurge] will resist the purge. Users' [AntiPurge]	*|
			|* messages will still be purged. The bot will only send an [AntiPurge]			*|
			|* message if the command is called by a allowable user. See the AntiPurge		*|
			|* command for more details. 													*|
			\********************************************************************************/
			if(msg.getAuthor().equals(msg.getJDA().getSelfUser()) && msg.getContentDisplay().startsWith("[AntiPurge]"))
				return false;
			
			//If there is a pattern and it does not match it, return false.
			if(pattern != null && !pattern.matcher(msg.getContentDisplay()).matches())
				return false;
			
			if(members.length > 0) {
				for(Member mem : members)
					if(msg.getMember().equals(mem))
						return true;
				return false;
			}
			
			return true;
		});
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return Permissions.getInstance().hasPermissionsFor(event.getMember(), "purge");
	}
	
	@Override
	public String[] usages() {
		return new String[] {
			"`purge [@user] [num=100]`",
			" - Purges the last `num` messages of any specified person, or of every person if none is specified.",
			"``purge regex `<regex>` [@user] [num=100]``",
			" - Purges messages matching the given regex of a specific (or any) person. Searches the last `num` messages. Regex follow JAVA format.",
			" - Example: purge regex `.\\d{1,4}\\S+ blah` 50 --> Would scan the last 50 messages for something that matched that regex crap.",
			" - For regex information, refer here: https://regexr.com/"
		};
	}
	
	@Override
	protected void unload() {
		instance = null;
	}
	@Override
	public List<String> getValidKeySet() {
		return Arrays.asList("purge");
	}
	@Override
	public String getDescription() {
		return "Deletes a number of messages from a specific user/from any user/that matches a regex.";
	}
}
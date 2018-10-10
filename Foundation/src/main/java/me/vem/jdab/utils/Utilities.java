package me.vem.jdab.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.vem.jdab.DiscordBot;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

public class Utilities {

	private static Pattern memberMentionPattern = Pattern.compile("<?@?(\\d+)>?");
	private static Pattern roleMentionPattern = Pattern.compile("<?@?&?(\\d+)>?");
	
	/**
	 * @param mention (e.g. <@##############>)
	 * @return null if the mention is not in the correct format.<br>
	 * null if the user is not found.<br>
	 * The user if it is a valid mention.
	 */
	public static User getUserFromMention(String mention) {
		Matcher matcher = memberMentionPattern.matcher(mention);
		if(!matcher.matches())
			return null;
		
		return DiscordBot.getInstance().getJDA().getUserById(matcher.group(1));
	}
	
	/**
	 * @param guild The guild associated with the user's membership.
	 * @param mention (e.g. <@##############>)
	 * @return null if the mention is not in the correct format.<br>
	 * null if the user is not found.<br>
	 * The member if it is a valid mention and they are apart of the given guild.
	 */
	public static Member getMemberFromMention(Guild guild, String mention) {
		User u = getUserFromMention(mention);
		if(u == null) return null;
		return guild.getMember(u);
	}
	
	/**
	 * @param guild The guild associated with this role.
	 * @param mention (e.g. <@&#############>)
	 * @return null if the mention is not in the correct format.<br>
	 * null if the role is not found.<br>
	 * The role if the mention is valid and the role is apart of this guild.
	 */
	public static Role getRoleFromMention(Guild guild, String mention) {
		Matcher matcher = roleMentionPattern.matcher(mention);
		if(!matcher.matches())
			return null;
		
		return guild.getRoleById(matcher.group(1));
	}
}

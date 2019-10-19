package me.vem.jdab.cmd;

import java.util.concurrent.TimeUnit;

import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class Uptime extends Command{

	private static Uptime instance;
	public static Uptime getInstance() {
		return instance;
	}
	
	public static void initialize() {
		if(instance == null)
			instance = new Uptime();
	}
	
	private long startTime;
	
	private Uptime() {
		super("uptime");
		
		startTime = System.currentTimeMillis();
	}

	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		Respond.async(event.getChannel(), formatTime(System.currentTimeMillis() - startTime));
		
		return true;
	}
	
	private String formatTime(long millis) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
		long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
		long days = TimeUnit.MILLISECONDS.toDays(millis);
		
		boolean display_day = days > 0;
		boolean display_hour = hours > 0;
		boolean display_minute = minutes > 0;
		boolean display_second = seconds > 0;
		
		boolean plural_day = days > 1;
		boolean plural_hour = hours > 1;
		boolean plural_minute = minutes > 1;
		boolean plural_second = seconds > 1;
		
		StringBuilder str = new StringBuilder("This bot has been online for ");
		
		if(display_day){
			str.append(days).append(" day");
			if(plural_day) 
				str.append('s');
		}
		
		if(display_hour) {
			if(display_day)
				if(!(display_minute || display_second))
					str.append(" and ");
				else str.append(", ");
			str.append(hours).append(" hour");
			if(plural_hour)
				str.append('s');
		}
		
		if(display_minute) {
			if(display_day || display_hour) {
				if(display_second || display_day && display_hour)
					str.append(',');
				
				str.append(' ');
				
				if(!display_second)
					str.append("and ");
			}
			
			str.append(minutes).append(" minute");
			if(plural_minute)
				str.append('s');
		}
		
		if(display_second) {
			int count_before = count(display_day, display_hour, display_minute);
			
			if(count_before > 0) {
				if(count_before != 1)
					str.append(',');
				str.append(" and ");
			}
			
			str.append(seconds).append(" second");
			if(plural_second)
				str.append('s');
		}
		
		return str.toString();
	}
	
	private int count(boolean... bools) {
		int out = 0;
		for(boolean b : bools)
			if(b) out++;
		return out;
	}
	
	@Override
	public String[] usages() {
		return new String[] {"`uptime` -- Displays the amount of time that the bot has been up for."};
	}

	@Override
	public String getDescription() {
		return "Prints the time that the bot has been up.";
	}

	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return true;
	}

	@Override
	protected void unload() {
		instance = null;
	}
}
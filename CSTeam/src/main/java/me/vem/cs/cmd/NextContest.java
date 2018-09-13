package me.vem.cs.cmd;

import java.util.Calendar;

import me.vem.cs.Bot;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * Prints out the next contest, determined by date, and its information.
 * @author Vemahk
 */
public class NextContest extends Command{
	
	private static NextContest instance;
	public static NextContest getInstance() { return instance; }
	public static void initialize() {
		if(instance == null)
			instance = new NextContest();
	}
	
	private NextContest() { super("nextcontest"); }

	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(Contests.getInstance().getNumberOfContests() == 0) {
			Bot.respondAsync(event, "There are no contests listed currently... That doesn't sound quite right.");
			return true;
		}
		
		Contest nextContest = getNextContest();
		
		if(nextContest != null)
			Bot.respondAsync(event, "Next contest:\n"+nextContest.formatOut());
		else Bot.respondAsync(event, "There is no contest listed for after today. Try again later?");
		
		return true;
	}
	
	public Contest getNextContest() {
		Calendar cal = Calendar.getInstance();
		int curMonth = cal.get(Calendar.MONTH);
		int curDay = cal.get(Calendar.DAY_OF_MONTH); 
		int curYear = cal.get(Calendar.YEAR);
		
		for(Contest c : Contests.getInstance().getContestSet())
			if(c.isAfter(curMonth, curDay, curYear))
				return c;
		
		return null;
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return true; //Everyone can use this.
	}

	@Override
	public String help() {
		return "Usage: `nextcontest`";
	}
}


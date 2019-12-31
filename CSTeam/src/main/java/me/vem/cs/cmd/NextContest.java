package me.vem.cs.cmd;

import java.util.Calendar;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(Contests.getInstance().getNumberOfContests() == 0) {
			Respond.async(event.getChannel(), "There are no contests listed currently... That doesn't sound quite right.");
			return true;
		}
		
		Contest nextContest = getNextContest();
		
		if(nextContest != null)
			Respond.async(event.getChannel(), "Next contest:\n"+nextContest.formatOut());
		else Respond.async(event.getChannel(), "There is no contest listed for after today. Try again later?");
		
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
	public boolean hasPermissions(Member member, String... args) {
		return true; //Everyone can use this.
	}

	@Override
	public String[] usages() {
		return new String[] {
			"`nextcontest`"
		};
	}
	
	@Override
	protected void unload() {
		instance = null;
	}
	@Override
	public String getDescription() {
		return "Prints the time, date, and location of the next known contest.";
	}
}


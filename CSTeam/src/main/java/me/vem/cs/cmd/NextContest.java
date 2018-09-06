package me.vem.cs.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Scanner;
import java.util.TreeSet;

import me.vem.cs.Main;
import me.vem.cs.Main.TextFormat;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * Prints out the next contest, determined by date, and its information.
 * @author Vemahk
 */
public class NextContest implements Command {
	
	/**
	 * The list of all contests. Saved to and loaded from contests.dat
	 */
	private TreeSet<Contest> contests;
	
	public NextContest() {
		loadData();
	}

	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		if(contests.size() == 0) {
			Main.respond("There are no contests listed currently... That doesn't sound quite right.", event);
			return;
		}
		
		Calendar cal = Calendar.getInstance();
		int curMonth = cal.get(Calendar.MONTH) + 1; //Current month, Jan = 1
		int curDay = cal.get(Calendar.DAY_OF_MONTH); 
		int curYear = cal.get(Calendar.YEAR);
		
		Contest nextContest = null;
		for(Contest c : contests) { //TreeSet iterator is in-order by date, .: the first to be after today is the next one.
			if(c.isAfter(curMonth, curDay, curYear)) {
				nextContest = c;
				break;
			}
		}
		
		if(nextContest != null)
			Main.respond("Next contest:\n"+nextContest.formatOut(), event);
		else Main.respond("There is no contest listed for after today. Try again later?", event);
	}

	/**
	 * Adds Contest c to the contests set and saves it to the file.
	 * @param c
	 */
	public void addContest(Contest c) {
		contests.add(c);
		saveData();
	}
	
	/**
	 * @param name
	 * @return The contest with name 'name'.
	 */
	public Contest getContest(String name) {
		for(Contest c : contests)
			if(c.getName().equalsIgnoreCase(name)) return c;
		return null;
	}
	
	/**
	 * @return A code-formatted list of all the known contests' names, in order.
	 */
	public String getNameList() {
		String out = "";
		for(Contest c : contests) out += c.getName() + "\n";
		return Main.format(out.trim(), TextFormat.CODE);
	}
	
	/**
	 * Saves the contests treeset to contests.dat
	 */
	public void saveData() {
		File f = new File("contests.dat");
		try {
			if(f.exists()) f.delete();
			f.createNewFile();
			
			PrintWriter writer = new PrintWriter(f);
			
			for(Contest c : contests)
				writer.println(c.saveFormat());
			
			writer.flush();
			writer.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads contest info from the file.
	 */
	private void loadData() {
		contests = new TreeSet<>();
		try {
			File f = new File("contests.dat");
			if(!f.exists()) return;
			Scanner file = new Scanner(f);
			
			while(file.hasNextLine())
				contests.add(Contest.loadFrom(file.nextLine()));
			
			file.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return true; //Everyone can use this.
	}

	@Override
	public String help() {
		return "Usage: ~!nextcontest";
	}
}

/**
 * Contest object. Simply an information holder, mostly.
 * @author Vemahk
 */
class Contest implements Comparable<Contest> {

	/**
	 * Loads a contest object from a string. Formatted 'name--date--start--end--location--address'.
	 * @param s
	 * @return
	 */
	public static Contest loadFrom(String s) {
		Scanner sc = new Scanner(s);
		sc.useDelimiter("--"); //Delimiters make everything easier.
		Contest out = new Contest(sc.next(), sc.next(), sc.next(), sc.next(), sc.next(), sc.next());
		sc.close();
		return out;
	}
	
	private String name;
	private String date;
	private int month;
	private int day;
	private int year;
	
	private String startTime;
	private String endTime;
	private String location;
	private String address;
	
	public Contest(String name, String date, String start, String end, String loc, String add) {
		setDate(date);
		this.name = name;
		startTime = start;
		endTime = end;
		location = loc;
		address = add;
	}
	
	//Setters
	public void setName(String s) { name = s; }
	public void setDate(String s) {
		Scanner test = new Scanner(s);
		test.useDelimiter(",? ");
		month = "JanFebMarAprMayJunJulAugSepOctNovDec".indexOf(test.next())/3 + 1;
		day = test.nextInt();
		year = test.nextInt();
		test.close();
		
		date = s;
	}
	public void setStartTime(String s) { startTime = s; }
	public void setEndTime(String s) { endTime = s; }
	public void setLocation(String s) { location = s; }
	public void setAddress(String s) { address = s; } 
	
	//Ye 'ole getters.
	public String getName() { return name; }
	public String getDate() { return date; }
	public String getStartTime() { return startTime; }
	public String getEndTime() { return endTime; }
	public String getLocation() { return location; }
	public String getAddress() { return address; }

	//Checks to see if the given date (in months, days-of-month, and years) is before the date held by the contest object.
	public boolean isAfter(int m, int d, int y) {
		if(year > y) return true;
		if(month > m) return true;
		if(day > d) return true;
		return false;
	}
	
	/**
	 * Format for ~!nextcontest.
	 * @return
	 */
	public String formatOut() {
		String out = String.format("- Name: %s%n"
								 + "- Date: %s%n"
								 + "- Depature Time: %s%n"
								 + "- Projected Return Time: %s%n"
								 + "- Location: %s%n"
								 + "- Address: %s", name, date, startTime, endTime, location, address);
		
		return Main.format(out, TextFormat.CODE);
	}

	/**
	 * Converts info to a string for saving to a file.
	 * @return
	 */
	public String saveFormat() {
		return name+"--"+date+"--"+startTime+"--"+endTime+"--"+location+"--"+address;
	}
	
	@Override
	public int compareTo(Contest o) {
		if(year != o.year) return year - o.year;
		if(month != o.month) return month - o.month;
		if(day != o.day) return day - o.day;
		return 0;
	}
}
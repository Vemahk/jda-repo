package me.vem.cs.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 * This command allows for the creation/editing of contests. Removing is currently not a functionality.
 * @author Vemahk
 */
public class Contests extends Command implements Configurable{
	
	private static Contests instance;
	public static Contests getInstance() { return instance; }
	public static void initialize() {
		if(instance != null) return;
		instance = new Contests();
	}

	/**
	 * The list of all contests. Saved to and loaded from contests.dat
	 */
	private TreeSet<Contest> contests;
	
	private Contests() {
		super("contests");
		load();
	}
	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) 
			return sendHelp(event.getChannel(), true);
		else if(args.length==1) {
			//The real help menu.
			String vars = "\n```- date [required]\n"
					+ "- name [required]\n"
					+ "- expectedLeaveTime\n"
					+ "- expectedReturnTime\n"
					+ "- location\n"
					+ "- locationAddress```\n";
			if(args[0].equals("add"))
				Respond.async(event.getChannel(), "Recognized variables:" + vars
									  + "Example: contests add date=\\`Oct 14, 2017\\` name=\\`ffb\\` expectedLeaveTime=\\`7:30AM\\` expectedReturnTime=\\`1:00PM\\` location=\\`Frisco Libery High School\\` locationAddress=\\`15250 Rolater Rd, Frisco, TX 75035\\`");
			else if(args[0].equals("edit"))
				Respond.async(event.getChannel(), "List of all known contests:\n"+getNameList()	
										+"\nRecognized variables:\n"+vars);
			return true;
		}
		
		if(args[0].equals("add")) {
			String name = getVar(args, "name");
			String date = getVar(args, "date");
			
			if(name == null || date == null) {
				Respond.async(event.getChannel(), "Missing required fields: 'date' and/or 'name'.");
				return false;
			}
			
			String start = getVar(args, "expectedLeaveTime");
			String end = getVar(args, "expectedReturnTime");
			String loc = getVar(args, "location");
			String add = getVar(args, "locationAddress");
			contests.add(new Contest(name, date, start, end, loc, add));
		}else if(args[0].equals("edit")) {
			String cName = args[1];
			
			Contest c = getContestFromString(cName);
			if(c == null) {
				Respond.async(event.getChannel(), "Unknown contest, '"+cName+"'. List of all known contests:\n"+getNameList());
				return false;
			}
			
			c.updateName(getVar(args, "name"));
			c.updateDate(getVar(args, "date"));
			c.updateStartTime(getVar(args, "expectedLeaveTime"));
			c.updateEndTime(getVar(args, "expectedReturnTime"));
			c.updateLocation(getVar(args, "location"));
			c.updateAddress(getVar(args, "locationAddress"));

			contests.remove(c);
			contests.add(c);
			
			Respond.async(event.getChannel(), "Changes made:\n"+c.formatOut());
		}
		
		return true;
	}
	
	public Set<Contest> getContestSet(){ return contests; }
	public int getNumberOfContests() { return contests.size(); }
	
	private Contest getContestFromString(String str) {
		for(Contest c : contests)
			if(c.getName().equals(str))
				return c;
		return null;
	}
	
	/**
	 * @return A code-formatted list of all the known contests' names, in order.
	 */
	private String getNameList() {
		StringBuilder out = new StringBuilder("```\n");
		for(Contest c : contests)
			out.append(c.getName()).append('\n');
		return out.append("```").toString();
	}
	
	/**
	 * Searches a string for 'var="words go here"'.
	 * @param args
	 * @param var
	 * @return Returns whatever it found, if anything, in quotaions after var=.
	 */
	private String getVar(String[] args, String var) {
		for(String arg : args)
			if(arg.startsWith(var))
				return arg.substring(arg.indexOf('=') + 1);
		return null;
	}

	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String[] usages() {
		return new String[] {
			"`contests add <vars...>`",
			"`contests edit <contestname> <vars...>"
		};
	}
	
	@Override
	public void save() {
		try (PrintWriter writer = ExtFileManager.getConfigOutput("contests.json")){
			writer.print(ExtFileManager.getGsonPretty().toJson(contests));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void load() {
		contests = new TreeSet<>();
		
		File config = ExtFileManager.getConfigFile("contests.json");
		if(config == null) return;
		
		String json = ExtFileManager.readFileAsString(config);
		if(json == null) return;
		
		contests = ExtFileManager.getGsonPretty().fromJson(json, new TypeToken<TreeSet<Contest>>() {}.getType());
	}
	@Override
	protected void unload() {
		save();
		instance = null;
	}
	@Override
	public String getDescription() {
		return "Stores contest information.";
	}
}

/**
 * Contest object. Simply an information holder, mostly.
 * @author Vemahk
 */
class Contest implements Comparable<Contest> {
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
		updateDate(date);
		this.name = name;
		startTime = start;
		endTime = end;
		location = loc;
		address = add;
	}
	
	//Setters
	public void updateDate(String s) {
		if(s == null) return;
		
		Scanner test = new Scanner(s);
		test.useDelimiter(",? ");
		month = "JanFebMarAprMayJunJulAugSepOctNovDec".indexOf(test.next())/3;
		day = test.nextInt();
		year = test.nextInt();
		test.close();
		
		date = s;
	}

	public void updateName(String s) {
		if(s==null) return;
		name = s;
	}
	
	public void updateStartTime(String s) {
		if(s==null) return;
		startTime = s;
	}
	
	public void updateEndTime(String s) {
		if(s==null) return;
		endTime = s;
	}
	
	public void updateLocation(String s) {
		if(s==null) return;
		location = s;
	}
	
	public void updateAddress(String s) {
		if(s==null) return;
		address = s;
	} 
	
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
	 * Format for nextcontest.
	 * @return
	 */
	public String formatOut() {
		return String.format("```%n"
						   + "- Name: %s%n"
						   + "- Date: %s%n"
						   + "- Depature Time: %s%n"
						   + "- Projected Return Time: %s%n"
						   + "- Location: %s%n"
						   + "- Address: %s%n```", name, date, startTime, endTime, location, address);
	}
	
	@Override
	public int compareTo(Contest o) {
		if(year != o.year) return year - o.year;
		if(month != o.month) return month - o.month;
		if(day != o.day) return day - o.day;
		return 0;
	}
}
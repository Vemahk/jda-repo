package me.vem.cs.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.reflect.TypeToken;

import me.vem.cs.Bot;
import me.vem.cs.Bot.TextFormat;
import me.vem.cs.utils.ExtFileManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) {
			getHelp(event);
			return true;
		} else if(args.length==1) {
			//The real help menu.
			String vars = "\n```- date [required]\n"
					+ "- name [required]\n"
					+ "- expectedLeaveTime\n"
					+ "- expectedReturnTime\n"
					+ "- location\n"
					+ "- locationAddress```\n";
			if(args[0].equals("add"))
				Bot.respondAsync(event, "Recognized variables:" + vars
									  + "Example: contests add date=\"Oct 14, 2017\" name=\"ffb\" expectedLeaveTime=\"7:30AM\" expectedReturnTime=\"1:00PM\" location=\"Frisco Libery High School\" locationAddress=\"15250 Rolater Rd, Frisco, TX 75035\"");
			else if(args[0].equals("edit"))
				Bot.respondAsync(event, "List of all known contests:\n"+getNameList()	
										+"\nRecognized variables:\n"+vars);
			return true;
		}
		
		if(args[0].equals("add")) {
			String nargs = "";
			for(int i=1;i<args.length;i++)
				nargs += args[i] + (i + 1 < args.length ? " " : "");

			String name = getVar(nargs, "name");
			String date = getVar(nargs, "date");
			
			if(date.equals("Unknown") || name.equals("Unknown")) {
				Bot.respondAsync(event, "Missing required fields: 'date' and/or 'name'.");
				return false;
			}
			
			String start = getVar(nargs, "expectedLeaveTime");
			String end = getVar(nargs, "expectedReturnTime");
			String loc = getVar(nargs, "location");
			String add = getVar(nargs, "locationAddress");
			contests.add(new Contest(name, date, start, end, loc, add));
		}else if(args[0].equals("edit")) {
			int ind = 2;
			String cName = args[1];
			if(cName.startsWith("\"")) {
				if(!cName.endsWith("\"")) {
					cName+=" ";
					while(!cName.matches(".*\"\\s?$"))
						cName += args[ind++] + " ";
					cName = cName.trim();
				}
				cName = cName.substring(1, cName.length()-1);
			}
			
			Contest c = getContestFromString(cName);
			if(c == null) {
				Bot.respondAsync(event, "Unknown contest, '"+cName+"'. List of all known contests:\n"+getNameList());
				return false;
			}
			
			String rest = "";
			for(;ind<args.length;ind++)
				rest += args[ind] + (ind + 1 < args.length ? " " : "");
			
			String name = getVar(rest, "name");
			if(!name.equals("Unknown")) c.setName(name);
			
			String date = getVar(rest, "date");
			if(!date.equals("Unknown"))
				Bot.respondAsync(event, "Due to sorting reasons in the TreeSet, the date cannot be safely changed. Sorry.");
			
			String start = getVar(rest, "expectedLeaveTime");
			if(!start.equals("Unknown")) c.setStartTime(start);
			
			String end = getVar(rest, "expectedReturnTime");
			if(!end.equals("Unknown")) c.setEndTime(end);
			
			String loc = getVar(rest, "location");
			if(!loc.equals("Unknown")) c.setLocation(loc);
			
			String add = getVar(rest, "locationAddress");
			if(!loc.equals("Unknown")) c.setAddress(add);
			
			Bot.respondAsync(event, "Changes made:\n"+c.formatOut());
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
	 * @param s
	 * @param var
	 * @return Returns whatever it found, if anything, in quotaions after var=.
	 */
	private String getVar(String s, String var) {
		if(!s.contains(var+"=")) return "Unknown";
		int ind = s.indexOf(var) + var.length() + 2;
		return s.substring(ind, s.indexOf("\"", ind));
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help() {
		return "Usage:\n"
			 + "`contests add <vars...>`\n"
			 + "`contests edit <contestname> <vars...>`";
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
		
		return TextFormat.CODE.apply(out);
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
package me.vem.cs.cmd;

import me.vem.cs.Main;
import me.vem.cs.Main.TextFormat;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * This command allows for the creation/editing of contests. Removing is currently not a functionality.
 * @author Vemahk
 */
public class Contests implements Command{
	
	private NextContest nc;
	public Contests() {
		//MUST BE REGISTERED AFTER NEXT CONTEST
		nc = (NextContest)Main.commands.get("nextcontest");
	}
	
	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		if(args.length == 0) {
			Main.respond("Usage:\n```~!contests add <vars...>\n~!contests edit <contestname> <vars...>```", event);
			return;
		} else if(args.length==1) {
			//The real help menu.
			String vars = Main.format("- date [required]\n"
					+ "- name [required]\n"
					+ "- expectedLeaveTime\n"
					+ "- expectedReturnTime\n"
					+ "- location\n"
					+ "- locationAddress\n", TextFormat.CODE);
			if(args[0].equals("add"))
				Main.respond("Recognized variables:\n"+vars
							+"Example: ~!contests add date=\"Oct 14, 2017\" name=\"ffb\" expectedLeaveTime=\"7:30AM\" expectedReturnTime=\"1:00PM\" location=\"Frisco Libery High School\" locationAddress=\"15250 Rolater Rd, Frisco, TX 75035\"", event);
			else if(args[0].equals("edit"))
				Main.respond("List of all known contests:\n"+nc.getNameList()	
							+"\nRecognized variables:\n"+vars, event);
			return;
		}
		
		if(args[0].equals("add")) {
			String nargs = "";
			for(int i=1;i<args.length;i++)
				nargs += args[i] + (i + 1 < args.length ? " " : "");

			String name = getVar(nargs, "name");
			String date = getVar(nargs, "date");
			
			if(date.equals("Unknown") || name.equals("Unknown")) {
				Main.respond("Missing required fields: 'date' and/or 'name'.", event);
				return;
			}
			
			String start = getVar(nargs, "expectedLeaveTime");
			String end = getVar(nargs, "expectedReturnTime");
			String loc = getVar(nargs, "location");
			String add = getVar(nargs, "locationAddress");
			nc.addContest(new Contest(name, date, start, end, loc, add));
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
			
			Contest c = nc.getContest(cName);
			if(c == null) {
				Main.respond("Unknown contest, '"+cName+"'. List of all known contests:\n"+nc.getNameList(), event);
				return;
			}
			
			String rest = "";
			for(;ind<args.length;ind++)
				rest += args[ind] + (ind + 1 < args.length ? " " : "");
			
			String name = getVar(rest, "name");
			if(!name.equals("Unknown")) c.setName(name);
			
			String date = getVar(rest, "date");
			if(!date.equals("Unknown"))
				Main.respond("Due to sorting reasons in the TreeSet, the date cannot be safely changed. Sorry.", event);
			
			String start = getVar(rest, "expectedLeaveTime");
			if(!start.equals("Unknown")) c.setStartTime(start);
			
			String end = getVar(rest, "expectedReturnTime");
			if(!end.equals("Unknown")) c.setEndTime(end);
			
			String loc = getVar(rest, "location");
			if(!loc.equals("Unknown")) c.setLocation(loc);
			
			String add = getVar(rest, "locationAddress");
			if(!loc.equals("Unknown")) c.setAddress(add);
			
			Main.respond("Changes made:\n"+c.formatOut(), event);
			nc.saveData();
		}
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
		//Only admins can use this command.
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help() {
		return Main.format("This command can only be used by admins (e.g. Mrs. Ford, Samuel, Hudson, or others depending.)", TextFormat.ALL);
	}
}

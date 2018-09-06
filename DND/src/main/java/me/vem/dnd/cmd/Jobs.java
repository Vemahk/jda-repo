package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import me.vem.dnd.Main;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Jobs implements Command{
	
	private HashMap<String, ArrayList<Job>> map;
	
	public Jobs() {
		map = new HashMap<>();
		loadSettings();
	}

	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		if(args.length == 0) {
			Main.respondTimeout("Usage: ~jobs <cityname>.", 5, event);
			return;
		}
		
		String city = args[0];
		if(city.equals("add")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Main.respondTimeout("Only the GM can add job listings.", 5, event);
				return;
			}
			
			if(args.length < 4) {
				Main.respondTimeout("Not enough arguments..", 5, event);
				return;
			}
			
			String trueCity = casify(args[1]);
			String key = args[2];
			String desc = "";
			for(int i=3;i<args.length;)
				desc += args[i] + (++i<args.length ? " " : "");
			
			Job j = new Job(trueCity, desc, key);
			addJob(trueCity, j);
			
			saveSettings();
			Main.respondTimeout("Job added to "+trueCity+"!", 5, event);
			
		}else if(city.equals("remove")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Main.respondTimeout("Only the GM can remove job listings.", 5, event);
				return;
			}
			
			if(args.length < 3) {
				Main.respondTimeout("Not enough arguments..", 5, event);
				return;
			}
			
			String trueCity = casify(args[1]);
			String key = args[2];
			
			if(map.containsKey(trueCity)) {
				ArrayList<Job> jobs = map.get(trueCity);
				Job target = null;
				for(Job j : map.get(trueCity)) {
					if(j.getKey().equals(key)) {
						target = j;
						break;
					}
				}
				
				if(target==null) Main.respondTimeout("Job not found by key "+key, 5, event);
				else{
					jobs.remove(target);
					if(jobs.size() == 0) map.remove(trueCity);
					Main.respondTimeout("Job '"+key+"' removed!", 5, event);
					saveSettings();
				}
			}else Main.respondTimeout("No such city.", 5, event);
		
		}else if(city.equals("cities")){
			String rsp = "";
			for(String s : map.keySet()) rsp+=s+"\n";
			Main.respondTimeout("Known Cities:\n"+rsp, 10, event);
		}else{
			city = casify(city);
			
			if(args.length > 1 && args[1].equals("getkeys"))
				if(event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					if(map.containsKey(city)) {
						String rsp = "";
						for(Job j : map.get(city)) rsp += j.getKey()+"\n";
						Main.respondTimeout(city+" Job Keys: \n"+rsp, 10, event);
						return;
					}else{
						Main.respondTimeout("No such city.", 5, event);
						return;
					}
				}else{
					Main.respondTimeout("Only the GM can get the keys of the jobs.", 5, event);
					return;
				}
			
			if(map.containsKey(city)) {
				String rsp = "";
				ArrayList<Job> jobs = map.get(city);
				for(Job j : jobs) rsp += j.getDesc() + "\n\n";
				Main.respondTimeout("Jobs at "+city+":\n"+rsp+"(Note: This message will be removed in 1 minute)", 60, event);
			}else Main.respondTimeout(city + " does not appear to have any job listings at this moment. Try again later when the GM isn't a lazy bum.", 5, event);
		}
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return true;
	}

	private boolean addJob(String city, Job j) {
		if(map.containsKey(city)) {
			ArrayList<Job> jobs = map.get(city);
			for(Job job : jobs) 
				if(job.getKey().equals(j.getKey()))
					return false;
			jobs.add(j);
		}
		else {
			ArrayList<Job> l = new ArrayList<>();
			l.add(j);
			map.put(city, l);
		}
		Main.info("Job '"+j.getKey()+"' added!");
		return true;
	}
	
	private void saveSettings() {
		File f = new File("jobs.dat");
		
		try {
			if(f.exists()) f.delete();
			f.createNewFile();
			
			PrintWriter file = new PrintWriter(f);
			for(ArrayList<Job> jobArrs : map.values())
				for(Job j : jobArrs)
					file.println(j.saveFormat());
			
			file.flush();
			file.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSettings() {
		File f = new File("jobs.dat");
		if(!f.exists()) return;
		
		try {
			Scanner file = new Scanner(f);
			while(file.hasNextLine()) {
				//Line format should be the following: <city>, <jobkey>: <desc>
				String ln = file.nextLine();
				String city = ln.substring(0, ln.indexOf(','));
				String key = ln.substring(ln.indexOf(',')+2, ln.indexOf(':'));
				String desc = ln.substring(ln.indexOf(':') + 2);
				
				Job j = new Job(city, desc, key);
				addJob(city, j);
			}
			file.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private String casify(String s) {
		s = s.toLowerCase();
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
	
	class Job{
		private String city;
		private String desc;
		private String key;
		
		public Job(String city, String desc, String key) {
			this.city = city;
			this.desc = desc;
			this.key = key;
		}
		
		public String getCity() { return city; }
		public String getDesc() { return desc; }
		public String getKey() { return key; }
		
		public String saveFormat() {
			return city + ", " +key+": "+desc;
		}
	}
}
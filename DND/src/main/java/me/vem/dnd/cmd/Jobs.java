package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Jobs extends Command implements Configurable{
	
	private static Jobs instance;
	public static Jobs getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new Jobs();
	}
	
	private Map<String, List<Job>> jobsDatabase;
	
	private Jobs() {
		super("jobs");
		load();
	}

	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length == 0) {
			Respond.timeout(event, 10000, help());
			return true;
		}
		
		String city = args[0];
		if(city.equals("add")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Respond.timeout(event, 5000, "Only the GM can add job listings.");
				return false;
			}
			
			if(args.length < 4) {
				Respond.timeout(event, 5000, "Not enough arguments..");
				return false;
			}
			
			String trueCity = casify(args[1]);
			String key = args[2];
			String desc = "";
			for(int i=3;i<args.length;)
				desc += args[i] + (++i<args.length ? " " : "");
			
			Job j = new Job(trueCity, desc, key);
			addJob(trueCity, j);
			
			Respond.timeout(event, 5000, "Job added to "+trueCity+"!");
			
		}else if(city.equals("remove")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Respond.timeout(event, 5000, "Only the GM can remove job listings.");
				return false;
			}
			
			if(args.length < 3) {
				Respond.timeout(event, 5000, "Not enough arguments..");
				return false;
			}
			
			String trueCity = casify(args[1]);
			String key = args[2];
			
			if(jobsDatabase.containsKey(trueCity)) {
				List<Job> jobs = jobsDatabase.get(trueCity);
				Job target = null;
				for(Job j : jobs) {
					if(j.getKey().equals(key)) {
						target = j;
						break;
					}
				}
				
				if(target==null) Respond.timeout(event, 5000, "Job not found by key "+key);
				else{
					jobs.remove(target);
					if(jobs.size() == 0) jobsDatabase.remove(trueCity);
					Respond.timeout(event, 5000, "Job '"+key+"' removed!");
				}
			}else Respond.timeout(event, 5000, "No such city.");
		
		}else if(city.equals("cities")){
			String rsp = "";
			for(String s : jobsDatabase.keySet()) rsp+=s+"\n";
			Respond.timeout(event, 10000, "Known Cities:\n"+rsp);
		}else{
			city = casify(city);
			
			if(args.length > 1 && args[1].equals("getkeys"))
				if(event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					if(jobsDatabase.containsKey(city)) {
						String rsp = "";
						for(Job j : jobsDatabase.get(city)) rsp += j.getKey()+"\n";
						Respond.timeout(event, 10000, city+" Job Keys: \n"+rsp);
						return true;
					}else{
						Respond.timeout(event, 5000, "No such city.");
						return true;
					}
				}else{
					Respond.timeout(event, 5000, "Only the GM can get the keys of the jobs.");
					return false;
				}
			
			if(jobsDatabase.containsKey(city)) {
				String rsp = "";
				for(Job j : jobsDatabase.get(city)) rsp += j.getDesc() + "\n\n";
				Respond.timeout(event, 60000, "Jobs at "+city+":\n"+rsp+"(Note: This message will be removed in 1 minute)");
			}else Respond.timeout(event, 5000, city + " does not appear to have any job listings at this moment. Try again later when the GM isn't a lazy bum.");
		}
		return true;
	}

	@Override public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		return true;
	}

	private boolean addJob(String city, Job j) {
		if(jobsDatabase.containsKey(city)) {
			List<Job> jobs = jobsDatabase.get(city);
			for(Job job : jobs) 
				if(job.getKey().equals(j.getKey()))
					return false;
			jobs.add(j);
		}
		else {
			ArrayList<Job> l = new ArrayList<>();
			l.add(j);
			jobsDatabase.put(city, l);
		}
		Logger.infof("Job '%s' added!", j.getKey());
		return true;
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

	@Override protected String help() {
		return "Usage: jobs <cityname>\n"
			 + "\tjobs <cityname> getkeys\n"
			 + "\tjobs cities";
	}

	@Override
	public void save() {
		try {
			PrintWriter out = ExtFileManager.getConfigOutput("jobs.json");
			out.print(ExtFileManager.getGsonPretty().toJson(jobsDatabase));
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Logger.info("Jobs database saved.");
	}

	@Override
	public void load() {
		jobsDatabase = new HashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("jobs.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		jobsDatabase = gson.fromJson(content, new TypeToken<HashMap<String, List<Job>>>(){}.getType());
	}
	@Override
	protected void unload() {
		save();
		instance = null;
	}
}
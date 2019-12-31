package me.vem.dnd.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class Jobs extends Command implements Configurable{
	
	private static Jobs instance;
	public static Jobs getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new Jobs();
	}
	
	private Map<Long, Map<String, List<Job>>> database;
	
	private Jobs() {
		super("jobs");
		load();
	}

	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		TextChannel channel = event.getChannel();
		Message userMsg = event.getMessage();
		Guild guild = event.getGuild();
		
		if(args.length == 0)
			return sendHelp(channel, true);
		
		Map<String, List<Job>> gDatabase = database.get(guild.getIdLong());
		if(gDatabase == null)
			database.put(guild.getIdLong(), gDatabase = new LinkedHashMap<>());
		
		if("cities".equals(args[0])) {
			if(gDatabase.size() != 0) {
				StringBuilder response = new StringBuilder("Known Cities:\n```\n");
				for(String s : gDatabase.keySet())
					response.append(s).append('\n');
				Respond.timeout(channel, userMsg, 7500, response.append("```").toString());
			}else Respond.timeout(channel, userMsg, 5000, "Your guild does not have any cities with job listings.");
		}else if("jobkeys".equals(args[0])) {
			if(args.length < 2)
				return sendHelp(channel, false);
			
			args[1] = casify(args[1]);
			
			List<Job> jobs = gDatabase.get(args[1]);
			if(jobs == null) {
				Respond.timeoutf(channel, userMsg, 5000, "`%s` is not a known city.", args[1]);
				return false;
			}
			
			StringBuilder response = new StringBuilder("Keys of Jobs in ").append(args[1]).append(":\n```\n");
			for(Job job : jobs)
				response.append(job.key).append('\n');
			Respond.timeout(channel, userMsg, 10000, response.append("```").toString());
		}else if("add".equals(args[0])){
			if(args.length < 4)
				return sendHelp(channel, false);
			
			args[1] = casify(args[1]);
			List<Job> jobs = gDatabase.get(args[1]);
			if(jobs == null)
				gDatabase.put(args[1], jobs = new LinkedList<>());
			
			Job j = new Job(args[2], args[3]);
			for(Job job : jobs)
				if(job.equals(j)) {
					Respond.timeoutf(channel, userMsg, 5000, "There's already a job with the key `%s`.", args[2]);
					return false;
				}
			
			jobs.add(j);
			Respond.timeoutf(channel, userMsg, 5000, "Job `%s` added to the city `%s`", args[2], args[1]);
		}else if("remove".equals(args[0])){
			if(args.length < 3)
				return sendHelp(channel, false);
			
			args[1] = casify(args[1]);
			
			List<Job> jobs = gDatabase.get(args[1]);
			if(jobs == null) {
				Respond.timeoutf(channel, userMsg, 5000, "The city `%s` has no job listings.", args[1]);
				return false;
			}
			
			boolean found = false;
			
			Iterator<Job> iter = jobs.iterator();
			while(iter.hasNext()) {
				Job cur = iter.next();
				if(cur.key.equals(args[2])) {
					iter.remove();
					found = true;
					break;
				}
			}
			
			if(found) Respond.timeoutf(channel, userMsg, 5000, "Job `%s` removed from the posting in city `%s`.", args[2], args[1]);
			else Respond.timeoutf(channel, userMsg, 5000, "`%s` is not the key of a listed job in `%s`.", args[2], args[1]);
		}else {
			args[0] = casify(args[0]);
			List<Job> jobs = gDatabase.get(args[0]);
			if(jobs == null) {
				Respond.timeoutf(channel, userMsg, 5000, "`%s` does not have any job listings.", args[0]);
				return false;
			}
			
			StringBuilder response = new StringBuilder("Jobs in ").append(args[0]).append(":\n```\n");
			int i=1;
			for(Job job : jobs) 
				response.append(i++).append(") ").append(job.desc).append("\n\n");
			
			Respond.timeoutf(channel, userMsg, 60000, response.append("```").toString());
		}
		
		return true;
	}

	@Override public String[] usages() {
		return new String[] {
			"`jobs <city>` -- Prints the list of jobs in that city.",
			"`jobs cities` -- Prints a list of cities.",
			"`jobs jobkeys <city>` -- Prints the keys of the jobs in that city.",
			"`jobs add <city> <job-key> <description>` -- Adds a job to a city.",
			"`jobs remove <city> <job-key>` -- Removes a job from a city."
		};
	}
	
	@Override public boolean hasPermissions(Member member, String... args) {
		if(args.length > 0 && ("add".equals(args[0]) || "remove".equals(args[0])))
			return member.hasPermission(Permission.ADMINISTRATOR);
		return true;
	}
	
	private String casify(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
	}
	
	class Job{
		final String key, desc;
		
		public Job(String key, String desc) {
			this.key = key;
			this.desc = desc;
		}
		
		public boolean equals(Job other) {
			if(other == null) return false;
			return key.equals(other.key);
		}
	}

	@Override
	public void save() {
		try {
			PrintWriter out = ExtFileManager.getConfigOutput("jobs.json");
			out.print(ExtFileManager.getGsonPretty().toJson(database));
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Logger.info("Jobs database saved.");
	}

	@Override
	public void load() {
		database = new LinkedHashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("jobs.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		database = gson.fromJson(content, new TypeToken<LinkedHashMap<Long, LinkedHashMap<String, LinkedList<Job>>>>(){}.getType());
	}
	
	@Override
	protected void unload() {
		save();
		instance = null;
	}
	@Override
	public String getDescription() {
		return "Allows for the posting of jobs to a 'job board' for players to read from on their own.";
	}
}
package me.vem.role.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import me.vem.role.Bot;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Rankup implements Command{
	
	private HashMap<Guild, RoleInfo> info;
	private File datafile;
	
	public Rankup(JDA jda) {
		loadRoles(jda);
	}
	
	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		Guild g = event.getGuild();
		RoleInfo ri = info.get(g);
		if(ri == null)
			info.put(g, ri = new RoleInfo(g));
		
		if(args.length == 0) {
			StringBuffer roleList = new StringBuffer();
			Iterator<String> iter = ri.getStringIterator();
			
			while(iter.hasNext()) {
				roleList.append(iter.next());
				if(iter.hasNext())
					roleList.append('\n');
			}
			
			if(roleList.length() == 0) {
				roleList.append("This guild currently has no assignable roles.\nAsk an admin to 'rankup allow' to add roles.");
			}
			
			Bot.respond(help(event) + "\nList of assignable roles:" + Bot.format(roleList.toString(), Bot.TextFormat.CODE), event);
			return;
		}
		
		
		if(args[0].equals("allow")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Bot.respond("You do not have the permissions to allow roles.", event);
				return;
			}
			
			if(args.length != 3) {
				Bot.respond("Usage: rankup allow <@role> <rolereference>", event);
				return;
			}
			
			if(ri.hasRole(args[2])) {
				Bot.respond(String.format("The reference '%s' is already used. Try another.", args[2]), event);
				return;
			}
			
			List<Role> roleList = event.getMessage().getMentionedRoles();
			if(roleList.size() != 1) {
				Bot.respond("Specific role mention not found. Please mention exactly one role.", event);
				return;
			}
			
			Role r = roleList.get(0);
			ri.addRole(args[2], r);
			saveRoles();
			
			Bot.respond(String.format("Role '%s' added with alias '%s'", r.getName(), args[2]), event);
			
		}else if(args[0].equals("disallow")) {
			if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
				Bot.respond("You do not have the permissions to disallow roles.", event);
				return;
			}
			
			if(args.length != 2) {
				Bot.respond("Usage: rankup disallow <rolereference>", event);
				return;
			}
			
			if(!ri.hasRole(args[1])) {
				Bot.respond("Role with alias '"+ args[1] + "' does not exist.", event);
				return;
			}
			
			ri.removeRole(args[1]);
			saveRoles();
			
			Bot.respond("Role alias '"+ args[1] +"' removed.", event);
		}else {
			if(ri.size() == 0) {
				Bot.respond("This guild currently has no assignable roles.\nAsk an admin to 'rankup allow' to add roles.", event);
				return;
			}
			
			Member mem = event.getMember();
			StringBuffer response = new StringBuffer();
			for(String ref : args) {
				
				if(ri.hasRole(ref)) {
					Role r = ri.getRole(ref);
					if(mem.getRoles().contains(r)) {
						g.getController().removeSingleRoleFromMember(mem, r).queue();
						response.append(String.format("Role '%s' removed.\n", ref));
					} else {
						g.getController().addSingleRoleToMember(mem, r).queue();
						response.append(String.format("Role '%s' given.\n", ref));
					}
				}else
					response.append(String.format("Role '%s' not found.%n", ref));
			}
			
			Bot.respond(response.toString(), event);
		}
	}

	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return true;
	}

	@Override
	public String help(MessageReceivedEvent event) {
		return String.format("Usage: %srankup <role> [role2] [role3] [role...etc]", Bot.prefix.getPrefix(event.getGuild()));
	}
	
	private void saveRoles() {
		try {
			PrintWriter pw = new PrintWriter(datafile);
			
			for(Guild g : info.keySet())
				pw.println(info.get(g).toString());
			
			pw.flush();
			pw.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadRoles(JDA jda) {
		info = new HashMap<Guild, RoleInfo>();
		datafile = new File("ranks.dat");
		
		if(!datafile.exists()) {
			try {
				datafile.createNewFile();
			}catch(IOException e) { e.printStackTrace(); }
			return;
		}
		
		try {
			Scanner read = new Scanner(datafile);
			
			while(read.hasNextLine()) {
				Scanner in = new Scanner(read.nextLine());
				
				Guild g = jda.getGuildById(in.nextLong());
				Role r = g.getRoleById(in.nextLong());
				String rep = in.next();
				
				if(info.containsKey(g)) 
					info.get(g).addRole(rep, r);
				else info.put(g, new RoleInfo(g).addRole(rep, r));
				
				in.close();
			}
			
			read.close();
		}catch(IOException e) {} //File is ensured to exist.
	}
}

class RoleInfo{
	
	private HashMap<String, Role> data;
	private Guild guild;
	
	public RoleInfo(Guild guild) {
		data = new HashMap<>();
		this.guild = guild;
	}
	
	public RoleInfo addRole(String rep, Role r) {
		data.put(rep, r);
		return this;
	}
	
	public RoleInfo removeRole(String rep) {
		data.remove(rep);
		return this;
	}
	
	public boolean hasRole(String rep) {
		return data.containsKey(rep);
	}
	
	public Role getRole(String rep) {
		return data.get(rep);
	}
	
	public Iterator<String> getStringIterator(){
		return data.keySet().iterator();
	}
	
	public int size() {
		return data.size();
	}
	
	public String toString() {
		StringBuffer out = new StringBuffer();
		Iterator<String> iter = data.keySet().iterator();
		while(iter.hasNext()) {
			String rep = iter.next();
			out.append(guild.getIdLong() + " " + data.get(rep).getIdLong() + " " + rep);
			if(iter.hasNext())
				out.append('\n');
		}
		return out.toString();
	}
}
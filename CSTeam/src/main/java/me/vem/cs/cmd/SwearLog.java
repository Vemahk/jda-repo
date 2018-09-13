package me.vem.cs.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import me.vem.cs.Bot;
import me.vem.cs.utils.ExtFileManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.impl.UserImpl;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class SwearLog extends Command implements Configurable{

	private static SwearLog instance;
	public static SwearLog getInstance() { return instance; }
	public static void initialize() {
		if(instance != null) return;
		instance = new SwearLog();
	}
	
	private HashSet<Long> userDatabase;
	
	private SwearLog() {
		super("swearlog");
		load();
	}
	
	@Override
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		if(args.length==0) {//No arguments
			getHelp(event);
			return true;
		}else if(args.length == 1) { //1 argument
			if(args[0].equalsIgnoreCase("add")) //Add self
				userDatabase.add(event.getAuthor().getIdLong());
		}else if(args.length != 2){//>2 arguments; invalid
			getHelp(event);
			return true;
		}else {//2 Arguments
			if(!args[0].equals("add")) {
				getHelp(event);
				return false;
			}
			List<Member> mentions = event.getMessage().getMentionedMembers();
			if(mentions.size() == 0) { //No mentions
				Bot.respondAsync(event, "Second argument must be a mention.");
				return false;
			}else if(mentions.size() > 1) {// Multiple mentions
				Bot.respondAsync(event, "Cannot interpret multiple mentions. Please only mention 1 user.");
				return false;
			}

			userDatabase.add(mentions.get(0).getUser().getIdLong());
			Bot.respondAsyncf(event, "Added `%s` to the list of swear notifications.", mentions.get(0).getNickname());
		}
		
		return true;
	}
	
	/**
	 * Notifies the admins listed in the ids HashSet that someone said a curse word.
	 * @param badword
	 * @param event
	 */
	public void notifyAdmins(String badword, MessageReceivedEvent event) {
		for(long l : userDatabase) {
			User u = event.getJDA().getUserById(l);
			if(!u.hasPrivateChannel()) u.openPrivateChannel().complete();
			MessageChannel mc = ((UserImpl)u).getPrivateChannel();
			mc.sendMessage(event.getMember().getEffectiveName() + " said the badword '"+badword+"'");
		}
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help() {
		return "Usage: `swearlog add [@user]`";
	}
	
	@Override
	public void save() {
		try (PrintWriter writer = ExtFileManager.getConfigOutput("swearlog.json")){
			writer.print(ExtFileManager.getGsonPretty().toJson(userDatabase));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void load() {
		userDatabase = new HashSet<>();
		
		File file = ExtFileManager.getConfigFile("swearlog.json");
		if(file == null) return;
		
		String data = ExtFileManager.readFileAsString(file);
		if(data == null) return;
		
		userDatabase = ExtFileManager.getGsonPretty().fromJson(data, new TypeToken<HashSet<Long>>() {}.getType());
	}

}

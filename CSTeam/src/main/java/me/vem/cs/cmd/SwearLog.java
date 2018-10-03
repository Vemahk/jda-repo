package me.vem.cs.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Respond;
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
			if(args[0].equals("add")) //Add self
				addMember(event, event.getMember());
			else if(args[0].equals("remove"))
				removeMember(event, event.getMember());
		}else if(args.length != 2){//>2 arguments; invalid
			getHelp(event);
			return true;
		}else {//2 Arguments
			boolean isAdd = "add".equals(args[0]);
			boolean isRem = "remove".equals(args[0]);
			if(!(isAdd || isRem)) {
				getHelp(event);
				return false;
			}
			
			List<Member> mentions = event.getMessage().getMentionedMembers();
			if(mentions.size() == 0) { //No mentions
				Respond.async(event, "Second argument must be a mention.");
				return false;
			}else if(mentions.size() > 1) {// Multiple mentions
				Respond.async(event, "Cannot interpret multiple mentions. Please only mention 1 user.");
				return false;
			}
			
			if(isAdd) addMember(event, mentions.get(0));
			else if(isRem) removeMember(event, mentions.get(0));
		}
		
		return true;
	}
	
	private void addMember(MessageReceivedEvent event, Member m) {
		if(userDatabase.add(m.getUser().getIdLong()))
			Respond.asyncf(event, "Added `%s` to the list of swear notifications.", m.getNickname());
		else Respond.asyncf(event, "`%s` already is a receiver of swear notifications.", m.getNickname());
	}
	
	private void removeMember(MessageReceivedEvent event, Member m) {
		if(userDatabase.remove(m.getUser().getIdLong()))
			Respond.asyncf(event, "Remove `%s` from the list of swear notifications.", m.getNickname());
		else Respond.asyncf(event, "`%s` was not found to be a receiver of swear notifications.", m.getNickname());
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
			
			mc.sendMessage(String.format("`%s` said the badword `%s`!",
					event.getMember().getNickname(), badword)).queue();
		}
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
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
	
	@Override public void unload() {
		save();
		instance = null;
	}

}

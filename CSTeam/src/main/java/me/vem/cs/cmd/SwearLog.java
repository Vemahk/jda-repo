package me.vem.cs.cmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Scanner;

import me.vem.cs.Main;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.impl.UserImpl;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class SwearLog implements Command{

	private HashSet<Long> ids;
	
	public SwearLog() {
		loadData();
	}
	
	@Override
	public void run(String[] args, MessageReceivedEvent event) {
		if(args.length==0) {
			Main.respond("Usage: ~!swearlog add [name]", event);
			return;
		}else if(args.length == 1) {
			if(args[0].equalsIgnoreCase("add"))
				add(event.getMember().getUser().getIdLong());
		}
	}
	
	/**
	 * Notifies the admins listed in the ids HashSet that someone said a curse word.
	 * @param badword
	 * @param event
	 */
	public void notifyAdmins(String badword, MessageReceivedEvent event) {
		for(long l : ids) {
			User u = event.getJDA().getUserById(l);
			if(!u.hasPrivateChannel()) u.openPrivateChannel().complete();
			MessageChannel mc = ((UserImpl)u).getPrivateChannel();
			mc.sendMessage(event.getMember().getEffectiveName() + " said the badword '"+badword+"'");
		}
	}
	
	/**
	 * Adds a user to the ids hashset and saves it to a file.
	 * @param l
	 */
	private void add(long l) {
		ids.add(l);
		saveData();
	}

	/**
	 * Saves the ids hashset to swearlog.dat.
	 */
	private void saveData() {
		try {
			File f = new File("swearlog.dat");
			if(f.exists()) f.delete();
			f.createNewFile();
			
			PrintWriter writer = new PrintWriter(f);
			
			for(long s : ids)
				writer.println(s);
			
			writer.flush();
			writer.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads ids from swearlog.dat
	 */
	private void loadData() {
		ids = new HashSet<>();
		try {
			File f = new File("swearlog.dat");
			if(!f.exists()) return;
			Scanner file = new Scanner(f);
			
			while(file.hasNextLong())
				ids.add(file.nextLong());
			
			file.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event) {
		//Admins only.
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public String help() {
		return "You need to be an admin to run view the help for this command.";
	}

}

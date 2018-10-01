package me.vem.dnd.cmd;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import me.vem.dnd.utils.ExtFileManager;
import me.vem.dnd.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ExportChannel extends Command {

	private static ExportChannel instance;
	public static ExportChannel getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new ExportChannel();
	}
	
	private ExportChannel() { super("exportchannel"); }
	
	private SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMM dd, yyyy");
	
	public boolean run(MessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		List<Message> history = fullHistory(event.getTextChannel());
		Iterator<Message> iter = history.iterator();
		if(iter.hasNext()) iter.next();
		
		String guildName = event.getGuild().getName();
		String channelName = event.getTextChannel().getName();
		String date = dateTimeFormatter.format(Calendar.getInstance().getTime());
		
		try {
			PrintWriter writer = ExtFileManager.getConfigOutput("channel_export/", channelName + " " + date);
			writer.printf("%s%n%s%n%s%n", guildName, channelName, date);

			GregorianCalendar day = null;
			
			while(iter.hasNext()) {
				Message m = iter.next();
				OffsetDateTime creation = m.getCreationTime();
				if(day == null || creation.getDayOfMonth() != day.get(GregorianCalendar.DAY_OF_MONTH) || 
						creation.getMonthValue()-1 != day.get(GregorianCalendar.MONTH) ||
						creation.getYear() != day.get(GregorianCalendar.YEAR)) {
					day = new GregorianCalendar(creation.getYear(), creation.getMonthValue()-1, creation.getDayOfMonth());
					writer.printf("%n%s%n", dateFormatter.format(day.getTime()));
				}
				
				
				writer.printf("[%02d:%02d] %s: %s%n", creation.getHour(), creation.getMinute(), m.getAuthor().getName(), m.getContentDisplay());
			}

			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		event.getMessage().delete().complete();
		Respond.timeout(event.getTextChannel(), 5000, "Channel exported...");
		
		return true;
	}
	
	private List<Message> fullHistory(TextChannel channel){
		ArrayList<Message> out = new ArrayList<>();

		Message next = channel.getMessageById(channel.getLatestMessageIdLong()).complete();
		out.add(next);
		
		do {
			MessageHistory mh = channel.getHistoryBefore(next, 100).complete();
			if(mh.size() == 0) break;
			
			Iterator<Message> iter = mh.getRetrievedHistory().iterator();
			
			while(iter.hasNext()) {
				Message m = iter.next();
				out.add(m);
				if(!iter.hasNext())
					next = m;
			}
		}while(true);
		
		return out;
	}
	
	@Override
	public boolean hasPermissions(MessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}
	
	@Override protected String help() {
		return "Usage: none at the moment.";
	}
	@Override
	protected void unload() {
		instance = null;
	}
}

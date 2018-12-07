package me.vem.dbgm.cmd;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Respond;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class ExportChannel extends Command {

	private static ExportChannel instance;
	public static ExportChannel getInstance() { return instance; }
	public static void initialize() {
		if(instance == null) instance = new ExportChannel();
	}
	
	private ExportChannel() { super("exportchannel"); }
	
	private SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMM dd, yyyy");
	
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args)) return false;
		
		Message response = Respond.sync(event.getChannel(), "Creating channel export ...");
		long start = System.currentTimeMillis();
		
		if(args.length == 1 && "all".equals(args[0])) {
			for(TextChannel channel : event.getGuild().getTextChannels()) {
				Category parent = channel.getParent();
				String catname = (parent == null) ? "global_category" : parent.getName();
				export(channel, event.getGuild().getName() + '/' + catname + '/');
			}
		}else if(args.length > 0){
			for(TextChannel channel : event.getMessage().getMentionedChannels()) {
				Category parent = channel.getParent();
				String catname = (parent == null) ? "global_category" : parent.getName();
				export(channel, event.getGuild().getName() + '/' + catname + '/');
			}
		}else export(event.getChannel(), "channel_export/");
		
		response.editMessage("Export completed\nRuntime: " + (System.currentTimeMillis() - start) +"ms").queue();
		Respond.deleteMessages(event.getChannel(), 5000, response, event.getMessage());
		
		return true;
	}
	
	private GregorianCalendar day;
	private void export(TextChannel channel, String dir) {
		day = null;
		String guildName = channel.getGuild().getName();
		String channelName = channel.getName();
		String date = dateTimeFormatter.format(Calendar.getInstance().getTime());
		
		try (PrintWriter writer = ExtFileManager.getConfigOutput(dir, channelName + " " + date)){
			
			writer.printf("%s%n%s%n%s%n", guildName, channelName, date);


			channel.getIterableHistory().cache(false).forEach(m -> {
				OffsetDateTime creation = m.getCreationTime();
				if(day == null || creation.getDayOfMonth() != day.get(GregorianCalendar.DAY_OF_MONTH) || 
						creation.getMonthValue()-1 != day.get(GregorianCalendar.MONTH) ||
						creation.getYear() != day.get(GregorianCalendar.YEAR)) {
					day = new GregorianCalendar(creation.getYear(), creation.getMonthValue()-1, creation.getDayOfMonth());
					writer.printf("%n%s%n", dateFormatter.format(day.getTime()));
				}
				
				writer.printf("[%02d:%02d] %s: %s%n", creation.getHour(), creation.getMinute(), m.getAuthor().getName(), m.getContentDisplay());
				
				for(Attachment a : m.getAttachments())
					writer.printf("[Embeded: %s]%n", a.getUrl());
			});
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}
	
	@Override public String[] usages() {
		return new String[] {
			"`exportchannel` -- Exports this channel to a file.",
			"`exportchannel all` -- Exports the entire guild to a folder.",
			"`exportchannel [#channel] [#channel2] ...` -- Exports the mentioned channels to files."
		};
	}
	
	@Override
	protected void unload() {
		instance = null;
	}
	
	@Override
	public String getDescription() {
		return "Allows admins to export an entire channel's history to a file.";
	}
}

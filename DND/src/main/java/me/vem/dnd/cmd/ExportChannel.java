package me.vem.dnd.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.utils.Logger;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
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
		
		event.getMessage().delete().complete();
		
		File baseDir = new File(event.getGuild().getName() + " Export/");
		if(!baseDir.exists()) baseDir.mkdirs();
		
		List<TextChannel> toExport = new LinkedList<>();
		
		if(args.length == 1 && "all".equals(args[0])) {
			for(TextChannel channel : event.getGuild().getTextChannels())
				toExport.add(channel);
		}else if(args.length > 0){
			for(TextChannel channel : event.getMessage().getMentionedChannels())
				toExport.add(channel);
		}else toExport.add(event.getChannel());
		
		for(TextChannel channel : toExport)
			export(channel, baseDir);
		
		try {
			File ret = zip(baseDir);
			sendFileSync(event.getAuthor(), ret);
			ret.delete();
			baseDir.delete();
		} catch (IOException e) {
			e.printStackTrace();
			baseDir.delete();
			return false;
		}
		
		
		return true;
	}
	
	/**
	 * @param channel
	 * @param base
	 * @return the 
	 */
	private void export(TextChannel channel, File baseDir) {
		String guildName = channel.getGuild().getName();
		String channelName = channel.getName();
		String date = dateTimeFormatter.format(Calendar.getInstance().getTime());
		
		File parent = channel.getParent() == null ? baseDir : new File(baseDir, channel.getParent().getName() + "/");
		if(!parent.exists()) parent.mkdirs();
		if(!parent.isDirectory()) Logger.errf("Directory is not a directory? %s", parent.getAbsolutePath());
		
		File file = new File(parent, channelName + "-" + date + ".txt");
		
		try (PrintWriter writer = new PrintWriter(file)){
			writer.printf("%s%n%s%n%s%n", guildName, channelName, date);
			
			Stack<Message> msgs = new Stack<>();
			
			for(Message msg : channel.getIterableHistory().cache(false))
				msgs.push(msg);
			
			GregorianCalendar day = null;
			while(!msgs.isEmpty()) {
				Message m = msgs.pop();

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
			}
			
			writer.flush();
			writer.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendFileSync(User u, File f) {
		if(u.isFake() || f == null || f.isDirectory())
			return;
		u.openPrivateChannel().complete().sendFile(f).complete();
	}
	
	/**
	 * @param file The file, or directory, that you want to zip up.
	 * @return the zipped file.
	 * @throws IOException 
	 */
	private File zip(File file) throws IOException {
		File tmpZip = File.createTempFile("tmp", ".zip");
		FileOutputStream fos = new FileOutputStream(tmpZip);
		ZipOutputStream zos = new ZipOutputStream(fos);
		
		zip(file, file.getName(), zos);
		zos.close();
		
		return tmpZip;
	}
	
	private void zip(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if(fileToZip.isHidden()) return;
		
		if(fileToZip.isDirectory()) {
			if(!fileName.endsWith("/"))
				fileName += '/';
			
			zipOut.putNextEntry(new ZipEntry(fileName));
			zipOut.closeEntry();
			
			for(File child : fileToZip.listFiles())
				zip(child, fileName + child.getName(), zipOut);
			return;
		}
		
		FileInputStream fis = new FileInputStream(fileToZip);
		zipOut.putNextEntry(new ZipEntry(fileName));
		byte[] buf = new byte[1024];
        for (int len; (len = fis.read(buf)) >= 0;) 
            zipOut.write(buf, 0, len);
        fis.close();
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
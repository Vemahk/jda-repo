package me.vem.dnd;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JTextArea;

import me.vem.dnd.cmd.ClearOOC;
import me.vem.dnd.cmd.Command;
import me.vem.dnd.cmd.ExportChannel;
import me.vem.dnd.cmd.Jobs;
import me.vem.dnd.cmd.Meme;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Main {

	private static JDA jda;
	public static ConsoleMenu openConsole;
	
	public static JTextArea outArea;
	public static PrintStream outStream;
	
	public static void main(String[] args) throws IOException {
		
		if(SystemTray.isSupported()) {
			outArea = new JTextArea();
			outArea.setEditable(false);
			
			outStream = new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException{
					outArea.append(""+(char)b);
					outArea.setCaretPosition(outArea.getDocument().getLength());
					outArea.update(outArea.getGraphics());
				}
			});
			System.setOut(outStream);
			System.setErr(outStream);
			
			openConsole = new ConsoleMenu();
			
			PopupMenu popup = new PopupMenu();
			
			MenuItem close = new MenuItem("Exit");
			close.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});

			popup.add(close);
			
			try {
				SystemTray tray = SystemTray.getSystemTray();
				Image image = ImageIO.read(Main.class.getResource("tray.png"));
				TrayIcon trayIcon = new TrayIcon(image, "DNDBot", popup);
				trayIcon.setImageAutoSize(true);
				trayIcon.addMouseListener(new MouseListener() {
					public void mouseClicked(MouseEvent e) {
						if(e.getButton() == MouseEvent.BUTTON1 && openConsole == null)
							openConsole = new ConsoleMenu();
					}
					
					public void mouseEntered(MouseEvent arg0) {}
					public void mouseExited(MouseEvent arg0) {}
					public void mousePressed(MouseEvent arg0) {}
					public void mouseReleased(MouseEvent arg0) {}
				});
				tray.add(trayIcon);
			}catch (AWTException e) {
				e.printStackTrace();
			}
		}
		
		String token = "";
		try{
			File f = new File("token.dat");
			if(f.exists()){
				Scanner file = new Scanner(f);
				token = file.nextLine();
				file.close();
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		
		if(token.equals("")){
			Main.info("Token is blank. Make sure there is a valid token.dat file.");
			return;
		}
		
		try {
			jda = new JDABuilder(AccountType.BOT).addEventListener(new BotListener()).setToken(token).buildBlocking();
			jda.setAutoReconnect(true);
		}catch (Exception e){
			e.printStackTrace();
		}
		
		registerCommand("clearooc", new ClearOOC());
		registerCommand("exportchannel", new ExportChannel());
		registerCommand("jobs", new Jobs());
		registerCommand("meme", new Meme());
	}
	
	public static void handle(String pcmd, MessageReceivedEvent e) {
		if(!pcmd.contains(" ")) {
			runCommand(pcmd, new String[] {}, e);
			return;
		}
		String str = pcmd.substring(0, pcmd.indexOf(' '));
		runCommand(str, pcmd.substring(pcmd.indexOf(' ')+1).split(" "), e);
	}
	
	private static HashMap<String, Command> commandMap = new HashMap<>();
	public static void registerCommand(String str, Command cmd) {
		commandMap.put(str, cmd);
	}
	
	public static boolean isCommand(String str) {
		return commandMap.containsKey(str);
	}
	
	public static void runCommand(String str, String[] args, MessageReceivedEvent event) {
		if(isCommand(str)) {
			info(String.format("%s ran ~%s with arguments %s.", event.getAuthor().getName(), str,  Arrays.toString(args)));
			
			Command cmd = commandMap.get(str);
			if(cmd.hasPermissions(event))
				cmd.run(args, event);
			else respondTimeout("You do not have permissions to run this command.", 5, event);
		}else respondTimeout("No such command known.", 5, event);
	}
	
	public static void respondTimeout(String msg, int timeout, MessageReceivedEvent event) {
		Message m = event.getTextChannel().sendMessage(msg).complete();
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		
		executor.schedule(new Runnable() {
			@Override
			public void run() {
				HashSet<Message> clearTmp = new HashSet<>();
				clearTmp.add(m);
				clearTmp.add(event.getMessage());
				event.getTextChannel().deleteMessages(clearTmp).complete();
			}
		}, timeout, TimeUnit.SECONDS);
		executor.shutdown();
	}
	
	public static void respond(String msg, MessageReceivedEvent event) {
		event.getTextChannel().sendMessage(msg).complete();
	}
	
	private static String getLogTime(String sev) {
		return String.format("[%s] [%s] [DNDBot]: ", new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()), sev);
	}
	
	public static void info(String msg) {
		System.out.println(getLogTime("Info") + msg);
	}
	
}

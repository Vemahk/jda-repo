package me.vem.bot;
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
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JTextArea;

import me.vem.bot.cmd.AntiPurge;
import me.vem.bot.cmd.Command;
import me.vem.bot.cmd.Prefix;
import me.vem.bot.cmd.Purge;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * @author Vemahk
 * @JDAVersion 
 * 3.6.0_354
 */
public class Bot {

	private static JDA jda;
	public static ConsoleMenu openConsole;
	
	public static JTextArea outArea;
	public static PrintStream outStream;
	
	public static Prefix prefix;
	
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
				Image image = ImageIO.read(Bot.class.getClassLoader().getResource("GM.png"));
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
			Bot.info("Token is blank. Make sure there is a valid token.dat file.");
			return;
		}
		
		try {
			jda = new JDABuilder(AccountType.BOT).addEventListener(new Listener()).setToken(token).buildBlocking();
			jda.setAutoReconnect(true);
			jda.getPresence().setGame(Game.playing("@me for help!"));
		}catch (Exception e){
			e.printStackTrace();
		}
		
		registerCommand(Prefix.cmd_name, prefix = new Prefix(jda));
		registerCommand(Purge.cmd_name, new Purge());
		registerCommand(AntiPurge.cmd_name, new AntiPurge());
	}
	
	public static void handle(String pcmd, MessageReceivedEvent e) {
		String pre = prefix.getPrefix(e);
		if(pcmd.startsWith(pre))
			pcmd = pcmd.replaceFirst(pre, "");
		
		pcmd = pcmd.trim();
		
		if(!pcmd.contains(" ")) {
			runCommand(pcmd, new String[] {}, e);
			return;
		}
		int space = pcmd.indexOf(' ');
		String cmd = pcmd.substring(0, space);
		runCommand(cmd, pcmd.substring(space+1).split(" "), e);
	}
	
	private static LinkedHashMap<String, Command> commandMap = new LinkedHashMap<>();
	public static void registerCommand(String str, Command cmd) {
		commandMap.put(str, cmd);
	}
	
	public static Set<String> commandSet(){
		return commandMap.keySet();
	}
	
	public static boolean isCommand(String str) {
		return commandMap.containsKey(str);
	}
	
	public static void runCommand(String str, String[] args, MessageReceivedEvent event) {
		if(isCommand(str)) {
			info(String.format("%s ran the command '%s' with arguments> %s.", event.getAuthor().getName(), str, Arrays.toString(args)));
			
			Command cmd = commandMap.get(str);
			if(cmd.hasPermissions(event))
				cmd.run(args, event);
			else respond("You do not have permissions to run this command.", event);
		}else respond("No such command known.", event);
	}
	
	public static void respond(String msg, MessageReceivedEvent event) {
		event.getTextChannel().sendMessage(msg).queue();
	}
	
	public static Message respondSync(String msg, MessageReceivedEvent event) {
		Message m = event.getTextChannel().sendMessage(msg).complete();
		return m;
	}
	
	private static String getLogTime(String sev) {
		return String.format("[%s] [%s] [GenMan]: ", new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()), sev);
	}
	
	public static void info(String msg) {
		System.out.println(getLogTime("Info") + msg);
	}
	
	public static void err(String msg) {
		System.err.println(getLogTime("Error") + msg);
	}
	
}

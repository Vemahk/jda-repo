package me.vem.cs;

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
import javax.security.auth.login.LoginException;
import javax.swing.JTextArea;

import me.vem.cs.cmd.Command;
import me.vem.cs.cmd.Contests;
import me.vem.cs.cmd.Help;
import me.vem.cs.cmd.NextContest;
import me.vem.cs.cmd.SwearLog;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Main {

	//The JDA Object
	private static JDA jda;
	
	//The JFrame menu which holds the output console. Only applicable if running Windows.
	public static ConsoleMenu openConsole;
	
	//The text area that takes over for System.out
	public static JTextArea outArea;
	
	//The stream which replaces System.out as the main output stream. outArea reads from this stream.
	public static PrintStream outStream;
	
	public static void main(String[] args) throws IOException {
		
		//Check if the user has a system tray on which this bot can reside.
		if(SystemTray.isSupported()) {
			
			outArea = new JTextArea();
			outArea.setEditable(false);
			
			outStream = new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException{
					outArea.append(""+(char)b);
					outArea.setCaretPosition(outArea.getDocument().getLength());
					if(((char)b) == '\n')
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
				TrayIcon trayIcon = new TrayIcon(image, "CSTeam Bot", popup);
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
		}//End System Tray
		
		//Fetch bot token.
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
			jda = new JDABuilder(AccountType.BOT).addEventListener(new Listener()).setToken(token).buildBlocking();
			jda.setAutoReconnect(true);
		} catch (LoginException | IllegalArgumentException | InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		//Registers all the commands. This is required for each command, else the bot won't recognize the command.
		Main.registerCommand("help", new Help());
		Main.registerCommand("nextcontest", new NextContest());
		Main.registerCommand("contests", new Contests());
		Main.registerCommand("swearlog", new SwearLog());
	}
	
	/**
	 * Splits the text after the command into a string array and hands it to the appropriate command (if it exists).
	 * @param pcmd
	 * @param e
	 */
	public static void handle(String pcmd, MessageReceivedEvent e) {
		if(!pcmd.contains(" ")) { //If there's not a space, then it's a no-argument command. Thus the string is the command.
			runCommand(pcmd, new String[] {}, e);
			return;
		}
		String str = pcmd.substring(0, pcmd.indexOf(' ')); //copies the command name from the string.
		runCommand(str, pcmd.substring(pcmd.indexOf(' ')+1).split(" "), e); //Runs the command name (str) with the rest of the string split into a string array.
	}
	
	/**
	 * This is the map of all the commands to their string name.
	 */
	public static HashMap<String, Command> commands = new HashMap<>();
	/**
	 * Saves a command object and its name to the commands HashMap so the program can recognize a command as a command.
	 * @param str
	 * @param cmd
	 */
	public static void registerCommand(String str, Command cmd) {
		commands.put(str, cmd);
	}
	
	/**
	 * @param str
	 * @return True if the given string is a key in the commands HashMap.
	 */
	public static boolean isCommand(String str) {
		return commands.containsKey(str);
	}
	
	/**
	 * Checks for a command with name 'str' and, if it exists, hands it args.
	 * @param str
	 * @param args
	 * @param event
	 */
	public static void runCommand(String str, String[] args, MessageReceivedEvent event) {
		if(isCommand(str)) {
			info(String.format("%s ran ~%s with arguments %s.", event.getAuthor().getName(), str,  Arrays.toString(args)));
			
			Command cmd = commands.get(str);
			if(cmd.hasPermissions(event))
				cmd.run(args, event);
			else respond("You do not have permissions to run this command.", event);
		}else respond("No such command known.", event);
	}
	
	/**
	 * This is for Discord's text formatting.
	 * @author Vemahk
	 */
	public enum TextFormat{
		LINEDCODE("`"), CODE("```\n"), ITALICS("*"), BOLD("**"), BOLDITALICS("***"), UNDERLINE("__"), UNDERLINEITALICS("__*"), ALL("__***"), STRIKETHROUGH("--");
		
		private String s;
		private TextFormat(String s) { this.s = s; }
		private String apply(String x) { return s + x + new StringBuffer(s).reverse().toString(); }
	}
	
	/**
	 * Formats a given string based on the Discord formatting codes.
	 * @param s String you want to format
	 * @param tf TextFormat object you're formatting it with.
	 * @return The formatted string.
	 */
	public static String format(String s, TextFormat tf) {
		return tf.apply(s);
	}
	
	/**
	 * Makes the bot send a message in the text channel given by the event.
	 * @param msg Message sent.
	 * @param event Event given.
	 */
	public static void respond(String msg, MessageReceivedEvent event) {
		event.
		getTextChannel().
		sendMessage(msg).
		complete();
	}
	
	/**
	 * Makes the bot send a message in the text channel given by the event. Its message, as well as the command-user's message, will be deleted after 'timeout' seconds.
	 * @param msg
	 * @param event
	 * @param timeout
	 */
	public static void respond(String msg, MessageReceivedEvent event, int timeout) {
		Message m = event.getTextChannel().sendMessage(msg).complete();
		if(timeout <= 0) return;
		
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		
		executor.schedule(() -> {
			HashSet<Message> clearTmp = new HashSet<>();
			clearTmp.add(m);
			clearTmp.add(event.getMessage());
			event.getTextChannel().deleteMessages(clearTmp).complete();
		} , timeout, TimeUnit.SECONDS);
		executor.shutdown();
	}
	
	/**
	 * Text formatting for the console window.
	 * @param sev
	 * @return
	 */
	private static String getLogTime(String sev) {
		return String.format("[%s] [%s] [DNDBot]: ", new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()), sev);
	}
	
	/**
	 * The fast and easy way to format an info outprint.
	 * @param msg
	 */
	public static void info(String msg) {
		System.out.println(getLogTime("Info") + msg);
	}
	
}

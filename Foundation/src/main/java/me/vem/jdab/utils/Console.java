package me.vem.jdab.utils;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import me.vem.jdab.DiscordBot;

public class Console {

	private static JFrame console;
	private static JTextArea consoleOutput;
	private static PrintStream out;

	private static TrayIcon tray;

	/** @return true if there is an open console. */
	public static boolean hasConsole() { return console != null; }

	/**
	 * Creates the console only if it does not already exist.
	 * 
	 * @return true if build was successful; <br>
	 *         false if there is already an open console that exists.
	 */
	public static boolean buildConsole() {
		if (hasConsole())
			return false;

		buildTextArea();
		activateTrayIcon();

		console = new JFrame(Version.getVersion() + " Console");
		console.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		console.setContentPane(new JScrollPane(consoleOutput));
		console.setJMenuBar(getNewMenuBar());
		console.setSize(new Dimension(600, 400));
		console.setLocationRelativeTo(null);
		console.setVisible(true);
		
		console.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent windowEvent) {
				disposeConsole();
			}
		});
		
		return true;
	}

	private static JMenuBar getNewMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		JMenu menu = new JMenu("Options");
		menuBar.add(menu);
		
		JMenuItem shutdown = new JMenuItem("Shutdown Bot");
		shutdown.addActionListener(e -> shutdown());
		menu.add(shutdown);
		
		return menuBar;
	}
	
	/**
	 * Closes the current console window.
	 */
	public static void disposeConsole() {
		if (!hasConsole())
			return;

		if (!SystemTray.isSupported() || tray == null) {
			shutdown();
		}else {
			console.dispose();
			console = null;
		}
	}
	
	/**
	 * Loads the text area object and redirection System.out and System.err to print to the text area.
	 */
	private static void buildTextArea() {
		if(consoleOutput != null) return;
		consoleOutput = new JTextArea();
		consoleOutput.setEditable(false);
		
		out = new PrintStream(new OutputStream() {
			@Override public void write(int i) throws IOException {
				char c = (char)i;
				consoleOutput.append(String.valueOf(c));
				if(c == '\n') consoleOutput.update(consoleOutput.getGraphics());
			}
		});
		
		PrintThread.getInstance().addOut(out);
		PrintThread.getInstance().addErr(out);
	}
	
	/**
	 * Activates the Windows Tray icon that this application will run out of. <br>
	 * Will not do anything for non-Windows systems (To my knowledge).
	 */
	private static void activateTrayIcon() {
		if(!SystemTray.isSupported() || tray != null)
			return;
		
		try {
			SystemTray systray = SystemTray.getSystemTray();
			InputStream imageStream = Console.class.getClassLoader().getResourceAsStream("tray.png");
			if(imageStream == null) {
				Logger.err("tray.png resource not found! The tray has failed to load.");
				return;
			}
			Image icon = ImageIO.read(imageStream);
			tray = new TrayIcon(icon, Version.getVersion().getName(), null);
			tray.setImageAutoSize(true);
			tray.addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1)
						buildConsole();
				}
			});
			systray.add(tray);
		}catch(AWTException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void destroyTray() {
		if(tray == null) return;
		SystemTray.getSystemTray().remove(tray);
		tray = null;
	}
	
	public static boolean shutdown() {
		int res = JOptionPane.showConfirmDialog(console,
				"Are you sure?", "Shutdown Bot", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		
		if(res == JOptionPane.YES_OPTION){
			DiscordBot bot = DiscordBot.getInstance();
			if(bot != null)
				bot.shutdown();
			
			if(hasConsole()) {
				console.dispose();
				console = null;
			}
			
			destroyTray();
			
			if(PrintThread.hasInstance()) {
			    PrintThread printer = PrintThread.getInstance();

			    printer.removeOut(out);
			    printer.removeErr(out);
			    printer.kill();
			}
			
			out = null;
			consoleOutput = null;

			Logger.info("Goodbye!");
			return true;
		}
		return false;
	}
}
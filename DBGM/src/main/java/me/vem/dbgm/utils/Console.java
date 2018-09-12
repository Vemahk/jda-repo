package me.vem.dbgm.utils;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
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

import me.vem.dbgm.Bot;

public class Console {

	private static JFrame console;
	private static JTextArea consoleOutput;

	private static boolean trayActive;

	/** @return The active console. */
	public static JFrame getConsole() {
		return console;
	}

	/** @return true if there is an open console. */
	public static boolean hasConsole() {
		return console != null;
	}

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

		console = new JFrame(Version.getVersion().getName() + " Console");
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
		
		JMenuItem close = new JMenuItem("Close Window");
		close.addActionListener(e -> disposeConsole());
		menu.add(close);
		
		JMenuItem shutdown = new JMenuItem("Shutdown Bot");
		shutdown.addActionListener(e -> {
			if(JOptionPane.showConfirmDialog(console,
					"Are you sure?", "Shutdown Bot", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
				Bot.shutdown();
		});
		menu.add(shutdown);
		
		return menuBar;
	}
	
	/**
	 * Closes the current console window.
	 */
	public static void disposeConsole() {
		if (!hasConsole())
			return;

		if (!SystemTray.isSupported() || !trayActive) {
				if(JOptionPane.showConfirmDialog(console,
					"Bot function is dependent on this window.\nClosing it will shutdown the bot.\nAre you sure?",
					"Shutdown Bot", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
				Bot.shutdown();
			return;
		}
		
		console.dispose();
		console = null;
	}
	
	private static PrintStream stdoutOld;
	private static PrintStream stderrOld;
	
	/**
	 * Restores System.out & System.err to their original state in the case that they are changed by buildTextArea().
	 */
	public static void restoreSTDPrintStreams() {
		if(stdoutOld != null) {
			System.setOut(stdoutOld);
			Logger.info("stdout restored.");
		}
		if(stderrOld != null) System.setErr(stderrOld);
	}
	
	/**
	 * Loads the text area object and redirection System.out and System.err to print to the text area.
	 */
	private static void buildTextArea() {
		if(consoleOutput != null) return;
		consoleOutput = new JTextArea();
		consoleOutput.setEditable(false);
		
		PrintStream out = new PrintStream(new OutputStream() {
			@Override public void write(int i) throws IOException {
				char c = (char)i;
				consoleOutput.append(String.valueOf(c));
				if(c == '\n') consoleOutput.update(consoleOutput.getGraphics());
			}
		});
		
		System.out.println("Switching stdout & stderr to the Console Window...");
		
		stdoutOld = System.out;
		stderrOld = System.err;
		
		System.setOut(out);
		System.setErr(out);
	}
	
	/**
	 * Activates the Windows Tray icon that this application will run out of. <br>
	 * Will not do anything for non-Windows systems.
	 */
	private static void activateTrayIcon() {
		if(!SystemTray.isSupported() || trayActive)
			return;
		
		try {
			PopupMenu popup = new PopupMenu();
			MenuItem close = new MenuItem("Close Bot");
			close.addActionListener(e -> Bot.shutdown());
			popup.add(close);
			
			SystemTray tray = SystemTray.getSystemTray();
			InputStream imageStream = Console.class.getClassLoader().getResourceAsStream("tray.png");
			if(imageStream == null) {
				Logger.err("tray.png resource not found! The tray has failed to load.");
				return;
			}
			Image icon = ImageIO.read(imageStream);
			TrayIcon trayIcon = new TrayIcon(icon, Version.getVersion().getName(), popup);
			trayIcon.setImageAutoSize(true);
			trayIcon.addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1)
						buildConsole();
				}
			});
			tray.add(trayIcon);
		}catch(AWTException | IOException e) {
			e.printStackTrace();
		}
		
		trayActive = true;
	}
}
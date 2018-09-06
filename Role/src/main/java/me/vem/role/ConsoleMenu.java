package me.vem.role;

import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * This is the JFrame window that shows the out stream. Removes the need for the jar file to be run with CommandPrompt.
 * @author Vemahk
 */
public class ConsoleMenu extends JFrame{
	private static final long serialVersionUID = -8625918584572386761L;

	public ConsoleMenu() {
		super("Rolebot Console");
		
		this.setResizable(false);
		
		JScrollPane scroll = new JScrollPane(Bot.outArea);
		scroll.setPreferredSize(new Dimension(500, 300));
		JPanel panel = new JPanel();
		
		panel.add(scroll);
		this.add(panel);
		
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		
		this.addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent arg0) {}
			public void windowClosed(WindowEvent arg0) {}
			public void windowClosing(WindowEvent e) {
				Bot.openConsole = null;
			}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
		});
	}
	
	
	
}

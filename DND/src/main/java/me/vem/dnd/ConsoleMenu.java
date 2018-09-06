package me.vem.dnd;

import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ConsoleMenu extends JFrame{
	private static final long serialVersionUID = -8625918584572386761L;

	public ConsoleMenu() {
		super("DNDBot Console");
		
		this.setResizable(false);
		
		JScrollPane scroll = new JScrollPane(Main.outArea);
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
				Main.openConsole = null;
			}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
		});
	}
	
	
	
}

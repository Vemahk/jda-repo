package me.vem.jdab.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {

	private static enum Severity{ INFO, WARNING, ERROR, DEBUG; }
	
	public static void multithread() {
		PrintThread.init();
	}
	
	private static final SimpleDateFormat dateTime = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
	public static void setupFileLogging() {
		PrintThread.init();
		File f = ExtFileManager.getFile("logs", dateTime.format(Calendar.getInstance().getTime()));
		
		try {
			PrintStream out = new PrintStream(f);
			PrintThread.addSTDOut(out);
			PrintThread.addSTDErr(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void log(Severity sev, Object o){
		String outStr = String.format("[%s][%s] %s", getFormattedTime(), sev, o);
		if(sev == Severity.ERROR)
			 System.err.println(outStr);
		else System.out.println(outStr);
	}
	
	private static void logf(Severity sev, String f, Object... objs) { log(sev, String.format(f, objs)); }
	
	public static void info(Object o) { log(Severity.INFO, o); }
	public static void warn(Object o) { log(Severity.WARNING, o); }
	public static void err(Object o) { log(Severity.ERROR, o); }
	public static void debug(Object o) { log(Severity.DEBUG, o); }
	
	public static void infof(String f, Object... objs) { logf(Severity.INFO, f, objs); }
	public static void warnf(String f, Object... objs) { logf(Severity.WARNING, f, objs); }
	public static void errf(String f, Object... objs) { logf(Severity.ERROR, f, objs); }
	public static void debugf(String f, Object... objs) { logf(Severity.DEBUG, f, objs); }
	
	private static SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");
	public static String getFormattedTime() {
		return hms.format(Calendar.getInstance().getTime());
	}
}
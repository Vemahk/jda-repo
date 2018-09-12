package me.vem.bot.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {

	private enum Severity{ INFO, WARNING, ERROR, DEBUG; }
	
	private static void log(Severity sev, String s){
		String outStr = String.format("[%s][%s][%s]: %s", getFormattedTime(), Version.getVersion().getName(), sev, s);
		if(sev == Severity.ERROR)
			 System.err.println(outStr);
		else System.out.println(outStr);
	}
	
	private static void logf(Severity sev, String f, Object... objs) { log(sev, String.format(f, objs)); }
	
	public static void info(String s) { log(Severity.INFO, s); }
	public static void warn(String s) { log(Severity.WARNING, s); }
	public static void err(String s) { log(Severity.ERROR, s); }
	public static void debug(String s) { log(Severity.DEBUG, s); }
	
	public static void infof(String f, Object... objs) { logf(Severity.INFO, f, objs); }
	public static void warnf(String f, Object... objs) { logf(Severity.WARNING, f, objs); }
	public static void errf(String f, Object... objs) { logf(Severity.ERROR, f, objs); }
	public static void debugf(String f, Object... objs) { logf(Severity.DEBUG, f, objs); }
	
	private static SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");
	public static String getFormattedTime() {
		return hms.format(Calendar.getInstance().getTime());
	}
}
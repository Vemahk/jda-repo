package me.vem.dbgm.cmd;

import java.util.LinkedHashMap;

import me.vem.jdab.cmd.Command;

public abstract class SecureCommand extends Command{
	
	private static LinkedHashMap<String, SecureCommand> database = new LinkedHashMap<>();
	public static boolean isSecureCommand(String cmdname) { return database.containsKey(cmdname); }
	public static SecureCommand getSecureCommand(String cmdname) { return database.get(cmdname); }
	public static String[] getSecureCommandLabels() {
		return database.keySet().toArray(new String[0]);
	}
	
	private int permissionLevel;
	
	protected SecureCommand(String cmdname) {
		super(cmdname);
		database.put(cmdname, this);
	}
	
	protected SecureCommand(String cmdname, int level) {
		this(cmdname);
		setPermissionLevel(level);
	}
	
	public int getPermissionLevel() { return permissionLevel; }
	public SecureCommand setPermissionLevel(int level) {
		permissionLevel = level;
		return this;
	}
}

package me.vem.dbgm.cmd;

import java.util.HashMap;

import me.vem.jdab.cmd.Command;

public abstract class SecureCommand extends Command{
	
	private static HashMap<Long, SecureCommand> database = new HashMap<>();
	public static SecureCommand getFromToken(long token) { return database.get(token); }
	
	protected SecureCommand(String cmdname) {
		super(cmdname);
		database.put(getToken(), this);
	}
	
	public abstract int getPermissionLevel();
	public abstract SecureCommand setPermissionLevel(int i);
	public abstract long getToken();
	public abstract SecureCommand setToken(long l);
}

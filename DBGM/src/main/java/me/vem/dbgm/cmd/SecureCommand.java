package me.vem.dbgm.cmd;

public abstract class SecureCommand extends Command{
	protected SecureCommand(String cmdname) { super(cmdname); }
	public abstract int getPermissionLevel();
	public abstract SecureCommand setPermissionLevel(int i);
	public abstract long getToken();
	public abstract SecureCommand setToken(long l);
}

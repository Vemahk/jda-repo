package me.vem.bot.cmd;

public interface SecureCommand extends Command{
	public int getPermissionLevel();
	public SecureCommand setPermissionLevel(int i);
	public long getToken();
	public SecureCommand setToken(long l);
}

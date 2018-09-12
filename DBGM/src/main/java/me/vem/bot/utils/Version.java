package me.vem.bot.utils;

public class Version {

	private static final Version instance = new Version(0, 0, 0, 0, "DBGM");
	public static Version getVersion() {
		return instance;
	}
	
	private int major, minor, rev, build;
	private String name;
	
	private Version(int major, int minor, int rev, int build, String name) {
		this.major = major;
		this.minor = minor;
		this.rev = rev;
		this.build = build;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public String toString() {
		return String.format("%s[%d.%d.%d_%d]", name, major, minor, rev, build);
	} //3.2.4_512
	
}

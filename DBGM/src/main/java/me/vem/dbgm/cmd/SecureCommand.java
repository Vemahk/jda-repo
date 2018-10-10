package me.vem.dbgm.cmd;

import java.util.List;

import me.vem.jdab.cmd.Command;

public abstract class SecureCommand extends Command{

	protected SecureCommand(String cmdname) {super(cmdname);}
	
	public abstract List<String> getValidKeySet();
	
}

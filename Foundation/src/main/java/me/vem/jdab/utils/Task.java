package me.vem.jdab.utils;

import java.util.TimerTask;

public class Task extends TimerTask {
	private Runnable func;
	public Task(Runnable r) { this.func = r; }
	@Override public void run() { func.run(); }
}
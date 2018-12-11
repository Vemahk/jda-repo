package me.vem.dbgm.requ;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import me.vem.jdab.utils.Task;

public final class Request {

	private static long next = 0;
	private static Timer timeoutQueue = new Timer();
	private static Queue<Request> active = new LinkedList<>();
	
	private static Request get(long id) {
		for(Request r : active)
			if(r.getId() == id) 
				return r;
		return null;
	}
	
	public static void shutdown() {
		timeoutQueue.cancel();
		active.clear();
	}
	
	public static boolean accept(Object caller, long id) {
		Request target = get(id);
		if(target != null && target.isActive && caller.equals(target.owner)) {
			target.accept();
			return true;
		}
		return false;
	}
	
	public static boolean deny(Object caller, long id) {
		Request target = get(id);
		if(target != null && target.isActive && caller.equals(target.owner)) {
			target.deny();
			return true;
		}
		return false;
	}
	
	public static void makeRequest(Object owner, Runnable success, Runnable failure) {
		new Request(owner, success, failure);
	}
	
	private final Object owner;
	private final Runnable success, failure;
	private final long id;
	private long timeout;
	private boolean isActive;
	
	private Request(Object owner, Runnable success, Runnable failure) {
		this.owner = owner;
		this.success = success;
		this.failure = failure;
		this.id = next++;
		timeout = -1;
		
		active.add(this);
	}

	public long getId() { return id; }
	public boolean isActive() { return isActive; }
	
	public Request setTimeout(long timeout) {
		return setTimeout(timeout, TimeUnit.MILLISECONDS);
	}
	
	public Request setTimeout(long duration, TimeUnit unit){
		if(timeout == -1) {
			timeoutQueue.schedule(new Task(() -> {
				if(isActive()) deny();
			}), timeout = unit.toMillis(duration));
		}
		return this;
	}
	
	public Request setTimeout(long timeout, Runnable timeoutAction) {
		return setTimeout(timeout, TimeUnit.MILLISECONDS, timeoutAction);
	}
	
	public Request setTimeout(long duration, TimeUnit unit, Runnable timeoutAction) {
		if(timeout == -1) {
			timeoutQueue.schedule(new Task(() -> {
				if(isActive()) {
					deny();
					timeoutAction.run();
				}
			}), timeout = unit.toMillis(duration));
		}
		return this;
	}
	
	public void deactivate() {
		isActive = false;
		active.remove(this);
	}
	
	public void accept() {
		success.run();
		deactivate();
	}
	
	public void deny() {
		failure.run();
		deactivate();
	}
}

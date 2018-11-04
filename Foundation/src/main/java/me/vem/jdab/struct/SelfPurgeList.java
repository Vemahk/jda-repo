package me.vem.jdab.struct;

import java.util.Collection;
import java.util.Iterator;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

public class SelfPurgeList implements Collection<Message>, Iterable<Message>{
	
	private static final int msptw = 1000 * 60 * 60 * 24 * 7 * 2; //msptw -> milliseconds per two weeks.
	
	private Message[] list;
	private int len;
	
	private TextChannel linkedChannel;
	
	public SelfPurgeList(TextChannel tc){
		list = new Message[100];
		len = 0;
		linkedChannel = tc;
	}
	
	@Override
	public boolean add(Message e) {
		long dt = System.currentTimeMillis() - e.getCreationTime().toEpochSecond() * 1000;
		if(dt - msptw >= -1000) //If it's two weeks or older (with a grace of a second, for paranoia reasons).
			return false;
		
		if(len >= 100) 
			clear();
		list[len++] = e;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends Message> c) {
		for(Message m : c)
			add(m);
		return true;
	}

	@Override
	public void clear() {
		if(len == 1) linkedChannel.deleteMessageById(list[0].getIdLong()).queue();
		if(len > 1)  linkedChannel.deleteMessages(this).queue();
		
		len = 0;
	}

	@Override
	public boolean contains(Object o) {
		if(!(o instanceof Message))
			return false;
		
		for(int i=0;i<len;i++)
			if(list[i] == o)
				return true;
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object m : c)
			if(!contains(m))
				return false;
		return true;
	}

	@Override public boolean isEmpty() { return len == 0; }
	@Override public int size() { return len; }

	@Override
	public Iterator<Message> iterator() {
		return new Iterator<Message>() {
			private int next = 0;
			@Override public boolean hasNext() { return next < len; }
			@Override public Message next() { return list[next++]; }
		};
	}

	@Override public Object[] toArray() { return list; } 
	
	/*
	 * Unwritten as this is a purge list. Anything added is meant to be destroyed.
	 */
	@Override public boolean remove(Object o) { return false; }
	@Override public boolean removeAll(Collection<?> c) { return false; }
	@Override public boolean retainAll(Collection<?> c) { return false; }

	/*
	 * Unimplemented because this is not a generic list.
	 */
	@Override public <T> T[] toArray(T[] a) { return null; }
}
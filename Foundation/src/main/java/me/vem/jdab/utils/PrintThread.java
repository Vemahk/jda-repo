package me.vem.jdab.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class PrintThread extends Thread{
	
	private static PrintStream oldout, olderr;
	
	public static final PrintStream out = new PrintStream(new OutputStream() {
		@Override public void write(int i) throws IOException {
			getInstance().outQ.write(i);
		}
	});
	
	public static final PrintStream err = new PrintStream(new OutputStream() {
		@Override public void write(int i) throws IOException {
			getInstance().errQ.write(i);
		}
	});
	
	private static PrintThread instance;
	private static PrintThread getInstance() {
		init();
		return instance;
	}
	
	public static void init() {
		if(instance == null) {
			instance = new PrintThread();
			
			instance.stdouts.add(oldout = System.out);
			System.setOut(out);
			
			instance.stderrs.add(olderr = System.err);
			System.setErr(err);
			
			instance.start();
		}
	}
	
	public static void kill() {
		if(instance != null)
			instance.end();
	}
	
	public static void addSTDOut(PrintStream stream) {
		getInstance().stdouts.add(stream);
	}
	
	public static void addSTDErr(PrintStream stream) {
		getInstance().stderrs.add(stream);
	}
	
	public static boolean removeSTDOut(PrintStream stream) {
		return getInstance().stdouts.remove(stream);
	}
	
	public static boolean removeSTDErr(PrintStream stream) {
		return getInstance().stderrs.remove(stream);
	}
	
	private final List<PrintStream> stdouts, stderrs;
	private final ByteQueue outQ, errQ;
	
	private PrintThread() {
		super("Print Thread");
		stdouts = new LinkedList<>();
		stderrs = new LinkedList<>();
		
		outQ = new ByteQueue(1 << 16);
		errQ = new ByteQueue(1 << 16);
		
		//this.setDaemon(true);
	}
	
	@Override public void run() {
		try {
			for(;!end;) {
				while(!end && outQ.isEmpty() && errQ.isEmpty())
					sleep(1);
				emptyOutQueue();
				emptyErrQueue();
			}
		} catch (InterruptedException e) { }

		System.setOut(oldout);
		System.setErr(olderr);
		instance = null;
	}
	
	private void emptyOutQueue() {
		while(!outQ.isEmpty()) {
			int i = outQ.readInt();
			for(PrintStream stream : stdouts)
				stream.write(i);
		}
	}
	
	private void emptyErrQueue() {
		while(!errQ.isEmpty()) {
			int i = errQ.readInt();
			for(PrintStream stream : stderrs)
				stream.write(i);
		}
	}
	
	private boolean end;
	public void end() {
		end = true;
	}
	
	class ByteQueue{
		private byte[] queue;
		private int read, write, size;
		
		public ByteQueue(int size) {
			queue = new byte[size];
			read = write = size = 0;
		}
		
		public void write(char c) { write((byte)c); }
		public void write(int i) { write((byte)i); }
		public void write(byte b) {
			if(size >= capacity()) 
				throw new RuntimeException("ByteQueue is full: cannot write");
			
			queue[write++] = b;
			size++;
			if(write == capacity())
				write = 0;
		}

		public char readChar() { return (char)readInt(); }
		public int readInt() { return read() & 0xFF; }
		public byte read() {
			if(isEmpty())
				throw new RuntimeException("ByteQueue is empty: cannot read");
			byte ret = queue[read++];
			size--;
			if(read == capacity())
				read = 0;
			return ret;
		}
		
		public int capacity() { return queue.length; }
		public boolean isEmpty() { return size == 0; }
		
		public void flushTo(PrintStream stream) {
			while(!isEmpty())
				stream.print(readChar());
		}
	}
}
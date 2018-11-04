package me.vem.jdab.struct;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SortedLinkedSet<T extends Comparable<T>> implements Set<T>{

	private Node<T> head, tail;
	private int size;
	
	public SortedLinkedSet() {}
	
	@Override public boolean add(T element) {
		Node<T> cur = head;
		
		int lastComp = 0;
		while(cur != null && (lastComp = element.compareTo(cur.getData())) > 0)
			cur = cur.getNext();
		
		
		if(lastComp == 0 && cur != null)
			return false;
		
		Node<T> node = new Node<>(element);
		if(cur == null) { //Then push_back
			if(head == null)
				head = node;
			else {
				tail.setNext(node);
				node.setPrev(tail);
			}
			
			tail = node;
		} else { //Then add before
			if(cur.hasPrev()) {
				node.setPrev(cur.getPrev());
				cur.getPrev().setNext(node);
			}else head = node;
			
			node.setNext(cur);
			cur.setPrev(node);
		}
		
		size++;
		return true;
	}

	@Override public boolean addAll(Collection<? extends T> col) {
		boolean out = false;
		for(T t : col)
			if(add(t))
				out = true;
		return out;
	}

	@Override public void clear() {
		head = null;
		tail = null;
		size = 0;
	}

	@Override public boolean contains(Object e) {
		for(T data : this)
			if(data.equals(e))
				return true;
		return false;
	}

	@Override public boolean containsAll(Collection<?> col) {
		for(Object o : col)
			if(!contains(o))
				return false;
		return true;
	}

	@Override public boolean isEmpty() { return size==0; }
	@Override public int size() { return size; }

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>(){
			private Node<T> next = head;
			@Override public boolean hasNext() {
				return next != null;
			}

			@Override public T next() {
				T out = next.getData();
				next = next.getNext();
				return out;
			}
		};
	}

	@Override public boolean remove(Object o) {
		Node<T> cur = head;
		while(cur != null && !cur.getData().equals(o))
			cur = cur.getNext();
		
		if(cur == null)
			return false;
		
		if(cur == head) head = cur.getNext();
		if(cur == tail) tail = cur.getPrev();
		if(cur.hasNext()) cur.getNext().setPrev(cur.getPrev());
		if(cur.hasPrev()) cur.getPrev().setNext(cur.getNext());
		
		size--;
		return true;
	}

	@Override public boolean removeAll(Collection<?> c) {
		boolean out = false;
		for(Object o : c)
			if(remove(o))
				out = true;
		return out;
	}

	/**
	 * Unimplemented; do not call.
	 */
	@Override public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("retailAll is not implemented by the SortedLinkedSet.");
	}

	@Override
	public Object[] toArray() {
		Object[] out = new Object[size];
		
		int i=0;
		for(T data : this)
			out[i++] = data;
		
		return out;
	}
	
	@SuppressWarnings("unchecked")
	@Override public <Type> Type[] toArray(Type[] a) {
		if(a.length < size)
			a = (Type[]) Array.newInstance(a.getClass().getComponentType(), size);
		else if(a.length > size) 
			a[size] = null;
		
		int i=0;
		for(T data : this)
			a[i++] = (Type) data;
		
		return a;
	}
	
	@Override
	public String toString() {
		StringBuilder out = new StringBuilder("[");
		Iterator<T> iter = this.iterator();
		while(iter.hasNext()) {
			out.append(iter.next().toString());
			if(iter.hasNext())
				out.append(", ");
		}
		return out.append(']').toString();
	}
}

class Node<T>{
	private Node<T> prev, next;
	private final T data;
	
	public Node(T data) { this.data = data; }
	
	public T getData() { return data; }
	public Node<T> getNext(){ return next; }
	public Node<T> getPrev(){ return prev; }
	public boolean hasNext() { return next != null; }
	public boolean hasPrev() { return prev != null; }
	public void setNext(Node<T> newNext) { next = newNext; }
	public void setPrev(Node<T> newPrev) { prev = newPrev; }
}
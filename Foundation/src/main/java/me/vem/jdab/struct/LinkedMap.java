package me.vem.jdab.struct;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class LinkedMap<K, V> implements Map<K, V>{

	private Node<Entry<K, V>> head;
	private Node<Entry<K, V>> tail;
	private int size;
	
	public LinkedMap(){}
	
	@Override public void clear() {
		head = null;
		tail = null;
		size = 0;
		//And let JAVA GC handle the rest
	}

	@Override public boolean containsKey(Object key) {
		for(Node<Entry<K, V>> node = head; node != null; node = node.getNext()) 
			if(node.getData().getKey().equals(key))
				return true;
		return false;
	}

	@Override public boolean containsValue(Object value) {
		for(Node<Entry<K, V>> node = head; node != null; node = node.getNext())
			if(node.getData().getValue().equals(value))
				return true;
		return false;
	}

	@Override public Set<Entry<K, V>> entrySet() {
		return new Set<Entry<K, V>>(){

			@Override
			public boolean contains(Object arg0) {
				for(Node<Entry<K, V>> node = head; node != null; node = node.getNext())
					if(node.getData().equals(arg0))
						return true;
				return false;
			}

			@Override
			public boolean containsAll(Collection<?> arg0) {
				for(Object o : arg0)
					if(!contains(o))
						return false;
				return true;
			}

			@Override
			public Iterator<Entry<K, V>> iterator() {
				return new Iterator<Entry<K, V>>(){
					private Node<Entry<K, V>> next = head;
					
					@Override public boolean hasNext() { return next != null; }

					@Override public Entry<K, V> next() {
						Entry<K, V> out = next.getData();
						next = next.getNext();
						return out;
					}
				};
			}

			@Override public boolean isEmpty() { return size==0; }
			@Override public int size() { return size; }
			
			/** Read Only */
			@Override public boolean add(Entry<K, V> arg0) {return false; }
			@Override public boolean addAll(Collection<? extends Entry<K, V>> arg0) { return false; }
			@Override public void clear() { }
			@Override public boolean remove(Object arg0) { return false; }
			@Override public boolean removeAll(Collection<?> arg0) { return false; }
			@Override public boolean retainAll(Collection<?> arg0) { return false; }
			
			/** Unimplemented. */
			@Override public Object[] toArray() { return null; }
			@Override public <T> T[] toArray(T[] arg0) { return null; }
		};
	}

	@Override public V get(Object key) {
		for(Node<Entry<K, V>> node = head; node != null; node = node.getNext())
			if(node.getData().getKey().equals(key))
				return node.getData().getValue();
		return null;
	}

	@Override public boolean isEmpty() {
		return size==0;
	}

	@Override public Set<K> keySet() {
		return new Set<K>() {

			@Override
			public boolean contains(Object o) {
				return containsKey(o);
			}

			@Override
			public boolean containsAll(Collection<?> c) {
				for(Object o : c)
					if(!contains(o))
						return false;
				return true;
			}

			@Override
			public Iterator<K> iterator() {
				return new Iterator<K>() {
					private Node<Entry<K, V>> next = head;
					
					@Override public boolean hasNext() { return next != null; }

					@Override public K next() {
						K out = next.getData().getKey();
						next = next.getNext();
						return out;
					}
				};
			}
			
			@Override public boolean isEmpty() { return size==0; }
			@Override public int size() { return size; }
			
			/** Read Only */
			@Override public boolean add(K arg0) {return false; }
			@Override public boolean addAll(Collection<? extends K> arg0) { return false; }
			@Override public void clear() { }
			@Override public boolean remove(Object arg0) { return false; }
			@Override public boolean removeAll(Collection<?> arg0) { return false; }
			@Override public boolean retainAll(Collection<?> arg0) { return false; }
			
			/** Unimplemented. */
			@Override public Object[] toArray() { return null; }
			@Override public <T> T[] toArray(T[] arg0) { return null; }
			
		};
	}

	@Override public V put(K key, V value) {
		for(Entry<K, V> entry : entrySet())
			if(entry.getKey().equals(key))
				return entry.setValue(value);
		
		Entry<K, V> entry = new EntryImpl<>(key, value);
		Node<Entry<K, V>> node = new Node<>(entry);
		
		if(tail == null) {
			head = node;
			tail = node;
			return null;
		}
		
		tail.setNext(node);
		tail = node;
		
		size++;
		return null;
	}

	@Override public void putAll(Map<? extends K, ? extends V> m) {
		for(Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override public V remove(Object key) {
		Node<Entry<K, V>> prev = null;
		Node<Entry<K, V>> node = head;
		while(node != null) {
			if(node.getData().getKey().equals(key)){
				V out = node.getData().getValue();
				
				if(node == head)
					head = node.getNext();
				
				if(node == tail)
					tail = prev;
				
				if(prev != null)
					prev.setNext(node.getNext());
				
				size--;
				return out;
			}
			
			prev = node;
			node = node.getNext();
		}
		
		return null;
	}

	@Override public int size() { return size; }

	@Override public Collection<V> values() {
		return new Collection<V>() {
			
			@Override public boolean contains(Object o) {
				if(head == null) return false;
				
				Node<Entry<K, V>> step = null;
				do {
					if(step == null) step = head;
					else step = step.getNext();
					
					if(step.getData().getValue().equals(o))
						return true;
				}while(step != tail);
				return false;
			}

			@Override public boolean containsAll(Collection<?> c) {
				for(Object o : c)
					if(!contains(o))
						return false;
				return true;
			}

			@Override public boolean isEmpty() {
				return size == 0;
			}

			@Override
			public Iterator<V> iterator() {
				return new Iterator<V>() {

					private Node<Entry<K, V>> step = head;
					
					@Override public boolean hasNext() { 
						return step != null;
					}

					@Override public V next() {
						V out = step.getData().getValue();
						step = step.getNext();
						return out;
					}					
				};
			}

			@Override public int size() {
				return size;
			}
			
			/** View only. */
			@Override public boolean add(V e) { return false; }

			/** View only */
			@Override public boolean addAll(Collection<? extends V> c) { return false; }

			/** View only. */
			@Override public void clear() { }

			/** View only. */
			@Override public boolean remove(Object o) { return false; }

			/** View only. */
			@Override public boolean removeAll(Collection<?> c) { return false; }

			/** View only. */
			@Override public boolean retainAll(Collection<?> c) { return false; }

			/** Unimplemented. */
			@Override public Object[] toArray() { return null; }

			/** Unimplemented. */
			@Override public <T> T[] toArray(T[] a) { return null; }
		};
	}

	
	private static class Node<E>{
		private Node<E> next;
		private E data;
		
		public Node(E data){
			this.data = data;
		}
		
		public Node<E> setNext(Node<E> next) {
			this.next = next;
			return this;
		}
		
		public Node<E> getNext(){ return next; }
		public E getData() { return data; }
	}
}

final class EntryImpl<K, V> implements Map.Entry<K, V>{

	private final K key;
	private V val;
	
	public EntryImpl(K key, V val) {
		this.key = key;
		this.val = val;
	}
	
	@Override public K getKey() { return key; }
	@Override public V getValue() {return val; }
	@Override public V setValue(V value) {
		V old = val;
		val = value;
		return old;
	} 
	
}
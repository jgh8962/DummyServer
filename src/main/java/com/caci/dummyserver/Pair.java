package com.caci.dummyserver;

import java.util.Map;

public class Pair<K, V> implements Map.Entry<K, V> {
	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	private K key;
	public K getKey() {
		return key;
	}
	public void setKey(K key) {
		this.key = key;
	}
	
	private V value;
	public V getValue() {
		return value;
	}
	public V setValue(V value) {
		this.value = value;
		return this.value;
	}
}

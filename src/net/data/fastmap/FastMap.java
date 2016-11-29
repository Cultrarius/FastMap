package net.data.fastmap;

import java.io.Serializable;
import java.util.*;

public class FastMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int INITIAL_CAPACITY = 16;

    private int size;
    private int capacity;

    private List<Integer> hashes;
    private List<MapEntry> entries;

    public FastMap(int capacity) {
        this.capacity = capacity;
        hashes = new ArrayList<>(capacity);
        entries = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            hashes.add(null);
            entries.add(null);
        }
    }

    public FastMap() {
        this(INITIAL_CAPACITY);
    }

    public FastMap(Map<? extends K, ? extends V> m) {
        this((int) Math.ceil(m.size() * 1.3));
        putAll(m);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return getEntry(hash(key), key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return entries.stream().anyMatch(e -> value == null ? e.value == null : value.equals(e.value));
    }

    @Override
    public V get(Object key) {
        int hash = hash(key);
        MapEntry entry = getEntry(hash, key);
        return entry == null ? null : entry.value;
    }

    @Override
    public V put(K key, V value) {
        int hash = hash(key);
        return putEntry(new MapEntry(key, value, hash));
    }

    private V putEntry(MapEntry entry) {
        if (getLoadFactor() >= 0.95 || (capacity - size == 0)) {
            increaseSize();
        }
        int pos = toPosition(entry.hash);
        int distance = 0;

        while (true) {
            Integer prevHash = hashes.get(pos);
            if (prevHash == null) {
                // found a new home
                size++;
                hashes.set(pos, entry.hash);
                entries.set(pos, entry);
                return null;
            }

            if (prevHash == entry.hash) {
                MapEntry prevEntry = entries.get(pos);
                if (keysEqual(prevEntry.key, entry.key)) {
                    // replace the old entry
                    hashes.set(pos, entry.hash);
                    entries.set(pos, entry);
                    return prevEntry.value;
                }
            }

            if (distance(prevHash, pos) < distance) {
                // take from the rich, give to the poor
                MapEntry prevEntry = entries.get(pos);
                hashes.set(pos, entry.hash);
                entries.set(pos, entry);
                entry = prevEntry;
            }
            pos = (pos + 1) % capacity;
            distance++;

            if (distance > capacity) {
                throw new IllegalStateException("No free map space, this should never happen. The map is not thread safe!");
            }
        }
    }

    private boolean keysEqual(Object key1, Object key2) {
        return (key1 == null && key2 == null) || (key1 != null && key1.equals(key2));
    }

    private int toPosition(int hash) {
        return Integer.remainderUnsigned(hash, capacity);
    }

    private int distance(int hash, int pos) {
        int desiredPos = toPosition(hash);
        return pos < desiredPos ? (capacity - desiredPos + pos) : pos - desiredPos;
    }

    private void increaseSize() {
        capacity = Math.multiplyExact(capacity, 2);

        List<MapEntry> oldEntries = entries;
        hashes = new ArrayList<>(capacity);
        entries = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            hashes.add(null);
            entries.add(null);
        }

        oldEntries.stream().filter(e -> e != null).forEach(this::putEntry);
    }

    private MapEntry getEntry(int hash, Object key) {
        int pos = toPosition(hash);
        int distance = 0;

        while (true) {
            Integer searchHash = hashes.get(pos);
            if (searchHash == null) {
                // found nothing
                return null;
            }
            if (searchHash == hash) {
                MapEntry searchEntry = entries.get(pos);
                if (keysEqual(key, searchEntry.key)) {
                    return searchEntry;
                }
            }

            if (distance(searchHash, pos) < distance) {
                // if it were here, we would have found it by now
                return null;
            }
            pos = (pos + 1) % capacity;
            distance++;

            if (distance > capacity) {
                throw new IllegalStateException("Searched over map bounds; this should never happen. The map is not thread safe!");
            }
        }
    }

    @Override
    public V remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        hashes.clear();
        entries.clear();
        size = 0;
    }

    private float getLoadFactor() {
        return size / (float) capacity;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        entries.forEach(e -> {
            if (e == null) {
                return;
            }
            b.append(e.key == null ? "null" : e.key.toString());
            b.append(" => ");
            b.append(e.value == null ? "null" : e.value.toString());
            b.append(", ");
        });
        if (b.length() > 0) {
            b.deleteCharAt(b.length() - 1);
            b.deleteCharAt(b.length() - 1);
        }
        b.append('}');
        return b.toString();
    }

    private static int hash(Object key) {
        if (key == null) {
            return 0;
        }
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private class MapEntry {
        final K key;
        final V value;
        final int hash;

        MapEntry(K key, V value, int hash) {
            this.key = key;
            this.value = value;
            this.hash = hash;
        }
    }

    private class EntryIterator implements Iterator<Entry<K, V>> {
        private int i = 0;

        @Override
        public boolean hasNext() {
            return i < size;
        }

        @Override
        public Entry<K, V> next() {
            while (i < size) {
                MapEntry entry = entries.get(i);
                if (entry != null) {
                    return new Entry<K, V>() {
                        @Override
                        public K getKey() {
                            return entry.key;
                        }

                        @Override
                        public V getValue() {
                            return entry.value;
                        }

                        @Override
                        public V setValue(V value) {
                            return put(entry.key, value);
                        }
                    };
                }
                i++;
            }
            throw new IllegalStateException("Illegal iterator position! Maybe the map was modified while iterating.");
        }
    }

    private class EntrySet implements Set<Entry<K, V>> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(Entry<K, V> kvEntry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Entry<K, V>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }
}

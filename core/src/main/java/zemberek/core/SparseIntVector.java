package zemberek.core;

import java.util.Iterator;

public class SparseIntVector implements Iterable<SparseIntVector.TableEntry> {

    static final int INITIAL_SIZE = 8;
    static final double DEFAULT_LOAD_FACTOR = 0.6;
    private int modulo = INITIAL_SIZE - 1;
    int[] keys;
    int[] values;
    int keyCount;
    int threshold = (int) (INITIAL_SIZE * DEFAULT_LOAD_FACTOR);
    private int removeCount;

    public SparseIntVector() {
        keys = new int[INITIAL_SIZE];
        values = new int[INITIAL_SIZE];
    }

    public SparseIntVector(int size) {
        int k = INITIAL_SIZE;
        while (k < size)
            k <<= 1;
        keys = new int[k];
        values = new int[k];
        threshold = (int) (k * DEFAULT_LOAD_FACTOR);
        modulo = k - 1;
    }

    private int hash(int key) {
        return key & modulo;
    }

    /*
     * locate operation does the following:
     * - finds the slot
     * - if there was a deleted key before (flag[slot]==-1) and pointer is not set yet (pointer==-1) pointer is set to this
     *   slot index and index is incremented.
     *   This is necessary for the following problem.
     *   Suppose we add key 5 first then key 9 with key clash. first one is put to slotindex=1 and the other one is
     *   located to slot=2. Then we erase the key 5. Now if we do not use the flag, and want to access the value of key 9.
     *   we would get a 0 because slot will be 1 and key does not exist there.
     *   that is why we use a flag for marking deleted slots. So when getting a value we pass the deleted slots. And when we insert,
     *   we use the first deleted slot if any.
     *    Key Val  Key Val  Key Val
     *     0   0    0   0    0   0
     *     5   2    5   2    -1  0
     *     0   0    9   3    9   3
     *     0   0    0   0    0   0
     * - if there was no deleted key in that slot, check the value. if value is 0 then we can put our key here. However,
     *   we cannot return the slot value immediately. if pointer value is set, we use it as the vacant index. we do not use
     *   the slot or the pointer value itself. we use negative of it, pointing the key does not exist in this list. Also we
     *   return -slot-1 or -pointer-1 to avoid the 0 index problem.
     *
     */
    private int locate(int key) {
        int slot = hash(key);
        int pointer = -1;
        while (true) {
            final int k = keys[slot];
            if (k < 0) {
                if (pointer < 0) {
                    pointer = slot;
                }
                slot = (slot + 1) & modulo;
                continue;
            }
            if (values[slot] == 0) {
                return pointer < 0 ? (-slot - 1) : (-pointer - 1);
            }
            if (k == key)
                return slot;
            slot = (slot + 1) & modulo;
        }
    }

    public int increment(int key) {
        return incrementByAmount(key, 1);
    }

    public int get(int key) {
        if (key < 0)
            throw new IllegalArgumentException("Key cannot be negative. But it is:" + key);
        int slot = hash(key);
        while (true) {
            final int k = keys[slot];
            if (k < 0) {
                slot = (slot + 1) & modulo;
                continue;
            }
            if (values[slot] == 0) {
                return 0;
            }
            if (k == key)
                return values[slot];
            slot = (slot + 1) & modulo;
        }
    }

    public int decrement(int key) {
        return incrementByAmount(key, -1);
    }

    public int incrementByAmount(int key, int amount) {
        if (key < 0)
            throw new IllegalArgumentException("Key cannot be negative. But it is:" + key);
        if (keyCount + removeCount == threshold) {
            expand();
        }
        int l = locate(key);
        if (l < 0) {
            l = -l - 1;
            values[l] = amount;
            keys[l] = key;
            keyCount++;
            return values[l];
        } else {
            values[l] += amount;
            if (values[l] == 0) {
                keyCount--;
                keys[l] = -1; // mark deletion
                removeCount++;
            }
            return values[l];
        }
    }

    public void remove(int key) {
        int k = locate(key);
        if (k < 0)
            return;
        values[k] = 0;
        keys[k] = -1; // mark deletion
        keyCount--;
        removeCount++;
    }

    private void expand() {
        SparseIntVector h = new SparseIntVector(values.length * 2);
        for (int i = 0; i < keys.length; i++) {
            if (values[i] != 0) {
                h.set(keys[i], values[i]);
            }
        }
        assert (h.keyCount == keyCount);
        this.values = h.values;
        this.keys = h.keys;
        this.keyCount = h.keyCount;
        this.modulo = h.modulo;
        this.threshold = h.threshold;
        this.removeCount = 0;
    }

    public void set(int key, int value) {
        if (key < 0)
            throw new IllegalArgumentException("Key cannot be negative. But it is:" + key);
        if (value == 0) {
            remove(key);
            return;
        }
        if (keyCount + removeCount == threshold) {
            expand();
        }
        int loc = locate(key);
        if (loc >= 0) {
            values[loc] = value;
            return;
        }
        loc = -loc - 1;
        keys[loc] = key;
        values[loc] = value;
        keyCount++;
    }

    public int size() {
        return keyCount;
    }

    public int capacity() {
        return keys.length;
    }

    @Override
    public Iterator<TableEntry> iterator() {
        return new TableIterator();
    }

    private class TableIterator implements Iterator<TableEntry> {

        public TableIterator() {
			super();
		}

		int i;
        int k;

        @Override
        public boolean hasNext() {
            return k < keyCount;
        }

        @Override
        public TableEntry next() {
            while (values[i] == 0) {
                i++;
            }
            TableEntry te = new TableEntry(keys[i], values[i]);
            i++;
            k++;
            return te;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class TableEntry {
        public final int key;
        public final int value;

        public TableEntry(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
}

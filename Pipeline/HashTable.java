import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;

public interface HashTable<T> {
  public void add(int key, T x);
  public boolean remove(int key);
  public T contains(int key);
  public void print();
}

class HopScotchOpenAddressHashTable<T> implements HashTable<T> {

        private class Node<T, K> {
            public K key;
            private T item;
            public AtomicInteger mark;

            public Node() {}

            public Node(K key, T item) {
                this.key = key;
                this.item = item;
                this.mark = new AtomicInteger(0);
            }
        }

	final ReentrantLock[] lock;
  	private final int maxBucketSize;
  	private final int slotMask;
  	private Node<T,Integer>[] table;
  	private int capacity;
        private final int tableSize = 1 << 21;
        private final int mask = tableSize - 1;
        private AtomicInteger timeStamp = new AtomicInteger(0);
        private int logSize;

        int parseSlot(int mark) {
            return mark & slotMask; 
        }
	
        int parseStamp(int mark) {
            return (mark | (~slotMask)) >> maxBucketSize; 
        }

        int constructMark(int slot, int stamp) {
            return (stamp << maxBucketSize) | slot;
        }

	public HopScotchOpenAddressHashTable(int capacity, int logSize, int maxBucketSize) {
	    this.logSize = logSize;
	    this.maxBucketSize = maxBucketSize;
            this.slotMask = (1 << maxBucketSize) - 1;
    	    this.table = new Node[tableSize];

            for (int i = 0; i < tableSize; i++)
                table[i] = new Node(-1, null);

	    this.capacity = capacity;
	
	    lock = new ReentrantLock[capacity];
	    for (int i = 0; i < this.capacity; i++)
		lock[i] = new ReentrantLock();
	    this.capacity--;
	}

        public void tryLock(int pre, int cur) {
            if ((pre & capacity) != (cur & capacity))
                lock[pre & capacity].lock();
        }

        public void tryUnLock(int pre, int cur) {
            if ((pre & capacity) != (cur & capacity))
                lock[pre & capacity].unlock();
        }

        public int moveForward(int cur) {
            while (true) {
                for (int move = 0; move < maxBucketSize - 1; move++) {                        
                    int pre = (cur + mask - move) & mask;

                    tryLock(pre, cur);
                    
                    if (table[pre].key != -1) {
                        int des = table[pre].key & mask;

                        int pd = (pre - des) & mask;
                        int nd = pd + move + 1;
                        if (nd < maxBucketSize) {
                            table[cur].key = table[pre].key;
                            table[cur].item = table[pre].item;
                            
                            table[pre].key = -1;
                            tryUnLock(cur, pre);
                             
                            int time = timeStamp.getAndIncrement(); 
                            while (true) {
                                int oldMark = table[des].mark.get();   
                                int oldSlot = parseSlot(oldMark);

                                if ((oldSlot & (1 << pd)) == 0) {
                                    //System.out.println("not found! wait in moveForward");
                                    continue;
                                }

                                int newSlot = (oldSlot | (1 << nd)) - (1 << pd);
                                int newMark = constructMark(newSlot, time);
                                if (table[des].mark.compareAndSet(oldMark, newMark))
                                    return move + 1;
                            }
                        }
                    }

                    tryUnLock(pre, cur);
                }
            }
        }
	
  	public void add(int key, T x) {
            //System.out.println("add");
            int bucket = key & mask;
            int i = 0;

            while (i < table.length) {
                int curBucket = (bucket + i) & mask;
                if (table[curBucket].key == -1) {
                    //System.out.println("Lock:" + (curBucket & capacity));            
                    lock[curBucket & capacity].lock();
                    if (table[curBucket].key == -1) {
                        while (i >= maxBucketSize) {
                            i = i - moveForward(curBucket);
                            curBucket = (bucket + i) & mask;
                        }
                        
                        table[curBucket].key = key;
                        table[curBucket].item = x;
                         
                        lock[curBucket & capacity].unlock();

                        int time = timeStamp.getAndIncrement(); 
                        while (true) {
                            int oldMark = table[bucket].mark.get();
                            int slot = parseSlot(oldMark);

                            if ((slot & (1 << i)) != 0) {
                                //System.out.println("No removed! wait in Add");
                                continue;
                            }

                            slot = slot | (1 << i);
                            int mark = constructMark(slot, time);
                            if (table[bucket].mark.compareAndSet(oldMark, mark))
                                return;
                        }
                    }
                    lock[curBucket & capacity].unlock();
                }
                i++;
            }
  	}
  	
  	public boolean remove(int key) {
            //System.out.println("remove");
            int bucket = key & mask;
            int mark = table[bucket].mark.get();
            int slot = parseSlot(mark); 
            
            boolean ret = false;
            for (int i = 0; i < maxBucketSize; i++) 
                if (((1 << i) & slot) != 0) {
                    int curBucket = (bucket + i) & mask;
                    if (table[curBucket].key == key) {
                        lock[curBucket & capacity].lock();
                        if (table[curBucket].key == key) {
                            table[curBucket].key = -1;
                            lock[curBucket & capacity].unlock();
                            
                            int time = timeStamp.getAndIncrement();
                            while (true) {
                                int oldMark = table[bucket].mark.get();
                                int oldSlot = parseSlot(oldMark);

                                if ((oldSlot & (1 << i)) == 0) {
                                    //System.out.println("No found! wait in remove");
                                    continue;
                                }

                                int newSlot = oldSlot - (1 << i);
                                int newMark = constructMark(newSlot, time);
                                if (table[bucket].mark.compareAndSet(oldMark, newMark))
                                    return true;
                            }
                        } 
                        lock[curBucket & capacity].unlock();
                    }
                }
            return ret;
  	}
  	
  	public T contains(int key) {                           
            //System.out.println("contains");

            int bucket = key & mask;
            int a = table[bucket].mark.get();
            int b = table[bucket].mark.get();

            int slot;
            if (a == b) {
                slot = parseSlot(a); 
            } else {
                slot = slotMask;
            }

            for (int i = 0; i < maxBucketSize; i++) 
                if (((1 << i) & slot) != 0) {
                    int curBucket = (bucket + i) & mask;

                    if (table[curBucket].key == key) {
                        //System.out.println("finish con");
                        return table[curBucket].item;
                    }
                }

            //System.out.println("finish con");
            return null;
  	}
                 
  	public void print() {
            /*
    	    for( int i = 0; i <= mask; i++ ) {
      		System.out.println("...." + i + "....");
      		if( table[i] != null)
                    table[i].printList();
    	    }
            */
  	}
        

}



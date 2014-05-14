import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;

public interface HashTable<T> {
  public void add(int key, T x);
  public boolean remove(int key);
  public boolean contains(int key);
  public void print();
}

class SerialHashTable<T> implements HashTable<T> {
  private SerialList<T,Integer>[] table;
  private int logSize;
  private int mask;
  private final int maxBucketSize;
  @SuppressWarnings("unchecked")
  public SerialHashTable(int logSize, int maxBucketSize) {
    this.logSize = logSize;
    this.mask = (1 << logSize) - 1;
    this.maxBucketSize = maxBucketSize;
    this.table = new SerialList[1 << logSize];
  }
  public void resizeIfNecessary(int key) {
    while( table[key & mask] != null 
          && table[key & mask].getSize() >= maxBucketSize )
      resize();
  }
  private void addNoCheck(int key, T x) {
    int index = key & mask;
    if( table[index] == null )
      table[index] = new SerialList<T,Integer>(key,x);
    else
      table[index].addNoCheck(key,x);
  }
  public void add(int key, T x) {
    resizeIfNecessary(key);
    addNoCheck(key,x);
  }
  public boolean remove(int key) {
    resizeIfNecessary(key);
    if( table[key & mask] != null )
      return table[key & mask].remove(key);
    else
      return false;
  }
  public boolean contains(int key) {
    SerialList<T,Integer>[] myTable = table;
    int myMask = myTable.length - 1;
    if( myTable[key & myMask] != null )
      return myTable[key & myMask].contains(key);
    else
      return false;
  }
  @SuppressWarnings("unchecked")
  public void resize() {
    SerialList<T,Integer>[] newTable = new SerialList[2*table.length];
    for( int i = 0; i < table.length; i++ ) {
      if( table[i] == null )
        continue;
      SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
      while( iterator != null ) {
        if( newTable[iterator.key & ((2*mask)+1)] == null )
          newTable[iterator.key & ((2*mask)+1)] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
        else
          newTable[iterator.key & ((2*mask)+1)].addNoCheck(iterator.key, iterator.getItem());
        iterator = iterator.getNext();
      }
    }
    table = newTable;
    logSize++;
    mask = (1 << logSize) - 1;
  }
  public void print() {
    for( int i = 0; i <= mask; i++ ) {
      System.out.println("...." + i + "....");
      if( table[i] != null)
        table[i].printList();
    }
  }
}



/*
class SerialHashTableTest {
  public static void main(String[] args) {  
    SerialHashTable<Integer> table = new SerialHashTable<Integer>(2, 8);
    for( int i = 0; i < 256; i++ ) {
      table.add(i,i*i);
    }
    table.printTable();    
  }
}
*/

class LockBasedClosedAddressHashTable<T> implements HashTable<T> {
	final ReentrantReadWriteLock[] lock;
  	private int logSize;
  	private int mask;
  	private final int maxBucketSize;
  	private SerialList<T,Integer>[] table;
  	private int capacity;
	
	public LockBasedClosedAddressHashTable(int capacity, int logSize, int maxBucketSize) {
	    this.logSize = logSize;
            this.mask = (1 << logSize) - 1;
	    this.maxBucketSize = maxBucketSize;
    	    this.table = new SerialList[1 << logSize];
	    this.capacity = capacity;
	
	    lock = new ReentrantReadWriteLock[capacity];
	    for (int i = 0; i < this.capacity; i++)
		lock[i] = new ReentrantReadWriteLock();
	    this.capacity--;
	}
	
	public boolean resizeIfNecessary(int key) {
    	    return ( table[key & mask] != null && table[key & mask].getSize() >= maxBucketSize);
  	}
	  
	private void addNoCheck(int key, T x) {
    	    int index = key & mask;
    	    if (table[index] == null)
      		table[index] = new SerialList<T,Integer>(key,x);
    	    else
      		table[index].addNoCheck(key,x);
  	}  	  	
  	
	private void addWithCheck(int key, T x) {
    	    int index = key & mask;
    	    if (table[index] == null)
      		table[index] = new SerialList<T,Integer>(key,x);
    	    else
      		table[index].add(key,x);
  	    }  	  	  	
  	
  	public void add(int key, T x) {
  	    lock[key & capacity].writeLock().lock();
  	
    	    //addWithCheck(key,x);
 	    addNoCheck(key,x);
    	    boolean isResize = resizeIfNecessary(key);
    	
  	    lock[key & capacity].writeLock().unlock();
  	    if (isResize) resize();
  	}
  	
  	public boolean remove(int key) {
  	    lock[key & capacity].writeLock().lock();
  	
  	    boolean ret = false;
    	    if( table[key & mask] != null )
      		ret = table[key & mask].remove(key);
      		
    	    lock[key & capacity].writeLock().unlock();

            return ret;
  	}
  	
  	public boolean contains(int key) {
 	    lock[key & capacity].readLock().lock();
  	
    	    SerialList<T,Integer>[] myTable = table;
    	    int myMask = myTable.length - 1;

            boolean ret = false;
    	    if( myTable[key & myMask] != null )
      		ret = myTable[key & myMask].contains(key);
	      	
	    lock[key & capacity].readLock().unlock();	      	

            return ret;
  	}
  
  	public void resize() {
            int original = table.length;
            for (int i = 0; i <= capacity; i++) {
                lock[i & capacity].writeLock().lock();
            }

            if (original == table.length) {
    	        SerialList<T,Integer>[] newTable = new SerialList[2*table.length];
    	        for( int i = 0; i < table.length; i++ ) {
      		    if( table[i] == null )
        	        continue;
      		    SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
      		    while( iterator != null ) {
        	        if( newTable[iterator.key & ((2*mask)+1)] == null )
          		    newTable[iterator.key & ((2*mask)+1)] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
        	        else
          		    newTable[iterator.key & ((2*mask)+1)].addNoCheck(iterator.key, iterator.getItem());
        	        iterator = iterator.getNext();
      		    }
    	        }
    	        table = newTable;
    	        logSize++;
    	        mask = (1 << logSize) - 1;
            }

            for (int i = 0; i <= capacity; i++) {
                lock[i & capacity].writeLock().unlock();
            }
  	}
  	
  	public void print() {
            /*
    	    for( int i = 0; i <= mask; i++ ) {
      		System.out.println("...." + i + "....");
      		if( table[i] != null)
                    table[i].printList();
    	    }
            */
            System.out.println(table.length);
  	}

}

class OptimisticClosedAddressHashTable<T> implements HashTable<T> {
	final ReentrantReadWriteLock[] lock;
  	private int logSize;
  	private int mask;
  	private final int maxBucketSize;
  	private SerialList<T,Integer>[] table;
  	private int capacity;
	
	public OptimisticClosedAddressHashTable(int capacity, int logSize, int maxBucketSize) {
	    this.logSize = logSize;
            this.mask = (1 << logSize) - 1;
	    this.maxBucketSize = maxBucketSize;
    	    this.table = new SerialList[1 << logSize];
	    this.capacity = capacity;
	
	    lock = new ReentrantReadWriteLock[capacity];
	    for (int i = 0; i < this.capacity; i++)
		lock[i] = new ReentrantReadWriteLock();
	    this.capacity--;
	}
	
	public boolean resizeIfNecessary(int key) {
    	    return ( table[key & mask] != null && table[key & mask].getSize() >= maxBucketSize);
  	}
	  
	private void addNoCheck(int key, T x) {
    	    int index = key & mask;
    	    if (table[index] == null)
      		table[index] = new SerialList<T,Integer>(key,x);
    	    else
      		table[index].addNoCheck(key,x);
  	}  	  	
  	
	private void addWithCheck(int key, T x) {
    	    int index = key & mask;
    	    if (table[index] == null)
      		table[index] = new SerialList<T,Integer>(key,x);
    	    else
      		table[index].add(key,x);
  	    }  	  	  	
  	
  	public void add(int key, T x) {
  	    lock[key & capacity].writeLock().lock();
  	
    	    //addWithCheck(key,x);
 	    addNoCheck(key,x);
    	    boolean isResize = resizeIfNecessary(key);
    	
  	    lock[key & capacity].writeLock().unlock();
  	    if (isResize) resize();
  	}
  	
  	public boolean remove(int key) {
  	    lock[key & capacity].writeLock().lock();
  	
  	    boolean ret = false;
    	    if( table[key & mask] != null )
      		ret = table[key & mask].remove(key);
      		
    	    lock[key & capacity].writeLock().unlock();

            return ret;
  	}
  	
  	public boolean contains(int key) {              
    	    SerialList<T,Integer>[] myTable = table;
    	    int myMask = myTable.length - 1;

    	    if( myTable[key & myMask] != null )
      		if (myTable[key & myMask].contains(key))
                    return true;

 	    lock[key & capacity].readLock().lock();  	

    	    myTable = table;
    	    myMask = myTable.length - 1;

            boolean ret = false;
    	    if( myTable[key & myMask] != null )
      		ret = myTable[key & myMask].contains(key);
	      	
	    lock[key & capacity].readLock().unlock();	      	

            return ret;
  	}
  
  	public void resize() {
            int original = table.length;
            for (int i = 0; i <= capacity; i++) {
                lock[i & capacity].writeLock().lock();
            }

            if (original == table.length) {
    	        SerialList<T,Integer>[] newTable = new SerialList[2*table.length];
    	        for( int i = 0; i < table.length; i++ ) {
      		    if( table[i] == null )
        	        continue;
      		    SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
      		    while( iterator != null ) {
        	        if( newTable[iterator.key & ((2*mask)+1)] == null )
          		    newTable[iterator.key & ((2*mask)+1)] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
        	        else
          		    newTable[iterator.key & ((2*mask)+1)].addNoCheck(iterator.key, iterator.getItem());
        	        iterator = iterator.getNext();
      		    }
    	        }
    	        table = newTable;
    	        logSize++;
    	        mask = (1 << logSize) - 1;
            }

            for (int i = 0; i <= capacity; i++) {
                lock[i & capacity].writeLock().unlock();
            }
  	}
  	
  	public void print() {
    	    for( int i = 0; i <= mask; i++ ) {
      		System.out.println("...." + i + "....");
      		if( table[i] != null)
                    table[i].printList();
    	    }
  	}

}

class LockFreeClosedAddressHashTable<T> implements HashTable<T> {
	final ReentrantLock[] lock;
  	private int logSize;
  	private int mask;
  	private final int maxBucketSize;
  	private SerialList<T,Integer>[] table;
  	private int capacity;
	
	public LockFreeClosedAddressHashTable(int capacity, int logSize, int maxBucketSize) {
	    this.logSize = logSize;
            this.mask = (1 << logSize) - 1;
	    this.maxBucketSize = maxBucketSize;
    	    this.table = new SerialList[1 << logSize];
	    this.capacity = capacity;
	
	    lock = new ReentrantLock[capacity];
	    for (int i = 0; i < this.capacity; i++)
		lock[i] = new ReentrantLock();
	    this.capacity--;
	}
	
	public boolean resizeIfNecessary(int key) {
    	    return ( table[key & mask] != null && table[key & mask].getSize() >= maxBucketSize);
  	}
	  
	private void addNoCheck(int key, T x) {
    	    int index = key & mask;
    	    if (table[index] == null)
      		table[index] = new SerialList<T,Integer>(key,x);
    	    else
      		table[index].addNoCheck(key,x);
  	}  	  	
  	
	private void addWithCheck(int key, T x) {
    	    int index = key & mask;
    	    if (table[index] == null)
      		table[index] = new SerialList<T,Integer>(key,x);
    	    else
      		table[index].add(key,x);
  	    }  	  	  	
  	
  	public void add(int key, T x) {
  	    lock[key & capacity].lock();
  	
    	    //addWithCheck(key,x);
 	    addNoCheck(key,x);
    	    boolean isResize = resizeIfNecessary(key);
    	
  	    lock[key & capacity].unlock();
  	    if (isResize) resize();
  	}
  	
  	public boolean remove(int key) {
  	    lock[key & capacity].lock();
  	
  	    boolean ret = false;
    	    if( table[key & mask] != null )
      		ret = table[key & mask].remove(key);
      		
    	    lock[key & capacity].unlock();

            return ret;
  	}
  	
  	public boolean contains(int key) {              
    	    SerialList<T,Integer>[] myTable = table;
    	    int myMask = myTable.length - 1;

            boolean ret = false;
    	    if( myTable[key & myMask] != null )
      		ret = myTable[key & myMask].contains(key);

            return ret;
  	}
  
  	public void resize() {
            int original = table.length;
            for (int i = 0; i <= capacity; i++) {
                lock[i & capacity].lock();
            }

            if (original == table.length) {
    	        SerialList<T,Integer>[] newTable = new SerialList[2*table.length];
    	        for( int i = 0; i < table.length; i++ ) {
      		    if( table[i] == null )
        	        continue;
      		    SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
      		    while( iterator != null ) {
        	        if( newTable[iterator.key & ((2*mask)+1)] == null )
          		    newTable[iterator.key & ((2*mask)+1)] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
        	        else
          		    newTable[iterator.key & ((2*mask)+1)].addNoCheck(iterator.key, iterator.getItem());
        	        iterator = iterator.getNext();
      		    }
    	        }
    	        table = newTable;
    	        logSize++;
    	        mask = (1 << logSize) - 1;
            }

            for (int i = 0; i <= capacity; i++) {
                lock[i & capacity].unlock();
            }
  	}
  	
  	public void print() {
    	    for( int i = 0; i <= mask; i++ ) {
      		System.out.println("...." + i + "....");
      		if( table[i] != null)
                    table[i].printList();
    	    }
  	}

}

class LinearlyProbedOpenAddressHashTable<T> implements HashTable<T> {
        private class Node<T, K> {
            public K key;
            private T item;
            public int step;

            public Node() {}

            public Node(K key, T item) {
                this.key = key;
                this.item = item;
                this.step = 0;
            }
        }

	final ReentrantLock[] lock;
  	private int logSize;
  	private int mask;
  	private final int maxBucketSize;
  	private Node<T,Integer>[] table;
  	private int capacity;
	
	public LinearlyProbedOpenAddressHashTable(int capacity, int logSize, int maxBucketSize) {
	    this.logSize = logSize;
            this.mask = (1 << logSize) - 1;
	    this.maxBucketSize = maxBucketSize;
    	    this.table = new Node[1 << logSize];

            for (int i = 0; i < (1 << logSize); i++)
                table[i] = new Node(-1, null);

	    this.capacity = capacity;
	
	    lock = new ReentrantLock[capacity];
	    for (int i = 0; i < this.capacity; i++)
		lock[i] = new ReentrantLock();
	    this.capacity--;
	}
	
	public boolean resizeIfNecessary(int key) {
    	    return ( table[key & mask].step >= maxBucketSize);
  	}
	  
  	public void add(int key, T x) {
            //System.out.println("add");
            Node<T, Integer>[] myTable = this.table;
            int myMask = myTable.length - 1;

            int currentKey = key & myMask;        

            int i = 0;
            boolean done = false;
            while (i < myTable.length) {                
                int k = (currentKey + i) & myMask;

                boolean ok = (myTable[k].key == -1);
                
                if (ok) {
                    int a = k & capacity;
                    int b = key & capacity;

                    if (a > b) {
                        int tmp = b; 
                        b = a;
                        a = tmp;
                    }
                    lock[a].lock();
                    lock[b].lock();

                    if (myTable == this.table) {
                        if (table[k].key == -1) {
                            if (i > table[currentKey].step)
                                table[currentKey].step = i;
                            table[k].key = key;
                            table[k].item = x;
                            done = true;
                        }
                    }
                    else {
                        i = -1;
                        myTable = this.table;
                        myMask = myTable.length - 1;
                    }
                    lock[a].unlock();
                    lock[b].unlock();

                    if (done) break;
                }             
                i++;
            }
  	
    	    boolean isResize = resizeIfNecessary(key);
  	    if (!done || isResize) resize();
            
            if (!done) 
                add(key, x);            
  	}
  	
  	public boolean remove(int key) {
            //System.out.println("remove");

            Node<T, Integer>[] myTable = this.table;
            int myMask = myTable.length - 1;
            int currentKey = key & myMask;
            int currentStep = myTable[currentKey].step;

            int i = 0;
            boolean ret = false;
            while (i <= currentStep) {                
                int k = (currentKey + i) & mask;

                boolean ok = (myTable[k & capacity].key == key);
                
                if (ok) {
                    lock[k & capacity].lock();

                    if (myMask == this.mask) {
                        if (table[k].key == key) {
                            table[k].key = -1;
                            ret = true;
                        }
                    }
                    else {
                        i = -1;
                        myTable = this.table;
                        myMask = myTable.length - 1;
                        currentKey = key & myMask; 
                        currentStep = myTable[currentKey].step;
                    }
                    lock[k & capacity].unlock();

                    if (ret) break;
                }
                i++;
            }

            return ret;
  	}
  	
  	public boolean contains(int key) {                           
            //System.out.println("contains");

            Node<T, Integer>[] myTable = this.table;
            int myMask = myTable.length - 1;
            int currentKey = key & myMask;
            int currentStep = myTable[currentKey].step;

            int i = 0;
            boolean ret = false;
            while (i <= currentStep) {                
                int k = (currentKey + i) & myMask;

                ret = (myTable[k].key == key);
                if (ret) break;
                i++;
            } 

            return ret;
  	}
  
  	public void resize() {
            //System.out.println("resize");
            int original = table.length;
            for (int i = 0; i <= capacity; i++) {
                lock[i & capacity].lock();
            }

            if (original == table.length) {
    	        Node<T,Integer>[] newTable = new Node[2 * table.length];
                for (int i = 0; i < 2 * table.length; i++)
                    newTable[i] = new Node(-1, null);

    	        logSize++;
    	        mask = (1 << logSize) - 1;

    	        for( int j = 0; j < table.length; j++ ) {
      		    if( table[j].key == -1 )
        	        continue;

                    int key = table[j].key;
                    T item = table[j].item;

                    int start = key & mask;
                    for (int i = 0; i < 2 * table.length; i++) {
                        int x = (start + i) & mask;
                        if (newTable[x].key == -1) {
                            if (i > newTable[start].step)
                                newTable[start].step = i;

                            newTable[x].key = key;
                            newTable[x].item = item;
                            break;
                        }
                    }
    	        }
    	        this.table = newTable;
            }

            for (int i = 0; i <= capacity; i++) {
                lock[i & capacity].unlock();
            }
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


class LockFreeConcurrentClosedAddressHashTable<T> implements HashTable<T> {        
    private class Node<T> {
        public int key;
        private T item;
        AtomicMarkableReference< Node<T> > next;

        public Node() {
            next = null;
        }

        public Node(int key, T item, Node<T> nextRef) {
            this.key = key;
            this.item = item;
            this.next = new AtomicMarkableReference< Node<T> >(nextRef, false);
        }         
    }
    
    private class Window {
        public Node<T> pred, curr;    

        public Window(Node<T> myPred, Node<T> myCurr) {
            pred = myPred;
            curr = myCurr;                    
        }
    }

    public Window find(Node<T> head, int key) {
        Node<T> pred = null, curr = null, succ = null;
        boolean[] marked = {false};
        boolean snip;

        retry: while (true) {
            pred = head;
            curr = pred.next.getReference();
            while (true) {
                succ = curr.next.get(marked);
                while (marked[0]) {
                    snip = pred.next.compareAndSet(curr, succ, false, false);

                    if (snip) {
                        curr = succ;
                    } else {
                        curr = pred.next.get(marked); 
                        if (marked[0]) continue retry;                        
                    }
                    succ = curr.next.get(marked);
                }            
                
                if (curr.key >= key)
                    return new Window(pred, curr);
                pred = curr;
                curr = succ;                
            }
        }
    }

    /*
    public Window findSplitPoint(Node<T> head, int bucket) {
        Node<T> pred = null, curr = null, succ = null;
        boolean[] marked = {false};
        boolean snip;

        retry: while (true) {
            pred = head;
            curr = pred.next.getReference();
            while (true) {
                succ = curr.next.get(marked);
                while (marked[0]) {
                    snip = pred.next.compareAndSet(curr, succ, false, false);
                    if (!snip) continue retry;
                    curr = succ;
                    succ = curr.next.get(marked);
                }            
                if ((curr.key >= bucket))
                    return new Window(pred, curr, 0);
                pred = curr;
                curr = succ;                
            }
        }
    }
    */

        private final int maxBucketSize;
        private AtomicReference< Node<T> >[] table;
        private AtomicInteger mask;
        private AtomicInteger setSize;
        final int maxCapacity = 1 << 21;
        private AtomicInteger count = new AtomicInteger(0);
	

        public int reverse(int key) {
            int ret = 0;
            for (int i = 0; i < 31; i++) {
                ret = ret | ((key & 1) << (30 - i));
                key = key >> 1;
            }
            return ret;
        }

        public int makeRegularKey(int key) {
            return reverse(key | 0x40000000);
        }

        public int makeSentryKey(int key) {
            return reverse(key);
        }

	public LockFreeConcurrentClosedAddressHashTable(int capacity, int logSize, int maxBucketSize) {
    	    this.table = new AtomicReference[maxCapacity];
            for (int i = 0; i < maxCapacity; i++)
                this.table[i] = new AtomicReference< Node<T> >(null);

            Node<T> tail = new Node<T>(makeSentryKey(0x3FFFFFFF), null, null);
            this.table[0].set(new Node<T>(makeSentryKey(0), null, tail));
            this.maxBucketSize = maxBucketSize;
            this.mask = new AtomicInteger(0);
            this.setSize = new AtomicInteger(0);
        }

        public int father(int bucket) {
            int ret = 0, i = 0;
            while (bucket > 1) {
                ret = ret | ((bucket & 1) << i);
                bucket = bucket >> 1;
                i = i + 1;
            }
            return ret;
        }

        public void newTable(int f, int bucket) {
            if (table[f].get() == null)
                newTable(father(f), f);

            int hashkey = makeSentryKey(bucket); 

            while (table[bucket].get() == null) {
                Window window = find(table[f].get(), hashkey);                    
                
                Node<T> pred = window.pred, curr = window.curr;
                if (curr.key == hashkey) continue;

                Node<T> sentry = new Node<T>(hashkey, null, curr);                    
                if (pred.next.compareAndSet(curr, sentry, false, false)) {                    
                    table[bucket].compareAndSet(null, sentry);
                }
                continue;
            }
        }

        public boolean resize() {
            //if (now + 1 == maxCapacity) 
            //    return false;

            //if (window.step >= maxBucketSize) {
            int now = mask.get();
            if (setSize.get() > now * maxBucketSize) { 
                mask.compareAndSet(now, now * 2 + 1);
                return true;
            }

            return false;
        }

  	public void add(int key, T item) {
            //System.out.println("add key:" + key);
            int nowMask = mask.get();
            int bucket = key & nowMask;
            int hashkey = makeRegularKey(key);
            boolean[] marked = {false};

            if (table[bucket].get() == null) 
                newTable(father(bucket), bucket);          
            Node<T> head = table[bucket].get(); 

            while (true) {         
                Window window = find(head, hashkey);
                
                //System.out.println("find!");
                
                //if (resize(window, nowMask)) continue;

                //System.out.println("go to add!");
                
                Node<T> pred = window.pred, curr = window.curr;
                //System.out.println(curr.key + " " + reverse(key));
                if (curr.key == hashkey) {
                    //return false;
                    return;
                } else {
                    Node<T> node = new Node<T>(hashkey, item, curr);
                    if (pred.next.compareAndSet(curr, node, false, false)) {
                        //return true;
                        //System.out.println("table size: " + tableSize.get());
                        //print();
                        setSize.getAndIncrement();
                        resize();
                        return;
                    }

                    curr = pred.next.get(marked);
                    if (!marked[0]) {
                        head = pred;                    
                    } else {
                        head = table[bucket].get();                        
                    }
                }
            }

  	}
  	
  	public boolean remove(int key) {
            //System.out.println("remove key:" + key);
            int nowMask = mask.get();
            int bucket = key & nowMask;
                
            if (table[bucket].get() == null)
                return false;
                
            int hashkey = makeRegularKey(key);
            while (true) {
                Window window = find(table[bucket].get(), hashkey);                

                //if (resize(window, nowMask)) continue;

                Node<T> pred = window.pred, curr = window.curr;
                if (curr.key != hashkey) {
                    return false;
                } else {                
                    Node<T> succ = curr.next.getReference();
                    boolean snip = curr.next.attemptMark(succ, true);
                    setSize.getAndDecrement();
                    if (!snip) continue;
                    pred.next.compareAndSet(curr, succ, false, false);
                    return true;
                }        
            }
        }
  	
  	public boolean contains(int key) {
            //System.out.println("contains key: " + key);
            int nowMask = mask.get();
            int bucket = key & nowMask;

            if (table[bucket].get() == null) return false;

            int hashkey = makeRegularKey(key);
            Window window = find(table[bucket].get(), hashkey);

            //if (resize(window, nowMask)) continue;
                
            return (window.curr.key == hashkey);            
        }
        
        public void print() {
            /*
            Node<T> p = table[0].get();
            while (p != null) {
                int key = p.key;
                if ((key & 1) == 0) {
                    System.out.println("Sentry Node: " + reverse(key));
                }
                else {                    
                    System.out.println("Regular Node: " + (reverse(key) & 0x3FFFFFFF));
                }
                p = p.next.getReference();
            }
            System.out.println();
            try {
                System.in.read();
            } catch (Exception ignore) {;}
            */

            //System.out.println(count);
            System.out.println(mask.get());
            System.out.println(setSize.get());
        }
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
  	
  	public boolean contains(int key) {                           
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
                        return true;
                    }
                }

            //System.out.println("finish con");
            return false;
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



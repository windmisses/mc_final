import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import org.deuce.Atomic;

public interface LookUpTable {
    public void change(int address, int start, int end, boolean validSource, boolean acceptingRange);

    public boolean check(int src, int des);

    public void debug();
}

class SerialLookUpTable implements LookUpTable {
    
    final int SkipList_Max_Level;

    boolean[] source;
    SkipList[] list;
    boolean[][] table;
    int count;
    int n;

    HashMap<Integer, Boolean>[] hash;
    
    public SerialLookUpTable(int numAddressesLog) {
        SkipList_Max_Level = 5;

        n = 1 << numAddressesLog;
        source = new boolean[n];
        list = new SkipList[n];
        hash = new HashMap[n];

        for (int i = 0; i < n; i++) {
            list[i] = new SkipList(SkipList_Max_Level);
            list[i].change(0, n - 1, true);
            source[i] = true;
            hash[i] = new HashMap<Integer, Boolean>();
        }
    }

    public void change(int address, int start, int end, boolean validSource, boolean acceptingRange) {
        int key = address;

        if (!validSource) 
            source[key] = true;
        else
            source[key] = false;
        
        list[key].change(start, end - 1, acceptingRange);
        hash[key] = new HashMap<Integer, Boolean>();
    }
   

    public boolean check(int start, int dest) {
        if (!source[start]) return false;

        Boolean hashAns = hash[dest].get(start);

        if (hashAns != null)
            return hashAns;                
            
        boolean ret = list[dest].check(start);
        hash[dest].put(start, ret);
        
        return ret;
    }

    public void debug() {}
}

class ParallelLookUpTable implements LookUpTable {    
    final int SkipList_Max_Level;

    ReentrantLock[] lock; 

    boolean[] source;
    //SegmentList[] list;
    SkipList[] list;
    int n;
    
    boolean SkipListUsed = true;
    boolean usedLock = true;
    boolean cache = true;

    ThreadLocal<Integer> lockCount;

    ConcurrentHashMap<Integer, Boolean>[] hash;

    public ParallelLookUpTable(int numAddressesLog) {
        //SkipList_Max_Level = numAddressesLog / 2;
        SkipList_Max_Level = 5;

        n = 1 << numAddressesLog;
        source = new boolean[n];
        list = new SkipList[n];
        //list = new SegmentList[n];

        lock = new ReentrantLock[n];
        hash = new ConcurrentHashMap[n];

        for (int i = 0; i < n; i++) {
            if (SkipListUsed) {
                list[i] = new SkipList(SkipList_Max_Level);
                //list[i] = new SegmentList(SkipList_Max_Level, 32);
            }
            list[i].change(0, n - 1, true);

            source[i] = true;
            lock[i] = new ReentrantLock();
            hash[i] = new ConcurrentHashMap<Integer, Boolean>();
        }

        lockCount = new ThreadLocal<Integer>() {
            protected Integer initialValue() {
                return 0;
            }
        };
    }

    void trylock(int key) {
        if (!lock[key].tryLock()) {
            int tmp = lockCount.get();
            lockCount.set(tmp + 1);
            lock[key].lock();
        }        
    }

    public int getlockCount() {
        return lockCount.get();
    }
    

    @Atomic
    public void change(int address, int start, int end, boolean validSource, boolean acceptingRange) {
        int key = address;
        
        if (usedLock)
            trylock(key);

        if (!validSource) 
            source[key] = true;
        else
            source[key] = false;
        
        list[key].change(start, end - 1, acceptingRange);
        //list[key].add(start, end, acceptingRange);
        
        if (cache)
            hash[key] = new ConcurrentHashMap<Integer, Boolean>();

        if (usedLock)
            lock[key].unlock();
    }


    void orderLock(int small, int large) {        
        if (usedLock) {
            trylock(small);
            if (small != large) 
                trylock(large);
        }
    }

    void orderUnLock(int small, int large) {        
        if (usedLock) {
            lock[small].unlock();
            if (small != large) lock[large].unlock();
        }
    }

    public boolean check(int start, int dest) {
        int small = Math.min(start, dest);
        int large = Math.max(start, dest);
        
        orderLock(small, large);

        boolean ret = source[start];
        if (!ret) {
            orderUnLock(small, large);
            return false;
        }
       
        if (cache) {
            Boolean hashAns = hash[dest].get(start);

            if (hashAns != null) {            
                orderUnLock(small, large);
                return hashAns;            
            }
            
            ret = list[dest].check(start);
            //ret = list[dest].contains(start);

            hash[dest].put(start, ret);

            orderUnLock(small, large);

            return ret;

        } else {
            ret = list[dest].check(start);
            //ret = list[dest].contains(start);
            if (ret) {
                orderUnLock(small, large);                
                return true;
            }

            ret = list[dest].check(start);
            //ret = list[dest].contains(start);

            orderUnLock(small, large);
            return ret;
        }
    }

    public void debug() {}
}

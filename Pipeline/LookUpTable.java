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
    
    public SerialLookUpTable(int numAddressesLog) {
        SkipList_Max_Level = numAddressesLog / 2;

        n = 1 << numAddressesLog;
        source = new boolean[n];
        list = new SkipList[n];

        for (int i = 0; i < n; i++) {
            list[i] = new SkipList(SkipList_Max_Level);
            source[i] = false;
        }
    }

    public void change(int address, int start, int end, boolean validSource, boolean acceptingRange) {
        int key = address;

        if (!validSource) 
            source[key] = true;
        else
            source[key] = false;
        
        list[key].change(start, end - 1, acceptingRange);
    }
   

    public boolean check(int start, int dest) {
        if (!source[start]) return false;
        return list[dest].check(start);
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

            source[i] = false;
            lock[i] = new ReentrantLock();
            hash[i] = new ConcurrentHashMap<Integer, Boolean>();
        }
    }
    
    @Atomic
    public void change(int address, int start, int end, boolean validSource, boolean acceptingRange) {
        int key = address;
        
        if (usedLock)
            lock[key].lock();

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
            lock[small].lock();
            if (small != large) lock[large].lock();
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

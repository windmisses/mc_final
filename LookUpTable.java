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
    SegmentTree[] tree;
    boolean[][] table;
    int count;
    int n;

    boolean tableUsed = false;
    boolean SkipListUsed = true;
    boolean SegmentTreeUsed = false;

    int add = 0, sum = 0;
    
    public SerialLookUpTable(int numAddressesLog) {
        SkipList_Max_Level = numAddressesLog / 2;

        n = 1 << numAddressesLog;
        source = new boolean[n];

        if (SkipListUsed) {
            list = new SkipList[n];

            for (int i = 0; i < n; i++) {
                list[i] = new SkipList(SkipList_Max_Level);
            }
        }
        
        if (SegmentTreeUsed) {
            tree = new SegmentTree[n];
            
            for (int i = 0; i < n; i++) {
                tree[i] = new SegmentTree(0, n - 1, -1);
            }
        }
        
        for (int i = 0; i < n; i++) {
            source[i] = false;
        }

        if (tableUsed) {
            table = new boolean[n][n];
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    table[i][j] = false;
        }
    }

    public void change(int address, int start, int end, boolean validSource, boolean acceptingRange) {
        int key = address;

        if (!validSource) 
            source[key] = true;
        else
            source[key] = false;
        
        if (tableUsed) {            
            for (int i = start; i < end; i++) {                
                table[key][i] = acceptingRange;
            }
        }

        if (SkipListUsed) {
            list[key].change(start, end - 1, acceptingRange);
        }

        if (SegmentTreeUsed) {
            if (acceptingRange) 
                tree[key].insert(start,end - 1, 1); 
            else 
                tree[key].insert(start,end - 1, -1);
        }
        
        /*
        boolean ok = true;
        for (int i = 0; i < n; i++)
            if (list[key].check(i) != table[key][i])
                ok = false;

        System.out.println(address + " " + start + " " + end + " " + validSource + " " + acceptingRange);
        if (!ok) {            
            for (int i = 0; i < n; i++) 
                System.out.println(i + " " + list[key].check(i) + " " + table[key][i]);
            try {
                System.in.read();
            } catch (Exception ignore) {;}
        } 
        */
    }
   

    public boolean check(int start, int dest) {
        if (!source[start]) return false;
        
        //System.out.println("ok");
        /*
        if (tree[dest].find(start) != table[dest][start]) {
            System.out.println("error!");

            for (int i = 0; i < n; i++) 
                System.out.println(i + " " + tree[dest].find(i) + " " + table[dest][i]);
        }
        */
        
        if (SkipListUsed) {
            /*
            if (list[dest].check(start) != table[dest][start]) {
                System.out.println("error skip!");

                for (int i = 0; i < n; i++) 
                    System.out.println(i + " " + list[dest].check(i) + " " + table[dest][i]);

                try {
                    System.in.read();
                } catch (Exception ignore) {;}
            }
            */
            return list[dest].check(start);
        }

        if (SegmentTreeUsed) {
            /*
            if (tree[dest].find(start) != table[dest][start]) {                 
                System.out.println("error tree!");
            }
            */
            return tree[dest].find(start);        
        }
        
        return false;
    }

    public void debug() {
        /*
        int cnt = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (table[i][j])
                    cnt++;
        System.out.println(sum + " " + cnt + " " + (n * n));
        */
    }
}

class ParallelLookUpTable implements LookUpTable {    
    final int SkipList_Max_Level;

    ReentrantLock[] lock; 

    boolean[] source;
    SegmentTree[] tree;
    SkipList[] list;
    int n;
    
    boolean tableUsed = false;
    boolean SkipListUsed = true;
    boolean SegmentTreeUsed = false;
    boolean usedLock = true;
    boolean cache = true;

    ConcurrentHashMap<Integer, Boolean>[] hash;

    public ParallelLookUpTable(int numAddressesLog) {
        SkipList_Max_Level = numAddressesLog / 2;

        n = 1 << numAddressesLog;
        source = new boolean[n];
        tree = new SegmentTree[n];
        list = new SkipList[n];
        lock = new ReentrantLock[n];
        hash = new ConcurrentHashMap[n];

        for (int i = 0; i < n; i++) {
            if (SegmentTreeUsed)
                tree[i] = new SegmentTree(0, n - 1);
            if (SkipListUsed)
                list[i] = new SkipList(SkipList_Max_Level);
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
        
        if (SkipListUsed) {
            list[key].change(start, end - 1, acceptingRange);
        }
        
        if (SegmentTreeUsed) {
            if (acceptingRange)
                tree[key].insert(start,end - 1,1); 
            else
                tree[key].insert(start,end - 1,-1);
        }
        if (cache)
            hash[key] = new ConcurrentHashMap<Integer, Boolean>();

        if (usedLock)
            lock[key].unlock();
    }
 
    public boolean check(int start, int dest) {
        int small = Math.min(start, dest);
        int large = Math.max(start, dest);
        
        //System.out.println(small + " " + large);
        if (usedLock) {
            lock[small].lock();
            if (small != large) lock[large].lock();
        }
        boolean ret = source[start];
        if (!ret) {
            if (usedLock) {
                lock[small].unlock();
                if (small != large) lock[large].unlock();
            }
            return false;
        }
       
        /*
        if (usedLock)
            lock[dest].lock();
        if (SkipListUsed)
            ret = list[dest].check(start);
        if (SegmentTreeUsed)
            ret = tree[dest].find(start);
        if (usedLock)
            lock[dest].unlock();
        */
        
        if (cache) {
            Boolean hashAns = hash[dest].get(start);

            if (hashAns != null) {            
                if (usedLock) {
                    lock[small].unlock();
                    if (small != large) lock[large].unlock();
                }

                return hashAns;            
            }
            
            ret = list[dest].check(start);
            hash[dest].put(start, ret);

            if (usedLock) {
                lock[small].unlock();
                if (small != large) lock[large].unlock();
            }

            return ret;

        } else {
            ret = list[dest].check(start);
            if (ret) {
                if (usedLock) {
                    lock[small].unlock();
                    if (small != large) lock[large].unlock();
                }
                return true;
            }

            ret = list[dest].check(start);

            if (usedLock) {
                lock[small].unlock();
                if (small != large) lock[large].unlock();
            }
            return ret;
        }
    }

    public void debug() {
    }
}

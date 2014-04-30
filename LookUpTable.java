import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.deuce.Atomic;

public interface LookUpTable {
    public void change(int address, int start, int end, boolean validSource, boolean acceptingRange);

    public boolean check(int src, int des);

    public void check();
}

class SerialLookUpTable implements LookUpTable {
    
    HashMap<Integer, Integer> list;

    boolean[] source;
    SegmentTree[] tree;
    //boolean[][] table;
    int count;
    int n;

    int add = 0, sum = 0;
    
    public SerialLookUpTable(int numAddressesLog) {
        n = 1 << numAddressesLog;
        source = new boolean[n];
        tree = new SegmentTree[n];

        for (int i = 0; i < n; i++) {
            tree[i] = new SegmentTree(0, n - 1, -1);
            source[i] = false;
        }

        /*
        table = new boolean[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                table[i][j] = false;
        */

        count = 0;
        list = new HashMap<Integer, Integer>();
    }

    public int findHashKey(int address) {
         Integer key = list.get(address);

         if (key == null) {
            count++;
            list.put(address, count);
            key = count;
         }

         return key;
    }
    
    public void change(int address, int start, int end, boolean validSource, boolean acceptingRange) {
        //int key = findHashKey(address);
        int key = address;

        if (validSource) 
            source[key] = true;
        else
            source[key] = false;
        
        /*
        int add = 0;
        for (int i = start; i < end; i++) {
            if (table[key][i] == false)
                add++;
            table[key][i] = acceptingRange;
        }
        sum = sum + add;
        System.out.println(add);
        */

        if (acceptingRange)
            tree[key].insert(start,end - 1, 1); 
        else
            tree[key].insert(start,end - 1, -1);

        /*
        boolean ok = true;
        for (int i = 0; i < n; i++)
            if (tree[key].find(i) != table[key][i])
                ok = false;

        //System.out.println(address + " " + start + " " + end + " " + validSource + " " + acceptingRange);
        if (!ok) {            
            for (int i = 0; i < n; i++) 
                System.out.println(i + " " + tree[key].find(i) + " " + table[key][i]);
            try {
                System.in.read();
            } catch (Exception ignore) {;}
        }
        */
    }
   

    public boolean check(int start, int dest) {
        //int src = findHashKey(start);
        //int des = findHashKey(dest);

        //System.out.println(start + " " + dest);

        if (!source[start]) return false;
        
        //System.out.println("ok");
        /*
        if (tree[dest].find(start) != table[dest][start]) {
            System.out.println("error!");

            for (int i = 0; i < n; i++) 
                System.out.println(i + " " + tree[dest].find(i) + " " + table[dest][i]);
        }
        */

        return tree[dest].find(start);        
    }

    public void check() {
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
    ConcurrentHashMap<Integer, Integer> list;

    //ReentrantLock[] lock; 

    boolean[] source;
    SegmentTree[] tree;
    int count;
    int n;
    
    public ParallelLookUpTable(int numAddressesLog) {
        n = 1 << numAddressesLog;
        source = new boolean[n];
        tree = new SegmentTree[n];
        //lock = new ReentrantLock[n];

        for (int i = 0; i < n; i++) {
            tree[i] = new SegmentTree(0, n - 1);
            source[i] = false;
            //lock[i] = new ReentrantLock();
        }

        count = 0;
        list = new ConcurrentHashMap<Integer, Integer>();
    }

    public int findHashKey(int address) {
         Integer key = list.get(address);

         if (key == null) {
            count++;
            list.put(address, count);
            key = count;
         }

         return key;
    }
    
    @Atomic
    public void change(int address, int start, int end, boolean validSource, boolean acceptingRange) {
        //int key = findHashKey(address);
        int key = address;
        
        //lock[key].lock();
        //System.out.println(address + " " + start + " " + end + " " + validSource + " " + acceptingRange);

        if (validSource) 
            source[key] = true;
        else
            source[key] = false;

        if (acceptingRange)
            tree[key].insert(start,end - 1,1); 
        else
            tree[key].insert(start,end - 1,-1);

        //lock[key].unlock();
    }
 
    public boolean check(int start, int dest) {
        //int src = findHashKey(start);
        //int des = findHashKey(dest);
        
        //lock[start].lock();
        boolean ret = source[start];
        //lock[start].unlock();
        if (!ret) return false;
        
        //lock[dest].lock();
        ret = tree[dest].find(start);
        //lock[dest].unlock();

        return ret;
    }


    public void check() {
    }
}

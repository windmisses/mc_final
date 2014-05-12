import java.util.Random;
import org.deuce.Atomic;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class SegmentList {
	class Node {
		public int level;
		public Node[] next;
		public Node(int level, long value, boolean is_seg) {
			this.level = level;
			this.value = value;
			this.is_seg = is_seg;
			this.next = new Node[level];
		}
		long value;
		boolean is_seg; // is followed by a segment.
	}
	class Window {
		public Node[] pred;
		public Node curr;
		public Window(Node[] pred, Node curr) {
			this.pred = pred;
			this.curr = curr;
		}
		public void print() {
			System.out.println("window");
			for(int level = pred.length-1; level >= 0; level--) {
				System.out.println("[pred "+level+"] "+this.pred[level].value);
			}
			System.out.println("[curr] "+this.curr.value);
			System.out.println();
		}
	}
	Node head, tail;

	final int maxLevel;
	final Random random;
	Lock lock;

	public SegmentList(int maxLevel, long numAddrLog) {
		this.maxLevel = maxLevel;
		this.head = new Node(maxLevel, -1, false);
		this.tail = new Node(maxLevel, (long)1<<numAddrLog, false);
		for(int level = 0; level < maxLevel; level++) {
			this.head.next[level] = this.tail;
		}
		this.random = new Random();
		this.lock = new ReentrantLock();
	}

	public boolean contains(long x) {
		// Debug.p(x, "x");
		Window window = this.find(x);
		if(window == null) return false;
		Node pred = window.pred[0], curr = window.curr;
		// Debug.p(pred.is_seg);
		return pred.is_seg;
	}

	public Window find(long x) {
		int level = maxLevel-1;
		Node pred = head, curr = null;
		Node[] preds = new Node[maxLevel];
		while(level >= 0) {
			curr = pred.next[level];
			if(curr == null) 
				return null;
			if(curr.value > x) {
				preds[level] = pred;
				level--;
			}else{
				pred = curr;
			}
		}
		return new Window(preds, curr);
	}

	public long shift(int x) {
		if(x < 0) {
			return (long)x+((long)1<<32);
		}else
			return (long)x;
	}


	public void add_closed(int l_int, int r_int, boolean create_seg) {
		// lock.lock();
		// try{
			add(l_int, r_int+1, create_seg);
		// }finally{
			// lock.unlock();
		// }
	}

	@Atomic
	/* change interval of form [a, b) */
	public void add(int l_int, int r_int, boolean create_seg) {
		/* make the interval in the right shape */
		if(l_int < 0 && r_int >= 0) {
			this.add(l_int, -1, create_seg);
			this.add(0, r_int, create_seg);
			return;
		}
		long l = shift(l_int), r = shift(r_int);
		// Debug.p(l, "l");
		// Debug.p(r, "r");
		/* insert it */
		Window window = this.find(l);
		// window.print();
		Node pred = window.pred[0], curr = window.curr;
		boolean is_seg = pred.is_seg;
		while(curr.value <= r) {
			is_seg = curr.is_seg;
			delete(new Window(window.pred, curr));
			curr = pred.next[0];
		}
		if(pred.is_seg != create_seg)  // insert left. 
			insert(window.pred, new Node(random.nextInt(maxLevel)+1, l, create_seg));
		if(window.pred[0].is_seg != is_seg)  // insert right.
			insert(window.pred, new Node(random.nextInt(maxLevel)+1, r, is_seg));
	}

	public void insert(Node[] preds, Node x) {
		for(int level = 0; level < x.level; level++) {
			x.next[level] = preds[level].next[level];
			preds[level].next[level] = x;
			preds[level] = x;
		}
	}

	public void delete(Window window) {
		for(int level = 0; level < window.curr.level; level++) {
			window.pred[level].next[level] = window.curr.next[level];
		}
	}

	public void printList() {
		Node node = head;
		while(node != null) {
			System.out.println("["+node.level+"] "+node.value+"("+node.is_seg+")");
			node = node.next[0];
		}
		System.out.println();
	}
}

/*
class SegmentListTest {
	public static void main(String[] args) {
		// special_case_test();
		random_test();
		// boundary_test();
		// special_closed_case_test();
	}
	public static void special_case_test() {
		SegmentList list = new SegmentList(5, 32);
		list.add(0, 5, true);
		// Debug.p(list.contains(0));
		list.add(7, 9, true);
		list.add(11, 13, true);
		list.printList();
		list.add(6, 8, false);
		list.printList();
	}
	public static void random_test() {
		SegmentList list = new SegmentList(5, 32);
		Random rand = new Random();
		long iteration = 10000;
		long startTime = System.nanoTime();
		for(int it = 0; it < iteration; it++) {
			int begin = rand.nextInt((int)iteration);
			int end = begin+rand.nextInt(512);
			list.add(begin, end, rand.nextBoolean());
		}
		for(int it = 0; it < iteration; it++) {
			Debug.p(list.contains(rand.nextInt((int)iteration)));
		}
		long time = System.nanoTime()-startTime;
		list.printList();
		System.out.println("time = "+time/1000000000.0);
	}
	public static void boundary_test() {
		SegmentList list = new SegmentList(5, 32);
		list.add(0xFFFFFFFF, 0xFFFFFFFF+512, true);
		list.add(0xFFFFFFFF+1024, 0xFFFFFFFF+1024+512, true);
		list.add(0xFFFFFFFF+256, 0xFFFFFFFF+256+512, false);
		list.printList();	
	}
	public static void special_closed_case_test() {
		SegmentList list = new SegmentList(5, 32);
		list.add_closed(0, 5, true);
		list.add_closed(5, 5, false);
		list.printList();
	}
}

*/

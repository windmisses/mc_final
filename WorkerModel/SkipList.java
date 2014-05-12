import java.util.*;

class SkipList {
    final int Max_Level;
    Random rand = new Random();

    class Node {
        int left;
        int right;            
        Node[] next;
        Node[] pred;
        int toplevel;

        public Node(int l, int r, int h) {
            left = l;
            right = r;
            next = new Node[h + 1];
            pred = new Node[h + 1];
            toplevel = h;
        }
    }

    final Node head;
    final Node tail;

    public SkipList(int height) {
        Max_Level = height;
        head = new Node(-2, -2, Max_Level);
        tail = new Node(0x7FFFFFFF, 0x7FFFFFFF, Max_Level);

        for (int i = 0; i <= Max_Level; i++) {
            head.next[i] = tail;
            tail.pred[i] = head;
        }
    }

    Node find(int key) {
        int lFound = -1;
        Node pred = head;
        for (int level = Max_Level; level >= 0; level--) {
            Node curr = pred.next[level];
            while (key > curr.right && curr.next != null) {
                pred = curr;
                curr = pred.next[level];
            }

            if (key >= curr.left) {
                return curr;
            }
        }
        return pred;
    }

    void delete(Node curr) {
        for (int level = curr.toplevel; level >= 0; level--) {
            curr.pred[level].next[level] = curr.next[level];
            curr.next[level].pred[level] = curr.pred[level];
        }
    }

    void search(int key, Node[] preds, Node[] succs) {
        Node pred = head;
        for (int level = Max_Level; level >= 0; level--) {
            Node curr = pred.next[level];
            while (key > curr.right) {
                pred = curr;
                curr = pred.next[level];
            }

            preds[level] = pred;
            succs[level] = curr;
        }
    }

    void insert(int left, int right) {        
        int level = 0;
        while (level < Max_Level && rand.nextDouble() < 0.5)
            level++;
        //int level = rand.nextInt(Max_Level + 1);

        Node curr = new Node(left, right, level);

        Node[] preds = new Node[Max_Level + 1];
        Node[] succs = new Node[Max_Level + 1];

        search(left, preds, succs);

        for (int i = 0; i <= level; i++) {
            curr.pred[i] = preds[i];
            curr.next[i] = succs[i];
            preds[i].next[i] = curr;
            succs[i].pred[i] = curr;
        }
    }

    public void change(int left, int right, boolean flag) {
        Node ret = find(left);

        //System.out.println(left + " " + right + " " + flag);

        while (ret.next[0].right <= right) {
            delete(ret.next[0]);
        }

        if (flag) {
            if (left >= ret.left && right <= ret.right)
                return;

            if (ret.right >= left - 1) {
                left = ret.left;
                delete(ret);
                ret = ret.pred[0];
            }

            Node tmp = ret.next[0];
            if (tmp.left <= right + 1) {
                right = tmp.right;
                delete(tmp);
            }

            insert(left, right);
        } else {
            if (left >= ret.left && right <= ret.right) {
                if (left == ret.left) {
                    if (right == ret.right) {
                        delete(ret);
                    } else {
                        ret.left = right + 1;
                    }
                } else {
                    if (right == ret.right) {
                        ret.right = left - 1;
                    } else {
                        int r = ret.right;
                        ret.right = left - 1;

                        insert(right + 1, r);
                    }
                }
                return;
            }

            Node tmp = ret.next[0];
            if (tmp.left <= right) {
                tmp.left = right + 1; 
            }
            
            if (ret.right >= left) {
                ret.right = left - 1;

                if (ret.left > ret.right) 
                    delete(ret);
            }

        }
    }

    public boolean check(int key) {
        Node ret = find(key);

        return (ret.left <= key && ret.right >= key);
    }
}

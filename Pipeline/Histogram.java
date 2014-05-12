import java.util.concurrent.atomic.*;

class Histogram {
    final int size = 1 << 16;
    AtomicInteger[] cnt;

    public Histogram() {
        cnt = new AtomicInteger[size];
        for (int i = 0; i < size; i++) {
            cnt[i] = new AtomicInteger(0);
        }
    }

    public void insert(int x) {
        cnt[x].getAndIncrement();
    }

    public void print() {
        for (int i = 0; i < size; i++) 
            System.out.println("i: " + cnt[i].get());
    }
}

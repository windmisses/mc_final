class FullException extends Exception {
    public FullException() {
        super("Error: The queue is full!");
    }
}

class EmptyException extends Exception {
    public EmptyException() {
        super("Error: The queue is empty!");
    }
}


class LamportQueue<T> {
    volatile int head = 0, tail = 0;
    T[] items;

    public LamportQueue(int capacity) {
        items = (T[]) new Object[capacity];
        head = 0; tail = 0;
    }

    public boolean empty() {
        return (tail - head == 0);
    }

    public boolean full() {
        return (tail - head == items.length);
    }

    //public void enq(T x) throws FullException {
    public void enq(T x) {
        //if (tail - head == items.length) 
        //    throw new FullException();

        items[tail % items.length] = x;
        tail++;        
    }

    public T deq() throws EmptyException {
        if (tail - head == 0)
            throw new EmptyException();

        T x = items[head % items.length];
        head++;
        return x;
    }
}



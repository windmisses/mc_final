import java.util.concurrent.locks.*;

public interface PacketDispatcher extends Runnable {
    public void run();
}

class ParallelPacketDispatcher implements PacketDispatcher {
    PaddedPrimitiveNonVolatile<Boolean> done;
    final PacketGenerator source;
    final int numWorkers;
    LamportQueue<Packet> queue[];
    long fullCount = 0;

    ReentrantLock lock;
    public int totalPackets = 0;

    public ParallelPacketDispatcher(
            PaddedPrimitiveNonVolatile<Boolean> done, 
            PacketGenerator source,
            int numWorkers,
            LamportQueue<Packet>[] queue,
            ReentrantLock lock
            ) {
    
        this.done = done;
        this.source = source;
        this.numWorkers = numWorkers;
        this.queue = queue;
        this.lock = lock;
    }
  
    public void run() {
        lock.lock();
        //System.out.println("dispatcher go!");

        Packet pkt = source.getPacket();
        while( !done.value ) {
            for (int i = 0; i < numWorkers; i++) {                
                if (queue[i].full()) {
                    fullCount++;
                    continue;
                }

                queue[i].enq(pkt);

                totalPackets++;
                pkt = source.getPacket();
            }
        }
    }  
}



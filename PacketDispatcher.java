public interface PacketDispatcher extends Runnable {
    public void run();
}

class ParallelPacketDispatcher implements PacketDispatcher {
    PaddedPrimitiveNonVolatile<Boolean> done;
    final PacketGenerator pkt;
    final int numWorkers;
    LamportQueue<Packet> queue[];
    public int totalPackets = 0;

    public ParallelPacketDispatcher(
            PaddedPrimitiveNonVolatile<Boolean> done, 
            PacketGenerator pkt,
            int numWorkers,
            LamportQueue<Packet>[] queue) {
    
        this.done = done;
        this.pkt = pkt;
        this.numWorkers = numWorkers;
        this.queue = queue;
    
    }
  
    public void run() {
        Packet tmp;
        while( !done.value ) {
            for( int i = 0; i < numWorkers; i++ ) {
                try {
            	    tmp = pkt.getPacket();
                    queue[i].enq(tmp);
                    totalPackets++;
                } catch (FullException e) {;}
            }
        }
    }  
}



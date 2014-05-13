public interface PacketDispatcher extends Runnable {
    public void run();
}

class ParallelPacketDispatcher implements PacketDispatcher {
    PaddedPrimitiveNonVolatile<Boolean> done;
    final PacketGenerator source;
    final int numWorkers;
    LamportQueue<Packet> queue[];
    long fullCount = 0;

    public int totalPackets = 0;

    public ParallelPacketDispatcher(
            PaddedPrimitiveNonVolatile<Boolean> done, 
            PacketGenerator source,
            int numWorkers,
            LamportQueue<Packet>[] queue
            ) {
    
        this.done = done;
        this.source = source;
        this.numWorkers = numWorkers;
        this.queue = queue;
    }
  
    public void run() {
        Packet pkt;
        int id = 0;
        while( !done.value ) {
            /*
            for( int i = 0; i < numWorkers; i++ ) {
                try {
             	    pkt = source.getPacket();
                    //pkt = source.getDataPacket();
                    queue[i].enq(pkt);
                    totalPackets++;
                } catch (FullException e) {;}
            } 
            */
                                
            pkt = source.getPacket();
            //pkt = source.getDataPacket();
            while (true) {
                try {
                    id = (id + 1) % numWorkers;
                    queue[id].enq(pkt);
                    totalPackets++;
                    break;
                } catch (FullException e) {
                    fullCount++;
                }
            }             
        }
    }  
}



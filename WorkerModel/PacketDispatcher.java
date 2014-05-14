public interface PacketDispatcher extends Runnable {
    public void run();
}

class ParallelPacketDispatcher implements PacketDispatcher {
    PaddedPrimitiveNonVolatile<Boolean> done;
    final PacketGenerator source;
    final int numWorkers;
    LamportQueue<Packet> queue[];
    LookUpTable table;
    Histogram histogram;
    Fingerprint fingerprint;
    long fullCount = 0;

    public long residue = 0;
    public int totalPackets = 0;
    public int numPackets = 0;
    public int checkOK = 0;

    public ParallelPacketDispatcher(
            PaddedPrimitiveNonVolatile<Boolean> done, 
            PacketGenerator source,
            int numWorkers,
            LamportQueue<Packet>[] queue,
            LookUpTable table,
            Histogram histogram
            ) {
    
        this.done = done;
        this.source = source;
        this.numWorkers = numWorkers;
        this.queue = queue;
        this.table = table;
        this.histogram = histogram;

        fingerprint = new Fingerprint();
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



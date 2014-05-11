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

    public long residue = 0;
    public int totalPackets = 0;
    public int numPackets = 0;
    public int checkOK = 0;

    boolean specialMode = true;

    public ParallelPacketDispatcher(
            PaddedPrimitiveNonVolatile<Boolean> done, 
            PacketGenerator source,
            int numWorkers,
            LamportQueue<Packet>[] queue,
            LookUpTable table,
            Histogram histogram,
            boolean spm) {
    
        this.done = done;
        this.source = source;
        this.numWorkers = numWorkers;
        this.queue = queue;
        this.table = table;
        this.histogram = histogram;
        this.specialMode = spm;

        fingerprint = new Fingerprint();
    }
  
    public void run() {
        Packet pkt;
        int id = 0;
        while( !done.value ) {

            if (!specialMode) {
                for( int i = 0; i < numWorkers; i++ ) {
                    try {
                	pkt = source.getPacket();
                        queue[i].enq(pkt);
                        totalPackets++;
                    } catch (FullException e) {;}
                }
            } else {
                totalPackets++;
                pkt = source.getPacket();

                if (pkt.type == Packet.MessageType.ConfigPacket) {                                        
                    Config config = pkt.config;
                    int address = config.address;
                    table.change(address, config.addressBegin, config.addressEnd, config.personaNonGrata, config.acceptingRange);
                } else {
                    int src = pkt.header.source;
                    int des = pkt.header.dest;

                    if (table.check(src, des)) {
                        checkOK++;
                        boolean ok = false;
                        for (int i = 0; i < numWorkers && !ok; i++) {
                            try {
                                queue[(id + i) % numWorkers].enq(pkt);
                                ok = true;
                            } catch (FullException e) {;}
                        }
                        if (numWorkers > 0)
                            id = (id + 1) % numWorkers;
                    
                        if (!ok) {
                            numPackets++;
                            int ret = (int)fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
                            residue += ret;
                            //histogram.insert(ret);
                        }
                    }
                }
            }
        }
    }  
}



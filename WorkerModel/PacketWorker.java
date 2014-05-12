import java.util.concurrent.locks.Lock;
import java.util.*;

public interface PacketWorker<T> extends Runnable {
    public void run();
}

class SerialPacketWorker implements PacketWorker {
    PaddedPrimitiveNonVolatile<Boolean> done;
    final PacketGenerator source;
    final SerialLookUpTable table;
    final Histogram histogram;
    long totalPackets = 0;
    long totalWorkPackets = 0;
    long residue = 0;
    long checkOK = 0;
    Fingerprint fingerprint;

    public SerialPacketWorker(
            PaddedPrimitiveNonVolatile<Boolean> done, 
            PacketGenerator source,
            SerialLookUpTable table,
            Histogram histogram) {
        this.done = done;
        this.source = source;
        this.table = table;
        this.histogram = histogram;

        fingerprint = new Fingerprint();
    }
  
    public void run() {
        Packet pkt;
        while( !done.value ) {
            totalPackets++;
            pkt = source.getPacket();


            if (pkt.type == Packet.MessageType.ConfigPacket) {                                        
                Config config = pkt.config;
                int address = config.address;
                table.change(address, config.addressBegin, config.addressEnd, config.personaNonGrata, config.acceptingRange);
            } else {
                int src = pkt.header.source;
                int des = pkt.header.dest;
                totalWorkPackets++;
                
                if (table.check(src, des)) {
                    checkOK++;
                    int ret = (int)fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
                    residue += ret;
                    //histogram.insert(ret);
                }
            }
        }
    }   
}

class ParallelPacketWorker implements PacketWorker {
    PaddedPrimitiveNonVolatile<Boolean> done;
    ParallelLookUpTable table;
    LamportQueue<Packet> queue;
    Histogram histogram;
    long totalPackets = 0;
    long totalWorkPackets = 0;
    long emptyCount = 0;
    long residue = 0;
    long checkOK = 0;
    Fingerprint fingerprint;

    public ParallelPacketWorker(  		
  	    PaddedPrimitiveNonVolatile<Boolean> done, 
    	    LookUpTable table,
    	    LamportQueue<Packet> queue,
            Histogram histogram
    	    ) {
        this.done = done;
        this.table = (ParallelLookUpTable)table;
        this.queue = queue;
        this.histogram = histogram;

        fingerprint = new Fingerprint();
    }
  
    public void run() {
        Packet pkt;

        //while( !done.value || !queue.empty() ) {
        while (!done.value) {
    	    try {
                pkt = queue.deq();
                totalPackets++;
                
                if (pkt.type == Packet.MessageType.ConfigPacket) {                                        
                    Config config = pkt.config;
                    int address = config.address;
                    table.change(address, config.addressBegin, config.addressEnd, config.personaNonGrata, config.acceptingRange);
                } else {
                    int src = pkt.header.source;
                    int des = pkt.header.dest;
                    totalWorkPackets++;
                    //table.check(src,des);
                        
                    //if (true) {
                    if (table.check(src, des)) {
                        checkOK++;
                        int ret = (int)fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
                        residue += ret;
                        //histogram.insert(ret);
                    }                        
                }
    	    } catch (EmptyException e) {
                emptyCount++;
            }
        }    
    }
    
}

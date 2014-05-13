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

class ConfigWorker implements PacketWorker {
    PaddedPrimitiveNonVolatile<Boolean> done;
    ParallelLookUpTable table;
    LamportQueue<Packet> queue;
    LamportQueue<Packet>[] sendQueue;
    long totalPackets = 0;
    long totalWorkPackets = 0;
    long totalWork = 0;
    long emptyCount = 0;
    long fullCount = 0;
    long checkOK = 0;
    int numWorkers;

    long residue = 0;    
    Fingerprint fingerprint;

    public ConfigWorker(  		
  	    PaddedPrimitiveNonVolatile<Boolean> done, 
    	    LookUpTable table,
            int numWorkers, 
    	    LamportQueue<Packet> queue,
            LamportQueue<Packet>[] sendQueue
    	    ) {
        this.done = done;
        this.table = (ParallelLookUpTable)table;
        this.numWorkers = numWorkers;
        this.queue = queue;
        this.sendQueue = sendQueue;

        fingerprint = new Fingerprint();
    }
  
    public void run() {
        Packet pkt;
        int id = -1;
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
                        
                    if (table.check(src, des)) {
                        checkOK++;
                        
                        if (false) {
                        //if (checkOK % (1 * (numWorkers + 1)) == 0) {
                            totalWork++;
                            int ret = (int)fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
                            residue += ret;                                                
                        } else {
                            //id = (id + 1) % (numWorkers);                               
                            boolean ok = true;
                            while (ok) {
                                try {
                                    id = (id + 1) % numWorkers;                               
                                /*
                                if (id == numWorkers) {
                                    id = -1;
                                    totalWork++;
                                    int ret = (int)fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
                                    residue += ret;                    
                                    break;
                                }
                                */

                                    sendQueue[id].enq(pkt);
                                    totalPackets++;
                                    ok = false;
                                } catch (FullException e) {
                                    fullCount++;
                                }
                            }  
                        }                       
                    }                        
                }
    	    } catch (EmptyException e) {
                emptyCount++;
            }
        }    
    }
    
}



class DataWorker implements PacketWorker {
    PaddedPrimitiveNonVolatile<Boolean> done;
    LamportQueue<Packet> queue;
    Histogram histogram;
    long totalPackets = 0;
    long emptyCount = 0;

    long residue = 0;
    Fingerprint fingerprint;

    public DataWorker(  		
  	    PaddedPrimitiveNonVolatile<Boolean> done, 
    	    LamportQueue<Packet> queue,
            Histogram histogram
    	    ) {
        this.done = done;
        this.queue = queue;
        this.histogram = histogram;

        fingerprint = new Fingerprint();
    }
  
    public void run() {
        Packet pkt;
        int id = 0;
        //while( !done.value || !queue.empty() ) {
        while (!done.value) {
    	    try {                
                pkt = queue.deq();
                totalPackets++;

                if (pkt.type != Packet.MessageType.DataPacket)
                    System.out.println("error!");

                int ret = (int)fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
                residue += ret;
                //histogram.insert(ret);                
    	    } catch (EmptyException e) {
                emptyCount++;
            }
        }    
    }
    
}

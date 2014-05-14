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

    int lockCount = 0;

    long residue = 0;    
    int test = 0;
    Histogram histogram;
    Fingerprint fingerprint;

    public ConfigWorker(  		
  	    PaddedPrimitiveNonVolatile<Boolean> done, 
    	    LookUpTable table,
            int numWorkers, 
    	    LamportQueue<Packet> queue,
            LamportQueue<Packet>[] sendQueue,
            Histogram histogram,
            int dptest
    	    ) {
        this.done = done;
        this.table = (ParallelLookUpTable)table;
        this.numWorkers = numWorkers;
        this.queue = queue;
        this.sendQueue = sendQueue;
        this.histogram = histogram;
        this.test = dptest;

        fingerprint = new Fingerprint();
    }
  
    public void run() {
        Packet pkt = null;
        int id = -1;
        while( !done.value || !queue.empty() ) {
        //while (!done.value) {
            if (queue.empty())
                emptyCount++;
            boolean flag = false;
            while (!flag) {
        	try {
                    pkt = queue.deq();
                    totalPackets++;
                    flag = true;
    	        } catch (EmptyException e) {;}

                if (done.value) break;
            }
            if (!flag) break;
                
            if (test == 0) {
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
                        
                        if (numWorkers != 0) {
                            boolean ok = true;
                            while (ok) {
                                //try {
                                    id = (id + 1) % numWorkers;
                                    if (sendQueue[id].full()) {
                                        fullCount++;
                                        continue;
                                    }

                                    sendQueue[id].enq(pkt);
                                    totalPackets++;
                                    ok = false;
                                //} catch (FullException e) {
                                //    fullCount++;
                                //}
                            }  
                        } else {
                            totalWork++;
                            int ret = (int)fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
                            residue += ret;
                            //histogram.insert(ret);                
                        }
                    }                        
                }   
            }
        }   
        this.lockCount = table.getlockCount();
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
        Packet pkt = null;
        int id = 0;
        while( !done.value || !queue.empty() ) {
        //while (!done.value) {
            if (queue.empty())
                emptyCount++;
            boolean flag = false;
            while (!flag) {
        	try {                
                    pkt = queue.deq();
                    totalPackets++;
                    flag = true;
    	        } catch (EmptyException e) {;}

                if (done.value) break;
            }
            if (!flag) break;


                if (pkt.type != Packet.MessageType.DataPacket)
                    System.out.println("error!");

                int ret = (int)fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
                residue += ret;
                //histogram.insert(ret);                
        }    
    }
    
}

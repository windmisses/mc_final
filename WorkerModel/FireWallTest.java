class SerialFireWall {
  public static void main(String[] args) {
    final int numMilliseconds = Integer.parseInt(args[0]);    
    final int numAddressesLog = Integer.parseInt(args[1]);
    final int numTrainsLog = Integer.parseInt(args[2]);
    final int meanTrainSize = Integer.parseInt(args[3]);
    final int meanTrainsPerComm = Integer.parseInt(args[4]);
    final int meanWindow = Integer.parseInt(args[5]);
    final int meanCommPerAddress = Integer.parseInt(args[6]);
    final int meanWork = Integer.parseInt(args[7]);
    final float configFraction = Float.parseFloat(args[8]);
    final float pngFraction = Float.parseFloat(args[9]);
    final float acceptingFraction = Float.parseFloat(args[10]);
    
    StopWatch timer = new StopWatch();
    PacketGenerator source = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow,
                                    meanCommPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);

    // prepare
    SerialLookUpTable table = new SerialLookUpTable(numAddressesLog);
    Histogram histogram = new Histogram();

    for (int i = 0; i < (1 << (numAddressesLog / 2 * 3)); i++) {
        Packet pkt = source.getConfigPacket();
        
        Config config = pkt.config;
        int address = config.address;
        table.change(address, config.addressBegin, config.addressEnd, config.personaNonGrata, config.acceptingRange);
    }

    // end prepare

    SerialPacketWorker workerData = new SerialPacketWorker(done, source, table, histogram);
    Thread workerThread = new Thread(workerData);

    workerThread.start();
    timer.startTimer();
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    done.value = true;
    memFence.value = true;

    try {
        workerThread.join();
    } catch (InterruptedException ignore) {;}      
    timer.stopTimer();

    final long totalCount = workerData.totalPackets;
    System.out.print("count " + totalCount);
    System.out.println(" time " + timer.getElapsedTime());
    System.out.println(" work : " + workerData.checkOK + "/" + workerData.totalPackets);
  }
}


class ParallelFireWall {
  public static void main(String[] args) {
    final int numMilliseconds = Integer.parseInt(args[0]);    
    final int numAddressesLog = Integer.parseInt(args[1]);
    final int numTrainsLog = Integer.parseInt(args[2]);
    final int meanTrainSize = Integer.parseInt(args[3]);
    final int meanTrainsPerComm = Integer.parseInt(args[4]);
    final int meanWindow = Integer.parseInt(args[5]);
    final int meanCommPerAddress = Integer.parseInt(args[6]);
    final int meanWork = Integer.parseInt(args[7]);
    final float configFraction = Float.parseFloat(args[8]);
    final float pngFraction = Float.parseFloat(args[9]);
    final float acceptingFraction = Float.parseFloat(args[10]);
    int numWorkers = Integer.parseInt(args[11]); 

    final int queueDepth = 256 / numWorkers;

    StopWatch timer = new StopWatch();
    PacketGenerator source = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow,
                                    meanCommPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);

    LamportQueue<Packet>[] queue = new LamportQueue[numWorkers];
    for (int i = 0; i < numWorkers; i++) 
        queue[i] = new LamportQueue<Packet>(queueDepth);
    
    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitiveNonVolatile<Boolean> doneWork = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
   
    // prepare
    ParallelLookUpTable table = new ParallelLookUpTable(numAddressesLog);
    Histogram histogram = new Histogram();

    for (int i = 0; i < (1 << (numAddressesLog / 2 * 3)); i++) {
        Packet pkt = source.getConfigPacket();
        
        Config config = pkt.config;
        int address = config.address;
        table.change(address, config.addressBegin, config.addressEnd, config.personaNonGrata, config.acceptingRange);
    }
    System.out.println("End initialization");
    // end prepare

    ParallelPacketDispatcher dispatcher = new ParallelPacketDispatcher(done, source, numWorkers, queue, table, histogram);
    ParallelPacketWorker[] workerDatas = new ParallelPacketWorker[numWorkers];

    for (int i = 0; i < numWorkers; i++) {
        workerDatas[i] = new ParallelPacketWorker(doneWork, table, queue[i], histogram);
    }

    Thread[] workerThread = new Thread[numWorkers];
    for (int i = 0; i < numWorkers; i++) {
        workerThread[i] = new Thread(workerDatas[i]);
        workerThread[i].start();
    }

    Thread dispatcherThread = new Thread(dispatcher);
    timer.startTimer();
    dispatcherThread.start();

    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    done.value = true;
    memFence.value = true;    
    try {
        dispatcherThread.join();
    } catch (InterruptedException ignore) {;}

    doneWork.value = true;
    memFence.value = false;
    for (int i = 0; i < numWorkers; i++) {
        try {
            workerThread[i].join();
        } catch (InterruptedException ignore) {;}
    }
    timer.stopTimer();

    long totalCount = dispatcher.totalPackets;
    System.out.print("count " + totalCount);
    System.out.print(" time " + timer.getElapsedTime());
    System.out.println(" throughput " + totalCount / timer.getElapsedTime());

    System.out.println(" Dispatcher fullCount: " + dispatcher.fullCount);

    for (int i = 0; i < numWorkers; i++) {
        totalCount -= workerDatas[i].totalPackets;
        System.out.println(" Thread " + i + " work : " + workerDatas[i].checkOK + "/" + workerDatas[i].totalWorkPackets + " EmptyCount: " + workerDatas[i].emptyCount);
    }

    if (totalCount != 0) {
        System.out.println("check failure: " + timer.getElapsedTime());    
    }
  }
}

import java.util.concurrent.locks.*;

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
    //System.out.println(" work : " + workerData.checkOK + "/" + workerData.totalPackets);
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

    
    int configThread = numWorkers;
    int numDispatcher = 1;
    int collect = 0;
    if (args.length > 12)
        configThread = Integer.parseInt(args[12]); 
    if (args.length > 13)
        numDispatcher = Integer.parseInt(args[13]); 
    if (args.length > 14)
        collect = Integer.parseInt(args[14]); 

    int dispatcherRateTest = 0;
    if (args.length > 15)
        dispatcherRateTest = Integer.parseInt(args[15]);

    int configThreadPer = configThread / numDispatcher;    
    final int queueDepth = 256 / configThread;
    final int dataThreadPer = (numWorkers - configThread) / configThread;

    StopWatch timer = new StopWatch();

    PacketGenerator[] source = new PacketGenerator[numDispatcher];
    for (int i = 0; i < numDispatcher; i++) {
        source[i] = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow,
                                    meanCommPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
    }

    LamportQueue<Packet>[][] configQueue = new LamportQueue[numDispatcher][configThreadPer];

    for (int i = 0; i < numDispatcher; i++) 
        for (int j = 0; j < configThreadPer; j++) {
            configQueue[i][j] = new LamportQueue<Packet>(queueDepth);
        }
    
    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitiveNonVolatile<Boolean> doneConfig = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitiveNonVolatile<Boolean> doneWork = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);

    ReentrantLock startLock[] = new ReentrantLock[numDispatcher];
    for (int i = 0; i < numDispatcher; i++) {
        startLock[i] = new ReentrantLock();
        startLock[i].lock();
    }        
   
    // prepare
    ParallelLookUpTable table = new ParallelLookUpTable(numAddressesLog);
    Histogram histogram = new Histogram();
    
    if (dispatcherRateTest == 0) {
        for (int i = 0; i < (1 << (numAddressesLog / 2 * 3)); i++) {
            Packet pkt = source[0].getConfigPacket();
        
            Config config = pkt.config;
            int address = config.address;
            table.change(address, config.addressBegin, config.addressEnd, config.personaNonGrata, config.acceptingRange);
        }
    }
    //System.out.println("End initialization");
    // end prepare

    ParallelPacketDispatcher[] dispatcher = new ParallelPacketDispatcher[numDispatcher];
    for (int i = 0; i < numDispatcher; i++) {
        dispatcher[i] = new ParallelPacketDispatcher(done, source[i], configThreadPer, configQueue[i], startLock[i]);
    }


    ConfigWorker[] configDatas = new ConfigWorker[configThread];
    DataWorker[][] dataDatas = new DataWorker[configThread][dataThreadPer];

    for (int i = 0; i < configThread; i++) {
        LamportQueue<Packet>[] dataQueue = new LamportQueue[dataThreadPer];
        for (int j = 0; j < dataThreadPer; j++)
            dataQueue[j] = new LamportQueue<Packet>(queueDepth);

        configDatas[i] = new ConfigWorker(doneConfig, table, dataThreadPer, configQueue[i / configThreadPer][i % configThreadPer], dataQueue, histogram, dispatcherRateTest);

        for (int j = 0; j < dataThreadPer; j++)
            dataDatas[i][j] = new DataWorker(doneWork, dataQueue[j], histogram);
    }



    Thread[] workerThread = new Thread[numWorkers];
    for (int i = 0; i < configThread; i++) {
        workerThread[i] = new Thread(configDatas[i]);
        workerThread[i].start();
    }
    int k = configThread;
    for (int i = 0; i < configThread; i++) 
        for (int j = 0; j < dataThreadPer; j++) {
            workerThread[k] = new Thread(dataDatas[i][j]);
            workerThread[k].start();
            k++;
        }

 
    Thread[] dispatcherThread = new Thread[numDispatcher];
    for (int i = 0; i < numDispatcher; i++) {
        dispatcherThread[i] = new Thread(dispatcher[i]);
        dispatcherThread[i].start();
    }

    try {
      Thread.sleep(200);
    } catch (InterruptedException ignore) {;}

    //System.out.println("start");
    timer.startTimer();
    for (int i = 0; i < numDispatcher; i++) {
        startLock[i].unlock();
    }

    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}

    done.value = true;
    memFence.value = true;    
    for (int i = 0; i < numDispatcher; i++) {
        try {
            dispatcherThread[i].join();
        } catch (InterruptedException ignore) {;}
        if (collect == 0)
            System.out.println("dispatcher " + i + " end");
    }

    doneConfig.value = true;
    memFence.value = false;
    for (int i = 0; i < configThread; i++) {
        try {
            workerThread[i].join();
        } catch (InterruptedException ignore) {;}
        if (collect == 0)
            System.out.println("config worker" + i + " end");
    }
    timer.stopTimer();


    doneWork.value = true;
    memFence.value = true;
    for (int i = configThread; i < configThread + configThread * dataThreadPer; i++) {
        try {
            workerThread[i].join();
        } catch (InterruptedException ignore) {;}
        if (collect == 0)
            System.out.println("data worker :" + i + " end");
    }
    timer.stopTimer();

    long totalCount = 0, fullCount = 0;;
    for (int i = 0; i < numDispatcher; i++) {
        totalCount += dispatcher[i].totalPackets;
        if (collect == 0)
            System.out.println("FullCount Dispatcher " + i + ": " + dispatcher[i].fullCount);
    }
    System.out.print("count " + totalCount);
    System.out.println(" time " + timer.getElapsedTime());

    if (collect == 0) {
        System.out.println(" throughput " + totalCount / timer.getElapsedTime());

        for (int i = 0; i < configThread; i++)  {
            System.out.println(" Thread " + i + " work : " + configDatas[i].totalWork + " accepted: " + configDatas[i].checkOK + "/" 
                            + configDatas[i].totalWorkPackets + " EmptyCount: " + (double)configDatas[i].emptyCount + " FullCount: " + configDatas[i].fullCount
                            + " LockCount: " + configDatas[i].lockCount + " totalPacket: " + configDatas[i].totalPackets);

            for (int j = 0; j < dataThreadPer; j++) 
                System.out.println("      Thread " + j + " work : " + dataDatas[i][j].totalPackets 
                                + " EmptyCount: " + (double)dataDatas[i][j].emptyCount);
        }
    }
  }
}

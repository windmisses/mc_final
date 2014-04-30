JFLAGS= 
#JC= $(JAVA_HOME)"/bin/javac"
JC= javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        LamportQueue.java \
	PaddedPrimitive.java \
	StopWatch.java \
	Fingerprint.java \
	RandomGenerator.java \
        PacketGenerator.java \
        SegmentTree.java \
	LookUpTable.java \
	PacketWorker.java \
        PacketDispatcher.java \
	FireWallTest.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class

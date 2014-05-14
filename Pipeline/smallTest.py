import utils
import os
import numpy as np
import scipy.io as sio
import sys

n_list = [2, 4, 8]
par1 = [11,   12,   12,   14,   15,   15,   15,   16];
par2 = [12,   10,   10,   10,   14,   15,   15,   14];
par3 = [5,    1,    4,    5,    9,    9,    10,   15];
par4 = [1,    3,    3,    5,    16,   10,   13,   15];
par5 = [3,    3,    6,    6,    7,    9,    8,    9 ];
par6 = [3,    1,    2,    2,    10,   9,    10,   5 ];
par7 = [3822, 2644, 1304, 315,  4007, 7125, 5328, 8840];
par8 = [0.24, 0.11, 0.10, 0.08, 0.02, 0.01, 0.04, 0.04];
par9 = [0.04, 0.09, 0.03, 0.05, 0.10, 0.20, 0.18, 0.19];
par0 = [0.96, 0.92, 0.90, 0.90, 0.84, 0.77, 0.80, 0.76];

re = 1

count = np.zeros((len(n_list) + 1, re))
time = np.zeros((len(n_list) + 1, re))

dpcount = np.zeros((len(n_list) + 1, re))
dptime = np.zeros((len(n_list) + 1, re))

index = int(sys.argv[1]);
collect = int(sys.argv[2]);

print collect

for ni in range(re):
    print 'Serial ni = %d'%(ni)
    if (collect == 1):
        os.popen('$JAVA_HOME/bin/java SerialFireWall %d %d %d %d %d %d %d %d %f %f %f > test.txt'%(2000, par1[index], par2[index], par3[index], par4[index], par5[index], par6[index], par7[index], par8[index], par9[index], par0[index]))
        res = utils.readCounterData('test.txt')
        time[0][ni] = res['time']
        count[0][ni] = res['count']
    else:
        os.popen('$JAVA_HOME/bin/java SerialFireWall %d %d %d %d %d %d %d %d %f %f %f > test.txt'%(2000, par1[index], par2[index], par3[index], par4[index], par5[index], par6[index], par7[index], par8[index], par9[index], par0[index]))
        with open('test.txt', 'r') as fin:
            print fin.read()
        


for t in range(len(n_list)):
    for ni in range(re):
        thread = n_list[t]
	print 'Parallel ni = %d, t = %d'%(ni, thread)
        if (collect == 1):
            os.popen('$JAVA_HOME/bin/java ParallelFireWall %d %d %d %d %d %d %d %d %f %f %f %d %d %d %d 1 > test.txt'%(2000, par1[index], par2[index], par3[index], par4[index], par5[index], par6[index], par7[index], par8[index], par9[index], par0[index], thread, thread, 1, collect))
    	    dpres = utils.readCounterData('test.txt')
    	    dptime[t + 1][ni] = dpres['time']
	    dpcount[t + 1][ni] = dpres['count']

            os.popen('$JAVA_HOME/bin/java ParallelFireWall %d %d %d %d %d %d %d %d %f %f %f %d %d %d %d > test.txt'%(2000, par1[index], par2[index], par3[index], par4[index], par5[index], par6[index], par7[index], par8[index], par9[index], par0[index], thread, thread, 1, collect))
    	    res = utils.readCounterData('test.txt')
    	    time[t + 1][ni] = res['time']
	    count[t + 1][ni] = res['count']
        else:
            os.popen('$JAVA_HOME/bin/java ParallelFireWall %d %d %d %d %d %d %d %d %f %f %f %d %d %d %d 1 > test.txt'%(2000, par1[index], par2[index], par3[index], par4[index], par5[index], par6[index], par7[index], par8[index], par9[index], par0[index], thread, thread, 1, collect))
            with open('test.txt', 'r') as fin:
                print fin.read()
 
            os.popen('$JAVA_HOME/bin/java ParallelFireWall %d %d %d %d %d %d %d %d %f %f %f %d %d %d %d > test.txt'%(2000, par1[index], par2[index], par3[index], par4[index], par5[index], par6[index], par7[index], par8[index], par9[index], par0[index], thread, thread, 1, collect))
            with open('test.txt', 'r') as fin:
                print fin.read()
            
if (collect == 1):
    for t in range(len(n_list) + 1):
        ans = 0
        dpt = 0
        for ni in range(re):
            ans = ans + count[t][ni] / time[t][ni]
            dpt = dpt + dpcount[t][ni] / dptime[t][ni]
        ans = ans / re;
        dpt = dpt / re;
        print ans, ' ', dpt

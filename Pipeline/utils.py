def extract_value(st):
	st = st.split()
	

def readCounterData(file):
	f = open(file, 'r')
	
	res = {}
	for line in f:
		input = line.split()
		print input
		
		for i in range(len(input) / 2):
			res[input[i * 2]] = float(input[i * 2 + 1])
	
	return res

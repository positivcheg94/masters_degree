import os
from os import path
import sys

import numpy as np
from plotly import offline as pltly
from plotly import graph_objs as pltly_obj

def plot_results(fpath, fname):
    fname_noex = fname[:fname.rfind(".")]
    full_data = np.loadtxt(path.join(fpath, fname), skiprows=1, delimiter=',')

    data = full_data
    
    x, time= data[:,0], data[:,1]
    
    trace1 = pltly_obj.Scatter(
            x=x,
            y=time
        )
        
    pltly.plot([trace1], filename=fname_noex+".html", auto_open=False)


def plot_results(datas, names):
	traces = []
	for d, nm in zip(datas, names):
		name = nm[:nm.rfind(".")]
		trace = pltly_obj.Scatter(x=d[:,0],y=d[:,1], name = name)
		traces.append(trace)
	pltly.plot(traces, filename="result"+".html", auto_open=False)


if __name__ == "__main__":
    files = os.listdir("./")
    target_files = list(filter(lambda x: path.isfile(path.join("./",x)) and x.endswith(".txt"), files))
    datas = []
    for tf in target_files:
    	full_data = np.loadtxt(path.join("./", tf), skiprows=1, delimiter=',')
    	datas.append(full_data)
    plot_results(datas, target_files)


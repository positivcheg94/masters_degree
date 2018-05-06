import os
from os import path
import sys

import numpy as np
from plotly import offline as pltly
from plotly import graph_objs as pltly_obj

def plot_results(fpath, fname, stop_point = 100):
    fname_noex = fname[:fname.rfind(".")]
    
    data = np.loadtxt(path.join(fpath, fname), skiprows=3, delimiter=',')
    
    x, y = data[:stop_point,0], data[:stop_point,2]
    
    trace = pltly_obj.Scatter(
        x = x,
        y = y
    )
        
    pltly.plot([trace], filename=fname_noex+".html", auto_open=False)

if __name__ == "__main__":
    stop_point = -1
    if len(sys.argv)>1:
        stop_point = int(sys.argv[1])
    files = os.listdir("./")
    target_files = filter(lambda x: path.isfile(path.join("./",x)) and x.endswith(".txt"), files)
    for tf in target_files:
        plot_results("./", tf, stop_point)


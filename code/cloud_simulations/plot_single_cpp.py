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

if __name__ == "__main__":
    files = os.listdir("./")
    target_files = filter(lambda x: path.isfile(path.join("./",x)) and x.endswith(".txt"), files)
    for tf in target_files:
        plot_results("./", tf)


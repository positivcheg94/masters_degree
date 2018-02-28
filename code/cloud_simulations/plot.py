import os
from os import path
import sys

import numpy as np
from plotly import offline as pltly
from plotly import graph_objs as pltly_obj

def plot_results(fpath, fname, subset = 0.01):
    fname_noex = fname[:fname.rfind(".")]
    
    full_data = np.loadtxt(path.join(fpath, fname), skiprows=3, delimiter=',')

    idx = np.random.choice([True,False], size=len(full_data), p=[subset, 1-subset])

    data = full_data[idx]
    
    x, y, time1, time2= data[:,0], data[:,1], data[:,2], data[:,3]
    
    trace1 = pltly_obj.Scatter3d(
            x=x,
            y=y,
            z=time1,
            mode='markers',
            marker={'size':2, 'color':time1, 'colorscale':'Jet'},
            stream={'maxpoints':20}
        )

    trace2 = pltly_obj.Scatter3d(
            x=x,
            y=y,
            z=time2,
            mode='markers',
            marker={'size':2, 'color':time2, 'colorscale':'Viridis'},
            stream={'maxpoints':20}
        )
        
    pltly.plot([trace1,trace2], filename=fname_noex+".html", auto_open=False)

if __name__ == "__main__":
    subset = 0.01
    if len(sys.argv) > 1:
        subset = float(sys.argv[1])
    files = os.listdir("./")
    target_files = filter(lambda x: path.isfile(path.join("./",x)) and x.endswith(".txt"), files)
    for tf in target_files:
        plot_results("./", tf, subset)


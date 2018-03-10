import os
from os import path
import sys

import numpy as np
from plotly import offline as pltly
from plotly import graph_objs as pltly_obj

def plot_results(fpath, fname, subset = 1):
    fname_noex = fname[:fname.rfind(".")]
    
    full_data = np.loadtxt(path.join(fpath, fname), skiprows=1, delimiter=',')

    idx = np.random.choice([True,False], size=len(full_data), p=[subset, 1-subset])

    data = full_data#[idx]
    
    x  = data[:,0]
    mn = int(np.min(x))
    mx = int(np.max(x))

    x = np.array(range(mn, mx+1))
    y = np.array(range(mn, mx+1))
    z1 = data[:,2].reshape((mx - mn + 1,-1))
    z2 = data[:,3].reshape((mx - mn + 1,-1))
    
    trace1 = pltly_obj.Surface(
            x=x,
            y=y,
            z=z1
        )

    trace2 = pltly_obj.Surface(
            x=x,
            y=y,
            z=z2
        )
        
    pltly.plot([trace1], filename=fname_noex+".html", auto_open=False)

if __name__ == "__main__":
    subset = 0.01
    if len(sys.argv) > 1:
        subset = float(sys.argv[1])
    files = os.listdir("./")
    target_files = filter(lambda x: path.isfile(path.join("./",x)) and x.endswith(".txt"), files)
    for tf in target_files:
        plot_results("./", tf, subset)


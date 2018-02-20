import os
from os import path

import numpy as np
from plotly import offline as pltly
from plotly import graph_objs as pltly_obj

def plot_results(fpath, fname, subset_ratio = 0.005):
    fname_noex = fname[:fname.rfind(".")]
    
    data = np.loadtxt(path.join(fpath, fname), skiprows=1, delimiter=',')
    
    idx = np.random.choice([True,False], size=len(data), p=[subset_ratio,1-subset_ratio])
    subdata = data[idx]
    x, y, time = subdata[:,0], subdata[:,1], subdata[:,2]
    
    trace1 = pltly_obj.Scatter3d(
        x=x,
        y=y,
        z=time,
        mode='markers',
        marker=dict(
            size=3,
            color=time,
            colorscale='Viridis',
            opacity=0.8
        )
    )
        
    pltly.plot([trace1], filename=fname_noex+".html", auto_open=False)

if __name__ == "__main__":
    files = os.listdir("./")
    target_files = filter(lambda x: path.isfile(path.join("./",x)) and x.endswith(".txt"), files)
    for tf in target_files:
        plot_results("./", tf)


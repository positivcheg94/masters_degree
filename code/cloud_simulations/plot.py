import os
from os import path

import numpy as np
from plotly import offline as pltly
from plotly import graph_objs as pltly_obj

def plot_results(fpath, fname):
    fname_noex = fname[:fname.rfind(".")]
    
    data = np.loadtxt(path.join(fpath, fname), skiprows=1, delimiter=',')
    
    x, y, time = data[:,0], data[:,1], data[:,2]
    
    trace1 = pltly_obj.Scatter3d(
            x=x,
            y=y,
            z=time,
            mode='markers',
            marker={'size':2, 'color':time, 'colorscale':'Jet'},
            stream={'maxpoints':20}
        )
        
    pltly.plot([trace1], filename=fname_noex+".html", auto_open=False)

if __name__ == "__main__":
    files = os.listdir("./")
    target_files = filter(lambda x: path.isfile(path.join("./",x)) and x.endswith(".txt"), files)
    for tf in target_files:
        plot_results("./", tf)


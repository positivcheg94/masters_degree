import os
from os import path
import sys

import colorlover as cl

import numpy as np
from scipy.interpolate import griddata

from plotly import offline as pltly
from plotly import graph_objs as pltly_obj

colors = ['#FF0000', '#00FF00', '#0000FF','#FFFF00', '#00FFFF', '#FF00FF']

def plot_results(datas, fnames, subset = 1):

    lines = []
    counter = 0
    for (full_data,fname) in zip(datas, fnames):
        fname_noex = fname[:fname.rfind(".")]

        #idx = np.random.choice([True,False], size=len(full_data), p=[subset, 1-subset])

        data = full_data
        x,y,z1,z2 = data[:, 0],data[:, 1],data[:, 2],data[:, 3]
        
        xi = np.linspace(min(x), max(x), num=int(max(x) - min(x)))
        yi = np.linspace(min(y), max(y), num=int(max(y) - min(y)))
        X, Y = np.meshgrid(xi, yi)
        Z = griddata((x,y), z1, (X, Y), method='cubic')

        line_marker = dict(color=colors[counter], width=2)
        lines.append(pltly_obj.Scatter3d(x=[0], y=[0], z=[0], name = fname_noex+"wf", legendgroup=fname_noex+"wf", showlegend=True, visible='legendonly', mode='marker', marker={'size':0.01}))
        for i, j, k in zip(X, Y, Z):
            lines.append(pltly_obj.Scatter3d(x=i, y=j, z=k, legendgroup=fname_noex+"wf", showlegend=False, mode='lines', line=line_marker))
        counter = counter + 1

    layout = pltly_obj.Layout(title = ' ',showlegend=True)
    fig = dict(data=lines, layout = layout)
    pltly.plot(fig, filename="result.html", auto_open=False)

if __name__ == "__main__":
    subset = 0.01
    if len(sys.argv) > 1:
        subset = float(sys.argv[1])
    files = os.listdir("./")
    target_files = list(filter(lambda x: path.isfile(path.join("./",x)) and x.endswith(".txt"), files))

    datas = [np.loadtxt(path.join('./', fname), skiprows=1, delimiter=',') for fname in target_files]
    plot_results(datas, target_files, subset)


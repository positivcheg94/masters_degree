import os
from os import path
import sys

import numpy as np
from scipy.interpolate import griddata

from matplotlib import cm
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D


def plot_results(fpath, fname, subset = 1):
    fname_noex = fname[:fname.rfind(".")]
    
    full_data = np.loadtxt(path.join(fpath, fname), skiprows=1, delimiter=',')

    #idx = np.random.choice([True,False], size=len(full_data), p=[subset, 1-subset])

    data = full_data

    x,y,z1,z2 = data[:, 0],data[:, 1],data[:, 2],data[:, 3]
    X,Y = np.meshgrid(x,y)
    Z1 = griddata((x,y), z1, (X,Y), method='cubic')
    Z2 = griddata((x,y), z2, (X,Y), method='cubic')


    fig = plt.figure()
    ax = fig.gca(projection='3d')
    surf = ax.plot_surface(X, Y, Z2, linewidth=0, antialiased=False)
    plt.show()




if __name__ == "__main__":
    subset = 0.01
    if len(sys.argv) > 1:
        subset = float(sys.argv[1])
    files = os.listdir("./")
    target_files = filter(lambda x: path.isfile(path.join("./",x)) and x.endswith(".txt"), files)
    for tf in target_files:
        plot_results("./", tf, subset)


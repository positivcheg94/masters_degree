import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import nash

def divisorGenerator(n):
    large_divisors = []
    for i in range(1, int(np.sqrt(n) + 1)):
        if n % i == 0:
            yield i
            if i*i != n:
                large_divisors.append(n / i)
    for divisor in reversed(large_divisors):
        yield divisor
        
def divisors(n):
    return np.array(list(divisorGenerator(n)), dtype=np.int)

def argmin(array):
    return np.unravel_index(np.argmin(array, axis=None), array.shape)


def read_file(file_name):
    file = open(file_name, "r")
    general_info = file.readline()
    slices = file.readline()
    slices = list(map(lambda x: int(x),slices[slices.find("=")+1:].split("-")))
    mips = file.readline()
    file.readline()
    data = np.loadtxt(file,delimiter=",")
    general_info = {g[:g.find("=")]:float(g[g.find("=")+1:]) for g in general_info.split("|||")}
    
    n = len(slices)
    data1 = data[:,2]
    data2 = data[:,3]
    df1 = pd.DataFrame(data1.reshape((n,n)), index=slices, columns=slices)
    df2 = pd.DataFrame(data2.reshape((n,n)), index=slices, columns=slices)
    
    return general_info, np.array(slices), df1, df2


def descend(matrix, a_0, b_0):
    n_v = matrix.shape[0]
    n_h = matrix.shape[1]
    p = np.array([a_0,b_0])
    p_history = [p]
    
    while True:
        fl = max(p[0]-1,0)
        fr = min(p[0]+2,n_h)
        sl = max(p[1]-1,0)
        sr = min(p[1]+2,n_v)
        block = matrix[fl:fr, sl:sr]
        next_offset = argmin(block)
        p_next = p + next_offset
        if p[0] != 0:
            p_next[0] = p_next[0] - 1
        if p[1] != 0:
            p_next[1] = p_next[1] - 1
        p = p_next
        p_history.append(p)
        if np.all(p_history[-1] == p_history[-2]):
            break
    return np.array(p_history[:-1])

gen, slices, data_f, data_s = read_file("./sim_min_min_n_2500_m_1.000e+10_bw_1.000e+08_p_0.000e+00_rd_10.txt")

def run_descend_print(matrix, a_0, b_0, slices):
    idx = descend(matrix, a_0, b_0)
    print(idx)
    print("start = ", slices[idx[0]])
    print("end   = ", slices[idx[-1]])
    print("time  = ", matrix[idx[0][0], idx[0][1]])
    print("time  = ", matrix[idx[-1][0], idx[-1][1]])
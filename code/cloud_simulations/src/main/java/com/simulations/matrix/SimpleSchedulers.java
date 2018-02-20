package com.simulations.matrix;

import org.cloudbus.cloudsim.core.CloudSim;

public class SimpleSchedulers
{
    public static class MaxMaxScheduler extends BasicScheduler
    {
        public MaxMaxScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator((vm1,vm2)->Double.compare(vm2.getMips(), vm1.getMips()));
            setCloudletComparator((c1,c2)->Long.compare(c2.getLength(), c1.getLength()));
        }
    }

    public static class MinMinScheduler extends BasicScheduler
    {
        public MinMinScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator((vm1,vm2)->Double.compare(vm1.getMips(), vm2.getMips()));
            setCloudletComparator((c1,c2)->Long.compare(c1.getLength(), c2.getLength()));
        }
    }

    public static class MinMaxScheduler extends BasicScheduler
    {
        public MinMaxScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator((vm1,vm2)->Double.compare(vm2.getMips(), vm1.getMips()));
            setCloudletComparator((c1,c2)->Long.compare(c1.getLength(), c2.getLength()));
        }
    }

    public static class MaxMinScheduler extends BasicScheduler
    {
        public MaxMinScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator((vm1,vm2)->Double.compare(vm1.getMips(), vm2.getMips()));
            setCloudletComparator((c1,c2)->Long.compare(c2.getLength(), c1.getLength()));
        }
    }
}

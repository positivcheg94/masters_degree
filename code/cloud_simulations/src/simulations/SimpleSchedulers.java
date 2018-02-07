package simulations;

import org.cloudbus.cloudsim.core.CloudSim;

public class SimpleSchedulers
{
    public static class MaxMaxScheduler extends BasicScheduler
    {
        MaxMaxScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator(new CloudSimComparators.VmMaxFirstComparator());
            setCloudletComparator(new CloudSimComparators.CloudletMaxFirstComparator());
        }
    }

    public static class MinMinScheduler extends BasicScheduler
    {
        MinMinScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator(new CloudSimComparators.VmMinFirstComparator());
            setCloudletComparator(new CloudSimComparators.CloudletMinFirstComparator());
        }
    }

    public static class MinMaxScheduler extends BasicScheduler
    {
        MinMaxScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator(new CloudSimComparators.VmMaxFirstComparator());
            setCloudletComparator(new CloudSimComparators.CloudletMinFirstComparator());
        }
    }

    public static class MaxMinScheduler extends BasicScheduler
    {
        MaxMinScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator(new CloudSimComparators.VmMinFirstComparator());
            setCloudletComparator(new CloudSimComparators.CloudletMaxFirstComparator());
        }
    }
}

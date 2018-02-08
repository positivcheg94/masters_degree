package simulations;

import org.cloudbus.cloudsim.core.CloudSim;

public class SimpleSchedulers
{
    public static class MaxMaxScheduler extends BasicScheduler
    {
        public MaxMaxScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator(new CloudSimComparators.VmMaxFirstComparator());
            setCloudletComparator(new CloudSimComparators.CloudletMaxFirstComparator());
        }
    }

    public static class MinMinScheduler extends BasicScheduler
    {
        public MinMinScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator(new CloudSimComparators.VmMinFirstComparator());
            setCloudletComparator(new CloudSimComparators.CloudletMinFirstComparator());
        }
    }

    public static class MinMaxScheduler extends BasicScheduler
    {
        public MinMaxScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator(new CloudSimComparators.VmMaxFirstComparator());
            setCloudletComparator(new CloudSimComparators.CloudletMinFirstComparator());
        }
    }

    public static class MaxMinScheduler extends BasicScheduler
    {
        public MaxMinScheduler(CloudSim simulation)
        {
            super(simulation);
            setVmComparator(new CloudSimComparators.VmMinFirstComparator());
            setCloudletComparator(new CloudSimComparators.CloudletMaxFirstComparator());
        }
    }
}

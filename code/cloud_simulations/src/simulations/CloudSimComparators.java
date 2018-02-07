package simulations;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Comparator;

public class CloudSimComparators
{
    public static class VmMaxFirstComparator implements Comparator<Vm>
    {
        public int compare(Vm vm1, Vm vm2)
        {
            if (vm2.getMips() == vm1.getMips())
                return 0;
            return vm2.getMips() - vm1.getMips() > 0.0 ? 1 : -1;
        }
    }

    public static class VmMinFirstComparator implements Comparator<Vm>
    {
        public int compare(Vm vm1, Vm vm2)
        {
            if (vm2.getMips() == vm1.getMips())
                return 0;
            return vm2.getMips() - vm1.getMips() > 0.0 ? 1 : -1;
        }
    }

    public static class CloudletMaxFirstComparator implements  Comparator<Cloudlet>
    {
        public int compare(Cloudlet c1, Cloudlet c2)
        {
            if (c2.getLength() == c1.getLength())
                return 0;
            return c2.getLength() - c1.getLength() > 0 ? 1 : -1;
        }
    }

    public static class CloudletMinFirstComparator implements  Comparator<Cloudlet>
    {
        public int compare(Cloudlet c1, Cloudlet c2)
        {
            if (c2.getLength() == c1.getLength())
                return 0;
            return c2.getLength() - c1.getLength() > 0 ? 1 : -1;
        }
    }
}

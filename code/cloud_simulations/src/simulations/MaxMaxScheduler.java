package simulations;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import  org.cloudbus.cloudsim.brokers.DatacenterBrokerAbstract;
import org.cloudbus.cloudsim.vms.Vm;

public class MaxMaxScheduler extends DatacenterBrokerAbstract
{
    MaxMaxScheduler(CloudSim simulation)
    {
        super(simulation);
        setDatacenterSupplier(this::selectDatacenterForWaitingVms);
        setFallbackDatacenterSupplier(this::selectFallbackDatacenterForWaitingVms);
        setVmMapper(this::selectVmForWaitingCloudlet);
    }

    protected Datacenter selectDatacenterForWaitingVms() {
        return (getDatacenterList().isEmpty() ? Datacenter.NULL : getDatacenterList().get(0));
    }

    protected Datacenter selectFallbackDatacenterForWaitingVms() {
        return getDatacenterList().stream()
                .filter(dc -> !getDatacenterRequestedList().contains(dc))
                .findFirst()
                .orElse(Datacenter.NULL);
    }

    protected Vm selectVmForWaitingCloudlet(Cloudlet cloudlet) {
        if (cloudlet.isBindToVm() && getVmExecList().contains(cloudlet.getVm())) {
            return cloudlet.getVm();
        }
        
        return getVmFromCreatedList(getNextVmIndex());
    }

    private int getNextVmIndex() {
        if (getVmExecList().isEmpty()) {
            return -1;
        }

        final int vmIndex = getVmExecList().indexOf(getLastSelectedVm());
        return (vmIndex + 1) % getVmExecList().size();
    }

}

package simulations;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;

public class DataCenterSimpleFixed extends DatacenterSimple{

    private static double small_period = 0.0001;
    DataCenterSimpleFixed(Simulation simulation,
                          DatacenterCharacteristics characteristics,
                          VmAllocationPolicy vmAllocationPolicy)
    {
        super(simulation, characteristics, vmAllocationPolicy);
    }

    @Override
    protected boolean isTimeToUpdateCloudletsProcessing()
    {
        return getSimulation().clock() < 0.111 ||
                getSimulation().clock() >= getLastProcessTime() + small_period;
    }
}

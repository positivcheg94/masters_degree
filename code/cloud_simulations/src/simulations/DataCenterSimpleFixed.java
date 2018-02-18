package simulations;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;

public class DataCenterSimpleFixed extends DatacenterSimple{

    private double UpdatePeriodThreshold = 0.001;
    DataCenterSimpleFixed(Simulation simulation,
                          DatacenterCharacteristics characteristics,
                          VmAllocationPolicy vmAllocationPolicy)
    {
        super(simulation, characteristics, vmAllocationPolicy);
    }

    public void setUpdatePeriodThreshold(double value)
    {
        UpdatePeriodThreshold = value;
    }

    public double getUpdatePeriodThreshold()
    {
        return UpdatePeriodThreshold;
    }

    @Override
    protected boolean isTimeToUpdateCloudletsProcessing()
    {
        return getSimulation().clock() < 0.111 || getSimulation().clock() >= getLastProcessTime() + UpdatePeriodThreshold;
    }
}

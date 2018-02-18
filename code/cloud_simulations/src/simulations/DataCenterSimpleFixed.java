package simulations;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;

public class DataCenterSimpleFixed extends DatacenterSimple{

    private double UpdatePeriodThreshold;
    private static double UpdatePeriodThresholdAddition = 0.01;
    private boolean can_schedule_after_period = true;

    //private double UpdatePeriodThreshold = 0.001;
    DataCenterSimpleFixed(Simulation simulation,
                          DatacenterCharacteristics characteristics,
                          VmAllocationPolicy vmAllocationPolicy)
    {
        super(simulation, characteristics, vmAllocationPolicy);
        UpdatePeriodThreshold = simulation.getMinTimeBetweenEvents() + UpdatePeriodThresholdAddition;
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

    @Override
    protected double updateCloudletProcessing() {
        if (!isTimeToUpdateCloudletsProcessing())
        {
            // Schedule VM update processing
            // This fixes a bug when many ( bug is visible when all  finished ) before UpdatePeriodThreshold happens
            if(can_schedule_after_period)
            {
                schedule(getId(), UpdatePeriodThreshold + UpdatePeriodThresholdAddition, CloudSimTags.VM_UPDATE_CLOUDLET_PROCESSING_EVENT);
                can_schedule_after_period = false;
            }
            return Double.MAX_VALUE;
        }
        can_schedule_after_period = true;

        double nextSimulationTime = updateHostsProcessing();
        if (nextSimulationTime != Double.MAX_VALUE) {
            nextSimulationTime = getCloudletProcessingUpdateInterval(nextSimulationTime);
            schedule(getId(),
                    nextSimulationTime,
                    CloudSimTags.VM_UPDATE_CLOUDLET_PROCESSING_EVENT);
        }
        setLastProcessTime(getSimulation().clock());
        return nextSimulationTime;
    }
}

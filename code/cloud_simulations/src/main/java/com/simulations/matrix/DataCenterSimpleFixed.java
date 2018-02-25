package com.simulations.matrix;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.List;
import java.util.stream.Collectors;

public class DataCenterSimpleFixed extends DatacenterSimple{

    private double UpdatePeriodThreshold = 0.001;
    private static double UpdatePeriodThresholdAddition = 0.0001;
    private boolean can_schedule_after_period = true;

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

    @Override
    protected double updateHostsProcessing() {
        double nextSimulationTime = Double.MAX_VALUE;
        for (final Host host : getHostList()) {
            final double time = host.updateProcessing(getSimulation().clock());
            nextSimulationTime = Math.min(time, nextSimulationTime);
        }

        // Guarantees a minimal interval before scheduling the event
        final double minTimeBetweenEvents = UpdatePeriodThreshold + UpdatePeriodThresholdAddition;
        nextSimulationTime = Math.max(nextSimulationTime, minTimeBetweenEvents);

        if (nextSimulationTime == Double.MAX_VALUE) {
            return nextSimulationTime;
        }

        return nextSimulationTime;
    }

    @Override
    public void checkCloudletsCompletionForGivenVm(Vm vm) {
        CloudletScheduler cl_scheduler = vm.getCloudletScheduler();
        List<CloudletExecution> finishedList = cl_scheduler.getCloudletFinishedList();
        if(finishedList.size() > 0) {
            Cloudlet cl = finishedList.get(finishedList.size() - 1).getCloudlet();
            if(!cl_scheduler.isCloudletReturned(cl))
            {
                this.sendNow(cl.getBroker().getId(), 20, cl);
                cl_scheduler.addCloudletToReturnedList(cl);
            }
        }
    }
}

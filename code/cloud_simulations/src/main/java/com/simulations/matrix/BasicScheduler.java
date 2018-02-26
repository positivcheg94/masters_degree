package com.simulations.matrix;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerAbstract;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.function.Function;

public class BasicScheduler extends DatacenterBrokerAbstract {
    private final HashSet<Cloudlet> submittedCloudlets = new HashSet<Cloudlet>();
    private final List<Cloudlet> finishedCloudlets = new ArrayList<>();
    private int runningCloudlets = 0;

    private final CloudSim sim;

    BasicScheduler(CloudSim simulation) {
        super(simulation);
        sim = simulation;

        setDatacenterSupplier(this::selectDatacenterForWaitingVms);
        setFallbackDatacenterSupplier(this::selectFallbackDatacenterForWaitingVms);
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

    @Override
    public Set<Cloudlet> getCloudletCreatedList() {
        return submittedCloudlets;
    }

    @Override
    public <T extends Cloudlet> List<T> getCloudletFinishedList() {
        return (List<T>) new ArrayList<>(finishedCloudlets);
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {
        final Cloudlet c = (Cloudlet) ev.getData();
        finishedCloudlets.add(c);
        println(String.format("%.2f: %s: %s %d finished and returned to broker.",
                getSimulation().clock(), getName(), c.getClass().getSimpleName(), c.getId()));
        --runningCloudlets;

        if(!getCloudletWaitingList().isEmpty()) {
            sendOneCloudletToVm(c.getVm());
        }

        if (runningCloudlets > 0) {
            return;
        }

        final Function<Vm, Double> func = getVmDestructionDelayFunction().apply(c.getVm()) < 0 ? vm -> 0.0 : getVmDestructionDelayFunction();
        //If gets here, all running cloudlets have finished and returned to the broker.
        if (getCloudletWaitingList().isEmpty()) {
            println(String.format(
                    "%.2f: %s: All submitted Cloudlets finished executing.",
                    getSimulation().clock(), getName()));
            requestIdleVmsDestruction(func);
            return;
        }

        /*There are some cloudlets waiting their VMs to be created.
        Then, destroys finished VMs and requests creation of waiting ones.
        When there is waiting Cloudlets, it always request the destruction
        of idle VMs to possibly free resources to start waiting
        VMs. This way, the a VM destruction delay function is not set,
        defines one which always return 0 to indicate
        that in this situation, idle VMs must be destroyed immediately.
        */
        requestIdleVmsDestruction(func);
        requestDatacenterToCreateWaitingVms();
    }

    protected boolean sendOneCloudletToVm(Vm vm) {
        List<Cloudlet> waiting = getCloudletWaitingList();
        if (waiting.isEmpty())
            return false;
        Cloudlet cloudlet = waiting.remove(0);
        cloudlet.setVm(vm);
        send(getVmDatacenter(vm).getId(), 0.0, CloudSimTags.CLOUDLET_SUBMIT_ACK, cloudlet);
        ++runningCloudlets;

        return true;
    }

    @Override
    protected void requestDatacentersToCreateWaitingCloudlets()
    {
        for (Vm vm : getVmExecList())
        {
            sendOneCloudletToVm(vm);
        }
    }
}

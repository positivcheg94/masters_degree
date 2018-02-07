package simulations;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerAbstract;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.function.Function;

public class BasicScheduler extends DatacenterBrokerAbstract
{
    private final HashSet<Cloudlet> submittedCloudlets = new HashSet<Cloudlet>();
    private final List<Cloudlet> finishedCloudlets = new ArrayList<>();
    private int runningCloudlets = 0;

    BasicScheduler(CloudSim simulation)
    {
        super(simulation);
        setDatacenterSupplier(this::selectDatacenterForWaitingVms);
        setFallbackDatacenterSupplier(this::selectFallbackDatacenterForWaitingVms);
        setVmMapper(this::selectFirstFreeVm);
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

    protected Vm selectFirstFreeVm(Cloudlet cloudlet) {
        if (cloudlet.isBindToVm() && getVmExecList().contains(cloudlet.getVm())) {
            return cloudlet.getVm();
        }

        List<Cloudlet> finished = getCloudletFinishedList();
        Set<Cloudlet> created = getCloudletCreatedList();
        Set<Vm> running = new HashSet<Vm>();
        for(Iterator<Cloudlet> i = created.iterator(); i.hasNext();)
        {
            Cloudlet current = i.next();
            if(!finished.contains(current))
            {
                running.add(current.getVm());
            }
        }

        List<Vm> runningVms = getVmExecList();
        for (Iterator<Vm> i = runningVms.iterator(); i.hasNext();) {
            Vm vm = i.next();
            // Assume that vm is not used if CPU usage is 0
            if(!running.contains(vm))
                return vm;
        }

        return Vm.NULL;
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
    public void processEvent(SimEvent ev) {
        super.processEvent(ev);
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {
        final Cloudlet c = (Cloudlet) ev.getData();
        finishedCloudlets.add(c);
        println(String.format("%.2f: %s: %s %d finished and returned to broker.",
                getSimulation().clock(), getName(), c.getClass().getSimpleName(), c.getId()));
        --runningCloudlets;

        if(runningCloudlets > 0)
        {
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

    @Override
    protected void requestDatacentersToCreateWaitingCloudlets()
    {
        List<Cloudlet> waiting = getCloudletWaitingList();
        final List<Cloudlet> successfullySubmitted = new ArrayList<>();
        int nVms = getVmExecList().size();
        Iterator<Cloudlet> i = waiting.iterator();
        Iterator<Vm> ii = getVmExecList().iterator();
        while(nVms > 0 && i.hasNext())
        {
            Cloudlet cloudlet = i.next();
            Vm vm = ii.next();
            cloudlet.setVm(vm);
            send(getVmDatacenter(vm).getId(),
                    cloudlet.getSubmissionDelay(), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            successfullySubmitted.add(cloudlet);

            final String delayStr =
                    cloudlet.getSubmissionDelay() > 0 ?
                            String.format(" with a requested delay of %.0f seconds", cloudlet.getSubmissionDelay()) :
                            "";
            println(String.format(
                    "%.2f: %s: Sending %s %d to %s in %s%s.",
                    getSimulation().clock(), getName(), cloudlet.getClass().getSimpleName(), cloudlet.getId(),
                    vm, vm.getHost(), delayStr));
            --nVms;
            ++runningCloudlets;
        }

        // remove created cloudlets from waiting list
        waiting.removeAll(successfullySubmitted);
        submittedCloudlets.addAll(successfullySubmitted);
    }
}

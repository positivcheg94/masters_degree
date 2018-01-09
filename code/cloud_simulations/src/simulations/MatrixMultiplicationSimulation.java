/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2016  Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package simulations;

import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MatrixMultiplicationSimulation {
    private static final int HUGE_VALUE = 1000000000;
    // Matrix multiplication complexity
    private static final double additionComplexity = 0.0001;
    private static final double multiplicationComplexityMultiplier = 1.4;
    private static double additionMIPS = additionComplexity;

    private static final int HOSTS = 1;
    private static final int HOST_PES = 100;
    private static final int HOST_P_MIPS = HUGE_VALUE;

    private static int counter = 0;

    //members
    private final int problem_size;
    private final int slice1;
    private final int slice2;

    public MatrixMultiplicationSimulation(int n, int slice_size1, int slice_size2) {
        problem_size = n;
        slice1 = slice_size1;
        slice2 = slice_size2;
    }

    int problemSize()
    {
        return problem_size;
    }

    int getSlise1()
    {
        return slice1;
    }

    int getSlise2()
    {
        return slice2;
    }

    private static double CalculateOverallComplexity(long m, long n, long k, double addComplexity, double mulComplexityMultiplication)
    {
        return m*k*addComplexity*(n*n*mulComplexityMultiplication + n-1);
    }

    private static List<Vm> createVmsWithMIPS(List<Long> MIPS) {
        final List<Vm> list = new ArrayList<>(MIPS.size());
        for (int i = 0; i < MIPS.size(); i++) {
            Vm vm =  new VmSimple(i, MIPS.get(i), 1)
                    .setRam(512).setBw(1000)
                    .setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
        }
        return list;
    }

    private static Datacenter createDatacenter(Simulation sim) {
        final List<Host> hostList = new ArrayList<>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            Host host = createHost();
            hostList.add(host);
        }
        DatacenterCharacteristics characteristics = new DatacenterCharacteristicsSimple(hostList);
        final Datacenter dc = new DatacenterSimple(sim, characteristics, new VmAllocationPolicySimple());
        return dc;
    }

    private static Host createHost() {
        List<Pe> peList = new ArrayList<>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(HOST_P_MIPS, new PeProvisionerSimple()));
        }

        final long ram = HUGE_VALUE; //in Megabytes
        final long bandwidth = HUGE_VALUE; //in Megabits/s
        final long storage = HUGE_VALUE; //in Megabytes
        ResourceProvisioner ramProvisioner = new ResourceProvisionerSimple();
        ResourceProvisioner bwProvisioner = new ResourceProvisionerSimple();
        VmScheduler vmScheduler = new VmSchedulerTimeShared();
        Host host = new HostSimple(ram, bandwidth, storage, peList);
        host.setRamProvisioner(ramProvisioner).setBwProvisioner(bwProvisioner).setVmScheduler(vmScheduler);
        return host;
    }



    private static List<Cloudlet> createCloudlets(long n, long slice) {

        long n_slices = n/slice;
        boolean partial_slice = n%slice != 0;
        long partial_slice_size = n - n_slices*slice;

        long n_cloudlets = n_slices;
        if(partial_slice);
        n_cloudlets++;
        n_cloudlets = n_cloudlets*n_cloudlets;

        final List<Cloudlet> list = new ArrayList<>((int)n_cloudlets);
        UtilizationModel utilization = new UtilizationModelFull();

        long full_complexity = (long)(additionMIPS* CalculateOverallComplexity(slice,n,slice,additionComplexity,multiplicationComplexityMultiplier));
        long p_complexity = (long)(additionMIPS* CalculateOverallComplexity(slice,n,partial_slice_size,additionComplexity,multiplicationComplexityMultiplier));
        long pp_complexity = (long)(additionMIPS* CalculateOverallComplexity(n,slice,n,additionComplexity,multiplicationComplexityMultiplier));

        for (int i = 0; i < n_slices; i++)
        {
            for (int j = 0; i < n_slices; i++)
            {
                list.add(new CloudletSimple(counter++, full_complexity, 1)
                        .setUtilizationModel(utilization));
            }
        }
        if(partial_slice)
        {
            for (int i = 0; i < n_slices; i++)
            {
                list.add(new CloudletSimple(counter++, p_complexity, 1)
                        .setUtilizationModel(utilization));
                list.add(new CloudletSimple(counter++, p_complexity, 1)
                        .setUtilizationModel(utilization));
            }
            list.add(new CloudletSimple(counter++, pp_complexity, 1)
                    .setUtilizationModel(utilization));
        }

        return list;
    }

    public static double simulateProblem(long size, long slice1, long slice2, List<Long> MIPSCapacities)
    {
        MatrixMultiplicationSimulation MMS = new MatrixMultiplicationSimulation(100000, 100, 200);

        CloudSim simulation = new CloudSim();

        Datacenter datacenter = createDatacenter(simulation);

        DatacenterBroker broker0 = new MaxMaxScheduler(simulation);

        List<Vm> vmList = createVmsWithMIPS(MIPSCapacities);
        broker0.submitVmList(vmList);

        List<Cloudlet> firstPlayerCloudlets = createCloudlets(MMS.problemSize(), MMS.getSlise1());
        List<Cloudlet> secondPlayerCloudlets = createCloudlets(MMS.problemSize(), MMS.getSlise2());

        broker0.submitCloudletList(firstPlayerCloudlets);
        broker0.submitCloudletList(secondPlayerCloudlets);

        simulation.start();

        return simulation.clock();
    }


    public static void main(String[] args)
    {
        Log.disable();

        final int max_slice_size = 100;
        
        List<Long> MipsCapacities = new ArrayList<>();
        MipsCapacities.add((long)(100000*additionMIPS));
        MipsCapacities.add((long)(50002341*additionMIPS));
        MipsCapacities.add((long)(600240*additionMIPS));
        MipsCapacities.add((long)(2000213*additionMIPS));

        double results[][] = new double[max_slice_size][];
        for(int i = 1; i < max_slice_size; ++i)
        {
            double[] results_i = new double[max_slice_size];
            for (int j = 1; j < max_slice_size; ++j)
            {
                results_i[j] = simulateProblem(1000, i, j, MipsCapacities);
            }
            results[i] = results_i;
        }


    }
}
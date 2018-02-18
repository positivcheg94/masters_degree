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

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


// Theoretical values:
// machine can do ~ 10000000000 additions per second
// so make it a nominal value

public class MatrixMultiplicationSimulation {
    // Nominal MIPS (i7-6700K 1 core)
    private static final long NOMINAL_MIPS = 10000000000L;
    private static final long TAKEN_NOMINAL_MIPS = NOMINAL_MIPS;

    // Matrix multiplication complexity
    private static final double multiplicationComplexityMultiplier = 1.4;

    private static final int HOSTS = 1;
    private static final int HOST_PES = 100;
    private static final long HUGE_VALUE = 10000000000000000L;
    private static final long HOST_P_MIPS = TAKEN_NOMINAL_MIPS*1000;

    private static int counter = 0;

    //members
    private final long problem_size;
    private final long slice1;
    private final long slice2;

    public MatrixMultiplicationSimulation(long n, long slice_size1, long slice_size2) {
        problem_size = n;
        slice1 = slice_size1;
        slice2 = slice_size2;
    }

    long problemSize()
    {
        return problem_size;
    }

    long getSlise1()
    {
        return slice1;
    }

    long getSlise2()
    {
        return slice2;
    }

    // Calculate complexity of (m,n) dot (n,k).
    // In general - O(m*n*k)
    // Returns MIPS estimation assuming double+double = 1 MI
    private static double CalculateOverallComplexity(long m, long n, long k, double mulComplexityMultiplication)
    {
        //return m*k*(n*mulComplexityMultiplication + n-1);
        return m*k*n*mulComplexityMultiplication;
    }

    private static List<Vm> createVmsWithMIPS(List<Long> MIPS) {
        final List<Vm> list = new ArrayList<>(MIPS.size());
        for (int i = 0; i < MIPS.size(); i++) {
            Vm vm =  new VmSimple(i, MIPS.get(i), 1)
                    .setRam(512).setBw(1000)
                    .setCloudletScheduler(new CloudletSchedulerSpaceShared());
                    //.setCloudletScheduler(new CloudletSchedulerTimeShared());
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
        final Datacenter dc = new DataCenterSimpleFixed(sim, characteristics, new VmAllocationPolicySimple());
        return dc;
    }

    private static Host createHost() {
        List<Pe> peList = new ArrayList<>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(HOST_P_MIPS, new PeProvisionerSimple()));
        }

        final long ram = 1000000; //in Megabytes
        final long bandwidth = 10000000; //in Megabits/s
        final long storage = HUGE_VALUE; //in Megabytes
        ResourceProvisioner ramProvisioner = new ResourceProvisionerSimple();
        ResourceProvisioner bwProvisioner = new ResourceProvisionerSimple();
        VmScheduler vmScheduler = new VmSchedulerSpaceShared();
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

        long full_complexity = (long)(CalculateOverallComplexity(slice,n,slice,multiplicationComplexityMultiplier));
        long p_complexity = (long)(CalculateOverallComplexity(slice,n,partial_slice_size,multiplicationComplexityMultiplier));
        long pp_complexity = (long)(CalculateOverallComplexity(n,slice,n,multiplicationComplexityMultiplier));

        for (int i = 0; i < n_slices; i++)
        {
            for (int j = 0; j < n_slices; j++)
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

    public static double simulateProblem(Class brokerClass, List<Long> MIPSCapacities,
                                         long size, long slice1, long slice2, boolean debug_info)
            throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        MatrixMultiplicationSimulation MMS = new MatrixMultiplicationSimulation(size, slice1, slice2);

        double start, end;

        CloudSim simulation = new CloudSim();

        Datacenter datacenter = createDatacenter(simulation);

        Constructor constructor = brokerClass.getConstructor(CloudSim.class);
        DatacenterBroker broker = (DatacenterBroker)constructor.newInstance(simulation);

        List<Vm> vmList = createVmsWithMIPS(MIPSCapacities);
        broker.submitVmList(vmList);

        start = System.nanoTime();
        List<Cloudlet> cloudlets = createCloudlets(MMS.problemSize(), MMS.getSlise1());
        cloudlets.addAll(createCloudlets(MMS.problemSize(), MMS.getSlise2()));
        end = System.nanoTime();

        // DEBUG INFO
        if(debug_info)
        {
            System.out.println("Cloudlet creation duration ");
            System.out.println(Double.toString((end - start) / 1e9));
            System.out.println(String.format("Submited %d cloudlets", cloudlets.size()));
        }

        broker.submitCloudletList(cloudlets);

        start = System.nanoTime();
        simulation.start();
        end = System.nanoTime();

        List<Cloudlet> received = broker.getCloudletFinishedList();
        received.sort((o1, o2) -> (int)(o1.getExecStartTime() - o2.getExecStartTime()));

        double last_arrival_time = received.stream().mapToDouble(cl -> cl.getFinishTime()).max().getAsDouble();

        // DEBUG INFO
        if(debug_info)
        {
            System.out.println(String.format("Simulation duration %f seconds", (end - start) / 1e9));
            System.out.println(String.format("Simulation internal duration %f seconds", last_arrival_time));

            System.out.println(String.format("Received %d cloudlets", received.size()));
            new CloudletsTableBuilder(cloudlets).build();
            new CloudletsTableBuilder(received).build();
        }

        return last_arrival_time;
    }

    static void sim1(Class brokerClass, List<Long> MipsCapacities,
                     int problem_size, int max_slice_size, int start_slice_size,
                     boolean debug_info)
            throws IOException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        double start = System.nanoTime();
        double results[][] = new double[max_slice_size][];
        for(int i = start_slice_size; i < max_slice_size; ++i)
        {
            double[] results_i = new double[max_slice_size];
            for (int j = start_slice_size; j < max_slice_size; ++j)
            {
                System.out.println(String.format("Processing 1 - %d of %d 2 - %d of %d", i, max_slice_size, j, max_slice_size));
                results_i[j] = simulateProblem(brokerClass, MipsCapacities, problem_size, i, j, debug_info);
            }
            results[i] = results_i;
        }
        double end = System.nanoTime();
        System.out.println("Time for all simulations(in seconds) ");
        System.out.println(Double.toString((end-start)/1e9));

        FileWriter writer = new FileWriter("results.txt", false);
        writer.write("First player,");
        writer.write("Second player,");
        writer.write("Time(in seconds)");
        writer.write('\n');
        for(int i = start_slice_size; i < max_slice_size; ++i) {
            for (int j = start_slice_size; j < max_slice_size; ++j) {
                writer.write(Integer.toString(i));
                writer.write(',');
                writer.write(Integer.toString(j));
                writer.write(',');
                writer.write(Double.toString(results[i][j]));
                writer.write('\n');
            }
        }
        writer.close();
    }



    public static void main(String[] args)
            throws IOException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException
        {
            List<Long> MipsCapacities = new ArrayList<>();
            MipsCapacities.add(TAKEN_NOMINAL_MIPS);
            MipsCapacities.add(TAKEN_NOMINAL_MIPS*2);
            MipsCapacities.add(TAKEN_NOMINAL_MIPS*3);
            MipsCapacities.add(TAKEN_NOMINAL_MIPS*7);

            //Log.disable();
            //sim1(SimpleSchedulers.MaxMaxScheduler.class ,MipsCapacities, 1000, 100, 20, true);
            simulateProblem(SimpleSchedulers.MaxMaxScheduler.class ,MipsCapacities, 1000, 100, 20, true);

            //sim2(SimpleSchedulers.MaxMinScheduler.class, MipsCapacities, 5000, 5, 7);
            //sim2(SimpleSchedulers.MaxMinScheduler.class, MipsCapacities, 5000, 50, 70);
            //sim2(SimpleSchedulers.MaxMinScheduler.class, MipsCapacities, 5000, 200, 300);
            //sim2(SimpleSchedulers.MaxMinScheduler.class, MipsCapacities, 5000, 700, 700);
            //sim2(SimpleSchedulers.MaxMinScheduler.class, MipsCapacities, 5000, 1500, 1500);
            //sim2(SimpleSchedulers.MaxMinScheduler.class, MipsCapacities, 5000, 900, 900);
            //sim2(SimpleSchedulers.MaxMaxScheduler.class, MipsCapacities, 5000, 5000, 5000);
            //sim2(SimpleSchedulers.MaxMaxScheduler.class, MipsCapacities, 10000, 100, 200);
            //sim2(SimpleSchedulers.MaxMaxScheduler.class, MipsCapacities, 1000, 10, 20);
            //sim2(DatacenterBrokerSimple.class, MipsCapacities, 10000, 100, 200);
    }
}
package com.simulations.matrix;

import org.apache.commons.cli.*;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
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
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.concurrent.Executors.newFixedThreadPool;


// Theoretical values:
// machine can do ~ 10000000000 additions per second
// so make it a nominal value

public class MatrixMultiplicationSimulation {
    private static final Map<String, Class> scheduler_mapper = new HashMap<>();
    static
    {
        scheduler_mapper.put("minmin",  SimpleSchedulers.MinMinScheduler.class);
        scheduler_mapper.put("minmax",  SimpleSchedulers.MinMaxScheduler.class);
        scheduler_mapper.put("maxmin",  SimpleSchedulers.MaxMinScheduler.class);
        scheduler_mapper.put("maxmax",  SimpleSchedulers.MaxMaxScheduler.class);
        scheduler_mapper.put("all_mt", null);
    }

    // Nominal MIPS (i7-6700K 1 core)
    private static final long NOMINAL_MIPS = 10000000000L;

    // Matrix multiplication complexity
    private static final double multiplicationComplexityMultiplier = 1.;

    private static final int HOSTS = 1;
    private static final int HOST_PES = 100;
    private static final long HUGE_VALUE = 10000000000000000L;
    private static final long HOST_P_MIPS = NOMINAL_MIPS*1000;

    //members
    private final long problem_size;
    private final long slice1;
    private final long slice2;

    private static class RunResult
    {
        private final double time_first;
        private final double time_second;

        RunResult(double first, double second)
        {
            time_first = first;
            time_second = second;
        }

        double getFirst()
        {
            return time_first;
        }

        double getSecond()
        {
            return time_second;
        }
    }

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

    private static List<Cloudlet> createCloudlets(int userid, long n, long n1, Integer counter)
    {
        if(n1==0)
            return new ArrayList<>();
        long n_slices = n/n1;
        boolean partial_slice = n%n1 != 0;
        long n2 = n - n_slices*n1;

        long n_cloudlets = n_slices*n_slices;
        if(partial_slice)
        {
        	n_cloudlets+= 2*n_slices + 1;
        }

        final List<Cloudlet> list = new ArrayList<>((int)n_cloudlets);
        UtilizationModel utilization = new UtilizationModelFull();

        long full_complexity = (long)(CalculateOverallComplexity(n1,n,n1,multiplicationComplexityMultiplier));
        long p_complexity = (long)(CalculateOverallComplexity(n1,n,n2,multiplicationComplexityMultiplier));
        long pp_complexity = (long)(CalculateOverallComplexity(n2,n,n2,multiplicationComplexityMultiplier));

        for (int i = 0; i < n_slices; i++)
        {
            for (int j = 0; j < n_slices; j++)
            {
                list.add(new CloudletUser(userid, counter++, full_complexity, 1)
                        .setUtilizationModel(utilization));
            }
        }
        if(partial_slice)
        {
            for (int i = 0; i < n_slices; i++)
            {
                list.add(new CloudletUser(userid, counter++, p_complexity, 1)
                        .setUtilizationModel(utilization));
                list.add(new CloudletUser(userid, counter++, p_complexity, 1)
                        .setUtilizationModel(utilization));
            }
            list.add(new CloudletUser(userid, counter++, pp_complexity, 1)
                    .setUtilizationModel(utilization));
        }

        return list;
    }

    public static RunResult simulateProblem(Class brokerClass, List<Long> MIPSCapacities,
                                         long size, long slice1, long slice2, boolean debug_info)
            throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        MatrixMultiplicationSimulation MMS = new MatrixMultiplicationSimulation(size, slice1, slice2);

        double start, end;

        CloudSim simulation = new CloudSim(-1, new GregorianCalendar(), false, 0.0001);

        Datacenter datacenter = createDatacenter(simulation);

        Constructor constructor = brokerClass.getConstructor(CloudSim.class);
        DatacenterBroker broker = (DatacenterBroker)constructor.newInstance(simulation);

        List<Vm> vmList = createVmsWithMIPS(MIPSCapacities);
        broker.submitVmList(vmList);

        start = System.nanoTime();
        List<Cloudlet> cloudlets = new ArrayList<>();
        Integer counter = 0;
        cloudlets.addAll(createCloudlets(0, MMS.problemSize(), MMS.getSlise1(), counter));
        cloudlets.addAll(createCloudlets(1, MMS.problemSize(), MMS.getSlise2(), counter));
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

        if(debug_info)
        {
            System.out.println(String.format("Received %d cloudlets", received.size()));
        }
        if(received.size() != cloudlets.size()) {
            throw new RuntimeException(
                    String.format(
                            "Cloudlet processing corruption.\n" +
                            "Simulation case:\n" +
                            "Scheduler: %s. Problem size: %d. Slice size1: %d. Slice size2: %d.",
                            brokerClass.getName(), MMS.problemSize(), MMS.getSlise1(), MMS.getSlise2()
                    )
            );
        }
        received.sort((o1, o2) -> (Double.compare(o2.getFinishTime(),o1.getFinishTime())));

        double times[] = new double[2];

        CloudletUser last_cloudlet = (CloudletUser)received.get(0);

        int reverse_user = 0;
        if(last_cloudlet.getUser_id()==0)
        {
            reverse_user = 1;
            times[0] = last_cloudlet.getFinishTime();
        }
        else
        {
            reverse_user = 0;
            times[1] = last_cloudlet.getFinishTime();
        }
        for(Cloudlet cl : received)
        {
            CloudletUser cl_user = (CloudletUser)cl;
            if(cl_user.getUser_id() == reverse_user)
            {
                times[reverse_user] = cl.getFinishTime();
                break;
            }
        }


        double last_arrival_time = Math.max(times[0], times[1]);
        double last_arrival_time2 = received.stream().mapToDouble(cl -> cl.getFinishTime()).max().getAsDouble();
        if(last_arrival_time != last_arrival_time2)
            throw new RuntimeException("Arrival time corruption");


        // DEBUG INFO
        if(debug_info)
        {
            System.out.println(String.format("Simulation duration %f seconds", (end - start) / 1e9));
            System.out.println(String.format("Simulation internal duration %f seconds", last_arrival_time));

            System.out.println(String.format("Received %d cloudlets", received.size()));
            System.out.println("received cloudlets table sorted from last received to first");
            new CloudletsTableBuilderExtended(received).build();
        }

        return new RunResult(times[0], times[1]);
    }

    static void simulate(Class brokerClass, List<Long> MipsCapacities,
                         long problem_size, Iterable<Long> slices1, Iterable<Long> slices2,
                         String resulting_filename,
                         boolean debug_info)
    {
        try {
            double start = System.nanoTime();

            FileWriter writer = new FileWriter(resulting_filename, false);
            DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            Date date = new Date();
            writer.write(String.format("Starting time: %s\n", dateFormat.format(date)));
            writer.write(String.format("Using scheduler %s\n", brokerClass.getName()));
            writer.write("First player,");
            writer.write("Second player,");
            writer.write("TimeFirst(in seconds)");
            writer.write("TimeSecond(in seconds)\n");
            writer.flush();

            for (long i : slices1) {
                for (long j : slices2) {
                    RunResult time = simulateProblem(brokerClass, MipsCapacities, problem_size, i, j, debug_info);
                    writer.write(String.format("%d,%d,%f,%f\n", i, j, time.getFirst(), time.getSecond()));
                    writer.flush();
                }
            }

            double end = System.nanoTime();
            System.out.println("Time for all simulations(in seconds) ");
            System.out.println(Double.toString((end - start) / 1e9));

            writer.close();
        }
        catch (Exception e)
        {
            System.out.println(String.format("Exception in simulate: %s.\n Reason: %s. Stack trace: %s", e.getMessage(), e.getCause(), e.getStackTrace()));
        }
    }

    static void simulate_full(Class brokerClass, List<Long> MipsCapacities,
                              long problem_size, long start_slice, long max_slice,
                              String resulting_filename,
                              boolean debug_info)
    {
        List<Long> slices = new ArrayList<>((int)(max_slice-start_slice));
        for(long i = start_slice; i < max_slice; ++i)
            slices.add(i);
        simulate(brokerClass, MipsCapacities, problem_size, slices, slices, resulting_filename, debug_info);
    }

    // maybe do random slice list uniformly distributed?
    static void simulate_urandom(Class brokerClass, List<Long> MipsCapacities,
                                 long problem_size, long start_slice, long max_slice, long count,
                                 String resulting_filename,
                                 boolean debug_info)
    {
        List<Long> slices = new ArrayList<>((int)(max_slice-start_slice));
        for(long i = start_slice; i < max_slice; ++i)
            slices.add(i);
        simulate(brokerClass, MipsCapacities, problem_size, slices, slices, resulting_filename, debug_info);
    }

    static void simulate_step(Class brokerClass, List<Long> MipsCapacities,
                                 long problem_size, long start_slice, long max_slice, long step,
                                 String resulting_filename, boolean single,
                                 boolean debug_info)
    {
        List<Long> slices = new ArrayList<>((int)(max_slice-start_slice));
        for(long i = start_slice; i < max_slice; i+=step)
            slices.add(i);
        List<Long> slices2;
        if(single)
        {
            slices2 = new ArrayList<>();
            slices2.add(0L);
        }
        else
            slices2 = slices;
        simulate(brokerClass, MipsCapacities, problem_size, slices, slices2, resulting_filename, debug_info);
    }

    public static void main(String[] args)
            throws IOException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException
        {
            Options options = new Options();
            options.addOption(
                    Option.builder()
                            .longOpt("debug")
                            .hasArg(false)
                            .required(false)
                            .desc("print debug information")
                    .build()
            );
            options.addOption(
                    Option.builder()
                            .longOpt("single")
                            .hasArg(false)
                            .required(false)
                            .desc("single player")
                            .build()
            );
            options.addOption(
                    Option.builder("o")
                            .longOpt("output")
                            .hasArg(true)
                            .required(false)
                            .desc("output file")
                            .build()
            );
            options.addOption(
                    Option.builder()
                            .longOpt("print_logs")
                            .hasArg(false)
                            .required(false)
                            .desc("enable logging")
                            .build()
            );
            options.addOption(
                    Option.builder("n")
                            .longOpt("problem_size")
                            .hasArg(true)
                            .required(false)
                            .desc("problem size (n,n).dot(n,n)")
                            .build()
            );
            options.addOption(
                    Option.builder()
                            .longOpt("step_size")
                            .hasArg(true)
                            .required(false)
                            .desc("problem size increment stepsize")
                            .build()
            );
            options.addOption(
                    Option.builder()
                            .longOpt("start_slice")
                            .hasArg(true)
                            .required(false)
                            .desc("starting slice size")
                            .build()
            );
            options.addOption(
                    Option.builder()
                            .longOpt("max_slice")
                            .hasArg(true)
                            .required(false)
                            .desc("max slice size")
                            .build()
            );

            options.addOption(
                    Option.builder()
                            .longOpt("scheduler")
                            .hasArg(true)
                            .required(false)
                            .desc("scheduler type: minmin, minmax, maxmin, maxmax")
                            .build()
            );
            options.addOption(
                    Option.builder()
                            .longOpt("mips")
                            .hasArgs()
                            .required()
                            .valueSeparator(',')
                            .desc(String.format("comma separated mips capacities as multiplication of nominal ( %d )", NOMINAL_MIPS))
                    .build()
            );
            options.addOption(
                    Option.builder()
                            .longOpt("mips")
                            .hasArgs()
                            .required()
                            .valueSeparator(',')
                            .desc(String.format("comma separated mips capacities as multiplication of nominal ( %d )", NOMINAL_MIPS))
                            .build()
            );
            CommandLineParser parser = new DefaultParser();
            try
            {
                CommandLine line = parser.parse( options, args );

                final boolean print_debug = line.hasOption("debug") ? true : false;
                if(!line.hasOption("print_logs"))
                    Log.disable();
                final String output_filename = line.getOptionValue("output", "results.txt");

                Class brokerClass = null;
                String broker_class_str=line.getOptionValue("scheduler", "minmax");
                brokerClass = scheduler_mapper.getOrDefault(broker_class_str.toLowerCase(),SimpleSchedulers.MaxMinScheduler.class);

                String[] mipss_str = line.getOptionValues("mips");
                List<Long> MipsCapacities = new ArrayList<>();
                for(String mips_str : mipss_str)
                    MipsCapacities.add((long)(NOMINAL_MIPS*Double.parseDouble(mips_str)));

                boolean single = line.hasOption("single");

                final long n = Long.parseLong(line.getOptionValue("problem_size", "1000"));
                final long step = Long.parseLong(line.getOptionValue("step_size", "5"));
                final long start_slice;
                if(line.hasOption("start_slice")){
                    start_slice = Long.parseLong(line.getOptionValue("start_slice"));
                }
                else {
                    start_slice = Math.max(n/200, 50);
                }
                final long max_slice;
                if(line.hasOption("max_slice")){
                    max_slice = Long.parseLong(line.getOptionValue("max_slice"));
                }
                else {
                    max_slice = 3*n/4;
                }

                switch (broker_class_str)
                {
                    case "all_mt": {
                        ExecutorService es = newFixedThreadPool(4);
                        int counter = 1;
                        for (Class current_scheduler : scheduler_mapper.values()) {
                            String filename = String.format("results%d.txt", counter++);
                            if(current_scheduler!=null)
                                es.execute(() -> simulate_step(current_scheduler, MipsCapacities, n, start_slice, max_slice, step, filename, single, print_debug));
                        }
                        es.shutdown();
                        try {
                            es.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case "all_st": {
                        int counter = 1;
                        for (Class current_scheduler : scheduler_mapper.values()) {
                            String filename = String.format("results%d.txt", counter++);
                            if(current_scheduler!=null)
                                simulate_step(current_scheduler, MipsCapacities, n, start_slice, max_slice, step, filename, single, print_debug);
                        }
                        break;
                    }
                    default:
                        simulate_step(brokerClass, MipsCapacities, n, start_slice, max_slice, step, output_filename, single, print_debug);

                }
            }
            catch( ParseException exp )
            {
                System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "matrixsim", options );
            }
    }
}
package com.simulations.matrix;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.TableBuilder;

import java.util.List;

public class CloudletsTableBuilderExtended extends CloudletsTableBuilder
{
    public CloudletsTableBuilderExtended(final List<? extends Cloudlet> list){
        super(list);

        TableBuilder table = super.getTable();

        super.addColumn(table.addColumn("User", "ID"),
                cloudlet -> ((CloudletUser)cloudlet).getUser_id()
        );
    }
}

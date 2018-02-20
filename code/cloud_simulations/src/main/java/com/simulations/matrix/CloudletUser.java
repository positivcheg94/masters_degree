package com.simulations.matrix;

import org.cloudbus.cloudsim.cloudlets.CloudletSimple;

public class CloudletUser extends CloudletSimple
{
    private final int user_id;

    CloudletUser(final int userid, final long cloudletLength, final int pesNumber)
    {
        super(cloudletLength, pesNumber);
        user_id = userid;

    }

    CloudletUser(final int userid, final int id,  final long cloudletLength,  final long pesNumber)
    {
        super(id, cloudletLength, pesNumber);
        user_id = userid;

    }

    public int getUser_id()
    {
        return user_id;
    }
}

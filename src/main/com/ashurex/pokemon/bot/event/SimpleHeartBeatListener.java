package com.ashurex.pokemon.bot.event;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class SimpleHeartBeatListener implements HeartBeatListener
{
    private int heartBeatCount = 0;
    private List<BotActivity> activities = new ArrayList<>();
    private final int HEARTBEAT_PACE;

    public SimpleHeartBeatListener(int pace)
    {
        this.HEARTBEAT_PACE = pace;
    }

    @Override
    public void heartBeat()
    {
        int count = incrementHeartBeat();
        if (count % HEARTBEAT_PACE == 0)
        {
            System.out.println(StringUtils.repeat("♥", " ", count));
            getHeartbeatActivities().forEach(BotActivity::performActivity);
            setHeartBeatCount(1);
        }
    }

    @Override
    public synchronized int incrementHeartBeat()
    {
        return this.heartBeatCount++;
    }

    @Override
    public int getHeartBeatCount()
    {
        return heartBeatCount;
    }

    @Override
    public synchronized void setHeartBeatCount(int count)
    {
        this.heartBeatCount = count;
    }

    @Override
    public synchronized void addHeartBeatActivity(BotActivity activity)
    {
        this.activities.add(activity);
    }

    @Override
    public List<BotActivity> getHeartbeatActivities()
    {
        return activities;
    }
}

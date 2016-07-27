package com.ashurex.pokemon.bot.listener;
import com.ashurex.pokemon.bot.activity.BotActivity;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class SimpleHeartBeatListener implements HeartBeatListener
{
    private final AtomicInteger heartBeatCount = new AtomicInteger(0);
    private List<BotActivity> activities = new ArrayList<>();
    private final int HEARTBEAT_PACE;
    private final AtomicLong lastPulse = new AtomicLong(0);

    public SimpleHeartBeatListener(int pace)
    {
        this.HEARTBEAT_PACE = pace;
    }

    @Override
    public synchronized void heartBeat()
    {
        // Protecting from pulsing too often.
        if (shouldPulse() && (incrementHeartBeat() % HEARTBEAT_PACE == 0))
        {
            updateLastPulse();
            setHeartBeatCount(1);
            // TODO: Logging/System.out
            System.out.println(StringUtils.repeat("♥", " ", HEARTBEAT_PACE));
            getHeartbeatActivities().forEach(BotActivity::performActivity);
        }
    }

    public synchronized boolean shouldPulse()
    {
        long diff = System.currentTimeMillis() - getLastPulse();
        return diff > 1000;
    }

    public synchronized long getLastPulse()
    {
        return lastPulse.get();
    }

    public synchronized void updateLastPulse()
    {
        lastPulse.set(System.currentTimeMillis());
    }

    @Override
    public synchronized int incrementHeartBeat()
    {
        return heartBeatCount.getAndIncrement();
    }

    @Override
    public synchronized int getHeartBeatCount()
    {
        return heartBeatCount.get();
    }

    @Override
    public synchronized void setHeartBeatCount(int count)
    {
        heartBeatCount.set(count);
    }

    @Override
    public synchronized void addHeartBeatActivity(BotActivity activity)
    {
        this.activities.add(activity);
    }

    @Override
    public synchronized List<BotActivity> getHeartbeatActivities()
    {
        return activities;
    }
}

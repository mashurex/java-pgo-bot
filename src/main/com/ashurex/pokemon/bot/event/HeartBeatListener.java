package com.ashurex.pokemon.bot.event;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public interface HeartBeatListener
{
    void heartBeat();
    int incrementHeartBeat();
    int getHeartBeatCount();
    void setHeartBeatCount(int count);
    void addHeartBeatActivity(BotActivity activity);
    List<BotActivity> getHeartbeatActivities();
}

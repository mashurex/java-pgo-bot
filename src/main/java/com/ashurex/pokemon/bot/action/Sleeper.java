package com.ashurex.pokemon.bot.action;
/**
 * Author: Mustafa Ashurex
 * Created: 7/29/16
 */
public class Sleeper
{
    /**
     * Sleeps for a random amount of time, between 1-3s.
     * @return {@code false} if the thread was interrupted.
     */
    public static boolean longSleep()
    {
        return sleep(new Double((Math.random() * 2000)).intValue() + 1000);
    }

    /**
     * Sleeps for a random amount of time from 500ms - 1500ms
     * @return {@code false} if the thread was interrupted.
     */
    public static boolean sleep()
    {
        return sleep(new Double((Math.random() * 1000)).intValue() + 500);
    }

    /**
     * Sleeps for a random amount of time up to 100ms - 350ms
     * @return {@code false} if the thread was interrupted.
     */
    public static boolean shortSleep()
    {
        return sleep(new Double((Math.random() * 250)).intValue() + 100);
    }

    /**
     * Sleeps for the amount of time provided.
     * @param wait The number of milliseconds to sleep for.
     * @return {@code false} if the thread was interrupted.
     */
    public static boolean sleep(long wait)
    {
        try
        {
            Thread.sleep(wait);
            return true;
        }
        catch (InterruptedException ignore)
        {
            return false;
        }
    }
}

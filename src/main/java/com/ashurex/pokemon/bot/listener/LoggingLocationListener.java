package com.ashurex.pokemon.bot.listener;
import com.ashurex.pokemon.bot.PokemonBot;
import com.ashurex.pokemon.logging.LocationLogger;
import com.ashurex.pokemon.logging.SimpleLocationLogger;
import com.google.maps.model.LatLng;

import java.io.File;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class LoggingLocationListener implements LocationListener, AutoCloseable
{
    private final PokemonBot bot;
    private LocationLogger locationLogger;
    private boolean isShutdown = false;

    public LoggingLocationListener(final PokemonBot bot)
    {
        this.bot = bot;
        try
        {
            this.locationLogger = new SimpleLocationLogger(new File("./coords.txt"));
        }
        catch(Exception ex)
        {
            this.locationLogger = null;
        }
    }

    @Override
    public synchronized void updateCurrentLocation(LatLng point, double altitude)
    {
        if(isShutdown){ throw new RuntimeException("Listener already closed!"); }

        logLocation(point);
        getBot().setCurrentLocation(point, altitude);
    }

    protected final synchronized PokemonBot getBot()
    {
        return bot;
    }

    protected final synchronized void logLocation(LatLng loc)
    {
        try
        {
            if (locationLogger != null) { locationLogger.write(loc); }
        }
        catch (Exception ex)
        {
            System.err.println("Error writing location to log.");
            ex.printStackTrace();
        }
    }

    @Override
    public synchronized void close() throws Exception
    {
        this.isShutdown = true;
        this.locationLogger.close();
    }
}

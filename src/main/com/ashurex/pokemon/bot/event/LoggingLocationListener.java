package com.ashurex.pokemon.bot.event;
import com.ashurex.pokemon.PokemonBot;
import com.ashurex.pokemon.logging.LocationLogger;
import com.ashurex.pokemon.logging.SimpleLocationLogger;
import com.google.maps.model.LatLng;

import java.io.File;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class LoggingLocationListener implements LocationListener
{
    private final PokemonBot bot;
    private LocationLogger locationLogger;

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
        this.bot.setCurrentLocation(point, altitude);
        logLocation(point);
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
}

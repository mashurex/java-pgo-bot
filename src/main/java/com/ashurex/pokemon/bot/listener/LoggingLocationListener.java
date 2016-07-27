package com.ashurex.pokemon.bot.listener;
import com.ashurex.pokemon.bot.PokemonBot;
import com.ashurex.pokemon.logging.LocationLogger;
import com.ashurex.pokemon.logging.SimpleLocationLogger;
import com.google.maps.model.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class LoggingLocationListener implements LocationListener, AutoCloseable
{
    private final PokemonBot bot;
    private LocationLogger locationLogger;
    private boolean isShutdown = false;
    private final static Logger LOG = LoggerFactory.getLogger(LoggingLocationListener.class);

    public LoggingLocationListener(final PokemonBot bot)
    {
        this.bot = bot;
        try
        {
            this.locationLogger = new SimpleLocationLogger();
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
            LOG.error("Error writing location to log.");
            LOG.error(ex.getMessage());
        }
    }

    @Override
    public synchronized void close() throws Exception
    {
        this.isShutdown = true;
        this.locationLogger.close();
    }
}

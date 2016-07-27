package com.ashurex.pokemon.logging;
import com.google.maps.model.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Author: Mustafa Ashurex
 * Created: 7/25/16
 */
public class SimpleLocationLogger implements LocationLogger
{
    private static final Logger LOG = LoggerFactory.getLogger("Coordinates");

    public SimpleLocationLogger() throws IOException
    {

    }

    @Override
    public synchronized void write(LatLng point)
    {
        if(point == null){ return; }
        LOG.error(point.toString());
    }

    @Override
    public void close() throws Exception
    {

    }
}

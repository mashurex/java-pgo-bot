package com.ashurex.pokemon.logging;
import com.google.maps.model.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Author: Mustafa Ashurex
 * Created: 7/25/16
 */
public class SimpleLocationLogger implements LocationLogger
{
    // private final Writer writer;
    private static final Logger LOG = LoggerFactory.getLogger("Coordinates");

    public SimpleLocationLogger(File logFile) throws IOException
    {
        // this.writer = new FileWriter(logFile);
    }

    public SimpleLocationLogger() throws IOException
    {
        // this.writer = new FileWriter(new File("./output-points.log"));
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

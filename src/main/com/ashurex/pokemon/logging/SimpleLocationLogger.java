package com.ashurex.pokemon.logging;
import com.google.maps.model.LatLng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Author: Mustafa Ashurex
 * Created: 7/25/16
 */
public class SimpleLocationLogger implements LocationLogger
{
    private final Writer writer;

    public SimpleLocationLogger(File logFile) throws IOException
    {
        this.writer = new FileWriter(logFile);
    }

    public SimpleLocationLogger() throws IOException
    {
        this.writer = new FileWriter(new File("./output-points.log"));
    }

    @Override
    public void write(LatLng point)
    {
        try
        {
            this.writer.append(String.format("%f,%f\n", point.lat, point.lng));
            this.writer.flush();
        }
        catch (IOException io)
        {
            io.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception
    {
        this.writer.flush();
        this.writer.close();
    }
}

package com.ashurex.pokemon;
import com.ashurex.pokemon.location.LocationUtil;
import com.google.maps.model.LatLng;
import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/25/16
 */
public class BotStep
{
    private final LatLng start;
    private final LatLng end;
    private final double distanceMeters;
    private final Point destination;
    private final Point origin;
    private final double bearing;

    public BotStep(LatLng start, LatLng end, double distance)
    {
        this.start = start;
        this.end = end;
        this.distanceMeters = distance;
        this.destination = LocationUtil.convert(end);
        this.origin = LocationUtil.convert(start);
        this.bearing = LocationUtil.getBearing(origin, destination);
    }

    public static double getStepTicks(double totalDistance, double stepSize)
    {
        return totalDistance / stepSize;
    }

    public List<LatLng> getLatLngTicks(double stepSize)
    {
        Double ticks = getStepTicks(getDistanceMeters(), stepSize);
        List<LatLng> steps = new ArrayList<>(ticks.intValue());
        LatLng prv = getStart();
        steps.add(prv);

        for(int i = 0; i < ticks.intValue(); i++)
        {
            LatLng next = nextStep(prv, getEnd(), stepSize);
            steps.add(next);
            prv = next;
        }

        steps.add(getEnd());
        return steps;
    }

    /**
     *
     * @param origin The current position.
     * @param destination The final destination, used to determine bearing.
     * @param stepSize Distance the next step should be (in meters).
     * @return
     */
    public static LatLng nextStep(LatLng origin, LatLng destination, double stepSize)
    {
        Point current = LocationUtil.convert(origin);
        Point end = LocationUtil.convert(destination);
        double bearing = LocationUtil.getBearing(current, end);

        Point nextPoint = EarthCalc.pointRadialDistance(current, bearing, stepSize);
        return new LatLng(nextPoint.getLatitude(), nextPoint.getLongitude());
    }

    public LatLng getStart()
    {
        return start;
    }

    public LatLng getEnd()
    {
        return end;
    }

    public double getDistanceMeters()
    {
        return distanceMeters;
    }

    public Point getDestination()
    {
        return destination;
    }

    public Point getOrigin()
    {
        return origin;
    }

    public double getBearing()
    {
        return bearing;
    }
}

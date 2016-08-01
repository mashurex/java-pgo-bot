package com.ashurex.pokemon.location;
import com.google.maps.model.LatLng;
import com.grum.geocalc.DegreeCoordinate;
import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;
import com.pokegoapi.api.gym.Gym;

/**
 * Author: Mustafa Ashurex
 * Created: 7/25/16
 */
public final class LocationUtil
{
    /**
     * Converts Google Maps {@link LatLng} to GeoCalc {@link Point}.
     *
     * @param in
     * @return new {@code Point} with {@link DegreeCoordinate} from the provided {@code LatLng}.
     */
    public static Point convert(LatLng in)
    {
        return new Point(new DegreeCoordinate(in.lat), new DegreeCoordinate(in.lng));
    }

    /**
     * Returns the distance (in meters) from start to end.
     *
     * @param start Starting point to measure distance from.
     * @param end End point to measure distance to.
     *
     * @return The distance (in meters) using Vincenty distance formula.
     */
    public static double getDistance(LatLng start, LatLng end)
    {
        Point a = convert(start);
        Point b = convert(end);
        return EarthCalc.getVincentyDistance(a, b);
    }

    /**
     * Returns the (azimuth) bearing, in decimal degrees, from origin to destination.
     *
     * @param origin
     * @param dest
     *
     * @return
     */
    public static double getBearing(Point origin, Point dest)
    {
        return EarthCalc.getVincentyBearing(origin, dest);
    }

    /**
     * Returns the (azimuth) bearing, in decimal degrees, from origin to destination.
     *
     * @param origin
     * @param dest
     * @return
     */
    public static double getBearing(LatLng origin, LatLng dest)
    {
        return getBearing(convert(origin), convert(dest));
    }

    /**
     * Compares the distance from {@code loc} to a pair of Gyms.
     *
     * @param loc
     * @param a
     * @param b
     * @return
     */
    public static int compare(LatLng loc, Gym a, Gym b)
    {
        double distanceA = getDistance(loc, new LatLng(a.getLatitude(), a.getLongitude()));
        double distanceB = getDistance(loc, new LatLng(b.getLatitude(), b.getLongitude()));

        return Double.compare(distanceA, distanceB);
    }
}

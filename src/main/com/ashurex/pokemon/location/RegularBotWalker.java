package com.ashurex.pokemon.location;
import com.ashurex.pokemon.BotStep;
import com.ashurex.pokemon.PokemonBot;
import com.ashurex.pokemon.logging.LocationLogger;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class RegularBotWalker implements BotWalker
{
    private static final double MAX_WALKING_SPEED = 2.1;
    private final GeoApiContext geoApiContext;
    private final boolean USE_WALKING_SPEED = false;
    private final LocationLogger locationLogger;
    private final PokemonBot parentBot;

    private LatLng currentLocation;
    private long lastLocationMs = 0;
    private double lastAltitude = 2;

    // TODO: Remove parent bot requirement and use events
    // TODO: Manage map API cache updates

    public RegularBotWalker(PokemonBot parentBot, GeoApiContext geoApiContext, LocationLogger locationLogger)
    {
        this.geoApiContext = geoApiContext;
        this.locationLogger = locationLogger;
        this.parentBot = parentBot;
    }


    /**
     * Walks the bot from origin to destination, doing various tasks along the way.
     *
     * @param stepSize The size of each step, in meters.
     * @param origin The point to walk from.
     * @param destination The point to walk to.
     * @param wander If true will loot nearby Pokestops and catch nearby Pokemon en route to the destination.
     */
    public synchronized void walkTo(double stepSize, final LatLng origin, final LatLng destination, final boolean wander)
    {
        LatLng[] steps = getStepsToDestination(origin, destination, stepSize);
        if (steps == null)
        {
            System.err.println("No steps returned!");
            setCurrentLocation(destination);
            return;
        }
        else { System.out.println(steps.length + " steps to destination."); }

        for (LatLng step : steps)
        {
//            if (USE_WALKING_SPEED)
//            {
//                LatLng cur = getCurrentLocation();
//                double distance = LocationUtil.getDistance(cur, step);
//                if(Double.compare(distance, MAX_WALKING_SPEED) > 0)
//                {
//                    try
//                    {
//                        long timeout = getTimeoutForDistance(distance);
//                        if(timeout > 0)
//                        {
//                            System.out.println("Slowing for " + timeout + "ms");
//                            Thread.sleep(timeout);
//                        }
//                    }
//                    catch (InterruptedException ex)
//                    {
//                        System.err.println("Slowdown interrupted..." + ex.getMessage());
//                    }
//                }
//            }

            double speed = setCurrentLocation(step);

            parentBot.catchNearbyPokemon();
            parentBot.lootNearbyPokestops(false);
            parentBot.heartBeat();

            if (USE_WALKING_SPEED
                && !Double.isNaN(speed) && !Double.isInfinite(speed)
                && (Double.compare(speed, MAX_WALKING_SPEED) > 0))
            {
                System.out.println(String.format("Walking too fast (%2.2f m/s), slowing down.", speed));
                longSleep();
            }
        }
    }

    public double getSmallRandom()
    {
        return Math.random() * 0.0001 - 0.00005;
    }

    /**
     * Ignores walking speed and moves very fast to the destination.
     *
     * @param origin
     * @param destination
     */
    public synchronized void runTo(final LatLng origin, final LatLng destination)
    {
        double stepSize = 70;
        LatLng[] steps = getStepsToDestination(origin, destination, stepSize);
        if(steps == null)
        {
            System.err.println("Cannot run, no steps returned!");
            setCurrentLocation(destination);
            return;
        }
        else if(steps.length == 1)
        {
            setCurrentLocation(destination);
            parentBot.heartBeat();
            return;
        }


        System.out.println(String.format("Running to [%3.4f,%3.4f] from [%3.4f,%3.4f] in %d steps",
            destination.lat, destination.lng, origin.lat, origin.lng, steps.length));


        LatLng prv = origin;
        for(LatLng step: steps)
        {
            double speed = setCurrentLocation(step);
            printSpeed(speed, prv, step);
            prv = step;
            parentBot.catchNearbyPokemon();
        }

        sleep();
        parentBot.catchNearbyPokemon();
        parentBot.heartBeat();
    }

    /**
     * Calculates a list of {@link LatLng} points to follow in order to get to {@code destination}.
     * @param origin The point to start travelling from.
     * @param destination The point to travel to.
     * @param stepMeters The size, in meters, that each travel step should be.
     * @return All the points to travel on to get to the destination.
     */
    public final LatLng[] getStepsToDestination(final LatLng origin, final LatLng destination, final double stepMeters)
    {
        if(Double.compare(origin.lat, destination.lat) == 0 &&
            Double.compare(origin.lng, destination.lng) == 0)
        {
            return new LatLng[]{ origin };
        }

        try
        {
            final DirectionsResult result = DirectionsApi.newRequest(getGeoApiContext())
                                                         .mode(TravelMode.WALKING)
                                                         .units(Unit.METRIC)
                                                         .origin(origin)
                                                         .destination(destination).await();

            if (result.routes == null || result.routes.length == 0)
            {
                throw new Exception("Could not find any routes to destination!");
            }

            final DirectionsRoute route = result.routes[0];
            final DirectionsLeg leg = route.legs[0];
            List<BotStep> botStepList = new ArrayList<>();

            for (final DirectionsStep s : leg.steps)
            {
                botStepList.add(new BotStep(s.startLocation, s.endLocation, s.distance.inMeters));
            }

            List<LatLng> latLngSteps = new ArrayList<>();
            botStepList.forEach(s ->
            {
                latLngSteps.addAll(s.getLatLngTicks(stepMeters));
            });

            return latLngSteps.toArray(new LatLng[latLngSteps.size()]);
        }
        catch (Exception ex)
        {
            System.err.println("Error retrieving directions from API!: " + ex.getMessage());
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Calculates how long to wait to cover the specified distance to maintain walking speed.
     * @param distance
     * @return
     */
    protected static long getTimeoutForDistance(double distance)
    {
        if(Double.isInfinite(distance) || Double.isNaN(distance) || (Double.compare(distance, 1) < 1)){ return 0; }

        Double ms = ((distance / MAX_WALKING_SPEED) * 1000) + 100;
        return ms.longValue();
    }

    /**
     * Sleeps for a random amount of time, at least 1s.
     * @return {@code false} if the thread was interrupted.
     */
    private boolean longSleep()
    {
        return sleep(new Double((Math.random() * 2000)).intValue() + 1000);
    }

    /**
     * Sleeps for a random amount of time, up to 1s.
     * @return {@code false} if the thread was interrupted.
     */
    private boolean sleep()
    {
        return sleep(new Double((Math.random() * 1000)).intValue());
    }

    /**
     * Sleeps for the amount of time provided.
     * @param wait The number of milliseconds to sleep for.
     * @return {@code false} if the thread was interrupted.
     */
    private boolean sleep(long wait)
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

    public synchronized LatLng getCurrentLocation()
    {
        return currentLocation;
    }

    protected synchronized GeoApiContext getGeoApiContext()
    {
        return geoApiContext;
    }

    /**
     * Set the current location of the user to this {@link LatLng}.
     * @param newLocation The new location to set the user's position to.
     * @return The speed in meters per second the user traveled from the last position.
     */
    public final synchronized double setCurrentLocation(LatLng newLocation)
    {
        try
        {
            double speed = 0;
            boolean doUpdate = true;
            if(currentLocation != null)
            {
                if(Double.compare(newLocation.lat, currentLocation.lat) == 0 &&
                    Double.compare(newLocation.lng, currentLocation.lng) == 0)
                {
                    doUpdate = false;
                }

                speed = getCurrentSpeed(newLocation);
                printSpeed(speed, getCurrentLocation(), newLocation);
            }

            if(doUpdate)
            {
                newLocation.lat += getSmallRandom();
                newLocation.lng += getSmallRandom();

                parentBot.setLocation(newLocation, getAltitude(newLocation) + getSmallRandom());
                logLocation(newLocation);
            }

            setLastLocationMs(System.currentTimeMillis());
            this.currentLocation = newLocation;

            return speed;
        }
        catch (Exception ex)
        {
            System.err.println("Could not set location via API!");
            ex.printStackTrace();
        }

        return 0;
    }

    /**
     * Logs the {@link LatLng} to the {@link LocationLogger} instance.
     * @param loc
     */
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


    public final synchronized long getLastLocationMs() { return lastLocationMs; }

    protected final synchronized void setLastLocationMs(long ms) { this.lastLocationMs = ms; }

    /**
     * Returns the current speed in meters per second from the user's current location to the parameter.
     * @param newLocation The new location to set the user's location to.
     * @return The travel speed in meters per second.
     */
    protected double getCurrentSpeed(LatLng newLocation)
    {
        long lastMs = getLastLocationMs();
        if (lastMs > 0)
        {
            LatLng currentLocation = getCurrentLocation();
            long current = System.currentTimeMillis();
            Double distance = LocationUtil.getDistance(currentLocation, newLocation);

            if(distance.isInfinite() || distance.isNaN() || Double.compare(distance, 1) < 0){ return 0; }

            return getSpeed(distance, (current - lastMs));
        }

        return 0;
    }

    /**
     * Calculate speed from distance and time.
     *
     * @param distance
     * @param ms
     * @return
     */
    public static Double getSpeed(double distance, long ms)
    {
        Double val = distance / (ms / 1000);
        if(val.isInfinite() || val.isNaN()){ return (double)0; }

        return val;
    }

    public static void printSpeed(double speed, LatLng start, LatLng end)
    {
        if((!Double.isInfinite(speed) && !Double.isNaN(speed) && speed > 0.01))
        {
            System.out.println(String.format("Traveling %2.2fm at %3.2f m/s from [%3.9f,%3.9f] to [%3.9f,%3.9f]",
                speed, LocationUtil.getDistance(start, end), start.lat, start.lng, end.lat, end.lng));
        }
    }

    /**
     * Returns a pseudorandom altitude generally close to the last altitude that was given.
     *
     * @param latLng In the future could be used to get real altitude.
     * @return Altitude in meters.
     */
    public synchronized double getAltitude(LatLng latLng)
    {
        // TODO: Find real altitude?
        Double alt = this.lastAltitude;
        Random random = new Random(System.currentTimeMillis() + latLng.hashCode());
        final Double max = alt + 2;
        final Double min = alt > 2 ? alt - 2 : 0;

        for(int i = 0; i < random.nextInt(3); i++)
        {
            alt += random.nextDouble();
            alt -= random.nextDouble();
        }

        if(alt < min){ alt = min; }
        else if(alt > max){ alt = max; }

        this.lastAltitude = alt;

        return alt;
    }
}

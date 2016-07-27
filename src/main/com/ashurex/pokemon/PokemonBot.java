package com.ashurex.pokemon;
import POGOProtos.Data.Player.PlayerStatsOuterClass.PlayerStats;
import POGOProtos.Enums.PokemonFamilyIdOuterClass.PokemonFamilyId;
import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Map.Fort.FortDataOuterClass.FortData;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;
import com.ashurex.pokemon.bot.action.PokemonCatcher;
import com.ashurex.pokemon.bot.event.*;
import com.ashurex.pokemon.location.BotWalker;
import com.ashurex.pokemon.location.LocationUtil;
import com.ashurex.pokemon.location.RegularBotWalker;
import com.google.maps.GeoApiContext;
import com.google.maps.model.LatLng;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.CandyJar;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.map.Map;
import com.pokegoapi.api.map.fort.FortDetails;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.EvolutionResult;
import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.HatchedEgg;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Author: Mustafa Ashurex
 * Created: 7/25/16
 */
public class PokemonBot
{
    private static final double MAX_WALKING_SPEED = 2.1;
    private static final int POKESTOP_RADIUS = 1000;

    private final LatLng START_LOCATION;
    private final PokemonGo api;
    private final OkHttpClient httpClient;
    private final long START_EXPERIENCE;

    private final int HEARTBEAT_PACE;
    private final boolean DO_EVOLUTIONS;
    private final int MIN_CP_THRESHOLD;
    private final boolean DO_TRANSFERS;
    private final BotWalker botWalker;

    private double stepMeters = 4;
    private int heartBeatCount = 0;


    private OpStatus currentOperation = OpStatus.DUH;
    private OpStatus lastOperation = OpStatus.DUH;
    private final long SPAWN_TIME_MS;

    public enum OpStatus {
        DUH,
        WALKING,
        RUNNING,
        LOOTING,
        CATCHING
    }

    public PokemonBot(LatLng origin, BotOptions options)
    throws RemoteServerException, LoginFailedException, IOException
    {

        this.START_LOCATION = origin;
        this.httpClient = new OkHttpClient();
        this.api = new PokemonGo(options.getCredentialProvider(this.httpClient), this.httpClient);

        this.setStepMeters(options.getStepMeters());

        this.MIN_CP_THRESHOLD = options.getMinCpThreshold();
        this.DO_EVOLUTIONS = options.isDoEvolutions();
        this.DO_TRANSFERS = options.isDoTransfers();
        this.HEARTBEAT_PACE = options.getHeartBeatPace();
        this.START_EXPERIENCE = getCurrentExperience();
        this.SPAWN_TIME_MS = System.currentTimeMillis();

        // TODO: Move this outside the constructor
        CatchNearbyPokemonActivity catchNearbyPokemonActivity = new CatchNearbyPokemonActivity(this);
        TransferPokemonActivity transferPokemonActivity = new TransferPokemonActivity(this, MIN_CP_THRESHOLD);

        LocationListener locationListener = new LoggingLocationListener(this);

        SimpleHeartBeatListener heartBeatListener = new SimpleHeartBeatListener(HEARTBEAT_PACE);
        heartBeatListener.addHeartBeatActivity(catchNearbyPokemonActivity);
        heartBeatListener.addHeartBeatActivity(transferPokemonActivity);

        this.setCurrentLocation(origin, 0);
        this.botWalker = new RegularBotWalker(origin, locationListener, heartBeatListener, buildGeoApiContext());
    }

    /**
     * Travel to the destination and encounter the specified Pokemon, then travel back to the origin and finish the catch.
     *
     * @param origin
     * @param destination
     * @param pokemonId
     * @return
     */
    public List<CatchResult> snipe(LatLng origin, LatLng destination, PokemonIdOuterClass.PokemonId pokemonId)
    {
        // TODO: Clean up this code

        setCurrentLocation(destination, 0);
        Map map = getMap();
        map.clearCache();
        map.setUseCache(false);

        try
        {
            List<CatchablePokemon> pokemons = map.getCatchablePokemon();
            List<CatchablePokemon> filtered;
            if (pokemonId != null)
            {
                filtered = pokemons.stream()
                                   .filter(p -> p.getPokemonId().equals(pokemonId))
                                   .collect(Collectors.toList());
            }
            else { filtered = pokemons; }

            if(filtered.size() < 1 && pokemonId != null)
            {
                System.out.println("Couldn't find" + pokemonId + ", will try again");
                longSleep();
                filtered = map.getCatchablePokemon().stream()
                              .filter(p -> p.getPokemonId().equals(pokemonId))
                              .collect(Collectors.toList());
            }

            if(filtered.size() < 1){ return new ArrayList<>(); }

            List<CatchResult> catchResults = new ArrayList<>();
            for (CatchablePokemon p : filtered)
            {
                setCurrentLocation(destination, 0);
                p.encounterPokemon();
                System.out.println("Warping back to origin...");
                setCurrentLocation(origin, 0);
                CatchResult r = p.catchPokemon();
                if(!r.isFailed())
                {
                    System.out.println("Caught " + p.getPokemonId());
                    catchResults.add(r);
                }
                else { System.err.println("Couldn't catch " + p.getPokemonId() + ": " + r.getStatus()); }
            }

            map.setUseCache(true);
            map.clearCache();

            return catchResults;
        }
        catch(Exception ex)
        {
            System.err.println("Error trying to snipe!");
            ex.printStackTrace();
        }

        return new ArrayList<>();
    }

    /**
     * Attempts to fix a softban by spinning a close Pokestop repeatedly.
     *
     * @param destination
     * @return {@code true} if the ban was lifted.
     */
    public final boolean fixSoftBan(LatLng destination)
    {
        setCurrentLocation(destination, 0);
        Optional<Pokestop> nearest = getNearestPokestop();
        if(!nearest.isPresent())
        {
            System.err.println("Cannot find a nearby Pokestop!");
            return false;
        }

        Pokestop pokestop = nearest.get();

        try
        {
            long lon = Double.valueOf(pokestop.getLongitude()).longValue();
            long lat = Double.valueOf(pokestop.getLatitude()).longValue();

            Map map = getApi().getMap();
            map.setUseCache(false);
            map.clearCache();

            for(int i = 0; i < 80; i++)
            {
                FortDetails d = map.getFortDetails(pokestop.getId(), lon, lat);

                if(d != null)
                {
                    System.out.println(String.format("Attempting to spin on %s at [%3.5f,%3.5f]",
                        d.getName(), pokestop.getLatitude(), pokestop.getLongitude()));
                } else { System.err.println("Get fort details didn't work!"); }

                PokestopLootResult r = pokestop.loot();
                if(r.wasSuccessful() && r.getItemsAwarded().size() > 0)
                {
                    System.out.println(i + ": " + r.getItemsAwarded().size() + " items awarded for " + r.getExperience() + " exp");
                    return true;
                }
                else
                {
                    System.out.println(i + ": was not successful");
                }
                sleep();
            }

            PokestopLootResult finalTry = pokestop.loot();
            return finalTry.wasSuccessful();
        }
        catch(LoginFailedException | RemoteServerException ex)
        {
            System.err.println("Error spinning Pokestop: " + ex.getMessage());
            ex.printStackTrace();
        }

        return false;
    }

    /**
     *
     * @return The nearest Pokestop that can be looted.
     */
    public final Optional<Pokestop> getNearestPokestop()
    {
        List<Pokestop> pokestops = getNearbyPokestops();
        return pokestops.stream().filter(Pokestop::canLoot).findFirst();
    }

    public final long getCurrentExperience()
    {
        try
        {
            PlayerStats stats = getApi().getPlayerProfile().getStats();
            return stats.getExperience();
        }
        catch(Exception ex)
        {
            System.err.println("Couldn't get experience due to error.");
            ex.printStackTrace();
        }
        return -1;
    }

    protected final String getRuntime()
    {
        final long diffMs = System.currentTimeMillis() - SPAWN_TIME_MS;
        return new SimpleDateFormat("mm:ss:SSS").format(new Date(diffMs));
    }

    protected final GeoApiContext buildGeoApiContext()
    {
        return new GeoApiContext()
            // TODO: Get from configuration
            .setApiKey("AIzaSyCy9KVAL1d6LFw1ZiXZVGg6b2df6MJsK90")
            .setQueryRateLimit(10)
            .setConnectTimeout(5, TimeUnit.SECONDS)
            .setReadTimeout(5, TimeUnit.SECONDS)
            .setWriteTimeout(10, TimeUnit.SECONDS);
    }

    /**
     * Have the bot wander around nearby Pokestops for an infinite amount of time.
     */
    public void wander()
    {
        try
        {
            boolean doStop = false;
            while (!doStop)
            {
                List<Pokestop> pokestops = getNearbyPokestops();
                System.out.println("Found " + pokestops.size() + " pokestops nearby");
                if (pokestops.size() < 1) { break; }

                catchNearbyPokemon();
                lootNearbyPokestops(false);
                if(DO_TRANSFERS){ doTransfers(); }
                if(DO_EVOLUTIONS){ doEvolutions(); }

                for (Pokestop p : pokestops)
                {
                    botWalker.walkTo(getStepMeters(), getCurrentLocation(),
                        new LatLng(p.getLatitude(), p.getLongitude()), true);

                    catchNearbyPokemon();
                    lootNearbyPokestops(false);
                    if(DO_TRANSFERS){ doTransfers(); }
                    if(DO_EVOLUTIONS){ doEvolutions(); }
                    try
                    {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException ex)
                    {
                        doStop = true;
                        System.out.println("Stopping wander...");
                    }
                }
            }
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Done wandering.");
    }

    /**
     *
     * @return All nearby Pokestops sorted closest to farthest.
     */
    public List<Pokestop> getNearbyPokestops()
    {
        return getPokestops().stream().filter(a ->
            LocationUtil.getDistance(getCurrentLocation(), new LatLng(a.getLatitude(), a.getLongitude())) <= POKESTOP_RADIUS)
                             .sorted(
                                 (Pokestop a, Pokestop b) ->
                                     Double.compare(
                                         LocationUtil.getDistance(
                                             getCurrentLocation(), new LatLng(a.getLatitude(), a.getLongitude())
                                         ),
                                         LocationUtil.getDistance(
                                             getCurrentLocation(), new LatLng(b.getLatitude(), b.getLongitude())
                                         ))
                                )
                             .collect(Collectors.toList());
    }



    private PlayerStats getStats()
    {
        PlayerProfile profile = getApi().getPlayerProfile();
        PlayerStats stats = profile.getStats();
        long experience = stats.getExperience();
        int level = stats.getLevel();
        int hatched = stats.getEggsHatched();
        int evos = stats.getEvolutions();

        System.out.println(String.format("Level %d (%d) %d evolutions, %d eggs hatched",
            level, experience, evos, hatched));

        if(Long.compare(experience, START_EXPERIENCE) > 0)
        {
            System.out.println("Gained " + (experience - START_EXPERIENCE) + " experience in " + getRuntime());
        }
        return stats;
    }

    /**
     *
     * @return The first available egg from the inventory.
     */
    public EggPokemon getAvailableEgg()
    {
        Set<EggPokemon> eggs = getInventory().getHatchery().getEggs();
        if (eggs.size() > 0)
        {

            Optional<EggPokemon> result = eggs.stream().filter(p -> !p.isIncubate())
                                              .sorted((EggPokemon a, EggPokemon b) ->
                                                  Double.compare(a.getEggKmWalkedTarget(), b.getEggKmWalkedTarget()))
                                              .findFirst();
            if (result.isPresent()) { return result.get(); }
        }
        return null;
    }

    /**
     * Lists out hatched eggs and puts eggs in incubators that are not in use.
     */
    public void manageEggsAndIncubators()
    {
        try
        {
            List<HatchedEgg> hatchedEggs = getInventory().getHatchery().queryHatchedEggs();
            if (hatchedEggs != null && hatchedEggs.size() > 0)
            {
                hatchedEggs.forEach(e ->
                {
                    System.out.println(e.getId() + " egg hatched: " + e.toString());
                });
            }
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        List<EggIncubator> availableIncubators = new ArrayList<>();
        getInventory().getIncubators().forEach(i ->
        {
            if (!i.isInUse())
            {
                EggPokemon egg = getAvailableEgg();
                if (egg != null)
                {
                    try
                    {
                        i.hatchEgg(egg);
                    }
                    catch (Exception ex)
                    {
                        System.err.println("Could not incubate new egg: " + ex.getMessage());
                        ex.printStackTrace();
                        availableIncubators.add(i);
                    }
                }
                else { availableIncubators.add(i); }

            }
            else
            {
                System.out.println(String.format("Incubator %s (%d) %2.1f / %2.1fkm walked",
                    i.getId(), i.getUsesRemaining(), i.getKmWalked(), i.getKmTarget()));
            }
        });

        if (availableIncubators.size() > 0)
        {
            System.out.println(availableIncubators.size() + " incubators available.");
        }
    }

    public Optional<EvolutionResult> evolve(Pokemon pokemon)
    {
        try
        {
            EvolutionResult r = pokemon.evolve();
            if (r.isSuccessful())
            {
                System.out.println(String.format("Evolved [%d] %s to %s",
                    r.getExpAwarded(), pokemon.getPokemonId(), r.getEvolvedPokemon().getPokemonId()));
            }
            return Optional.of(r);
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    private List<ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result> doTransfers()
    {
        TransferPokemonActivity a = new TransferPokemonActivity(this, MIN_CP_THRESHOLD);
        return a.transferPokemon();
    }

    /**
     * Attempt to evolve all applicable Pokemon in the bot's inventory.
     * @return
     */
    private List<EvolutionResult> doEvolutions()
    {
        final Inventories inventories = getApi().getInventories();
        final CandyJar candyJar = inventories.getCandyjar();

        List<Pokemon> pokemons = inventories.getPokebank()
                                            .getPokemons()
                                            .stream()
                                            .sorted((Pokemon a, Pokemon b) ->
                                                Integer.compare(b.getCp(), a.getCp()))
                                            .collect(Collectors.toList());

        List<EvolutionResult> evolutionResults = new ArrayList<>();
        pokemons.forEach(p ->
        {
            boolean doEvolve = false;
            try
            {
                if (!p.getPokemonFamily().equals(PokemonFamilyId.FAMILY_ODDISH))
                {
                    int candies = candyJar.getCandies(p.getPokemonFamily());
                    Integer candiesToEvolve = p.getCandiesToEvolve();
                    doEvolve = (candies >= candiesToEvolve);
                }
            }
            catch (Exception ignore)
            {
                // System.out.println("Error getting evolution data for " + p.getPokemonId());
                // ex.printStackTrace();
            }

            if (doEvolve)
            {
                Optional<EvolutionResult> r = evolve(p);
                if (r.isPresent()) { evolutionResults.add(r.get()); }
            }
        });

        return evolutionResults;
    }

    public Inventories getInventory()
    {
        return getApi().getInventories();
    }

    protected synchronized final OpStatus updateOpStatus(OpStatus status)
    {
        this.lastOperation = this.currentOperation;
        this.currentOperation = status;

//        if(lastOperation != currentOperation)
//            System.out.println("Switching from " + this.lastOperation + " to " + this.currentOperation);

        return this.lastOperation;
    }

    public final Map getMap()
    {
        return getApi().getMap();
    }

    /**
     * Attempts to catch all nearby Pokemon.
     * @return All of the {@link CatchResult}
     */
    public List<CatchResult> catchNearbyPokemon()
    {
        updateOpStatus(OpStatus.CATCHING);
        List<CatchablePokemon> catchablePokemon = getCatchablePokemon();
        if (catchablePokemon.size() == 0) { return new ArrayList<>(); }

        System.out.println("Found " + catchablePokemon.size() + " nearby pokemon to try and catch.");

        return PokemonCatcher.catchPokemon(catchablePokemon);
    }




    public final Collection<Pokestop> getPokestops()
    {
        try
        {
            return getMap().getMapObjects().getPokestops();
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        return new ArrayList<>();
    }

    public Collection<FortData> getGyms()
    {
        try
        {
            return getMap().getMapObjects().getGyms();
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
        return new ArrayList<>();
    }
/*
    public Optional<FortData> goToGym()
    {
        Optional<FortData> r = getGyms().stream().sorted((FortData a, FortData b) -> Double.compare(
            LocationUtil.getDistance(getCurrentLocation(), new LatLng(a.getLatitude(), a.getLongitude())),
            LocationUtil.getDistance(getCurrentLocation(), new LatLng(b.getLatitude(), b.getLongitude()))))
                                        .filter(g -> g.getEnabled() && !g.getIsInBattle()).findFirst();

        if (!r.isPresent())
        {
            System.err.println("Could not find a close gym.");
            return r;
        }

        FortData gym = r.get();
        walkTo(new LatLng(gym.getLatitude(), gym.getLongitude()), false);

        System.out.println(String.format("GYM for %s guarded by %d %s for %d points",
            gym.getOwnedByTeam(), gym.getGuardPokemonCp(),
            gym.getGuardPokemonId(), gym.getGymPoints()));

        return r;
    }
*/

    /**
     *
     * @return All {@link CatchablePokemon} in the bot's vacinity.
     */
    public List<CatchablePokemon> getCatchablePokemon()
    {
        try
        {
            getCurrentLocation();
            return getMap().getCatchablePokemon();
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Loot all nearby Pokestops to the user's current location.
     *
     * @param walkToStops If {@code true} the bot will walk around to various Pokestops it finds along the way.
     * @return All the results from looting the Pokestops.
     */
    public synchronized List<PokestopLootResult> lootNearbyPokestops(boolean walkToStops)
    {
        final List<PokestopLootResult> results = new ArrayList<>();
        final LatLng origin = getCurrentLocation();

        getNearbyPokestops().forEach(p ->
        {
            if (p.canLoot())
            {
                Optional<PokestopLootResult> r = lootPokestop(p);
                if (r.isPresent()) { results.add(r.get()); }
            }
            else if (walkToStops && !p.inRange() && p.canLoot(true))
            {
                System.out.println("Wandering to nearby Pokestop...");
                botWalker.walkTo(getStepMeters(), getCurrentLocation(),
                    new LatLng(p.getLatitude(), p.getLongitude()), false);
                lootPokestop(p);
            }
        });

        botWalker.runTo(getCurrentLocation(), origin);

        return results;
    }

    /**
     * Attempt to loot the provided Pokestop.
     * @param pokestop The Pokestop to loot.
     * @return {@link Optional} will be empty if errors occurred.
     */
    public Optional<PokestopLootResult> lootPokestop(Pokestop pokestop)
    {
        try
        {
            updateOpStatus(OpStatus.LOOTING);
            System.out.println(String.format("Attempting to get loot from Pokestop at [%f,%f]",
                pokestop.getLongitude(), pokestop.getLatitude()));

            PokestopLootResult r = pokestop.loot();

            if (r.wasSuccessful())
            { System.out.println("    » Looted Pokestop"); }
            else
            { System.out.println("    × Could not loot Pokestop."); }

            return Optional.of(r);
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public final LatLng getStartLocation()
    {
        return START_LOCATION;
    }

    public final PokemonGo getApi()
    {
        return api;
    }

    protected final OkHttpClient getHttpClient()
    {
        return httpClient;
    }

    public final double getStepMeters()
    {
        return stepMeters;
    }

    public final synchronized void setStepMeters(double stepMeters)
    {
        this.stepMeters = stepMeters;
    }

    /**
     * Set the current location of the user to this {@link LatLng}.
     * @param newLocation The new location to set the user's position to.
     * @param altitude Altitude in meters at the current location.
     */
    public final synchronized void setCurrentLocation(LatLng newLocation, double altitude)
    {
        getApi().setLocation(newLocation.lat, newLocation.lng, altitude);
    }

    public final synchronized LatLng getCurrentLocation()
    {
        return new LatLng(getApi().getLatitude(), getApi().getLongitude());
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
}

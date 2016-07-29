package com.ashurex.pokemon.bot;
import POGOProtos.Data.Player.PlayerStatsOuterClass.PlayerStats;
import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;
import com.ashurex.pokemon.BotOptions;
import com.ashurex.pokemon.bot.action.*;
import com.ashurex.pokemon.bot.activity.CatchNearbyPokemonActivity;
import com.ashurex.pokemon.bot.activity.TransferPokemonActivity;
import com.ashurex.pokemon.bot.listener.LocationListener;
import com.ashurex.pokemon.bot.listener.LoggingLocationListener;
import com.ashurex.pokemon.bot.listener.SimpleHeartBeatListener;
import com.ashurex.pokemon.location.LocationUtil;
import com.google.maps.GeoApiContext;
import com.google.maps.model.LatLng;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Gym;
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
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.util.SystemTimeImpl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Author: Mustafa Ashurex
 * Created: 7/25/16
 */
public class SimplePokemonBot implements PokemonBot
{
    private static final Logger LOG = LoggerFactory.getLogger(SimplePokemonBot.class);
    private static final int POKESTOP_RADIUS = 1000;

    private final LatLng START_LOCATION;
    private final PokemonGo api;
    private final OkHttpClient httpClient;
    private final long START_EXPERIENCE;
    private final boolean DO_EVOLUTIONS;
    private final int MIN_CP_THRESHOLD;
    private final boolean DO_TRANSFERS;
    private final boolean DEBUG_MODE;
    private final boolean USE_WALKING_SPEED;
    private final BotOptions BOT_OPTIONS;

    private BotWalker botWalker;

    private double stepMeters = 4;
    private OpStatus currentOperation = OpStatus.DUH;
    private OpStatus lastOperation = OpStatus.DUH;
    private final long SPAWN_TIME_MS;
    private int errorSampleCount = 0;
    private long errorSampleStartMs = 0;

    public synchronized void sampleError()
    {
        if(errorSampleStartMs == 0)
        {
            errorSampleStartMs = System.currentTimeMillis();
            errorSampleCount = 1;
        }
        else
        {
            long diff = System.currentTimeMillis() - errorSampleStartMs;
            if(diff > 120000)
            {
                errorSampleCount = 1;
                errorSampleStartMs = System.currentTimeMillis();
            }
            else if(errorSampleCount > 10)
            {
                throw new RuntimeException("Error sample count exceeded bounds, shutting down.");
            }
            else
            {
                errorSampleCount++;
            }
        }
    }

    public SimplePokemonBot(BotOptions options)
    throws RemoteServerException, LoginFailedException, IOException
    {

        this.START_LOCATION = options.getBotOrigin();
        this.httpClient = new OkHttpClient();
        this.api = new PokemonGo(options.getCredentialProvider(this.httpClient), this.httpClient, new SystemTimeImpl());

        this.setStepMeters(options.getStepMeters());

        this.MIN_CP_THRESHOLD = options.getMinCpThreshold();
        this.DO_EVOLUTIONS = options.isDoEvolutions();
        this.DO_TRANSFERS = options.isDoTransfers();
        this.START_EXPERIENCE = getCurrentExperience();
        this.SPAWN_TIME_MS = System.currentTimeMillis();
        this.DEBUG_MODE = options.isDebugMode();
        this.USE_WALKING_SPEED = options.isUseWalkingSpeed();
        this.BOT_OPTIONS = options;

        this.setCurrentLocation(options.getBotOrigin(), 0);
    }

    public static PokemonBot build(BotOptions options)
        throws RemoteServerException, LoginFailedException, IOException
    {
        PokemonBot bot = new SimplePokemonBot(options);

        CatchNearbyPokemonActivity catchNearbyPokemonActivity = new CatchNearbyPokemonActivity(bot);
        TransferPokemonActivity transferPokemonActivity = new TransferPokemonActivity(bot, options.getMinCpThreshold());

        LocationListener locationListener = new LoggingLocationListener(bot);

        SimpleHeartBeatListener heartBeatListener = new SimpleHeartBeatListener(options.getHeartBeatPace());
        // FIXME: Disabling this for throttling testing
        heartBeatListener.addHeartBeatActivity(catchNearbyPokemonActivity);

        if(options.isDoTransfers())
        {
            // FIXME: Disabling this for throttling testing
            heartBeatListener.addHeartBeatActivity(transferPokemonActivity);
        }

        BotWalker walker = new RegularBotWalker(options.getBotOrigin(), locationListener, heartBeatListener,
            buildGeoApiContext(options.getMapsKey()), options);

        walker.addPostStepActivity(catchNearbyPokemonActivity);

        if(options.isDoTransfers())
        {
            walker.addPostStepActivity(transferPokemonActivity);
        }

        bot.setWalker(walker);

        return bot;
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
                LOG.info("Couldn't find " + pokemonId + ", will try again");
                longSleep();
                filtered = map.getCatchablePokemon().stream()
                              .filter(p -> p.getPokemonId().equals(pokemonId))
                              .collect(Collectors.toList());
            }

            if(filtered.size() < 1){ LOG.info("Still couldn't find " + pokemonId + ", giving up..."); return new ArrayList<>(); }

            List<CatchResult> catchResults = new ArrayList<>();
            for (CatchablePokemon p : filtered)
            {
                setCurrentLocation(destination, 0);
                p.encounterPokemon();
                LOG.info("Warping back to origin...");
                setCurrentLocation(origin, 0);
                CatchResult r = p.catchPokemon();
                if(!r.isFailed())
                {
                    LOG.info("Caught " + p.getPokemonId());
                    catchResults.add(r);
                }
                else { LOG.warn("Couldn't catch " + p.getPokemonId() + ": " + r.getStatus()); }
            }

            map.setUseCache(true);
            map.clearCache();

            return catchResults;
        }
        catch(Exception ex)
        {
            LOG.error("Error trying to snipe!");
            LOG.error(ex.getMessage(), ex);
            sampleError();
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
            LOG.warn("Cannot find a nearby Pokestop!");
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
                    LOG.info(String.format("Attempting to spin on %s at [%3.5f,%3.5f]",
                        d.getName(), pokestop.getLatitude(), pokestop.getLongitude()));
                } else { LOG.error("Get fort details didn't work!"); }

                PokestopLootResult r = pokestop.loot();
                if(r.wasSuccessful() && r.getItemsAwarded().size() > 0)
                {
                    LOG.info(i + ": " + r.getItemsAwarded().size() + " items awarded for " + r.getExperience() + " exp");
                    return true;
                }
                else
                {
                    LOG.info(i + ": was not successful");
                }
                sleep();
            }

            PokestopLootResult finalTry = pokestop.loot();
            return finalTry.wasSuccessful();
        }
        catch(LoginFailedException | RemoteServerException ex)
        {
            LOG.error("Error spinning Pokestop: " + ex.getMessage());
            LOG.error(ex.getMessage(), ex);
            sampleError();
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
            LOG.error("Couldn't get experience due to error.");
            LOG.error(ex.getMessage(), ex);
            sampleError();
        }
        return -1;
    }

    protected final String getRuntime()
    {
        final long diffMs = System.currentTimeMillis() - SPAWN_TIME_MS;
        return new SimpleDateFormat("mm:ss:SSS").format(new Date(diffMs));
    }

    protected static GeoApiContext buildGeoApiContext(String apiKey)
    {
        return new GeoApiContext()
            // TODO: Get from configuration
            .setApiKey(apiKey)
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
            printConfiguration();
            getStats();
            boolean doStop = false;
            while (!doStop)
            {
                List<Pokestop> pokestops = getNearbyPokestops();
                LOG.info("Found " + pokestops.size() + " pokestops nearby");
                if (pokestops.size() < 1) { break; }

                catchNearbyPokemon();
                lootNearbyPokestops(false);
                if(DO_TRANSFERS){ doTransfers(); sleep(100 + getRandom().longValue()); }
                if(DO_EVOLUTIONS){ doEvolutions(); sleep(100 + getRandom().longValue()); }

                for (Pokestop p : pokestops)
                {
                    getStats();
                    LOG.info(String.format("Walking to %s %3.2fm away...",
                        p.getDetails().getName(),
                        LocationUtil.getDistance(getCurrentLocation(), new LatLng(p.getLatitude(), p.getLongitude()))));

                    botWalker.walkTo(getStepMeters(), getCurrentLocation(),
                        new LatLng(p.getLatitude(), p.getLongitude()), true);

                    catchNearbyPokemon();
                    lootNearbyPokestops(false);

                    if(DO_TRANSFERS){ doTransfers(); }
                    if(DO_EVOLUTIONS){ doEvolutions(); }

                    try
                    {
                        Thread.sleep(500 + getRandom().longValue());
                    }
                    catch (InterruptedException ex)
                    {
                        LOG.warn("Stopping wander due to interruption...");
                        break;
                    }
                }

                doStop = true;
            }
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

        LOG.info("Done wandering.");
    }

    protected static Double getRandom()
    {
        return Math.random() * 750;
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

        LOG.info(String.format("Level %d (%d) %d evolutions, %d eggs hatched",
            level, experience, evos, hatched));

        if(Long.compare(experience, START_EXPERIENCE) > 0)
        {
            LOG.info("Gained " + (experience - START_EXPERIENCE) + " experience in " + getRuntime());
        }
        return stats;
    }

    /**
     * Lists out hatched eggs and puts eggs in incubators that are not in use.
     */
    public void manageEggsAndIncubators()
    {
        try
        {
            EggManager.queryHatchedEggs(getInventory().getHatchery()).forEach(e ->
            {
                LOG.info(e.getId() + " egg hatched: " + e.toString());
            });

            final List<EggIncubator> filled = EggManager.fillIncubators(getInventory());
            if (filled.size() > 0)
            {
                LOG.info("Filled " + filled.size() + " incubators with eggs.");
            }

            getInventory().getIncubators().stream().filter(EggIncubator::isInUse).forEach(i ->
            {
                LOG.info(String.format("Incubator %s (%d) %2.1f / %2.1fkm walked",
                    i.getId(), i.getUsesRemaining(), i.getKmWalked(), i.getKmTarget()));
            });
        }
        catch(Exception ex)
        {
            LOG.error(ex.getMessage());
            LOG.error(ex.getMessage(), ex);
            sampleError();
        }
    }

    /**
     * TODO: This doesn't really work yet
     */
    public void fightAtNearestGym()
    {
        System.out.println("Attempting to fight at nearest Gym");

        try
        {
            List<Gym> gyms = getGyms().stream()
                                      .filter(g -> {
                                            try {
                                                return g.isAttackable();
                                            } catch (Exception ignore){ return false; }
                                        })
                                      .sorted((Gym a, Gym b) -> {
                                          return LocationUtil.compare(getStartLocation(), a, b);
                                      })
                                      .collect(Collectors.toList());

            if(gyms.size() == 0){ LOG.error("Could not find a nearby gym."); return; }

            for(Gym gym: gyms)
            {
                if(gym.isAttackable())
                {
                    botWalker.runTo(getCurrentLocation(), new LatLng(gym.getLatitude(), gym.getLongitude()));
                    setCurrentLocation(new LatLng(gym.getLatitude(), gym.getLongitude()), 1.1);
                    GymFighter.fight(getApi().getPlayerProfile(), gym, getInventory().getPokebank());
                }
            }
        }
        catch(Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
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

        final List<Pokemon> pokemons = inventories.getPokebank()
                                            .getPokemons()
                                            .stream()
                                            .sorted((Pokemon a, Pokemon b) ->
                                                Integer.compare(b.getCp(), a.getCp()))
                                            .collect(Collectors.toList());

        return PokemonEvolver.evolvePokemon(pokemons, candyJar);
    }

    @Override
    public Inventories getInventory()
    {
        return getApi().getInventories();
    }

    protected synchronized final OpStatus updateOpStatus(OpStatus status)
    {
        this.lastOperation = this.currentOperation;
        this.currentOperation = status;

        if(DEBUG_MODE && lastOperation != currentOperation)
        {
            LOG.trace("Switching from " + this.lastOperation + " to " + this.currentOperation);
        }

        return this.lastOperation;
    }

    @Override
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

        LOG.debug("Found " + catchablePokemon.size() + " nearby pokemon to try and catch.");

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
            LOG.error(ex.getMessage(), ex);
            sampleError();
        }

        return new ArrayList<>();
    }

    public List<Gym> getGyms()
    {
        try
        {
            shortSleep();
            return getMap().getGyms();
        }
        catch (RemoteServerException ex)
        {
            // TODO: Retry/wait hack.
            sampleError();
            try
            {
                longSleep();
                return getMap().getGyms();
            }
            catch(Exception ignore){}
        }
        catch (Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
            sampleError();
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
            LOG.error("Could not find a close gym.");
            return r;
        }

        FortData gym = r.get();
        walkTo(new LatLng(gym.getLatitude(), gym.getLongitude()), false);

        LOG.info(String.format("GYM for %s guarded by %d %s for %d points",
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
            LOG.error(ex.getMessage());
            LOG.error(ex.getMessage(), ex);
            sampleError();
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
        final LatLng origin = getCurrentLocation();

        List<Pokestop> pokestops = getNearbyPokestops();
        final List<PokestopLootResult> results = PokestopLooter.lootPokestops(pokestops);

        if(!walkToStops){ return results; }

        pokestops.stream().filter(p -> p.canLoot(true)).forEach(p ->
        {
            LOG.debug("Wandering to nearby Pokestop...");
            botWalker.walkTo(getStepMeters(), getCurrentLocation(),
                new LatLng(p.getLatitude(), p.getLongitude()), false);
            results.add(lootPokestop(p));
        });

        botWalker.runTo(getCurrentLocation(), origin);

        return results;
    }

    /**
     * Attempt to loot the provided Pokestop.
     * @param pokestop The Pokestop to loot.
     * @return null if errors occurred fetching data
     */
    public PokestopLootResult lootPokestop(Pokestop pokestop)
    {
        updateOpStatus(OpStatus.LOOTING);
        return PokestopLooter.lootPokestop(pokestop);
    }

    public final LatLng getStartLocation()
    {
        return START_LOCATION;
    }

    public synchronized final PokemonGo getApi()
    {
        return api;
    }

    protected synchronized final OkHttpClient getHttpClient()
    {
        return httpClient;
    }

    public synchronized final double getStepMeters()
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
        return Sleeper.longSleep();
    }

    /**
     * Sleeps for a random amount of time, up to 1s.
     * @return {@code false} if the thread was interrupted.
     */
    private boolean sleep()
    {
        return Sleeper.sleep();
    }

    /**
     * Sleeps for the amount of time provided.
     * @param wait The number of milliseconds to sleep for.
     * @return {@code false} if the thread was interrupted.
     */
    private boolean sleep(long wait)
    {
        return Sleeper.sleep(wait);
    }

    private boolean shortSleep()
    {
        return Sleeper.shortSleep();
    }

    public synchronized BotWalker getWalker()
    {
        return botWalker;
    }

    public synchronized void setWalker(BotWalker botWalker)
    {
        this.botWalker = botWalker;
    }

    protected synchronized void printConfiguration()
    {
        if(!DEBUG_MODE){ return; }

        List<String> configs = new ArrayList<>();
        configs.add("Walking Speed: " + (USE_WALKING_SPEED ? "Enabled" : "Disabled"));
        configs.add("Evolutions: " + (DO_EVOLUTIONS ? "Enabled" : "Disabled"));
        configs.add("Transfers: " + (DO_TRANSFERS ? "Enabled" : "Disabled"));
        configs.add("Minimum CP Threshold: " + MIN_CP_THRESHOLD);
        configs.add("Step size: " + getStepMeters());
        configs.add(String.format("Starting Point: [%3.6f,%3.6f]", START_LOCATION.lat, START_LOCATION.lng));

        LOG.info("Startup configuration: ");
        LOG.info("\n\t" + StringUtils.join(configs, "\n\t"));
    }
}

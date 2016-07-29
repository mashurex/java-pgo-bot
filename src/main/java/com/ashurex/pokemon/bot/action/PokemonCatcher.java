package com.ashurex.pokemon.bot.action;
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus;
import POGOProtos.Networking.Responses.EncounterResponseOuterClass.EncounterResponse;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.EncounterResult;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class PokemonCatcher
{
    private static final Logger LOG = LoggerFactory.getLogger(PokemonCatcher.class);

    /**
     * Attempts to catch all the provided {@link CatchablePokemon}.
     *
     * @param pokemonList The Pokemon to try and catch.
     * @return A list of {@link CatchResult} for each Pokemon provided.
     */
    public static List<CatchResult> catchPokemon(List<CatchablePokemon> pokemonList)
    {
        List<CatchResult> results = new ArrayList<>(pokemonList.size());
        pokemonList.forEach(p -> {
            CatchResult r = attemptCatch(p);
            if(!r.isFailed()){ results.add(r); }
        });

        return results;
    }

    public static EncounterResult encounterPokemon(CatchablePokemon pokemon)
    {
        try
        {
            return pokemon.encounterPokemon();
        }
        catch (RemoteServerException ex)
        {
            Sleeper.shortSleep();
            return encounterPokemon(pokemon);
        }
        catch (Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }
        return new EncounterResult(EncounterResponse.getDefaultInstance());
    }

    private static CatchResult retryCatch(final CatchablePokemon pokemon)
    {
        return retryCatch(pokemon, 1);
    }

    private static CatchResult retryCatch(final CatchablePokemon pokemon, int count)
    {
        if(count > 10)
        {
            LOG.error("Exceeded retryCatch attempts, bailing out...");
            return new CatchResult();
        }

        try
        {
            // Try a back off
            Sleeper.sleep(150 + (100 * count));
            return pokemon.catchPokemon();
        }
        catch(RemoteServerException ex)
        {
            return retryCatch(pokemon, count + 1);
        }
        catch(LoginFailedException ex)
        {
            LOG.warn(ex.getMessage(), ex);
            return new CatchResult();
        }
    }

    /**
     * Try to encounter and catch the {@link CatchablePokemon} provided.
     * @param pokemon
     * @return The result will be empty if errors occurred.
     */
    public static CatchResult attemptCatch(CatchablePokemon pokemon)
    {
        EncounterResult encounterResult = encounterPokemon(pokemon);

        if (!encounterResult.wasSuccessful()) { return new CatchResult(); }

        CatchResult catchResult;
        try
        {
            Sleeper.shortSleep();
            catchResult = pokemon.catchPokemon();
        }
        catch(Exception ex)
        {
            catchResult = retryCatch(pokemon);
        }

        if(catchResult == null){ return new CatchResult(); }

        try
        {
            CatchStatus status = catchResult.getStatus();

            while (status == CatchStatus.CATCH_MISSED)
            {
                Sleeper.sleep();
                LOG.debug("Missed at " + pokemon.getPokemonId());
                catchResult = pokemon.catchPokemonWithRazzBerry();
                status = catchResult.getStatus();
            }

            switch (catchResult.getStatus())
            {
                case CATCH_SUCCESS:
                    LOG.info(String.format("Caught %s", pokemon.getPokemonId()));
                    break;
                default:
                    LOG.warn(String.format("%s got away! [%s]", pokemon.getPokemonId(), catchResult.getStatus()));
                    break;
            }

            return catchResult;
        }
        catch (Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }

        return new CatchResult();
    }
}

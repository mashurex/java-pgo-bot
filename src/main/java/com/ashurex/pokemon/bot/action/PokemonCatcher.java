package com.ashurex.pokemon.bot.action;
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus;
import POGOProtos.Networking.Responses.EncounterResponseOuterClass.EncounterResponse;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.EncounterResult;
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

    public static List<CatchResult> catchPokemon(List<CatchablePokemon> pokemonList)
    {
        List<CatchResult> results = new ArrayList<>(pokemonList.size());
        pokemonList.forEach(p -> {
            CatchResult r = attemptCatch(p);
            if(!r.isFailed()){ results.add(r); }
            // TODO: Log each one
        });

        return results;
    }

    public static EncounterResult encounterPokemon(CatchablePokemon pokemon)
    {
        try
        {
            return pokemon.encounterPokemon();
        }
        catch (Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }
        return new EncounterResult(EncounterResponse.getDefaultInstance());
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

        try
        {
            CatchResult catchResult = pokemon.catchPokemonWithRazzBerry();
            CatchStatus status = catchResult.getStatus();

            while (status == CatchStatus.CATCH_MISSED)
            {
                LOG.info("Missed at " + pokemon.getPokemonId());
                catchResult = pokemon.catchPokemonWithRazzBerry();
                status = catchResult.getStatus();
            }

            switch (catchResult.getStatus())
            {
                case CATCH_SUCCESS:
                    LOG.info(String.format("Caught %s", pokemon.getPokemonId()));
                    break;
                default:
                    LOG.info(String.format("%s got away! [%s]", pokemon.getPokemonId(), catchResult.getStatus()));
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

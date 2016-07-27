package com.ashurex.pokemon.bot.action;
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus;
import POGOProtos.Networking.Responses.EncounterResponseOuterClass.EncounterResponse;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.EncounterResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class PokemonCatcher
{
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
            // TODO:
            System.err.println(ex.getMessage());
            ex.printStackTrace();
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
                System.out.println("Missed at " + pokemon.getPokemonId());
                catchResult = pokemon.catchPokemonWithRazzBerry();
                status = catchResult.getStatus();
            }

            switch (catchResult.getStatus())
            {
                case CATCH_SUCCESS:
                    System.out.println(String.format("Caught %s", pokemon.getPokemonId()));
                    break;
                default:
                    System.out.println(String.format("%s got away!", pokemon.getPokemonId()));
                    break;
            }

            return catchResult;
        }
        catch (Exception ex)
        {
            // TODO: Replace w/ logging
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        return new CatchResult();
    }
}

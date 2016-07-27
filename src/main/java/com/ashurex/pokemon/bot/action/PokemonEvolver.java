package com.ashurex.pokemon.bot.action;
import com.pokegoapi.api.inventory.CandyJar;
import com.pokegoapi.api.map.pokemon.EvolutionResult;
import com.pokegoapi.api.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/27/16
 */
public class PokemonEvolver
{
    public static List<EvolutionResult> evolvePokemon(List<Pokemon> pokemons, CandyJar candyJar)
    {
        List<EvolutionResult> results = new ArrayList<>();

        pokemons.forEach(p ->
        {
            boolean doEvolve = false;
            try
            {
                int candies = candyJar.getCandies(p.getPokemonFamily());
                Integer candiesToEvolve = p.getCandiesToEvolve();
                doEvolve = (candies >= candiesToEvolve);
            }
            catch (Exception ignore)
            {
                // DEBUG LOG
            }

            if(doEvolve)
            {
                EvolutionResult r = evolve(p);
                if (r != null) { results.add(evolve(p)); }
            }
        });

        return results;
    }

    public static EvolutionResult evolve(Pokemon pokemon)
    {
        try
        {
            EvolutionResult r = pokemon.evolve();
            if (r.isSuccessful())
            {
                // TODO: System.out
                System.out.println(String.format("Evolved [%d] %s to %s",
                    r.getExpAwarded(), pokemon.getPokemonId(), r.getEvolvedPokemon().getPokemonId()));
            }
        }
        catch (Exception ex)
        {
            // TODO: Log
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        return null;
    }
}

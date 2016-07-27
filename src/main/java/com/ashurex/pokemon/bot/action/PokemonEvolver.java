package com.ashurex.pokemon.bot.action;
import com.pokegoapi.api.inventory.CandyJar;
import com.pokegoapi.api.map.pokemon.EvolutionResult;
import com.pokegoapi.api.pokemon.Pokemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/27/16
 */
public class PokemonEvolver
{
    private static final Logger LOG = LoggerFactory.getLogger(PokemonEvolver.class);
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
            catch (Exception ex)
            {
                LOG.debug(ex.getMessage(), ex);
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
                LOG.info(String.format("Evolved [%d] %s to %s",
                    r.getExpAwarded(), pokemon.getPokemonId(), r.getEvolvedPokemon().getPokemonId()));
            }
        }
        catch (Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }

        return null;
    }
}

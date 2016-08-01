package com.ashurex.pokemon.bot.action;
import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass;
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

    /**
     * Evolves all {@link Pokemon} provided if they can be.
     * For any Pokemon in the FAMILY_PIDGEY family, only PIDGEY will be evolved.
     *
     * @param pokemons The Pokemon to try and evolve.
     * @param candyJar The {@link CandyJar} used to determine if the bot has enough candy to evolve a specific Pokemon.
     * @return All the results from evolving Pokemon.
     */
    public static List<EvolutionResult> evolvePokemon(List<Pokemon> pokemons, CandyJar candyJar)
    {
        List<EvolutionResult> results = new ArrayList<>();

        pokemons.forEach(p ->
        {
            boolean doEvolve = false;
            try
            {
                PokemonFamilyIdOuterClass.PokemonFamilyId family = p.getPokemonFamily();

                int candies = candyJar.getCandies(p.getPokemonFamily());
                Integer candiesToEvolve = p.getCandiesToEvolve();
                doEvolve = (candies >= candiesToEvolve);

                if(family == PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_PIDGEY)
                {
                    if(p.getPokemonId() != PokemonIdOuterClass.PokemonId.PIDGEY){ doEvolve = false; }
                }
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

    /**
     * Attempts to safely evolve the provided Pokemon.
     *
     * @param pokemon The Pokemon to call {@code evolve()} on.
     * @return null if an error occured trying to evolve.
     */
    public static EvolutionResult evolve(Pokemon pokemon)
    {
        try
        {
            EvolutionResult r = pokemon.evolve();
            if (r.isSuccessful())
            {
                LOG.info(String.format("Evolved [+%dxp] %s to %s",
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

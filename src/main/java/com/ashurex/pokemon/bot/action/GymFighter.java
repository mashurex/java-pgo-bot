package com.ashurex.pokemon.bot.action;
import POGOProtos.Data.PokemonDataOuterClass;
import POGOProtos.Networking.Responses.AttackGymResponseOuterClass;
import POGOProtos.Networking.Responses.StartGymBattleResponseOuterClass;
import com.pokegoapi.api.gym.Battle;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.inventory.PokeBank;
import com.pokegoapi.api.pokemon.Pokemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: Mustafa Ashurex
 * Created: 7/27/16
 */
public class GymFighter
{
    private static final Logger LOG = LoggerFactory.getLogger(GymFighter.class);

    public static void fight(Gym gym, PokeBank pokeBank)
    {
        try
        {
            LOG.info("Attempting to fight at " + gym.getName());
            if (!gym.getEnabled() || !gym.isAttackable())
            {
                LOG.warn("Gym is disabled or not attackable!");
                return;
            }

            // TODO: Figure out good fight matchups
            List<PokemonDataOuterClass.PokemonData> defenders = gym.getDefendingPokemon();
            Pokemon[] team = getFightingTeam(defenders, pokeBank, defenders.size());
            Battle battle = gym.battle(team);
            StartGymBattleResponseOuterClass.StartGymBattleResponse.Result result = battle.start();

            if(result != StartGymBattleResponseOuterClass.StartGymBattleResponse.Result.SUCCESS)
            {
                LOG.warn("Could not start gym battle: " + result);
                return;
            }

            while (!battle.isConcluded())
            {
                AttackGymResponseOuterClass.AttackGymResponse r = battle.attack(5);
                LOG.info(String.format("%s %d/%d vs. %s %d/%d",
                    r.getActiveAttacker().getPokemonData().getPokemonId(),
                    r.getActiveAttacker().getCurrentHealth(),
                    r.getActiveAttacker().getCurrentEnergy(),
                    r.getActiveDefender().getPokemonData().getPokemonId(),
                    r.getActiveDefender().getCurrentHealth(),
                    r.getActiveAttacker().getCurrentEnergy()));
            }

            LOG.info("Battle outcome: " + battle.getOutcome());
        }
        catch(Exception ex)
        {
            LOG.error(ex.getMessage());
            ex.printStackTrace();
        }
    }


    public static Pokemon[] getFightingTeam(List<PokemonDataOuterClass.PokemonData> defenders, PokeBank pokeBank, int size)
    {
        List<Pokemon> list = pokeBank.getPokemons()
                     .stream()
                     .sorted((Pokemon a, Pokemon b) -> Integer.compare(b.getCp(), a.getCp()))
                     .collect(Collectors.toList()).subList(0, 6);
        // return list.toArray(new Pokemon[list.size()]);

        Pokemon[] val = new Pokemon[6];
        for(int i = 0; i < val.length; i++)
        {
            val[i] = list.get(i);
        }

        return val;
    }

}

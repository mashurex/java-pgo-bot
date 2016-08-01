package com.ashurex.pokemon.bot.activity;
import com.ashurex.pokemon.bot.PokemonBot;
import com.ashurex.pokemon.bot.action.PokemonCatcher;
import com.pokegoapi.api.map.pokemon.CatchResult;

import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class CatchNearbyPokemonActivity implements BotActivity
{
    private final PokemonBot pokemonBot;
    private long lastCatchAttemptMs = 0L;

    public CatchNearbyPokemonActivity(final PokemonBot bot)
    {
        this.pokemonBot = bot;
    }

    public List<CatchResult> catchNearbyPokemon()
    {
        return PokemonCatcher.catchPokemon(pokemonBot.getCatchablePokemon());
    }

    @Override
    public synchronized void performActivity()
    {
        if(getCurrentTimeMillis() - lastCatchAttemptMs > 10000L)
        {
            updateLastCatchAttempt();
            catchNearbyPokemon();
        }
    }

    private synchronized void updateLastCatchAttempt()
    {
        this.lastCatchAttemptMs = getCurrentTimeMillis();
    }

    private long getCurrentTimeMillis(){ return pokemonBot.getApi().currentTimeMillis(); }
}

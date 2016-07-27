package com.ashurex.pokemon.bot.event;
import com.ashurex.pokemon.PokemonBot;
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

    public CatchNearbyPokemonActivity(final PokemonBot bot)
    {
        this.pokemonBot = bot;
    }

    public List<CatchResult> catchNearbyPokemon()
    {
        return PokemonCatcher.catchPokemon(pokemonBot.getCatchablePokemon());
    }

    @Override
    public void performActivity()
    {
        catchNearbyPokemon();
    }
}

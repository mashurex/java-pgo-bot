package com.ashurex.pokemon.bot.event;
import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result;
import com.ashurex.pokemon.PokemonBot;
import com.pokegoapi.api.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class TransferPokemonActivity implements BotActivity
{
    private final PokemonBot bot;
    private final int MIN_CP_THRESHOLD;

    public TransferPokemonActivity(final PokemonBot bot, final int minimumCP)
    {
        this.bot = bot;
        this.MIN_CP_THRESHOLD = minimumCP;
    }

    /**
     * Attempts to transfer all Pokemon under a minimum CP threshold that are not favorites.
     * @return
     */
    public List<Result> transferPokemon()
    {
        List<Pokemon> pokemons = bot.getInventory().getPokebank().getPokemons();
        List<Result> transferred = new ArrayList<>();
        if (pokemons.size() > 0)
        {
            pokemons.forEach(p ->
            {
                if (!p.isFavorite() &&
                    p.getCp() < MIN_CP_THRESHOLD &&
                    !p.getPokemonFamily().equals(PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_NIDORAN_MALE) &&
                    !p.getPokemonFamily().equals(PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_NIDORAN_FEMALE) &&
                    !p.getPokemonFamily().equals(PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_PIKACHU))
                {
                    try
                    {
                        Result r = p.transferPokemon();
                        System.out.println(String.format("Transferred %d %s: %s", p.getCp(), p.getPokemonId(), r));
                        transferred.add(r);
                    }
                    catch (Exception ex)
                    {
                        // TODO:
                        System.err.println("Error transferring " + p.getPokemonId());
                        ex.printStackTrace();
                    }
                }
            });
        }

        return transferred;
    }

    @Override
    public void performActivity()
    {
        transferPokemon();
    }
}

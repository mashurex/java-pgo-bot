package com.ashurex.pokemon.bot.activity;
import POGOProtos.Enums.PokemonFamilyIdOuterClass.PokemonFamilyId;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result;
import com.ashurex.pokemon.bot.PokemonBot;
import com.ashurex.pokemon.bot.action.Sleeper;
import com.pokegoapi.api.pokemon.Pokemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(TransferPokemonActivity.class);

    // TODO: Make this configurable/better list
    private static final List<PokemonFamilyId> PROTECTED_FAMILIES = new ArrayList<PokemonFamilyId>(){{
        add(PokemonFamilyId.FAMILY_EEVEE);
        add(PokemonFamilyId.FAMILY_NIDORAN_FEMALE);
        add(PokemonFamilyId.FAMILY_NIDORAN_FEMALE);
        add(PokemonFamilyId.FAMILY_PIKACHU);
        add(PokemonFamilyId.FAMILY_ODDISH);
    }};

    public TransferPokemonActivity(final PokemonBot bot, final int minimumCP)
    {
        this.bot = bot;
        this.MIN_CP_THRESHOLD = minimumCP;
    }

    public boolean isProtected(PokemonFamilyId family)
    {
        return PROTECTED_FAMILIES.contains(family);
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
                if (!p.isFavorite() && p.getCp() < MIN_CP_THRESHOLD && !isProtected(p.getPokemonFamily()))
                {
                    try
                    {
                        Sleeper.shortSleep();
                        Result r = p.transferPokemon();
                        LOG.info(String.format("Transferred %d %s: %s", p.getCp(), p.getPokemonId(), r));
                        transferred.add(r);
                    }
                    catch (Exception ex)
                    {
                        LOG.error("Error transferring " + p.getPokemonId(), ex);
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

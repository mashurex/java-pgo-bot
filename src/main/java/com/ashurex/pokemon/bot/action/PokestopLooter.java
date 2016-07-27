package com.ashurex.pokemon.bot.action;
import POGOProtos.Networking.Responses.FortSearchResponseOuterClass;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/27/16
 */
public class PokestopLooter
{
    private static final Logger LOG = LoggerFactory.getLogger(PokestopLooter.class);

    public static List<PokestopLootResult> lootPokestops(final List<Pokestop> pokestops)
    {
        final List<PokestopLootResult> results = new ArrayList<>(pokestops.size());
        pokestops.forEach(p -> {
            PokestopLootResult r = lootPokestop(p);
            results.add(r);
        });

        return results;
    }

    public static PokestopLootResult lootPokestop(final Pokestop pokestop)
    {
        try
        {
            if(pokestop.canLoot())
            {
                PokestopLootResult r = pokestop.loot();
                LOG.info(String.format("Attempting to get loot from Pokestop at [%f,%f]",
                    pokestop.getLongitude(), pokestop.getLatitude()));
                if (r.wasSuccessful()) { LOG.info("    » Looted Pokestop"); }
                else { LOG.warn("    × Could not loot Pokestop."); }

                return r;
            }
        }
        catch(Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }

        return new PokestopLootResult(FortSearchResponseOuterClass.FortSearchResponse.getDefaultInstance());
    }
}

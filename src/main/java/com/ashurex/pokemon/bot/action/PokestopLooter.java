package com.ashurex.pokemon.bot.action;
import POGOProtos.Networking.Responses.FortSearchResponseOuterClass;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/27/16
 */
public class PokestopLooter
{
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
                // TODO: System.out
                System.out.println(String.format("Attempting to get loot from Pokestop at [%f,%f]",
                    pokestop.getLongitude(), pokestop.getLatitude()));
                if (r.wasSuccessful()) { System.out.println("    » Looted Pokestop"); }
                else { System.out.println("    × Could not loot Pokestop."); }

                return r;
            }
        }
        catch(Exception ex)
        {
            // TODO: Logging
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        return new PokestopLootResult(FortSearchResponseOuterClass.FortSearchResponse.getDefaultInstance());
    }
}

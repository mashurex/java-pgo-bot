package com.ashurex.pokemon.bot.action;
import POGOProtos.Networking.Responses.UseItemEggIncubatorResponseOuterClass.UseItemEggIncubatorResponse.*;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.inventory.Hatchery;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.HatchedEgg;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: Mustafa Ashurex
 * Created: 7/27/16
 */
public class EggManager
{
    public static List<HatchedEgg> queryHatchedEggs(Hatchery hatchery)
    {
        try
        {
            final List<HatchedEgg> hatchedEggs = hatchery.queryHatchedEggs();
            if (hatchedEggs != null && hatchedEggs.size() > 0)
            {
                hatchedEggs.forEach(e ->
                {
                    System.out.println(e.getId() + " egg hatched: " + e.toString());
                });
            }

            return hatchedEggs;
        }
        catch(Exception ex)
        {
            // TODO: Log
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        return new ArrayList<>();
    }

    public static List<EggIncubator> getAvailableIncubators(Inventories inventory)
    {
        return inventory.getIncubators().stream().filter(e -> !e.isInUse()).collect(Collectors.toList());
    }

    /**
     *
     * @return The first available egg from the inventory.
     */
    public static EggPokemon getAvailableEgg(Inventories inventory)
    {
        Set<EggPokemon> eggs = inventory.getHatchery().getEggs();
        if (eggs.size() > 0)
        {
            Optional<EggPokemon> result = eggs.stream().filter(p -> !p.isIncubate())
                                              .sorted((EggPokemon a, EggPokemon b) ->
                                                  Long.compare(a.getCreationTimeMs(), b.getCreationTimeMs()))
                                              .findFirst();
            if (result.isPresent()) { return result.get(); }
        }
        return null;
    }

    public static List<EggIncubator> fillIncubators(Inventories inventory)
    {
        final List<EggIncubator> filled = new ArrayList<>();
        getAvailableIncubators(inventory).forEach(i -> {
            EggPokemon egg = getAvailableEgg(inventory);
            if(egg != null)
            {
                Result r = hatchEgg(egg, i);
                if(r != null) {
                    // TODO: Log success
                    filled.add(i);
                }
            }
        });

        return filled;
    }

    public static Result hatchEgg(EggPokemon egg, EggIncubator incubator)
    {
        try
        {
            return incubator.hatchEgg(egg);
        }
        catch (Exception ex)
        {
            // TODO:
            System.err.println("Could not incubate new egg: " + ex.getMessage());
            ex.printStackTrace();
        }

        return null;
    }
}

package com.ashurex.pokemon.bot;
import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import com.ashurex.pokemon.bot.action.BotWalker;
import com.google.maps.model.LatLng;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.map.Map;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/27/16
 */
public interface PokemonBot
{
    enum OpStatus {
        DUH,
        WALKING,
        RUNNING,
        LOOTING,
        CATCHING
    }

    void setWalker(BotWalker botWalker);
    BotWalker getWalker();

    LatLng getCurrentLocation();
    void setCurrentLocation(LatLng point, double altitude);

    List<CatchablePokemon> getCatchablePokemon();
    Inventories getInventory();
    Map getMap();
    PokemonGo getApi();

    void wander();
    List<CatchResult> snipe(LatLng origin, LatLng destination, PokemonId pokemonId);
    boolean fixSoftBan(LatLng destination);
    void fightAtNearestGym();

}

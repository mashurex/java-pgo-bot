package com.ashurex.pokemon.bot.listener;
import com.google.maps.model.LatLng;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public interface LocationListener extends AutoCloseable
{
    void updateCurrentLocation(LatLng point, double altitude);
}

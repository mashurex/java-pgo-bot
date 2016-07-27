package com.ashurex.pokemon.bot.event;
import com.google.maps.model.LatLng;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public interface LocationListener
{
    void updateCurrentLocation(LatLng point, double altitude);
}

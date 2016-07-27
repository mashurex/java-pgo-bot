package com.ashurex.pokemon.logging;
import com.google.maps.model.LatLng;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public interface LocationLogger extends AutoCloseable
{
    void write(LatLng loc);
}

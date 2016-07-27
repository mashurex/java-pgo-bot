package com.ashurex.pokemon.bot.action;
import com.ashurex.pokemon.bot.activity.BotActivity;
import com.google.maps.model.LatLng;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public interface BotWalker
{
    LatLng getCurrentLocation();
    double setCurrentLocation(LatLng destination);
    double getAltitude(LatLng point);
    void walkTo(double stepSize, final LatLng origin, final LatLng destination, final boolean doWander);
    void runTo(final LatLng origin, final LatLng destination);
    void addPostStepActivity(BotActivity activity);
    void performPostStepActivities();
}

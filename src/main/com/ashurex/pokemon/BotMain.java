package com.ashurex.pokemon;
import com.ashurex.pokemon.logging.LocationLogger;
import com.ashurex.pokemon.logging.SimpleLocationLogger;
import com.google.maps.model.LatLng;

/**
 * Author: Mustafa Ashurex
 * Created: 7/25/16
 */
public class BotMain
{

    private static final LatLng MY_LOCATION = new LatLng(45.51308360513236,-122.68054962158202);
    public static void main(String... args) throws Exception
    {
        final LocationLogger simpleLocationLogger = new SimpleLocationLogger();

        try
        {
            // BotOptions options = new BotOptions(BotOptions.LoginProvider.GOOGLE, new File("./gtoken.txt"));
            BotOptions options = new BotOptions(BotOptions.LoginProvider.PTC, null);
            options.setUsername("ashurexm");
            options.setPassword("X8U2kpm4N)/QaVM");
            // options.setUsername("Jonpokemongo");
            // options.setPassword("Pokemon1234!");
            options.setUseWalkingSpeed(false);
            options.setStepMeters(20);
            options.setHeartBeatPace(20);
            options.setMinCpThreshold(300);

            PokemonBot bot = new PokemonBot(MY_LOCATION, options);
            bot.wander();

//            if(bot.fixSoftBan(MY_LOCATION))
//            {
//                System.out.println("Soft ban fixed!");
//            }
//            else
//            {
//                System.err.println("Still banned.");
//            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        simpleLocationLogger.close();
    }
}

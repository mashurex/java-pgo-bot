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
    // private static final String G_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjBiZDEwY2JmMDM2OGQ2MWE0NDBiZjYxZjNiM2EyZDI0NGExODQ5NDcifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhdF9oYXNoIjoibFZXajNwRFZpMFY4QzBZeTlzZDNlUSIsImF1ZCI6Ijg0ODIzMjUxMTI0MC03M3JpM3Q3cGx2azk2cGo0Zjg1dWo4b3RkYXQyYWxlbS5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsInN1YiI6IjExNzcwNTM0OTM5MzU1NjY1NzAyOSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhenAiOiI4NDgyMzI1MTEyNDAtNzNyaTN0N3Bsdms5NnBqNGY4NXVqOG90ZGF0MmFsZW0uYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJlbWFpbCI6ImFzaHVyZXguanVua0BnbWFpbC5jb20iLCJpYXQiOjE0Njk1NDc5OTQsImV4cCI6MTQ2OTU1MTU5NH0.LzV9TIowR_13WuV5JcKhXXelYZa2P0kuTlaIrZcgSF-TagdJSwHbwVoRW6w8zF_KdNpx8Zdr2FBF1EcH0Se6JfpoqLIuXEg6Ri0Bv9sx5T3B9kXy1WJ6xTPyQtv-a1nj9MaQTXnSzZSUGzcOQ0QkCuqHKi_6qa9UEathDxCGie1ffqHloriqg2_tTMTdLhVb9vTAB6Dr3GMWUmmc2z_Zk5n8LYG764Ve8W8HWRi1E6yD2OZKy7qf1yGCBlEQxAje-aJsmWD1x378J8MqMrfU7AUbE8fD4RGHQCSm-NKSe_Vqtc6m1kLphaqXZO5by_EO_Deg2Q1NTEuCaGy4vaZuUQ";
    private static final LatLng MY_LOCATION = new LatLng(47.61409049880896,-122.34467267990112);
    // private static final LatLng PIKE_PLACE = new LatLng(47.610255, -122.341349);
    // private static final LatLng PIKE_PLACE = new LatLng(47.607501247840226,-122.33781695365906);
    // private static final LatLng SNORL = new LatLng(34.066414172734056,-118.41403484344482);
    // private static final LatLng MY_LOCATION = new LatLng(40.783043, -73.965065);

    public static void main(String... args) throws Exception
    {
        final LocationLogger simpleLocationLogger = new SimpleLocationLogger();

        try
        {
            // BotOptions options = new BotOptions(BotOptions.LoginProvider.GOOGLE, new File("./gtoken.txt"));
            BotOptions options = new BotOptions(BotOptions.LoginProvider.PTC, null);
            // options.setUsername("ashurexm");
            // options.setPassword("X8U2kpm4N)/QaVM");
            options.setUsername("Jonpokemongo");
            options.setPassword("Pokemon1234!");
            options.setUseWalkingSpeed(false);
            options.setStepMeters(20);
            options.setHeartBeatPace(20);

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

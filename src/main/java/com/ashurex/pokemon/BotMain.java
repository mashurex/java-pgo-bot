package com.ashurex.pokemon;
import POGOProtos.Enums.PokemonIdOuterClass;
import com.ashurex.pokemon.bot.PokemonBot;
import com.ashurex.pokemon.bot.SimplePokemonBot;
import com.google.maps.model.LatLng;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Author: Mustafa Ashurex
 * Created: 7/25/16
 */
public class BotMain
{
    private static final Logger LOG = LoggerFactory.getLogger(BotMain.class);
    public static void main(String... args) throws Exception
    {

        // Sample start args

        // Basic wandering w/ PTC and Google Login
        // -lat "45.518502" -lng "-122.682156" -l PTC -u "username" -p "XXXXXX" -x -c 200 -e -t
        // -lat "45.518502" -lng "-122.682156" -l GOOGLE -x -c 200 -e -t

        // Snipe Eevee w/ Google login
        // -lat "45.518502" -lng "-122.682156" -l GOOGLE -snipe -dest-lat "45.474577292898935" -dest-lng "-122.64651775360109" -pokemon "EEVEE"

        Options options = new Options();
        Option latOpt = new Option(null, "lat", true, "Latitude");
        latOpt.setRequired(true);

        Option lngOpt = new Option(null, "lng", true, "Longitude");
        lngOpt.setRequired(true);

        Option debugOpt = new Option("x", "Debug mode");
        Option usernameOpt = new Option("u", null, true, "Username");
        Option passwordOpt = new Option("p", null, true, "Password");
        Option loginTypeOpt = new Option("l", null, true, "Login with: GOOGLE or PTC");
        loginTypeOpt.setRequired(true);

        Option useWalkingSpeedOpt = new Option("w", "Use walking speed to hatch eggs");
        Option stepSizeOpt = new Option("s", null, true, "The size of bot each bot 'step' in meters");
        Option minCpOpt = new Option("c", null, true, "Minimum CP threshold to keep");
        Option paceOpt = new Option("b", null, true, "Heart beat every [b] number of op ticks");
        Option evolveOpt = new Option("e", "Auto evolve Pokemon when possible");
        Option transferOpt = new Option("t", "Auto transfer Pokemon under the minimum CP threshold");

        Option snipeMode = new Option(null, "snipe", false, "Perform a snipe to target location");
        Option extraLat = new Option(null, "dest-lat", true, "Destination latitude");
        Option extraLng = new Option(null, "dest-lng", true, "Destination longitude");
        Option pokemonOpt = new Option(null, "pokemon", true, "Target Pokemon family name");

        options.addOption(latOpt);
        options.addOption(lngOpt);
        options.addOption(debugOpt);
        options.addOption(usernameOpt);
        options.addOption(passwordOpt);
        options.addOption(loginTypeOpt);
        options.addOption(useWalkingSpeedOpt);
        options.addOption(stepSizeOpt);
        options.addOption(minCpOpt);
        options.addOption(paceOpt);
        options.addOption(evolveOpt);
        options.addOption(transferOpt);
        options.addOption(snipeMode);
        options.addOption(extraLat);
        options.addOption(extraLng);
        options.addOption(pokemonOpt);

        CommandLineParser parser = new DefaultParser();
        final CommandLine commandLine;
        try
        {
            commandLine = parser.parse( options, args);

            if(commandLine.hasOption("snipe") && !(commandLine.hasOption("dest-lat") && commandLine.hasOption("dest-lng")))
            {
                throw new ParseException("Destination latitude and longitude are required for snipe mode.");
            }
        }
        catch( ParseException exp )
        {
            System.err.println(exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("pokebot", options);
            return;
        }

        final BotOptions botOptions = BotOptions.fromCommandLine(commandLine);

        if(botOptions.getLoginProvider() == BotOptions.LoginProvider.GOOGLE)
        {
            // TODO: Regenerate token file when login errors occur
            botOptions.setTokenFile(new File("./gtoken.txt"));
        }

        try
        {
            if(!botOptions.hasValidLoginSettings())
            {
                throw new IllegalArgumentException("Invalid login parameters");
            }
            if(!botOptions.isValid()){ throw new IllegalArgumentException("Missing or invalid required options"); }

            final PokemonBot bot = SimplePokemonBot.build(botOptions);

            if(commandLine.hasOption("snipe"))
            {
                LatLng destination = new LatLng(Double.valueOf(commandLine.getOptionValue("dest-lat")),
                    Double.valueOf(commandLine.getOptionValue("dest-lng")));

                final String toBeSniped = commandLine.getOptionValue("pokemon");

                PokemonIdOuterClass.PokemonId target;
                if(StringUtils.isEmpty(toBeSniped)){ target = null; }
                else
                {
                    try
                    {
                        target = PokemonIdOuterClass.PokemonId.valueOf(toBeSniped);
                    }
                    catch(IllegalArgumentException ex)
                    {
                        throw new IllegalArgumentException("Unknown Pokemon family: " + toBeSniped);
                    }
                }

                bot.snipe(botOptions.getBotOrigin(), destination, target);

                System.out.println("Done sniping: " + (target != null ? target : " everything"));
            }
            else
            {
                bot.wander();
                System.out.println("Done wandering.");
            }
        }
        catch(IllegalArgumentException ex)
        {
            LOG.info(ex.getMessage(), ex);
            System.err.println(ex.getMessage());
        }
        catch(Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
            System.err.println(ex.getMessage());
        }
    }
}

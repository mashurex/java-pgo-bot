package com.ashurex.pokemon;
import com.ashurex.pokemon.bot.PokemonBot;
import com.ashurex.pokemon.bot.SimplePokemonBot;
import org.apache.commons.cli.*;
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
        // -lat "45.518502" -lng "-122.682156" -l PTC -u ashurexm -p "XXXXXX" -x -c 200 -e -t
        // -lat "45.518502" -lng "-122.682156" -l GOOGLE -x -c 200 -e -t

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

        CommandLineParser parser = new DefaultParser();
        final CommandLine line;
        try {
            line = parser.parse( options, args);
        }
        catch( ParseException exp )
        {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("pokebot", options);
            return;
        }

        final BotOptions botOptions = BotOptions.fromCommandLine(line);

        if(botOptions.getLoginProvider() == BotOptions.LoginProvider.GOOGLE)
        {
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
            bot.wander();
        }
        catch(IllegalArgumentException ex)
        {
            LOG.debug(ex.getMessage(), ex);
        }
        catch(Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }

        System.out.println("Done.");
    }
}

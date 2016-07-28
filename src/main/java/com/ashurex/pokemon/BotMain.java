package com.ashurex.pokemon;
import POGOProtos.Enums.PokemonIdOuterClass;
import com.ashurex.pokemon.bot.PokemonBot;
import com.ashurex.pokemon.bot.SimplePokemonBot;
import com.pokegoapi.exceptions.RemoteServerException;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.SocketTimeoutException;

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
        Option lngOpt = new Option(null, "lng", true, "Longitude");
        Option debugOpt = new Option("x", "Debug mode");
        Option usernameOpt = new Option("u", null, true, "Username");
        Option passwordOpt = new Option("p", null, true, "Password");
        Option loginTypeOpt = new Option("l", null, true, "Login with: GOOGLE or PTC");
        Option useWalkingSpeedOpt = new Option("w", "Use walking speed to hatch eggs");
        Option stepSizeOpt = new Option("s", null, true, "The size of bot each bot 'step' in meters");
        Option minCpOpt = new Option("c", null, true, "Minimum CP threshold to keep");
        Option paceOpt = new Option("b", null, true, "Heart beat every [b] number of op ticks");
        Option evolveOpt = new Option("e", "Auto evolve Pokemon when possible");
        Option transferOpt = new Option("t", "Auto transfer Pokemon under the minimum CP threshold");
        Option doFightsOpt = new Option("f", "Attempt to fight at Gyms when nearby");
        Option snipeModeOpt = new Option(null, "snipe", false, "Perform a snipe to target location");
        Option destLatOpt = new Option(null, "dest-lat", true, "Destination latitude");
        Option destLngOpt = new Option(null, "dest-lng", true, "Destination longitude");
        Option pokemonOpt = new Option(null, "pokemon", true, "Target Pokemon family name");

        Option spinFixModeOpt = new Option(null, "softban", false, "Attempt to spin fix a soft ban");
        Option fightModeOpt = new Option(null, "fight", false, "Walk to nearby Gyms and fight");

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
        options.addOption(snipeModeOpt);
        options.addOption(destLatOpt);
        options.addOption(destLngOpt);
        options.addOption(pokemonOpt);
        options.addOption(spinFixModeOpt);
        options.addOption(fightModeOpt);
        options.addOption(doFightsOpt);

        CommandLineParser parser = new DefaultParser();
        final CommandLine commandLine;
        try
        {
            commandLine = parser.parse( options, args);
            if(commandLine.hasOption(snipeModeOpt.getLongOpt()) &&
                !(commandLine.hasOption(destLatOpt.getLongOpt()) && commandLine.hasOption(destLngOpt.getLongOpt())))
            {
                throw new ParseException("Destination latitude and longitude are required for snipe mode.");
            }
        }
        catch(ParseException exp)
        {
            System.err.println(exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("pgobot", options);
            return;
        }

        final BotOptions botOptions = PokebotProperties.readProperties(commandLine);

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

            if(commandLine.hasOption(fightModeOpt.getLongOpt()))
            {
                LOG.debug("Attempt to fight at nearest gym...");
                bot.fightAtNearestGym();
            }
            else if(commandLine.hasOption(snipeModeOpt.getLongOpt()))
            {
                LOG.debug("Attempting to snipe...");
                snipe(bot, botOptions, commandLine.getOptionValue(pokemonOpt.getLongOpt()));
            }
            else if(commandLine.hasOption(spinFixModeOpt.getLongOpt()))
            {
                LOG.debug("Attempting to spinfix...");
                bot.fixSoftBan(botOptions.getBotOrigin());
            }
            else
            {
                LOG.debug("Attempting to wander...");
                bot.wander();
            }
        }
        catch(IllegalArgumentException ex)
        {
            System.err.println(ex.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("pgobot", options);
        }
        catch(RemoteServerException ex)
        {
            if(ex.getCause() instanceof SocketTimeoutException)
            {
                LOG.error("Could not connect to Pokemon servers due to timeout.");
            }
            else
            {
                LOG.error("Error communicating with Pokemon servers: " + ex.getMessage(), ex);
            }
        }
        catch(Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private static void snipe(final PokemonBot bot, final BotOptions botOptions, final String targetPokemon)
    throws IllegalArgumentException
    {
        final PokemonIdOuterClass.PokemonId toBeSniped;
        if(StringUtils.isEmpty(targetPokemon)){ toBeSniped = null; }
        else
        {
            try
            {
                toBeSniped = PokemonIdOuterClass.PokemonId.valueOf(targetPokemon);
            }
            catch(IllegalArgumentException ex)
            {
                throw new IllegalArgumentException("Unknown Pokemon family: " + targetPokemon);
            }
        }

        bot.snipe(botOptions.getBotOrigin(), botOptions.getBotDestination(), toBeSniped);
        LOG.info("Done sniping: " + (toBeSniped != null ? toBeSniped : " everything"));
    }
}

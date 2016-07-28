package com.ashurex.pokemon;
import com.google.maps.model.LatLng;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Author: Mustafa Ashurex
 * Created: 7/27/16
 */
public class PokebotProperties
{
    private static final String DEFAULT_FILENAME = "pokebot.properties";
    private static final Logger LOG = LoggerFactory.getLogger(PokebotProperties.class);
    private String mapsApiKey = null;
    private LatLng defaultOrigin = null;
    private String username = null;
    private String password = null;
    private BotOptions.LoginProvider loginProvider = null;

    private static final String MAPS_KEY = "pokebot.maps.key";
    private static final String USERNAME = "pokebot.auth.username";
    private static final String PASSWORD = "pokebot.auth.password";
    private static final String DEFAULT_ORIGIN = "pokebot.defaults.origin";
    private static final String LOGIN_PROVIDER = "pokebot.auth.provider";

    public PokebotProperties()
    {

    }

    protected static PokebotProperties readProperties() throws IOException
    {
        Properties properties = new Properties();
        InputStream inputStream = null;
        InputStream fileInputStream = null;

        try
        {
            inputStream = PokebotProperties.class.getClassLoader().getResourceAsStream(DEFAULT_FILENAME);

            try
            {
                if (inputStream != null) { properties.load(inputStream); }
                else
                {
                    LOG.debug("Could not find properties file on the classpath.");
                }
            }
            catch(Exception ex)
            {
                LOG.error("Error reading classpath properties: " + ex.getMessage(), ex);
            }

            // Check for and merge external props
            File externalProps = new File("./" + DEFAULT_FILENAME);
            if(externalProps.exists() && externalProps.canRead())
            {
                fileInputStream = new FileInputStream(externalProps);
                Properties external = new Properties();
                external.load(fileInputStream);
                properties.putAll(external);
            }

            PokebotProperties pokebotProperties = new PokebotProperties();
            pokebotProperties.setMapsApiKey(properties.getProperty(MAPS_KEY));
            pokebotProperties.setUsername(properties.getProperty(USERNAME));
            pokebotProperties.setPassword(properties.getProperty(PASSWORD));
            pokebotProperties.setDefaultOrigin(getDefaultOrigin(properties.getProperty(DEFAULT_ORIGIN)));
            pokebotProperties.setLoginProvider(getLoginProvider(properties.getProperty(LOGIN_PROVIDER)));

            return pokebotProperties;
        }
        finally
        {
            if(inputStream != null)
            {
                inputStream.close();
            }

            if(fileInputStream != null)
            {
                fileInputStream.close();
            }
        }
    }

    /**
     * Returns BotOptions merged from properties and command line arguments.
     * Precedence order CommandLine > External properties file > Classpath properties file.
     * @param args
     * @return
     * @throws IOException
     */
    public static BotOptions readProperties(CommandLine args) throws IOException
    {
        PokebotProperties properties = readProperties();
        if(args.hasOption("u")){ properties.setUsername(args.getOptionValue("u")); }
        if(args.hasOption("p")){ properties.setPassword(args.getOptionValue("p")); }
        if(args.hasOption("lat") && args.hasOption("lng"))
        {
            properties.setDefaultOrigin(new LatLng(Double.valueOf(args.getOptionValue("lat")),
                Double.valueOf(args.getOptionValue("lng"))));
        }

        if(args.hasOption("l"))
        {
            properties.setLoginProvider(getLoginProvider(args.getOptionValue("l")));
        }

        BotOptions options = BotOptions.fromCommandLine(args);
        options.setUsername(properties.getUsername());
        options.setPassword(properties.getPassword());
        options.setLoginProvider(properties.getLoginProvider());
        options.setBotOrigin(properties.getDefaultOrigin());
        options.setMapsKey(properties.getMapsApiKey());

        return options;
    }

    private static LatLng getDefaultOrigin(String value)
    {
        if(StringUtils.isEmpty(value)){ return null; }
        String[] parts = value.split(",");
        if(parts.length != 2)
        {
            throw new IllegalArgumentException("Invalid location value: " + value);
        }

        return new LatLng(Double.valueOf(parts[0]), Double.valueOf(parts[1]));
    }

    private static BotOptions.LoginProvider getLoginProvider(String value)
    {
        if(StringUtils.isEmpty(value)){ return null; }
        return BotOptions.LoginProvider.valueOf(value);
    }

    public String getMapsApiKey()
    {
        return mapsApiKey;
    }

    public void setMapsApiKey(String mapsApiKey)
    {
        this.mapsApiKey = mapsApiKey;
    }

    public LatLng getDefaultOrigin()
    {
        return defaultOrigin;
    }

    public void setDefaultOrigin(LatLng defaultOrigin)
    {
        this.defaultOrigin = defaultOrigin;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public BotOptions.LoginProvider getLoginProvider()
    {
        return loginProvider;
    }

    public void setLoginProvider(BotOptions.LoginProvider loginProvider)
    {
        this.loginProvider = loginProvider;
    }
}

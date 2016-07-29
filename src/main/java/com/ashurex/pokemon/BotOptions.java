package com.ashurex.pokemon;
import com.google.maps.model.LatLng;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.util.SystemTimeImpl;
import okhttp3.OkHttpClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class BotOptions
{
    private double stepMeters = 10;
    private int heartBeatPace = 50;
    private boolean doEvolutions = false;
    private boolean useWalkingSpeed = false;
    private boolean doTransfers = false;
    private int minCpThreshold = 250;
    private File tokenFile = null;
    private String username = null;
    private String password = null;
    private LoginProvider loginProvider = LoginProvider.PTC;
    private String refreshToken = null;
    private boolean debugMode = false;
    private LatLng botOrigin = null;
    private String mapsKey = null;
    private LatLng botDestination = null;
    private boolean doFights = false;

    public enum LoginProvider
    {
        GOOGLE,
        PTC
    }

    public BotOptions()
    {

    }

    public BotOptions(File tokenFile)
    {
        this.loginProvider = LoginProvider.GOOGLE;
        this.tokenFile = tokenFile;
    }

    public static BotOptions fromCommandLine(CommandLine args)
    {
        BotOptions options = new BotOptions();
        if(args.hasOption("s"))
        {
            options.setStepMeters(Double.valueOf(args.getOptionValue("s")));
        }

        if(args.hasOption("c"))
        {
            options.setMinCpThreshold(Integer.valueOf(args.getOptionValue("c")));
        }

        if(args.hasOption("b"))
        {
            options.setHeartBeatPace(Integer.valueOf(args.getOptionValue("b")));
        }

        options.setDoTransfers(args.hasOption("t"));
        options.setDoEvolutions(args.hasOption("e"));
        options.setDebugMode(args.hasOption("x"));
        options.setUseWalkingSpeed(args.hasOption("w"));
        options.setDoFights(args.hasOption("f"));

        if(args.hasOption("lat") && args.hasOption("lng"))
        {
            double lat = Double.valueOf(args.getOptionValue("lat"));
            double lng = Double.valueOf(args.getOptionValue("lng"));
            options.setBotOrigin(new LatLng(lat, lng));
        }

        options.setLoginProvider(
            LoginProvider.valueOf(args.getOptionValue("l", LoginProvider.PTC.toString()).toUpperCase()));
        options.setUsername(args.getOptionValue("u"));
        options.setPassword(args.getOptionValue("p"));

        if(args.hasOption("dest-lat") && args.hasOption("dest-lng"))
        {
            double lat = Double.valueOf(args.getOptionValue("dest-lat"));
            double lng = Double.valueOf(args.getOptionValue("dest-lng"));
            options.setBotDestination(new LatLng(lat, lng));
        }

        return options;
    }

    public boolean isValid()
    {
        return (getBotOrigin() != null) && hasValidLoginSettings();
    }

    public boolean hasValidLoginSettings()
    {
        if(getLoginProvider() == null){ return false; }
        else if(getLoginProvider() == LoginProvider.PTC)
        {
            return !(StringUtils.isEmpty(getUsername()) || StringUtils.isEmpty(getPassword()));
        }
        else if(getLoginProvider() == LoginProvider.GOOGLE)
        {
            return getTokenFile() != null;
        }

        return true;
    }

    public synchronized String getRefreshToken()
    {
        try
        {
            if (getTokenFile() != null && getTokenFile().canRead() && StringUtils.isEmpty(refreshToken))
            {
                this.refreshToken = getTokenString(tokenFile);
            }
        }
        catch(Exception ex)
        {
            System.err.println("Error reading token file: " + ex.getMessage());
            this.refreshToken = null;
        }

        return refreshToken;
    }

    private static String getTokenString(File file) throws IOException
    {
        if(!file.exists() || !file.canRead()){ return null; }

        List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()));
        if(lines.size() > 0){ return lines.get(0); }
        return null;
    }

    protected void writeTokenString(String token) throws IOException
    {
        Files.write(Paths.get(tokenFile.getAbsolutePath()), token.getBytes());
    }

    protected GoogleUserCredentialProvider cleanGoogleLogin(final OkHttpClient okHttpClient) throws LoginFailedException, RemoteServerException, IOException
    {
        GoogleUserCredentialProvider provider = new GoogleUserCredentialProvider(okHttpClient);
        System.out.println("Please go to " + GoogleUserCredentialProvider.LOGIN_URL);
        System.out.println("Enter authorisation code:");
        Scanner sc = new Scanner(System.in);
        String access = sc.nextLine();
        provider.login(access);

        writeTokenString(provider.getRefreshToken());

        return provider;
    }

    public CredentialProvider getCredentialProvider(final OkHttpClient okHttpClient)
    throws LoginFailedException, RemoteServerException, IOException
    {
        if(getLoginProvider() == LoginProvider.GOOGLE)
        {
            String refreshToken = getRefreshToken();
            if(StringUtils.isEmpty(refreshToken))
            {
                return cleanGoogleLogin(okHttpClient);
            }
            else
            {
                try
                {
                    return new GoogleUserCredentialProvider(okHttpClient, refreshToken, new SystemTimeImpl());
                }
                catch(Exception ex)
                {
                    return cleanGoogleLogin(okHttpClient);
                }
            }
        }
        else
        {
            return new PtcCredentialProvider(okHttpClient, getUsername(), getPassword(), new SystemTimeImpl());
        }
    }

    public double getStepMeters()
    {
        return stepMeters;
    }

    public void setStepMeters(double stepMeters)
    {
        this.stepMeters = stepMeters;
    }

    public int getHeartBeatPace()
    {
        return heartBeatPace;
    }

    public void setHeartBeatPace(int heartBeatPace)
    {
        this.heartBeatPace = heartBeatPace;
    }

    public boolean isDoEvolutions()
    {
        return doEvolutions;
    }

    public void setDoEvolutions(boolean doEvolutions)
    {
        this.doEvolutions = doEvolutions;
    }

    public boolean isUseWalkingSpeed()
    {
        return useWalkingSpeed;
    }

    public void setUseWalkingSpeed(boolean useWalkingSpeed)
    {
        this.useWalkingSpeed = useWalkingSpeed;
    }

    public boolean isDoTransfers()
    {
        return doTransfers;
    }

    public void setDoTransfers(boolean doTransfers)
    {
        this.doTransfers = doTransfers;
    }

    public int getMinCpThreshold()
    {
        return minCpThreshold;
    }

    public void setMinCpThreshold(int minCpThreshold)
    {
        this.minCpThreshold = minCpThreshold;
    }

    public File getTokenFile()
    {
        return tokenFile;
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

    public LoginProvider getLoginProvider()
    {
        return loginProvider;
    }

    public void setLoginProvider(LoginProvider loginProvider)
    {
        this.loginProvider = loginProvider;
    }

    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }

    public void setTokenFile(File tokenFile)
    {
        this.tokenFile = tokenFile;
    }

    public boolean isDebugMode()
    {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode)
    {
        this.debugMode = debugMode;
    }

    public LatLng getBotOrigin()
    {
        return botOrigin;
    }

    public void setBotOrigin(LatLng botOrigin)
    {
        this.botOrigin = botOrigin;
    }

    public String getMapsKey()
    {
        return mapsKey;
    }

    public void setMapsKey(String mapsKey)
    {
        this.mapsKey = mapsKey;
    }

    public LatLng getBotDestination()
    {
        return botDestination;
    }

    public void setBotDestination(LatLng botDestination)
    {
        this.botDestination = botDestination;
    }

    public boolean isDoFights()
    {
        return doFights;
    }

    public void setDoFights(boolean doFights)
    {
        this.doFights = doFights;
    }
}

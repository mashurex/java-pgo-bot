package com.ashurex.pokemon;
import com.ashurex.pokemon.auth.SimpleGoogleLoginOAuthCompleteListener;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class BotOptions
{
    private double stepMeters = 2;
    private int heartBeatPace = 50;
    private boolean doEvolutions = true;
    private boolean useWalkingSpeed = true;
    private boolean doTransfers = true;
    private int minCpThreshold = 70;
    private CredentialProvider credentialProvider;
    private final File tokenFile;
    private String username;
    private String password;
    private LoginProvider loginProvider = LoginProvider.PTC;
    private String refreshToken;

    public enum LoginProvider
    {
        GOOGLE,
        PTC
    }

    public BotOptions()
    {
        this.tokenFile = null;
    }

    public BotOptions(LoginProvider loginProvider, File tokenFile)
    {
        this.loginProvider = loginProvider;
        this.tokenFile = tokenFile;
    }

    public String getRefreshToken()
    {
        try
        {
            if (tokenFile != null && tokenFile.canRead() && StringUtils.isEmpty(refreshToken))
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

    public CredentialProvider getCredentialProvider(OkHttpClient okHttpClient) throws LoginFailedException,
        RemoteServerException
    {
        if(loginProvider == LoginProvider.GOOGLE)
        {
            String refreshToken = getRefreshToken();
            if(StringUtils.isEmpty(refreshToken))
            {
                return new GoogleCredentialProvider(okHttpClient,
                    new SimpleGoogleLoginOAuthCompleteListener(tokenFile));
            }
            else
            {
                return new GoogleCredentialProvider(okHttpClient, refreshToken);
            }
        }
        else
        {
            return new PtcCredentialProvider(okHttpClient, username, password);
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
}

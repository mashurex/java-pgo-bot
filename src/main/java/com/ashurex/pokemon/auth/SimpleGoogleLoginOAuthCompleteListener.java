package com.ashurex.pokemon.auth;
import com.pokegoapi.auth.GoogleAuthJson;
import com.pokegoapi.auth.GoogleAuthTokenJson;
import com.pokegoapi.auth.GoogleCredentialProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Author: Mustafa Ashurex
 * Created: 7/26/16
 */
public class SimpleGoogleLoginOAuthCompleteListener
    implements GoogleCredentialProvider.OnGoogleLoginOAuthCompleteListener

{
    private final File tokenFile;

    public SimpleGoogleLoginOAuthCompleteListener(final File tokenFile)
    {
        this.tokenFile = tokenFile;
    }

    protected void writeTokenString(String token) throws IOException
    {
        Files.write(Paths.get(tokenFile.getAbsolutePath()), token.getBytes());
    }

    @Override
    public void onInitialOAuthComplete(GoogleAuthJson googleAuthJson)
    {
        System.out.println(String.format("Please visit %s with code %s",
            googleAuthJson.getVerificationUrl(), googleAuthJson.getUserCode()));
    }

    @Override
    public void onTokenIdReceived(GoogleAuthTokenJson googleAuthTokenJson)
    {
        if (!StringUtils.isEmpty(googleAuthTokenJson.getError()))
        {
            System.err.println("Error authenticating with Google: " + googleAuthTokenJson.getError());
            return;
        }

        try
        {
            writeTokenString(googleAuthTokenJson.getAccessToken());
        }
        catch (Exception e)
        {
            System.err.println("Could not write token:\n" + googleAuthTokenJson.getAccessToken());
        }
    }
}

package com.ashurex.pokemon.logging;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import com.ashurex.pokemon.bot.SimplePokemonBot;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import static ch.qos.logback.core.pattern.color.ANSIConstants.*;

/**
 * Super rough colorizer for the console log to highlight certain classes/events/messages.
 * This is very likely to have a big performance cost, so it shouldn't be used if speed is of the essence.
 * Author: Mustafa Ashurex
 * Created: 7/28/16
 */
public class PokebotConsoleLoggingLayout extends LayoutBase<ILoggingEvent>
{
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
    final private static String SET_DEFAULT_COLOR = ESC_START + "0;" + DEFAULT_FG + ESC_END;

    public String doLayout(ILoggingEvent event)
    {
        try
        {
            if(StringUtils.isEmpty(event.getMessage()))
            {
                return "";
            }

            StringBuilder b = new StringBuilder();
            b.append(dateFormatter.format(new Date(event.getTimeStamp())));
            b.append(" ");
            b.append(getLogLevel(event.getLevel()));
            b.append(" ");
            b.append(getLogMessage(event));
            b.append(System.lineSeparator());

            return b.toString();
        }
        catch(Exception ex)
        {
            return "LAYOUTERR: " + event.getLoggerName() + " - " + event.getFormattedMessage() + System.lineSeparator();
        }
    }

    private String getLogMessage(final ILoggingEvent event)
    {
        final String name = event.getLoggerName();
        final String[] parts = name.split("\\.");
        final String lastPart = parts[parts.length - 1];
        final String msg = event.getFormattedMessage().toLowerCase();
        final String append =  " - " + event.getFormattedMessage();

        if(name.endsWith(SimplePokemonBot.class.getSimpleName()))
        {
            if(msg.contains("level") || msg.contains("gained"))
                return colorize(GREEN_FG, lastPart + append);
        }
        else if(name.contains("bot.action") || name.contains("bot.activity"))
        {
            if(msg.contains("caught") || msg.contains("success") || msg.contains("evolved") || msg.contains("looted"))
                return colorize(GREEN_FG, lastPart + append);
            else if(msg.contains("error") || msg.contains("escape") || msg.contains("got away") || msg.contains("missed") || msg.contains("could not"))
                return colorize(MAGENTA_FG, lastPart + append);
        }
        else if(name.contains("com.ashurex.pokemon.bot.listener"))
        {
            return colorize(CYAN_FG, lastPart + append);
        }


        if(name.startsWith("com.ashurex.pokemon"))
        {
            return lastPart + append;
        }

        return name  + append;
    }

    private String colorize(final String color, final String message)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(ESC_START);
        sb.append(color);
        sb.append(ESC_END);
        sb.append(message);
        sb.append(SET_DEFAULT_COLOR);

        return sb.toString();
    }

    private String getLogLevel(final Level level)
    {
        final String colorCode;

        switch(level.levelInt)
        {
            case Level.INFO_INT:
                colorCode = BOLD + BLUE_FG;
                break;
            case Level.WARN_INT:
                colorCode = BOLD + YELLOW_FG;
                break;
            case Level.ERROR_INT:
                colorCode = BOLD + RED_FG;
                break;
            default:
                return level.levelStr;
        }

        return colorize(colorCode, level.levelStr);
    }
}

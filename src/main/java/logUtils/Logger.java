package logUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

public class Logger
{
    private static boolean shouldLog = false;

    private static SimpleDateFormat time = new SimpleDateFormat("h:mm:ss");

    private Logger() {}

    public static void log(String message)
    {
        if (shouldLog)
            System.out.println(getTime() + " " + message);
    }

    public static void setShouldLog(boolean log) { shouldLog = log; }

    private static String getTime()
    {
        return "[" + LocalDateTime.now().getHour() + ":" +
            LocalDateTime.now().getMinute() + ":" +
            LocalDateTime.now().getSecond() + "]";
    }
}

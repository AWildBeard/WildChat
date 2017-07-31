package logUtils;

import java.time.LocalDateTime;
import java.util.concurrent.LinkedBlockingQueue;

public class Logger
{
    private static boolean shouldLog = false,
        threadStarted = false;

    private static LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

    private Logger() {}

    public static synchronized void log(String message)
    {
        if (! threadStarted && shouldLog)
        {
            new Thread(() ->
            {
                while (true)
                {
                    if (messages.size() > 0)
                    {
                        try
                        {
                            System.out.println(getTime() + " " + messages.take());
                        }
                        catch (InterruptedException e)
                        {
                            log(e.getMessage());
                        }
                    }
                }
            }).start();
            threadStarted = true;
        }
        messages.add(message);
    }

    public static void setShouldLog(boolean log) { shouldLog = log; }

    private static String getTime()
    {
        return "[" + LocalDateTime.now().getHour() + ":" +
            LocalDateTime.now().getMinute() + ":" +
            LocalDateTime.now().getSecond() + "]";
    }
}

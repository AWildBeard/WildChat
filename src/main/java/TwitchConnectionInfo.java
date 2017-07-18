public class TwitchConnectionInfo
{
    private static String host = "irc.chat.twitch.tv";

    private static int port = 6667;

    private TwitchConnectionInfo() {}

    public static String getHost() { return host; }

    public static int getPort() { return port; }
}

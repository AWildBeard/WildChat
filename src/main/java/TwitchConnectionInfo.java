public class TwitchConnectionInfo
{
    private final static String IRC_CHAT_TWITCH_TV = "irc.chat.twitch.tv";

    private final static int PORT = 6667;

    private TwitchConnectionInfo() {}

    public static String getIrcChatTwitchTv() { return IRC_CHAT_TWITCH_TV; }

    public static int getPort() { return PORT; }
}

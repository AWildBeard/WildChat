import java.util.ArrayList;

public class Session
{
    private static String channel = null;
    private String clientColor = null, clientDisplayName = null;

    private ArrayList<String> badgeSignatures = null;

    public Session() { clearData(); }

    public static String getChannel() { return channel; }

    public ArrayList<String> getBadgeSignatures() { return badgeSignatures; }

    public String getClientColor() { return clientColor; }

    public String getClientDisplayName() { return clientDisplayName; }

    public void setChannel(String newChannel) { channel = newChannel;}

    public void setBadgeSignatures(ArrayList<String> newBadgeSigs) { badgeSignatures = newBadgeSigs; }

    public void setClientColor(String newClientColor) { clientColor = newClientColor; }

    public void setClientDisplayName(String newClientDisplayName) { clientDisplayName = newClientDisplayName; }

    public static void clearData()
    {
        channel = null;
    }
}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Session
{
    private static String channel = null;
    private String clientColor = null, clientDisplayName = null;

    private ArrayList<String> badgeSignatures = null;

    private Map<String, String> emoteCodesAndIDs = new HashMap<>();

    boolean mapSet = false;

    public Session() { clearData(); }

    public static String getChannel() { return channel; }

    public ArrayList<String> getBadgeSignatures() { return badgeSignatures; }

    public String getClientColor() { return clientColor; }

    public String getClientDisplayName() { return clientDisplayName; }

    public Map<String, String> getEmoteCodesAndIDs() { return emoteCodesAndIDs; }

    public boolean isMapSet() { return mapSet; }

    public void setChannel(String newChannel) { channel = newChannel;}

    public void setBadgeSignatures(ArrayList<String> newBadgeSigs) { badgeSignatures = newBadgeSigs; }

    public void setClientColor(String newClientColor) { clientColor = newClientColor; }

    public void setClientDisplayName(String newClientDisplayName)
    { clientDisplayName = newClientDisplayName; }

    public void setEmoteCodesAndIDs(Map<String, String> emoteCodesAndIDs)
    {
        if (mapSet)
            return;

        this.emoteCodesAndIDs = emoteCodesAndIDs;
        mapSet = true;
    }

    public static void clearData()
    {
        channel = null;
    }
}

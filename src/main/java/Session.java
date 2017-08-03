import java.util.ArrayList;

public class Session
{
    private String channel = null, clientColor = null, clientDisplayName = null;

    private ArrayList<String> badgeSignatures = null;

    public Session(){}

    public String getChannel() { return channel; }

    public ArrayList<String> getBadgeSignatures() { return this.badgeSignatures; }

    public String getClientColor() { return this.clientColor; }

    public String getClientDisplayName() { return this.clientDisplayName; }

    public void setChannel(String channel) { this.channel = channel;}

    public void setBadgeSignatures(ArrayList<String> badgeSignatures) { this.badgeSignatures = badgeSignatures; }

    public void setClientColor(String clientColor) { this.clientColor = clientColor; }

    public void setClientDisplayName(String clientDisplayName) { this.clientDisplayName = clientDisplayName; }
}

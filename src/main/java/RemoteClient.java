import java.util.ArrayList;

// Data for the remote client. Contains things like their badges and custom color. etc.
public class RemoteClient
{
    private ArrayList<String> badges = new ArrayList<>();

    private ArrayList<Integer> badgeVers = new ArrayList<>();

    private String color;

    private String userName;

    private boolean mod, subscriber, turbo;

    private String userType;

    private String userID;

    public RemoteClient(ArrayList<String> badges, ArrayList<Integer> badgeVers, String color, String userName)
    {
        setBadges(badges, badgeVers);
        setColor(color);
        setUserName(userName);
    }

    public RemoteClient(ArrayList<String> badges, ArrayList<Integer> badgeVer, String color, String userName,
                        boolean mod, boolean subscriber, boolean turbo, String userType,
                        String userID)
    {
        setBadges(badges, badgeVer);
        setColor(color);
        setUserName(userName);
        setMod(mod);
        setSubscriber(subscriber);
        setTurbo(turbo);
        setUserType(userType);
        setUserID(userID);
    }

    // Mutators
    public void setBadges(ArrayList<String> badges, ArrayList<Integer> badgeVers)
    {
        for (String badge : badges)
            for (String correctBadge : Badges.getValidBadges())
                if (badge.equals(correctBadge))
                    addBadge(badge, badgeVers.get(badges.indexOf(badge)));
    }

    public void addBadge(String badge, Integer badgeVer)
    {
        // Simply prevent from adding the same badge twice
        if (! badges.contains(badge))
        {
            badges.add(badge);
            badgeVers.add(badgeVer);
        }
    }

    public void setColor(String color)
    {
        if (! (color.length() == 7))
            throw new IllegalArgumentException("Wrong RGB hex code length");

        else
            this.color = color;
    }

    public void setUserName(String userName) { this.userName = userName; }

    public void setMod(boolean mod) { this.mod = mod; }

    public void setSubscriber(boolean subscriber) { this.subscriber = subscriber; }

    public void setTurbo(boolean turbo) { this.turbo = turbo; }

    public void setUserType(String userType) { this.userType = userType; }

    public void setUserID(String userID) {this.userID = userID; }

    // Accessors

    public ArrayList<String> getBadges() { return this.badges; }

    public ArrayList<Integer> getBadgeVers() { return this.badgeVers; }

    public String getColor() { return this.color; }

    public String getUserName() { return this.userName; }

    public boolean isMod() { return mod; }

    public boolean isSubscriber() { return subscriber; }

    public boolean hasTurbo() { return turbo; }

    public String getUserType() { return userType;}

    public String getUserID() { return userID; }
}

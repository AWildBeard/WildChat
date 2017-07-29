import javafx.scene.image.Image;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static logUtils.Logger.log;

public class HandleData
{
    private String data = null,
        privMsgData = null,
        userName = null,
        userChannel = null,
        displayName = null,
        userNameColor = null;

    private boolean isPrivMsg = false,
        isUserJoinMsg = false,
        isUserLeaveMsg = false,
        isSucessfulConnectionNotification = false,
        isUserStateUpdate = false,
        isLocalMessage = false;

    private char[] rawData;

    private StringBuilder sb = new StringBuilder();

    public HandleData(String data)
    {
        this.data = data;
        rawData = data.toCharArray();
        determineMessageType();
    }

    private void determineMessageType()
    {
        if (data.contains("PRIVMSG"))
            isPrivMsg = true;

        else
        {
            isPrivMsg = false;
            isUserJoinMsg = data.contains("JOIN");
            isUserLeaveMsg = data.contains("PART");
            isSucessfulConnectionNotification = data.contains("001");
            isUserStateUpdate = data.contains("USERSTATE");
            isLocalMessage = data.substring(0, 4).contains("EEE");
        }
    }

    // PRIVMSG and USERSTATE
    public String getPrivMsgData()
    {

        if ((isPrivMsg || isUserStateUpdate) && privMsgData == null)
        {
            String message = null;
            int categoryStart = 0;

            // Grab the message
            categoryStart = (data.indexOf(':', data.indexOf("PRIVMSG")));
            message = data.substring(categoryStart + 1);

            // Remove EOL chars from the message
            privMsgData = message.substring(0, message.length() - 1);
        }

        return  privMsgData;
    }

    public String getUserNameColor()
    {
        if ((isPrivMsg || isUserStateUpdate) && userNameColor == null)
        {
            int categoryStart = 0, endOfCategoryLocation = 0;

            // Test for color
            categoryStart = data.indexOf("color=") + 6; // Always 6. Length of color declaration
            endOfCategoryLocation = data.indexOf(';', categoryStart);

            if (! (categoryStart == endOfCategoryLocation))
            { // Message has color data
                sb = new StringBuilder(); // Clear the StringBuilder
                for (int count = categoryStart ; count <= endOfCategoryLocation ; count++)
                {
                    if (count == endOfCategoryLocation) // The end of the color field
                    {
                        userNameColor = sb.toString();
                        break;
                    }

                    else
                        sb.append(rawData[count]);
                }
            }

            log("Calculated displayColor: " + userNameColor);
        }

        return userNameColor;
    }

    public String getDisplayName()
    {
        if ((isPrivMsg || isUserStateUpdate) && displayName == null)
        {
            // Grab the displayName
            int categoryStart = data.indexOf("display-name=") + 13;
            int endOfCategoryLocation = data.indexOf(';', categoryStart);
            displayName = data.substring(categoryStart, endOfCategoryLocation);
            log("Calculated displayName: " + displayName);
        }

        return displayName;
    }

    // TODO: refine
    public ArrayList<Image> getBadges()
    {
        ArrayList<Image> badges = null;

        if (isPrivMsg || isUserStateUpdate)
        {
            badges = new ArrayList<>();

            // Test for badges
            int categoryStart = data.indexOf("badges=") + 7; // Always 7. Length of badges declaration
            int endOfCategoryLocation = data.indexOf(';', categoryStart);

            if (! (categoryStart == endOfCategoryLocation))
            { // Message has badges data
                sb = new StringBuilder(); // Clear the StringBuilder
                for (int count = categoryStart; count <= endOfCategoryLocation; count++)
                {
                    // End of badges
                    if (count == endOfCategoryLocation)
                    {
                        log("No badges found");
                        break;
                    }

                    // Found badge name end and beginning of badge version
                    if (rawData[count] == '/')
                    {
                        // End of badge name found
                        String badgeNameString = sb.toString();

                        if (! Badges.getValidBadges().contains(badgeNameString))
                        {
                            log("Invalid badge found: " + badgeNameString);
                            sb = new StringBuilder();
                            continue;
                        }

                        count++; // Skip the /
                        sb = new StringBuilder(); // Clear the StringBuilder

                        while (rawData[count] != ',' && rawData[count] != ';')
                            sb.append(rawData[count++]);

                        String badgeVersion = sb.toString();

                        String key = badgeNameString + "/" + badgeVersion;
                        String value = Badges.getValue(key);

                        try
                        {
                            if (Badges.hasBadge(key))
                            {
                                log("Already have key: " + key);
                                badges.add(Badges.getBadge(key));
                            }
                            else
                            {
                                log("Do not have key: " + key);
                                Image badge = new Image(new URL(value).openStream());
                                Badges.cacheBadge(badge, key);
                                badges.add(badge);
                            }
                        }
                        catch (IOException z)
                        {
                            log("Didn't find icon for " + key);
                        }

                        sb = new StringBuilder(); // Clear the StringBuilder
                    }
                    else
                        sb.append(rawData[count]);
                }
            }

            log("Calculated badges");
        }

        return badges;
    }

    public ArrayList<String> getBadgeSignatures()
    {
        ArrayList<String> badgeSignatures = null;

        if (isPrivMsg || isUserStateUpdate)
        {
            badgeSignatures = new ArrayList<>();

            // Test for badges
            int categoryStart = data.indexOf("badges=") + 7; // Always 7. Length of badges declaration
            int endOfCategoryLocation = data.indexOf(';', categoryStart);

            if (! (categoryStart == endOfCategoryLocation))
            { // Message has badges data
                for (int count = categoryStart; count <= endOfCategoryLocation; count++)
                {
                    // End of badges
                    if (count == endOfCategoryLocation)
                        break;

                    // Found badge name end and beginning of badge version
                    if (rawData[count] == '/')
                    {
                        // End of badge name found
                        String badgeNameString = sb.toString();

                        if (! Badges.getValidBadges().contains(badgeNameString))
                        {
                            sb = new StringBuilder();
                            continue;
                        }

                        count++; // Skip the /
                        sb = new StringBuilder(); // Clear the StringBuilder

                        while (rawData[count] != ',' && rawData[count] != ';')
                            sb.append(rawData[count++]);

                        String badgeVersion = sb.toString();

                        badgeSignatures.add(badgeNameString + "/" + badgeVersion);

                        sb = new StringBuilder(); // Clear the StringBuilder
                    }
                    else
                        sb.append(rawData[count]);
                }
            }

            log("Calculated badge signatures");
        }

        return badgeSignatures;
    }
    // END TODO

    // PART and JOIN
    public String getUserName()
    {
        if ((isUserJoinMsg || isUserLeaveMsg) && userName == null)
        {
            int nameStart = (data.indexOf(':'));
            int endOfNameLocation = (data.indexOf('!', nameStart));
            userName = data.substring(nameStart + 1, endOfNameLocation);
            log("Calculated userName: " + userName);
        }

        return userName;
    }

    public String getChannel()
    {
        if ((isUserJoinMsg || isUserLeaveMsg) && userChannel == null)
        {
            int channelStart = data.indexOf('#');
            userChannel = data.substring(channelStart, data.length() - 1);
            log("Calculated userChannel: " + userChannel);
        }

        return userChannel;
    }

    public boolean isUserJoinMsg() { return isUserJoinMsg; }

    public boolean isPrivMsg() { return isPrivMsg; }

    public boolean isUserLeaveMsg() { return isUserLeaveMsg; }

    public boolean isSucessfulConnectionNotification() { return isSucessfulConnectionNotification; }

    public boolean isUserStateUpdate() { return isUserStateUpdate; }

    public boolean isLocalMessage() { return isLocalMessage; }
}

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static logUtils.Logger.log;

public class HandleData
{
    private String data = null,
        userName = null,
        userNameForPRIVMSG,
        userChannel = null,
        displayName = null,
        userNameColor = null,
        emoteDownloadURL = "http://static-cdn.jtvnw.net/emoticons/v1/%s/1.0";

    private boolean isPrivMsg = false,
        isUserJoinMsg = false,
        isUserLeaveMsg = false,
        isSucessfulConnectionNotification = false,
        isUserStateUpdate = false,
        hasEmoteData = false,
        isLocalMessage = false;

    private char[] rawData;

    private StringBuilder sb = new StringBuilder();

    private ArrayList<Node> privMsgData = null;

    public HandleData(String data)
    {
        this.data = data;
        rawData = data.toCharArray();
        determineMessageType();
    }

    private void determineMessageType()
    {
        if (data.contains("PRIVMSG"))
        {
            isPrivMsg = true;
            hasEmoteData = ! data.contains("emotes=;");
        }

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
    public ArrayList<Node> getPrivMsgData()
    {
        if ((isPrivMsg || isUserStateUpdate) && privMsgData == null)
        {
            privMsgData = new ArrayList<>();
            // Grab the message
            int categoryStart = data.indexOf(":", data.indexOf("PRIVMSG")) + 1;
            String message = data.substring(categoryStart);
            char[] rawMessage = message.substring(0, message.length() - 1).toCharArray();

            if (hasEmoteData)
            {
                ArrayList<String> emoteIDs = new ArrayList<>();
                ArrayList<String> emoteIndex = new ArrayList<>();
                int emoteStart = data.indexOf("emotes=") + 7;
                int endEmoteCategory = data.indexOf(';', emoteStart) + 1;
                StringBuilder sb = new StringBuilder();

                char[] emoteDataChars = data.substring(emoteStart, endEmoteCategory).toCharArray();

                int indexOfEmoteID = -1;
                int countOfEmoteIndex = 0;
                for (char c : emoteDataChars)
                {
                    if (c == ':')
                    {
                        emoteIDs.add(sb.toString());
                        sb = new StringBuilder();
                        indexOfEmoteID++;
                        continue;
                    }
                    if (c == ',')
                    {
                        emoteIndex.add(sb.toString());
                        emoteIDs.add(emoteIDs.get(indexOfEmoteID));
                        countOfEmoteIndex++;
                        sb = new StringBuilder();
                        continue;
                    }
                    if (c == '/')
                    {
                        emoteIndex.add(sb.toString());
                        countOfEmoteIndex++;
                        sb = new StringBuilder();
                        continue;
                    }
                    if (c == ';')
                    {
                        emoteIndex.add(sb.toString());
                        sb = new StringBuilder();
                        countOfEmoteIndex++;
                        break;
                    }

                    sb.append(c);
                }

                for (String emoteIndexes : emoteIndex)
                {
                    log(emoteIndexes);
                }

                try
                {
                    for (String id : emoteIDs)
                    {
                        if (! Emotes.hasEmote(id))
                        {
                            log("Getting emote: " + id);
                            Emotes.cacheEmote(new Image(new URL(String.format(emoteDownloadURL, id)).openStream()), id);
                        }
                        else
                            log("Already have emote: " + id);
                    }
                }
                catch (IOException e)
                {
                    log(e.getMessage());
                }

                sb = new StringBuilder();

                int[][] emoteIndexes = new int[countOfEmoteIndex][2];
                int firstNumber = 0;
                for (String combinedIndex : emoteIndex)
                {
                    int indexGroupLength = combinedIndex.toCharArray().length;
                    int index = 0;
                    for (char c : combinedIndex.toCharArray())
                    {
                        index++;
                        if (c == '-')
                        {
                            emoteIndexes[firstNumber][0] = Integer.parseInt(sb.toString());
                            sb = new StringBuilder();
                            continue;
                        }
                        if (index == indexGroupLength)
                        {
                            emoteIndexes[firstNumber][1] = (Integer.parseInt(sb.toString()) + 1); // + 1 to include last char
                            sb = new StringBuilder();
                            break;
                        }
                        sb.append(c);
                    }
                    firstNumber++;
                }

                for (int[] row : emoteIndexes)
                {
                    for (int index : row)
                    {
                        log(String.valueOf(index));
                    }
                }
            }
            else
            {
                int lastChar = rawMessage.length;
                int index = 0;
                for (char c : rawMessage)
                {
                    index++;
                    if ( c != 32)
                    {
                        sb.append(c);
                    }

                    if (c == 32 || index == lastChar)
                    {
                        privMsgData.add(new Label(sb.toString()));
                        sb = new StringBuilder();
                    }
                }
            }
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

    public String getUserNameForPRIVMSG()
    {
        if (isPrivMsg && userNameForPRIVMSG == null)
        {
            int nameStart = data.indexOf(":", data.indexOf("user-type="));
            int endOfNameLocation = data.indexOf("!", nameStart);
            userNameForPRIVMSG = data.substring(nameStart + 1, endOfNameLocation);
            log("Calculated userName: " + userNameForPRIVMSG);
        }

        return userNameForPRIVMSG;
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

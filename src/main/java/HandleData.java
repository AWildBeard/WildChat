import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static logUtils.Logger.log;

public class HandleData
{
    private final String CLIENT_ID = "fb7mlvnq5fgh7isjrx0ce14f27f6nq",
        EMOTE_DOWNLOAD_URL = "http://static-cdn.jtvnw.net/emoticons/v1/%s/1.0";

    private String data = null;

    private boolean isPrivMsg = false,
        isUserJoinMsg = false,
        isUserLeaveMsg = false,
        isSuccessfulConnectMsg = false,
        isUserStateUpdate = false,
        hasEmoteData = false,
        isRoomstateData = false,
        isLocalMessage = false;

    private char[] rawData;

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
            isSuccessfulConnectMsg = data.contains("001") || data.contains("376") || data.contains("002");
            isUserStateUpdate = data.contains("USERSTATE");
            isLocalMessage = data.substring(0, 4).contains("EEE");
        }
    }

    // PRIVMSG and USERSTATE
    public ArrayList<Node> getPrivMsgData()
    {
        ArrayList<Node> privMsgData = null;

        if ((isPrivMsg || isUserStateUpdate))
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
                        sb.setLength(0);
                        indexOfEmoteID++;
                        continue;
                    }
                    if (c == ',')
                    {
                        emoteIndex.add(sb.toString());
                        emoteIDs.add(emoteIDs.get(indexOfEmoteID));
                        countOfEmoteIndex++;
                        sb.setLength(0);
                        continue;
                    }
                    if (c == '/')
                    {
                        emoteIndex.add(sb.toString());
                        countOfEmoteIndex++;
                        sb.setLength(0);
                        continue;
                    }
                    if (c == ';')
                    {
                        emoteIndex.add(sb.toString());
                        sb.setLength(0);
                        countOfEmoteIndex++;
                        break;
                    }

                    sb.append(c);
                }

                // Get emotes in the message from twitch
                try
                {
                    for (String id : emoteIDs)
                    {
                        if (! Emotes.hasEmote(id))
                        {
                            log("Getting emote: " + id);
                            Emotes.cacheEmote(new Image(new URL(String.format(EMOTE_DOWNLOAD_URL, id)).openStream()), id);
                        }
                        else
                            log("Already have emote: " + id);
                    }
                }
                catch (IOException e)
                {
                    log(e.getMessage());
                }

                sb.setLength(0);

                int[][] emoteIndexes = new int[countOfEmoteIndex][2];
                int firstNumber = 0;
                for (String combinedIndex : emoteIndex)
                {
                    int indexGroupLength = combinedIndex.toCharArray().length;
                    int index = 0;
                    for (char c : combinedIndex.toCharArray())
                    {
                        index++;
                        if (c != '-')
                        {
                            sb.append(c);
                        }
                        if (c == '-')
                        {
                            emoteIndexes[firstNumber][0] = Integer.parseInt(sb.toString());
                            sb.setLength(0);
                            continue;
                        }
                        if (index == indexGroupLength)
                        {
                            emoteIndexes[firstNumber][1] = Integer.parseInt(sb.toString()); // + 1 to include last char
                            sb.setLength(0);
                        }
                    }
                    firstNumber++;
                }

                sb = new StringBuilder();

                // Add the words to the final message and all emotes too.
                int lasChar = rawMessage.length - 1;
                boolean emoteDetected;
                for (int index = 0 ; index < rawMessage.length ; index++)
                {
                    emoteDetected = false;
                    char c = rawMessage[index];

                    int count = 0; // row count
                    for (int[] row : emoteIndexes)
                    {
                        if (row[0] == index)
                        {
                            log("Emote detected");
                            emoteDetected = true;
                            privMsgData.add(new ImageView(Emotes.getEmote(emoteIDs.get(count))));
                            while (index != row[1])
                                index++; // skip over the emote data
                        }
                        count++;
                    }

                    if (c != 32 && !emoteDetected)
                    {
                        sb.append(c);
                    }

                    if ((c == 32 || index == lasChar) && !emoteDetected)
                    {
                        privMsgData.add(new Label(sb.toString()));
                        sb.setLength(0);
                    }
                }
                log("Finished emote operation..");
            }
            else
            {
                int lastChar = rawMessage.length;
                int index = 0;
                StringBuilder sb = new StringBuilder();
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
                        sb.setLength(0); // Clear the string builder
                    }
                }
            }
        }

        return  privMsgData;
    }

    public String getUserNameColor()
    {
        String userNameColor = null;

        if ((isPrivMsg || isUserStateUpdate))
        {
            int categoryStart = 0, endOfCategoryLocation = 0;
            StringBuilder sb = new StringBuilder();

            // Test for color
            categoryStart = data.indexOf("color=") + 6; // Always 6. Length of color declaration
            endOfCategoryLocation = data.indexOf(';', categoryStart);

            if (! (categoryStart == endOfCategoryLocation))
            { // Message has color data
                for (int count = categoryStart ; count <= endOfCategoryLocation ; count++)
                {
                    if (count != endOfCategoryLocation)
                        sb.append(rawData[count]);

                    else
                        userNameColor = sb.toString();
                }
            }

            log("Calculated displayColor: " + userNameColor);
        }

        return userNameColor;
    }

    public String getDisplayName()
    {
        String displayName = null;

        if ((isPrivMsg || isUserStateUpdate))
        {
            // Grab the displayName
            int categoryStart = data.indexOf("display-name=") + 13,
                endOfCategoryLocation = data.indexOf(';', categoryStart);

            if (categoryStart == endOfCategoryLocation)
                return getUserName();

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

            for (String badge : getBadgeSignatures())
            try
            {
                if (Badges.hasBadge(badge))
                {
                    log("Already have key: " + badge);
                    badges.add(Badges.getBadge(badge));
                }
                else
                {
                    log("Do not have key: " + badge);
                    Image badgeImage = new Image(new URL(Badges.getValue(badge)).openStream());
                    Badges.cacheBadge(badgeImage, badge);
                    badges.add(badgeImage);
                }
            }
            catch (IOException z)
            {
                log("Didn't find icon for " + badge);
            }
        }

        return badges;
    }

    public ArrayList<String> getBadgeSignatures()
    {
        ArrayList<String> badgeSignatures = null;

        if (isPrivMsg || isUserStateUpdate)
        {
            badgeSignatures = new ArrayList<>();
            StringBuilder sb = new StringBuilder();

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
                            sb.setLength(0);
                            continue;
                        }

                        count++; // Skip the /
                        sb.setLength(0); // Clear the StringBuilder

                        while (rawData[count] != ',' && rawData[count] != ';')
                            sb.append(rawData[count++]);

                        String badgeVersion = sb.toString();

                        badgeSignatures.add(badgeNameString + "/" + badgeVersion);

                        sb.setLength(0); // Clear the StringBuilder
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
        String userName = null;

        if (isPrivMsg)
        {
            int nameStart = data.indexOf(":", data.indexOf("user-type="));
            int endOfNameLocation = data.indexOf("!", nameStart);
            userName = data.substring(nameStart + 1, endOfNameLocation);
            log("Calculated userName: " + userName);
        }
        else if ((isUserJoinMsg || isUserLeaveMsg))
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
        String userChannel = null;

        if ((isUserJoinMsg || isUserLeaveMsg || isRoomstateData))
        {
            int channelStart = data.indexOf('#');
            userChannel = data.substring(channelStart, data.length() - 1);
            log("Calculated userChannel: " + userChannel);
        }

        return userChannel;
    }

    public Map<String, String> getEmoteCodesAndIDs()
    {
        HashMap<String, String> map = null;
        if (isUserStateUpdate)
        {
            map = new HashMap<>();
            int emoteSetStart = data.indexOf("emote-sets=") + 11;
            int emoteSetEndLocation = data.indexOf(';', emoteSetStart) + 1;
            StringBuilder sb = new StringBuilder();
            ArrayList<String> emoteSetIds = new ArrayList<>();

            char[] rawEmoteSet = data.substring(emoteSetStart, emoteSetEndLocation).toCharArray();

            for (char c : rawEmoteSet)
            {
                sb.append(c);
                if (c == ',' || c == ';')
                {
                    emoteSetIds.add(sb.toString());
                    sb.setLength(0);
                }
            }

            try
            {
                for (String emoteID : emoteSetIds)
                {
                    log("Get link: " + String.format("http://api.twitch.tv/kraken/chat/emoticon_images?emotesets=%s", emoteID));
                    URL url = new URL(String.format("http://api.twitch.tv/kraken/chat/emoticon_images?emotesets=%s", emoteID));
                    HttpClient client = new DefaultHttpClient();
                    HttpUriRequest request = new HttpGet(url.toURI());
                    request.addHeader("Accept:", "application/vnd.twitchtv.v5+json");
                    request.addHeader("Client-ID:", CLIENT_ID);
                    HttpResponse response = client.execute(request);
                    log(response.toString());
                }
            }
            catch(IOException | URISyntaxException e)
            {
                log(e.getMessage());
            }

        }
        return map;
    }

    public boolean isUserJoinMsg() { return isUserJoinMsg; }

    public boolean isPrivMsg() { return isPrivMsg; }

    public boolean isUserLeaveMsg() { return isUserLeaveMsg; }

    public boolean isSuccessfulConnectMsg() { return isSuccessfulConnectMsg; }

    public boolean isUserStateUpdate() { return isUserStateUpdate; }

    public boolean isLocalMessage() { return isLocalMessage; }

    public boolean isRoomstateData() { return isRoomstateData; }
}

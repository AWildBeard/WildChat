import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.Image;

import java.util.ArrayList;

import static logUtils.Logger.log;

public class DataProcessor implements Runnable
{
    private String data;

    public DataProcessor(String dataToProcess) { this.data = dataToProcess; }

    public void run()
    {
        if (data == null)
            return;

        log(data);

        HandleData dataHandler = new HandleData(data);

        if (dataHandler.isPrivMsg())
        {
            log("PRIVMSG received");

            // Compute all the stuffs
            final String displayName = dataHandler.getDisplayName();
            final String uName = dataHandler.getUserName();
            final String uColor = dataHandler.getUserNameColor();
            final ArrayList<Node> msgData = dataHandler.getPrivMsgData();
            final ArrayList<Image> badges = dataHandler.getBadges();

            Platform.runLater(() ->
            {
                WildChat.userList.addUser(uName, badges);
                WildChat.displayMessage(msgData, uColor, displayName, badges);
            });
        }
        else if (dataHandler.isUserStateUpdate())
        {
            log("User state update received");

            // Compute all the stuffs
            final String channel = dataHandler.getChannel();
            final ArrayList<Image> badges = dataHandler.getBadges();

            // Compute all the stuffs
            WildChat.session.setBadgeSignatures(dataHandler.getBadgeSignatures());
            WildChat.session.setClientColor(dataHandler.getUserNameColor());
            WildChat.session.setClientDisplayName(dataHandler.getDisplayName());
            WildChat.hasUserState = true;

            Platform.runLater(() -> WildChat.userList.addUser(WildChat.client.getNick(), badges));
        }
        else if (dataHandler.isSuccessfulConnectMsg())
        {
            if (!WildChat.connectionMessageReceived)
            {
                WildChat.connectionMessageReceived = true;
                log("Connected to twitch.tv");

                Platform.runLater(() -> WildChat.displayMessage("> Connected to twitch.tv..."));
            }
        }
        else if (dataHandler.isLocalMessage())
        {
            log("Incorrect user credentials entered"); // only local message sent out at this time
            WildChat.credentialsAvailable = false;

            Platform.runLater(() -> WildChat.displayMessage("> Incorrect login credentials entered!"));
        }
        else if (dataHandler.isRoomstateData())
        {
            log("Roomstate data received");
        }
        else if (dataHandler.isUserJoinMsg())
        {
            log("User join channel received");

            // Compute all the stuffs
            final String uName = dataHandler.getUserName();
            WildChat.connectedToChannel = true;

            Platform.runLater(() -> WildChat.userList.addUser(uName));
        }
        else if (dataHandler.isUserLeaveMsg())
        {
            log("User left channel received");

            // Compute all the stuffs
            final String uName = dataHandler.getUserName();
            Platform.runLater(() -> WildChat.userList.removeUser(uName));
        }
    }
}

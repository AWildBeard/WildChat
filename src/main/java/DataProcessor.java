import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import java.util.ArrayList;

import static logUtils.Logger.log;

public class DataProcessor implements Runnable
{
    private String data;

    private static Paint messageColor = Paint.valueOf(WildChat.textFill);

    private static Font font = new Font(WildChat.messageFontSize);

    public DataProcessor(String dataToProcess) { this.data = dataToProcess; }

    public void run()
    {
        if (data == null)
            return;

        log(data);

        HandleData dataHandler = new HandleData(data);

        if (dataHandler.isPrivMsg())
        {
            if (! WildChat.connectedToChannel)
                return;

            log("PRIVMSG received");

            // Compute all the stuffs
            final String displayName = dataHandler.getDisplayName();
            final String uName = dataHandler.getUserName();
            final String uColor = dataHandler.getUserNameColor();
            final ArrayList<Node> msgData = dataHandler.getPrivMsgData();
            final ArrayList<Image> badges = dataHandler.getBadges();

            final FlowPane holder = formatMessage(badges, displayName, uColor, msgData);

            Platform.runLater(() ->
            {
                WildChat.userList.addUser(uName, badges);
                WildChat.displayMessage(holder);
            });
        }
        else if (dataHandler.isUserStateUpdate())
        {
            log("User state update received");

            // Compute all the stuffs
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

                Platform.runLater(() -> WildChat.displayMessage("> Connected to twitch.tv!"));
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
            if (! WildChat.connectedToChannel)
            {
                WildChat.connectedToChannel = true;
                Platform.runLater(() -> WildChat.displayMessage("> Connected to " + Session.getChannel() + "!"));
            }

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

    public static FlowPane formatMessage(ArrayList<Image> badges, String displayName, String color, ArrayList<Node> msgData)
    {
        FlowPane holder = new FlowPane();
        Label userName = null, messagePreAppen = new Label(">"), messageSeperator = new Label(":");

        messagePreAppen.setFont(font);
        messagePreAppen.setTextFill(messageColor);
        messageSeperator.setFont(font);
        messageSeperator.setTextFill(messageColor);

        holder.setOrientation(Orientation.HORIZONTAL);
        holder.setHgap(WildChat.messageFontSize * 0.33);
        holder.getChildren().add(messagePreAppen);

        if (badges != null)
            for (Image icon : badges)
                holder.getChildren().add(new ImageView(icon));

        if (displayName != null)
        {
            userName = new Label(displayName);
            if (color != null)
                userName.setTextFill(Paint.valueOf(color));
            else
                userName.setTextFill(messageColor);
            userName.setFont(font);
            holder.getChildren().addAll(userName, messageSeperator);
        }

        for (Node node : msgData)
        {
            if (node instanceof Label)
            {
                ((Label) node).setFont(font);
                ((Label) node).setTextFill(messageColor);
            }
            holder.getChildren().add(node);
        }

        return holder;
    }
}

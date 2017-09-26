/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;

import static UISettings.ReadOnlyUISettings.*;
import static logUtils.Logger.log;

public class DataProcessor implements Runnable
{
    private String data;

    private HandleData dataHandler = null;

    public DataProcessor(String dataToProcess)
    {
        this.data = dataToProcess;
        dataHandler = new HandleData(data);
        if (! isInitialized())
            setSettings(WildChat.uiSettings);
    }

    public static FlowPane formatMessage(ArrayList<Image> badges, String displayName,
                                         String color, ArrayList<Node> msgData, boolean isWhisper)
    {
        FlowPane holder = new FlowPane();
        Label userName = null, messagePreAppen = new Label(WildChat.uiSettings.getMessagePrefix()),
                messageSeperator = new Label(":");

        messagePreAppen.setStyle("-fx-font-size:" + getMessageFontSize() + ";" +
                "-fx-text-fill: " + getTextFill() + ";");
        messageSeperator.setStyle("-fx-font-size: " + getMessageFontSize() + ";" +
                "-fx-text-fill: " + getTextFill() + ";");

        holder.setOrientation(Orientation.HORIZONTAL);
        holder.setHgap(WildChat.messageFontSize * 0.33);
        holder.getChildren().add(messagePreAppen);

        // Add badges
        if (badges != null)
        {
            for (Image icon : badges)
            {
                ImageView img = new ImageView(icon);
                holder.getChildren().add(img);
            }
        }

        // Add display name with color effects
        if (displayName != null)
        {
            userName = new Label(displayName);

            if (color != null)
                userName.setStyle("-fx-font-size: " + getMessageFontSize() + ";" +
                        "-fx-text-fill: " + color + ";");

            else
                userName.setStyle("-fx-font-size: " + getMessageFontSize() + ";" +
                        "-fx-text-fill: " + getTextFill() + ";");

            holder.getChildren().addAll(userName, messageSeperator);
        }

        boolean isFirst = true, isAction = false;
        for (Node node : msgData)
        {
            if (node instanceof Label)
            {
                String potentialLink = ((Label) node).getText();
                String linkBegin = null;
                if (potentialLink.length() > 9)
                    linkBegin = potentialLink.substring(0, 8);


                if (isFirst)
                {
                    isFirst = false;
    
                    if (potentialLink.contains("ACTION"))
                    {
                        log("Action message detected");
                        isAction = true;
                        continue;
                    }
                }

                if (isAction)
                {
                    node.setStyle("-fx-font-size: " + getMessageFontSize() + ";" +
                            "-fx-text-fill: " + getActionColor() + ";");
                } else if (isWhisper)
                {
                    node.setStyle("-fx-font-size: " + getMessageFontSize() + ";" +
                            "-fx-text-fill: " + getWhisperTextColor() + ";");
                } else
                {
                    node.setStyle("-fx-font-size: " + getMessageFontSize() + ";" +
                            "-fx-text-fill: " + getTextFill());
                }


                if (linkBegin != null && (linkBegin.contains("http://") || linkBegin.contains("https://")))
                {
                    if (potentialLink.matches(".+"))
                    { // Is a link
                        log("Found link!");
                        node.setStyle("-fx-text-fill: dodgerblue;" +
                                "-fx-underline: true;" +
                                "-fx-font-size: " + getUiFont() + ";");
                        node.setOnMouseClicked(event -> BareBonesBrowserLaunch.openURL(potentialLink));
                        node.setOnMouseEntered(event -> WildChat.userList.getScene().setCursor(Cursor.HAND));
                        node.setOnMouseExited(event -> WildChat.userList.getScene().setCursor(Cursor.DEFAULT));
                    }
                }
            }

            holder.getChildren().add(node);
        }

        return holder;
    }

    public void run()
    {
        if (data == null)
            return;

        log(data);

        if (dataHandler.isPrivMsg())
        {
            if (!WildChat.connectedToChannel)
                return;

            log("PRIVMSG received");

            // Compute all the stuffs
            final String displayName = dataHandler.getDisplayName();
            final String uName = dataHandler.getUserName();
            final String uColor = dataHandler.getUserNameColor();
            final ArrayList<Node> msgData = dataHandler.getPrivMsgData();
            final ArrayList<Image> badges = dataHandler.getBadges();

            final FlowPane holder = formatMessage(badges, displayName, uColor, msgData, false);

            Platform.runLater(() ->
            {
                WildChat.userList.addUser(uName, badges);
                WildChat.displayMessage(holder);
            });

        } else if (dataHandler.isWhisperMsg())
        {
            log("Whisper message received");

            // Compute all the stuffs
            final String displayName = dataHandler.getDisplayName();
            final String uName = dataHandler.getUserName();
            final String uColor = dataHandler.getUserNameColor();
            final ArrayList<Node> msgData = dataHandler.getWhisperMsgData();
            final ArrayList<Image> badges = dataHandler.getBadges();

            log(displayName);
            log(uName);
            log(uColor);
            log(msgData.toString());
            log(badges.toString());

            final FlowPane holder = formatMessage(badges, displayName, uColor, msgData, true);

            log(holder.toString());

            Platform.runLater(() ->
            {
                WildChat.userList.addUser(uName, badges);
                WildChat.displayMessage(holder);
            });

        } else if (dataHandler.isUserStateUpdate())
        {
            log("User state update received");

            // Compute all the stuffs
            final ArrayList<Image> badges = dataHandler.getBadges();
            final String displayName = dataHandler.getDisplayName();

            // Compute all the stuffs
            WildChat.session.setBadgeSignatures(dataHandler.getBadgeSignatures());
            WildChat.session.setClientColor(dataHandler.getUserNameColor());
            WildChat.session.setClientDisplayName(dataHandler.getDisplayName());
            WildChat.hasUserState = true;

            log("Map Set: " + WildChat.session.isMapSet());
            if (!WildChat.session.isMapSet()) // No spam twitch. Twitch no likey
            { WildChat.session.setEmoteCodesAndIDs(dataHandler.getEmoteCodesAndIDs()); }

            Platform.runLater(() -> WildChat.userList.addUser(displayName, badges));
        } else if (dataHandler.isSuccessfulConnectMsg())
        {
            if (!WildChat.connectionMessageReceived)
            {
                WildChat.connectionMessageReceived = true;
                log("Connected to twitch.tv");

                Platform.runLater(() -> WildChat.displayMessage("Connected to twitch.tv!"));
            }
        } else if (dataHandler.isLocalMessage())
        {
            log("Incorrect user credentials entered"); // only local message sent out at this time
            WildChat.credentialsAvailable = false;

            Platform.runLater(() ->
            {
                WildChat.displayMessage("Incorrect login credentials entered!");
                WildChat.displayMessage("You must restart this application to " +
                        "enter correct credentials for twitch.tv.");
            });
        } else if (dataHandler.isRoomstateData())
        {
            log("Roomstate data received");

        } else if (dataHandler.isUserJoinMsg())
        {
            log("User join channel received");

            // Compute all the stuffs
            final String uName = dataHandler.getUserName();
            if (!WildChat.connectedToChannel)
            {
                WildChat.connectedToChannel = true;
                Platform.runLater(() ->
                {
                    WildChat.displayMessage("Connected to " + Session.getChannel() + "!") ;
                    WildChat.title.set("WildChat - " + WildChat.VERSION + " - " + Session.getChannel());
                });
            }

            if (!uName.equals(WildChat.client.getNick()))
            { Platform.runLater(() -> WildChat.userList.addUser(uName)); }
        } else if (dataHandler.isUserLeaveMsg())
        {
            log("User left channel received");

            // Compute all the stuffs
            final String uName = dataHandler.getUserName();
            Platform.runLater(() -> WildChat.userList.removeUser(uName));
        }
    }
}

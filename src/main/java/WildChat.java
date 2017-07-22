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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import static logUtils.Logger.*;

public class WildChat extends Application
{
    private static String[] launchArgs;

    private TwitchConnect socketRunner = null;

    private Thread th;

    private boolean credentialError = false,
            credentialsAvailable = false,
            debug = true,
            connected = false;

    private Client client = null;

    private Session session = new Session();

    private Stage primaryStage = null;

    private static int messageCount = 0;

    public WildChat()
    {
        if (launchArgs.length > 0 && launchArgs[0].equals("--no-debug"))
            debug = false;

        setShouldLog(debug);

        String credentialFile = "$HOME/.WildChat/credentials";

        log("Testing read/write on " + credentialFile);
        if (! FileUtil.canReadWrite(credentialFile))
        {
            log("File " + credentialFile + " either does not exist or can't be read/wrote to");
            log("Attempting to create necessary dirs and file for " + credentialFile);
            credentialError = ! FileUtil.createFileWithDirs(credentialFile);
        }
        log((credentialError) ? "Can read/write " + credentialFile : "Can't read/write " + credentialFile);

        if (credentialsAvailable = FileUtil.hasData(credentialFile))
        {
            log("Credential file has data");
            try
            {
                ObjectInputStream is = new ObjectInputStream(new FileInputStream(new File(credentialFile)));
                client = (Client) is.readObject();
                is.close();
            }
            catch (IOException | ClassNotFoundException e )
            {
                credentialError = true;
                return;
            }
        }
        log((credentialsAvailable) ? "Credentials read in" : "No credential data found");
    }

    public void start(Stage primaryStage)
    {
        this.primaryStage = primaryStage;

        log((credentialError) ? "Credential error detected, showing error message" : "");
        if (credentialError)
            showCredentialError();

        log((! credentialsAvailable) ? "No credentials available, showing input fields" : "");
        if (! credentialsAvailable)
            askForCredentials();

        if (client == null)
        {
            log("Client not initialized!");
            System.exit(1);
        }

        socketRunner = new TwitchConnect(client);
        th = new Thread(socketRunner);

        GridPane mainContent = new GridPane();

        MenuBar menuBar = new MenuBar();
        Menu connections = new Menu("Connections"),
            settings = new Menu("Settings");
        MenuItem connect = new MenuItem("Connect"),
            disconnect = new MenuItem("Disconnect");
        ScrollPane messagePane = new ScrollPane();
        VBox messageHolder = new VBox();
        AnchorPane messageAnchor = new AnchorPane();
        ListView<String> userList = new ListView<>();
        TextField messageField = new TextField();
        ColumnConstraints column1Constraints = new ColumnConstraints(),
            column2Constraints = new ColumnConstraints();
        RowConstraints row1Constraints = new RowConstraints(),
            row2Constraints = new RowConstraints(),
            row3Constraints = new RowConstraints();

        messagePane.setPannable(false);
        connections.getItems().addAll(connect, disconnect);
        menuBar.getMenus().addAll(connections, settings);
        menuBar.setMaxHeight(22.0);
        messageField.setPromptText("Message");
        userList.setMaxWidth(250.0);
        userList.setPrefWidth(175.0);
        userList.setMinWidth(100.0);
        messageHolder.setAlignment(Pos.BOTTOM_LEFT);
        messagePane.setPrefWidth(450.0);
        messagePane.setMinWidth(200.0);
        messagePane.setMaxWidth(Double.MAX_VALUE);
        messagePane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagePane.setFitToWidth(true);
        messagePane.vvalueProperty().bind(messageHolder.heightProperty());
        messageAnchor.getChildren().add(messageHolder);
        AnchorPane.setBottomAnchor(messageHolder, 0.0);
        column1Constraints.setHgrow(Priority.ALWAYS);
        column1Constraints.setHalignment(HPos.LEFT);
        column1Constraints.setFillWidth(true);
        column2Constraints.setHgrow(Priority.ALWAYS);
        column2Constraints.setHalignment(HPos.LEFT);
        column2Constraints.setFillWidth(true);
        column2Constraints.setMaxWidth(250.0);
        row1Constraints.setVgrow(Priority.NEVER);
        row1Constraints.setValignment(VPos.TOP);
        row1Constraints.setFillHeight(false);
        row2Constraints.setVgrow(Priority.ALWAYS);
        row2Constraints.setValignment(VPos.BOTTOM);
        row2Constraints.setFillHeight(true);
        row3Constraints.setVgrow(Priority.NEVER);
        row3Constraints.setFillHeight(false);
        mainContent.getColumnConstraints().addAll(column1Constraints, column2Constraints);
        mainContent.getRowConstraints().addAll(row1Constraints, row2Constraints, row3Constraints);

        messagePane.setContent(messageHolder);
        mainContent.add(menuBar, 0, 0, 2, 1);
        mainContent.add(messagePane, 0, 1);
        mainContent.add(userList, 1, 1);
        mainContent.add(messageField, 0, 2, 2, 1);

        Scene root = new Scene(mainContent, 650, 400);

        settings.setOnAction(e ->
        {

        });

        connect.setOnAction(e ->
        {
            showConnectWindow();
        });

        messageField.setOnKeyPressed(event ->
        {
            String message = messageField.getText();
            if (event.getCode().equals(KeyCode.ENTER))
            {
                if (message.length() > 0)
                {
                    socketRunner.sendMessage("PRIVMSG " + session.getChannel() + " :" + message);
                    messageField.setText("");
                }
            }
        });

        socketRunner.getDataProperty().addListener(e ->
        {
            String data = socketRunner.getData();

            log(data);
            log("");

            StringBuilder finalMessage = new StringBuilder();

            boolean part = data.contains("PART"),
                join = data.contains("JOIN"),
                msg = data.contains("PRIVMSG"),
                connection = data.substring(0, 18).contains("001");

            if (connection)
            {
                log("Connected to twitch.tv");
                finalMessage.append("> Connected to twitch.tv");
                connected = true;
            }

            if (msg)
            {
                log("Message received");
                StringBuilder sb = new StringBuilder();
                char[] rawData = data.toCharArray();
                int categoryStart;
                int endOfCategoryLocation = 0;

                finalMessage.append("> ");

                // Test for badges
                categoryStart = data.indexOf("badges=") + 7; // Always 7. Length of badges declaration
                endOfCategoryLocation = data.indexOf(';', categoryStart);

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
                            finalMessage.append(sb.toString() + "/");
                            count++; // Skip the /

                            while (rawData[count] != ',' && rawData[count] != ';')
                                finalMessage.append(rawData[count++]);

                            finalMessage.append(" ");
                            sb = new StringBuilder(); // Clear the StringBuilder
                        }
                        else
                            sb.append(rawData[count]);
                    }
                }

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
                            finalMessage.append(sb.toString() + " " );
                            break;
                        }

                        else
                            sb.append(rawData[count]);
                    }
                }

                // Grab the username
                categoryStart = data.indexOf("display-name=") + 13;
                endOfCategoryLocation = data.indexOf(';', categoryStart);
                String username = data.substring(categoryStart, endOfCategoryLocation);

                // Test for emotes
                categoryStart = data.indexOf("emotes=", endOfCategoryLocation) + 7; // 7 emotes= length
                endOfCategoryLocation = data.indexOf(';', categoryStart);

                if (! (categoryStart == endOfCategoryLocation))
                { // Message has emote data
                    finalMessage.append("[ignoring emotes] ");
                }

                // Grab the message
                categoryStart = (data.indexOf(':', data.indexOf("PRIVMSG")));
                String message = data.substring(categoryStart + 1);

                finalMessage.append(username + ": " + message);
                // END TODO
            }

            if (part || join)
            {
                int nameStart = (data.indexOf(':'));
                int endOfNameLocation = (data.indexOf('!', nameStart));
                String username = data.substring(nameStart + 1, endOfNameLocation);

                if (part)
                {
                    log("User left channel received");
                    finalMessage.append("> " + username + " left the channel");
                }

                if (join)
                {
                    log("User join channel received");
                    finalMessage.append("> " + username + " joined the channel");
                }
            }

            Platform.runLater(() ->
            {
                messageHolder.getChildren().add(messageCount, new Text(finalMessage.toString()));
                messageCount++;
            });
        });

        this.primaryStage.setScene(root);
        this.primaryStage.setTitle("WildChat");
        this.primaryStage.setOnCloseRequest(e ->
        {
            log("Primary Stage Close");
        });
        this.primaryStage.show();

        th.start();
    }

    private void showConnectWindow()
    {
        Stage secondaryStage = new Stage();

        VBox contentHolder = new VBox();

        Text title = new Text("Enter a streamers name to join their channel");
        TextField streamerField = new TextField();
        streamerField.setPromptText("Streamer");
        Button confirmButton = new Button("Confirm");

        contentHolder.setSpacing(20.0);
        contentHolder.setAlignment(Pos.CENTER);

        contentHolder.getChildren().addAll(title, streamerField, confirmButton);

        confirmButton.setOnAction(e ->
        {
            String channel = streamerField.getText();

            if (connected)
                sendMessage("PART" + session.getChannel());

            if (channel.substring(0, 1).contains("#"))
            {
                sendMessage("JOIN " + channel);
            }
            else
            {
                channel = "#" + channel;
                sendMessage("JOIN " + channel);
            }

            session.setChannel(channel);
        });

        Scene root = new Scene(contentHolder, 400, 300);

        secondaryStage.setScene(root);
        secondaryStage.setTitle("Connect");
        secondaryStage.show();

    }

    public void showCredentialError()
    {
        log("Opening credential error window");
        VBox content = new VBox();
        Text message = new Text("Error with credential file. Contact @AWildBeard");
        Button closeButton = new Button("Okay");

        content.getChildren().addAll(message, closeButton);

        closeButton.setOnAction(e -> {
            log("Exiting!");
            System.exit(1);
        });

        content.setAlignment(Pos.CENTER);
        content.setSpacing(10.0);

        Scene root = new Scene(content, 400, 300);

        primaryStage.setScene(root);
        primaryStage.show();
    }

    // TODO: Implement
    public void askForCredentials()
    {
        Stage secondaryStage = new Stage();
        VBox contentHolder = new VBox();

        Text title = new Text("Please enter credentials");
        TextField userNameTextField = new TextField();
        userNameTextField.setPromptText("Username");
        TextField oauthTextField = new TextField();
        oauthTextField.setPromptText("OAUTH KEY");
        Button confirmButton = new Button("Confirm");

        contentHolder.setSpacing(20.0);
        contentHolder.setAlignment(Pos.CENTER);

        contentHolder.getChildren().addAll(title, userNameTextField, oauthTextField, confirmButton);

        confirmButton.setOnAction(e -> {
            try
            {
                client = new Client(oauthTextField.getText(), userNameTextField.getText());
            }
            catch (IllegalOauthKey y)
            {
                log("Bad OAUTH key entered");
                title.setText("Bad OAUTH key");
                return;
            }

            if (client.isReady())
            {
                log("Correct client data entered");
                secondaryStage.close();
            }
            else
            {
                log("Incorrect client data");
                title.setText("You did not enter correct values!");
            }
        });

        Scene root = new Scene(contentHolder, 400, 300);
        secondaryStage.setScene(root);
        secondaryStage.setTitle("Enter credentials");
        secondaryStage.setOnShown(e -> title.requestFocus());
        secondaryStage.setOnCloseRequest(e ->
        {
            if (client == null || ! client.isReady())
            {
                log("Credential window exit");
                System.exit(1);
            }
        });
        secondaryStage.showAndWait();
    }
    // END TODO

    public void sendMessage(String message) { socketRunner.sendMessage(message); }

    public static void main(String[] args)
    {
        launchArgs = args;
        launch(args);
    }
}

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
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static logUtils.Logger.*;

public class WildChat extends Application
{
    private static String[] launchArgs;

    private Stage primaryStage = null;

    private TwitchConnect socketRunner = null;

    private ExecutorService executor = Executors.newCachedThreadPool();

    private Client client = null;

    private Session session = new Session();

    private GridPane mainContent = new GridPane();

    private MenuBar menuBar  = new MenuBar();

    private Menu connections = new Menu("Connections"),
            settings = new Menu("Settings");

    private MenuItem connect = new MenuItem("Connect"),
            disconnect = new MenuItem("Disconnect");

    private ScrollPane messagePane = new ScrollPane(),
            userListPane = new ScrollPane();

    private UserList userList = new UserList();

    private VBox messageHolder = new VBox();

    private TextField messageField = new TextField();

    private ColumnConstraints column1Constraints = new ColumnConstraints(),
            column2Constraints = new ColumnConstraints();

    private RowConstraints row1Constraints = new RowConstraints(),
            row2Constraints = new RowConstraints(),
            row3Constraints = new RowConstraints();

    private static ArrayList<String> clientBadgeSignatures = new ArrayList<>();

    private static String clientColorValue = null,
                          clientsDisplayName = null;

    private boolean credentialsAvailable = false,
                    connectedToChannel = false,
                    hasUserState = false;

    private final String filePrefix = ".WildChat/",
                         credentials = "credentials.dat",
                         dotDirLocation = (System.getProperty("os.name").contains("Windows")) ?
                             BasicIO.getEnvVars("APPDATA") + "/" : BasicIO.getEnvVars("HOME") + "/",
                         credentialFile = dotDirLocation + filePrefix + credentials;

    public static volatile boolean connected = false;

    private static boolean connectionMessageReceived = false;

    public WildChat()
    {
        boolean canAccessCredentials = false,
                debug = true;

        if (launchArgs.length > 0 && launchArgs[0].equals("--no-debug"))
            debug = false;

        setShouldLog(debug);


        log("Testing read/write on " + credentialFile);
        if (! FileUtil.canReadWrite(credentialFile))
        {
            log("File " + credentialFile + " either does not exist or can't be read/wrote to");
            log("Attempting to create necessary dirs and file for " + credentialFile);
            canAccessCredentials = FileUtil.createFileWithDirs(credentialFile);
        }
        else
            canAccessCredentials = true;

        log((canAccessCredentials) ? "Can read/write " + credentialFile : "Can't read/write " + credentialFile);

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
                log("Unrecoverable credential read operation");
                return;
            }
        }
        log((credentialsAvailable) ? "Credentials read in" : "No credential data found");
    }

    @Override
    public void stop()
    {
        connected = false;
        executor.shutdownNow();
        log("ShutDown");
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage)
    {
        this.primaryStage = primaryStage;

        log((! credentialsAvailable) ? "No credentials available, showing input fields" : "");
        if (! credentialsAvailable)
            askForCredentials();

        if (client == null)
        {
            log("Client not initialized!");
            System.exit(1);
        }

        socketRunner = new TwitchConnect(client);

        messagePane.setPannable(false);
        connections.getItems().addAll(connect, disconnect);
        menuBar.getMenus().addAll(connections, settings);
        menuBar.setMaxHeight(22.0);
        messageField.setPromptText("Message");
        userListPane.setMaxWidth(250.0);
        userListPane.setPrefWidth(175.0);
        userListPane.setMinWidth(100.0);
        messageHolder.setSpacing(3.0);
        messagePane.setPrefWidth(450.0);
        messagePane.setMinWidth(100.0);
        messagePane.setMaxWidth(Double.MAX_VALUE);
        messagePane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagePane.setFitToWidth(true);
        messagePane.vvalueProperty().bind(messageHolder.heightProperty());
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
        userListPane.setContent(userList);
        userList.setSpacing(3.0);
        VBox.setMargin(userList, new Insets(4, 0, 4, 4));
        mainContent.add(menuBar, 0, 0, 2, 1);
        mainContent.add(messagePane, 0, 1);
        mainContent.add(userListPane, 1, 1);
        mainContent.add(messageField, 0, 2, 2, 1);

        Scene root = new Scene(mainContent, 650, 400);

        for (int count = 0 ; count <= 125 ; count++)
        {
            messageHolder.getChildren().add(count, new Text(" "));
        }

        disconnect.setOnAction(e -> disconnectFromChannel());

        connect.setOnAction(e -> showConnectWindow());

        messageField.setOnKeyPressed(event ->
        {
            if (event.getCode().equals(KeyCode.ENTER))
            {
                String message = messageField.getText().trim();
                char[] rawMessage = message.toCharArray();
                if (message.length() > 0)
                {
                    if (connectedToChannel)
                    {
                        sendMessage("PRIVMSG " + session.getChannel() + " :" + message);

                        if (! hasUserState)
                            displayMessage("> " + client.getNick() + ": " + message);

                        else
                        {
                            ArrayList<Image> clientImageBadges = null;
                            ArrayList<Node> messageNodes = new ArrayList<>();
                            StringBuilder sb = new StringBuilder();

                            if (clientBadgeSignatures.size() >= 1)
                            {
                                clientImageBadges = new ArrayList<>();

                                for (String badge : clientBadgeSignatures)
                                    clientImageBadges.add(Badges.getBadge(badge));
                            }

                            userList.addUser(client.getNick(), clientImageBadges);

                            int messageLength = message.length();
                            int index = 0;
                            for (char c : rawMessage)
                            {
                                index++;
                                if (c == 32)
                                {
                                    messageNodes.add(new Label(sb.toString()));
                                    sb = new StringBuilder();
                                    continue;
                                }

                                sb.append(c);

                                if (index == messageLength)
                                    messageNodes.add(new Label(sb.toString()));
                            }

                            displayMessage(messageNodes, clientColorValue, clientsDisplayName, clientImageBadges);
                        }
                    }
                    else
                    {
                        displayMessage("> You are not connected to a channel yet!");
                    }
                    messageField.clear();
                }
            }
        });

        socketRunner.getDataProperty().addListener(e ->
            executor.execute(() ->
            {
                String data = socketRunner.getData();

                log(data);

                HandleData dataHandler = new HandleData(data);

                if (dataHandler.isPrivMsg() && connectedToChannel)
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
                        userList.addUser(uName, badges);
                        displayMessage(msgData, uColor, displayName, badges);
                    });
                }
                else if (dataHandler.isUserStateUpdate())
                {
                    log("User state update received");

                    // Compute all the stuffs
                    clientBadgeSignatures = dataHandler.getBadgeSignatures();
                    clientColorValue = dataHandler.getUserNameColor();
                    clientsDisplayName = dataHandler.getDisplayName();
                    hasUserState = true;

                    // Compute all the stuffs
                    final String channel = dataHandler.getChannel();
                    final ArrayList<Image> badges = dataHandler.getBadges();

                    if (!connectedToChannel)
                    {
                        Platform.runLater(() -> displayMessage("> Connected to channel: " + channel));
                        connectedToChannel = true;
                    }

                    Platform.runLater(() -> userList.addUser(client.getNick(), badges));
                }
                else if (dataHandler.isSuccessfulConnectMsg())
                {
                    if (!connectionMessageReceived)
                    {
                        connectionMessageReceived = true;
                        log("Connected to twitch.tv");
                        Platform.runLater(() ->
                        {
                            displayMessage("> Connected to twitch.tv");
                            displayMessage("> Please join a channel!");
                        });
                    }
                }
                else if (dataHandler.isLocalMessage())
                {
                    log("Incorrect user credentials entered"); // only local message sent out at this time
                    Platform.runLater(() -> displayMessage("Incorrect login credentials!"));
                    credentialsAvailable = false;
                }
                else if (dataHandler.isRoomstateData())
                {
                    log("Roomstate data received");

                    // Compute all the stuffs
                    final String channel = dataHandler.getChannel();

                    if (!connectedToChannel)
                    {
                        Platform.runLater(() -> displayMessage("> Connected to channel: " + channel));
                        connectedToChannel = true;
                    }
                }
                else if (dataHandler.isUserJoinMsg())
                {
                    log("User join channel received");

                    // Compute all the stuffs
                    final String uName = dataHandler.getUserName();
                    final String channel = dataHandler.getChannel();

                    if (!connectedToChannel)
                    {
                        Platform.runLater(() -> displayMessage("> Connected to channel: " + channel));
                        connectedToChannel = true;
                    }

                    Platform.runLater(() -> userList.addUser(uName));
                }
                else if (dataHandler.isUserLeaveMsg())
                {
                    log("User left channel received");

                    // Compute all the stuffs
                    final String uName = dataHandler.getUserName();
                    Platform.runLater(() -> userList.removeUser(uName));
                }
            })
        );

        this.primaryStage.setScene(root);
        this.primaryStage.setTitle("WildChat");
        this.primaryStage.setOnCloseRequest(e ->
        {
            log("Primary Stage Close");
        });
        this.primaryStage.show();

        // CTRL+Q exit application
        this.primaryStage.getScene().getAccelerators().put(KeyCombination.keyCombination("CTRL+Q"),() -> stop());

        executor.execute(socketRunner);
        displayMessage("> Connecting to twitch.tv...");
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

            if (connectedToChannel)
                disconnectFromChannel();

            if (! channel.substring(0, 1).contains("#"))
                channel = "#" + channel;

            channel = channel.toLowerCase();

            log("Connecting to channel " + channel);
            sendMessage("JOIN " + channel);

            session.setChannel(channel);
            displayMessage("> Joining channel " + session.getChannel());
            secondaryStage.close();
        });

        streamerField.setOnKeyPressed(event ->
        {
            if (event.getCode().equals(KeyCode.ENTER))
                confirmButton.fire();
        });

        Scene root = new Scene(contentHolder, 400, 300);

        secondaryStage.setScene(root);
        secondaryStage.setTitle("Connect");
        secondaryStage.show();

    }

    // TODO: Implement
    private void askForCredentials()
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
                Platform.runLater(() ->
                {
                    log("Recording client data");
                    ObjectOutputStream os = null;
                    try
                    {
                        os = new ObjectOutputStream(new FileOutputStream(new File(credentialFile)));
                        os.writeObject(client);
                        os.flush();
                        os.close();
                    } catch (IOException z)
                    {
                        // Something really fucked up.
                        log("I strangely did not find the credentials file...");
                    }
                });

                log("Correct client data entered");
                secondaryStage.close();
            }
            else
            {
                log("Incorrect client data");
                title.setText("You did not enter correct values!");
            }
        });

        oauthTextField.setOnKeyPressed(e ->
        {
            if (e.getCode().equals(KeyCode.ENTER))
                confirmButton.fire();
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

    private void disconnectFromChannel()
    {
        if (connectedToChannel)
        {
            log("Disconnecting from " + session.getChannel());
            sendMessage("PART " + session.getChannel());
            displayMessage("> Leaving channel " + session.getChannel());
            clientBadgeSignatures.clear();
            userList.removeAllUsers();
            hasUserState = false;
            connectedToChannel = false;
        }
        else
        {
            displayMessage("> You are not connected to a channel!");
        }
    }

    private void sendMessage(String message) { socketRunner.sendMessage(message.trim()); }

    private synchronized void displayMessage(String message)
    {
        Label finalMessage = new Label(message);
        finalMessage.prefWidthProperty().bind(messagePane.widthProperty());
        finalMessage.setWrapText(true);
        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(125, finalMessage);
    }

    private synchronized void displayMessage(ArrayList<Node> message, String color, String username, ArrayList<Image> badges)
    {
        FlowPane flowPane = new FlowPane();
        Label userName = new Label(username);

        flowPane.setOrientation(Orientation.HORIZONTAL);
        flowPane.setHgap(4.0);
        flowPane.prefWidthProperty().bind(messagePane.widthProperty());

        if (color != null)
            userName.setTextFill(Paint.valueOf(color));

        flowPane.getChildren().add(new Label(">"));

        if (badges != null)
            for (Image icon : badges)
                flowPane.getChildren().add(new ImageView(icon));

        flowPane.getChildren().addAll(userName, new Label(":"));

        for (Node node : message)
            flowPane.getChildren().add(node);

        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(125, flowPane);
    }

    public static void main(String[] args)
    {
        launchArgs = args;
        launch(args);
    }
}

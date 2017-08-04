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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static logUtils.Logger.*;

public class WildChat extends Application
{
    private static String[] launchArgs;

    private Stage primaryStage = null;

    private Thread baseConnectionThread = null;

    private TwitchConnect socketRunner = null;

    static Client client = null;

    static Session session = new Session();

    private GridPane mainContent = new GridPane();

    private MenuBar menuBar  = new MenuBar();

    private Menu connections = new Menu("Connections"),
            settings = new Menu("Settings");

    private MenuItem connect = new MenuItem("Connect"),
            uiSettings = new MenuItem("Customize UI"),
            disconnect = new MenuItem("Disconnect");

    private static ScrollPane messagePane = new ScrollPane(),
            userListPane = new ScrollPane();

    static UserList userList = new UserList();

    private Text title = new Text("Wild Chat");

    private static VBox messageHolder = new VBox();

    private TextField messageField = new TextField();

    private ColumnConstraints column1Constraints = new ColumnConstraints(),
            column2Constraints = new ColumnConstraints();

    private RowConstraints row1Constraints = new RowConstraints(),
            row2Constraints = new RowConstraints(),
            row3Constraints = new RowConstraints();

    private final String filePrefix = ".WildChat/",
                         credentials = "credentials.dat",
                         dotDirLocation = (System.getProperty("os.name").contains("Windows")) ?
                             BasicIO.getEnvVars("APPDATA") + "/" : BasicIO.getEnvVars("HOME") + "/",
                         credentialFile = dotDirLocation + filePrefix + credentials;

    static volatile boolean connected = false,
        connectionMessageReceived = false,
        connectedToChannel = false,
        credentialsAvailable = false,
        hasUserState = false;

    public WildChat()
    {
        boolean canAccessCredentials,
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
        log("ShutDown");
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage)
    {
        log("Stage start");
        this.primaryStage = primaryStage;

        log((! credentialsAvailable) ? "No credentials available, showing input fields" : "");
        if (! credentialsAvailable)
            askForCredentials();

        if (client == null)
        {
            log("Client not initialized!");
            System.exit(1);
        }

        log("Setting up networking");
        socketRunner = new TwitchConnect(client);

        log("initializing UI");
        initUI();

        log("Setting scene");
        Scene root = new Scene(mainContent, 650, 400);

        this.primaryStage.setScene(root);
        this.primaryStage.setTitle("WildChat");

        // CTRL+Q exit application
        this.primaryStage.getScene().getAccelerators().put(KeyCombination.keyCombination("CTRL+Q"),() -> stop());

        log("Showing window");
        this.primaryStage.show();

        log("Starting networking");
        baseConnectionThread = new Thread(socketRunner);
        baseConnectionThread.start();
        log("Networking started");
        displayMessage("> Connecting to twitch.tv...");
    }

    private void initUI()
    {
        setVisibleProperties();
        setInteractions();
        addNodesToParents();
    }

    private void setVisibleProperties()
    {
        log("Initializing container stuff");
        // Container stuff
        menuBar.setMaxHeight(22.0);
        messageField.setPromptText("Message");
        BorderPane.setMargin(title, new Insets(7.0, 0, 0, 0));
        VBox.setMargin(userList, new Insets(4, 0, 4, 4));
        userListPane.setMaxWidth(250.0);
        userListPane.setPrefWidth(175.0);
        userListPane.setMinWidth(100.0);
        userList.setSpacing(3.0);
        messageHolder.setSpacing(3.0);
        messagePane.setPrefWidth(450.0);
        messagePane.setMinWidth(100.0);
        messagePane.setMaxWidth(Double.MAX_VALUE);
        messagePane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagePane.setFitToWidth(true);
        messagePane.setPannable(false);
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
        row3Constraints.setValignment(VPos.BOTTOM);
        row3Constraints.setFillHeight(true);

        log("Populating the message holder");
        for (int count = 0 ; count <= 125 ; count++)
            messageHolder.getChildren().add(count, new Text(" "));
    }

    private void setInteractions()
    {
        log("Setting interactions");
        log("Setting main stage interactions");
        this.primaryStage.setOnCloseRequest(e -> log("Primary Stage Close"));

        log("Setting disconnect/ connect interactions");
        disconnect.setOnAction(e -> disconnectFromChannel());

        connect.setOnAction(e -> showConnectWindow());

        log("setting message field interactions");
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
                        sendMessage("PRIVMSG " + Session.getChannel() + " :" + message);

                        if (! hasUserState)
                        {
                            displayMessage("> " + client.getNick() + " : " + message);
                        }

                        else
                        {
                            ArrayList<Image> clientImageBadges = null;
                            ArrayList<Node> messageNodes = new ArrayList<>();
                            StringBuilder sb = new StringBuilder();

                            if (session.getBadgeSignatures().size() >= 1)
                            {
                                clientImageBadges = new ArrayList<>();

                                for (String badge : session.getBadgeSignatures())
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

                            displayMessage(DataProcessor.formatMessage(
                                clientImageBadges, session.getClientDisplayName(),
                                session.getClientColor(), messageNodes));
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
    }

    private void addNodesToParents()
    {
        log("Adding nodes to parents");
        connections.getItems().addAll(connect, disconnect);
        settings.getItems().add(uiSettings);
        menuBar.getMenus().addAll(connections, settings);
        mainContent.getColumnConstraints().addAll(column1Constraints, column2Constraints);
        mainContent.getRowConstraints().addAll(row1Constraints, row2Constraints, row3Constraints);
        messagePane.setContent(messageHolder);
        userListPane.setContent(userList);
        mainContent.add(menuBar, 0, 0, 2, 1);
        mainContent.add(messagePane, 0, 1);
        mainContent.add(userListPane, 1, 1);
        mainContent.add(messageField, 0, 2, 2, 1);
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

            log("Connecting to " + channel);
            sendMessage("JOIN " + channel);

            session.setChannel(channel);
            displayMessage("> Joining channel " + Session.getChannel() + "...");
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
        secondaryStage.initStyle(StageStyle.UTILITY);
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
                log("Credential window exit w/o client being initialized!");
                System.exit(1);
            }
        });
        secondaryStage.initStyle(StageStyle.UTILITY);
        secondaryStage.showAndWait();
    }
    // END TODO

    private void disconnectFromChannel()
    {
        if (connectedToChannel)
        {
            log("Disconnecting from " + Session.getChannel());
            sendMessage("PART " + Session.getChannel());
            displayMessage("> Leaving channel " + Session.getChannel());
            session = new Session();
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

    static void displayMessage(String message)
    {
        Label newMessage = new Label(message);
        newMessage.setWrapText(true);
        newMessage.prefWidthProperty().bind(messagePane.widthProperty());

        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(125, newMessage);
    }

    static void displayMessage(FlowPane holder)
    {
        holder.prefWidthProperty().bind(messagePane.widthProperty());

        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(125, holder);
    }

    public static void main(String[] args)
    {
        launchArgs = args;
        launch(args);
    }
}

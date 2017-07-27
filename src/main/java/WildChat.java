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
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;

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

    private ScrollPane messagePane = new ScrollPane();

    private VBox messageHolder = new VBox();

    private ListView<String> userList = new ListView<>();

    private TextField messageField = new TextField();

    private ColumnConstraints column1Constraints = new ColumnConstraints(),
            column2Constraints = new ColumnConstraints();

    private RowConstraints row1Constraints = new RowConstraints(),
            row2Constraints = new RowConstraints(),
            row3Constraints = new RowConstraints();

    private boolean credentialsAvailable = false,
                    connectedToChannel = false;

    private final String filePrefix = ".WildChat/",
                         credentials = "credentials.dat",
                         badges = "badges/",
                         dotDirLocation = (System.getProperty("os.name").contains("Windows")) ?
                             BasicIO.getEnvVars("APPDATA") + "/" : BasicIO.getEnvVars("HOME") + "/",
                         credentialFile = dotDirLocation + filePrefix + credentials;

    private static final String[][] badgesWithURL = {{"admin", "bits"},
                                                    {"://static-cdn.jtvnw.net/badges/v1/9ef7e029-4cdf-4d4d-a0d5-e2b3fb2583fe/1"}};

    public static volatile boolean connected = false;

    public WildChat()
    {
        boolean credentialError = false,
                debug = true;

        if (launchArgs.length > 0 && launchArgs[0].equals("--no-debug"))
            debug = false;

        setShouldLog(debug);


        log("Testing read/write on " + credentialFile);
        if (! FileUtil.canReadWrite(credentialFile))
        {
            log("File " + credentialFile + " either does not exist or can't be read/wrote to");
            log("Attempting to create necessary dirs and file for " + credentialFile);
            credentialError = FileUtil.createFileWithDirs(credentialFile);
        }
        else
            credentialError = true;

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
        userList.setMaxWidth(250.0);
        userList.setPrefWidth(175.0);
        userList.setMinWidth(100.0);
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
        mainContent.add(menuBar, 0, 0, 2, 1);
        mainContent.add(messagePane, 0, 1);
        mainContent.add(userList, 1, 1);
        mainContent.add(messageField, 0, 2, 2, 1);

        Scene root = new Scene(mainContent, 650, 400);

        for (int count = 0 ; count <= 125 ; count++)
        {
            messageHolder.getChildren().add(count, new Text(" "));
        }

        disconnect.setOnAction(e ->
        {
            if (connectedToChannel)
            {
                log("Disconnecting from " + session.getChannel());
                sendMessage("PART " + session.getChannel());
                displayMessage("> Leaving channel " + session.getChannel());
            }
            else
            {
                displayMessage("> You are not connected to a channel!");
            }
        });

        connect.setOnAction(e ->
        {
            showConnectWindow();
        });

        messageField.setOnKeyPressed(event ->
        {
            if (event.getCode().equals(KeyCode.ENTER))
            {
                String message = messageField.getText();
                if (message.length() > 0)
                {
                    if (connectedToChannel)
                    {
                        sendMessage("PRIVMSG " + session.getChannel() + " :" + message);
                        displayMessage("> " + client.getNick() + ": " + message);
                    }
                    else
                    {
                        displayMessage("> You are not connected to a channel!");
                    }
                    messageField.clear();
                }
            }
        });

        socketRunner.getDataProperty().addListener(e ->
        {
            String data = socketRunner.getData();

            log(data);

            executor.execute(() ->
            {
                String username = null;

                boolean part = data.contains("PART"),
                    join = data.contains("JOIN"),
                    msg = data.contains("PRIVMSG"),
                    connection = data.substring(0, 18).contains("001"),
                    localMessage = data.substring(0, 4).equals("EEE");

                if (msg)
                {
                    log("Message received");
                    StringBuilder sb = new StringBuilder();
                    char[] rawData = data.toCharArray();
                    int categoryStart;
                    int endOfCategoryLocation = 0;
                    String message;
                    String color = null;
                    ArrayList<String> badges = new ArrayList<>();
                    ArrayList<Integer> badgeVersions = new ArrayList<>();
                    ArrayList<File> badgeIcons = new ArrayList<>();

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
                                badges.add(sb.toString());
                                count++; // Skip the /
                                sb = new StringBuilder(); // Clear the StringBuilder

                                while (rawData[count] != ',' && rawData[count] != ';')
                                    sb.append(rawData[count++]);

                                badgeVersions.add(Integer.parseInt(sb.toString()));

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
                                color = sb.toString();
                                break;
                            }

                            else
                                sb.append(rawData[count]);
                        }
                    }

                    // Grab the username
                    categoryStart = data.indexOf("display-name=") + 13;
                    endOfCategoryLocation = data.indexOf(';', categoryStart);
                    username = data.substring(categoryStart, endOfCategoryLocation);

                    // Test for emotes
                    categoryStart = data.indexOf("emotes=", endOfCategoryLocation) + 7; // 7 emotes= length
                    endOfCategoryLocation = data.indexOf(';', categoryStart);

                    if (! (categoryStart == endOfCategoryLocation))
                    { // Message has emote data
                        log("Ignoring emotes");
                    }

                    // Grab the message
                    categoryStart = (data.indexOf(':', data.indexOf("PRIVMSG")));
                    message = data.substring(categoryStart + 1);

                    // Remove EOL chars from the message
                    message = message.substring(0, message.length() - 1);

                    // Grab badges


                    // Sarcasm
                    final String effectivelyFinalMessage = message;
                    final String effectivelyFinalColor = color;
                    final String effectivelyFinalUserName = username;
                    Platform.runLater(() ->
                        displayMessage(effectivelyFinalMessage, effectivelyFinalColor, effectivelyFinalUserName)
                    );
                    // END TODO
                }

                // Status stuff
                StringBuilder finalMessage = new StringBuilder();

                if (connection)
                {

                    log("Connected to twitch.tv");
                    Platform.runLater(() ->
                    {
                        displayMessage("> Connected to twitch.tv");
                    });
                    finalMessage.append("> Please join a channel!");
                }
                else if (localMessage)
                {
                    finalMessage.append("Incorrect login credentials!");
                    credentialsAvailable = false;
                }
                else if (part || join)
                {
                    int nameStart = (data.indexOf(':'));
                    int endOfNameLocation = (data.indexOf('!', nameStart));
                    int channelStart = data.indexOf('#');
                    username = data.substring(nameStart + 1, endOfNameLocation);
                    String channel = data.substring(channelStart, data.length() - 1);

                    if (part)
                    {
                        log("User left channel received");
                        finalMessage.append("> " + username + " left channel " + channel);
                    }

                    if (join)
                    {
                        log("User join channel received");
                        finalMessage.append("> " + username + " joined channel " + channel);
                    }
                }

                if (part || join || connection)
                {
                    Platform.runLater(() ->
                    {
                        displayMessage(finalMessage.toString());
                    });
                }
            });
        });

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
            {
                log("Disconnecting from " + session.getChannel());
                sendMessage("PART " + session.getChannel());
            }

            if (! channel.substring(0, 1).contains("#"))
                channel = "#" + channel;

            log("Connecting to channel " + channel);
            sendMessage("JOIN " + channel);

            session.setChannel(channel);
            displayMessage("> Joining channel " + session.getChannel());
            connectedToChannel = true;
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

    private void sendMessage(String message) { socketRunner.sendMessage(message.trim()); }

    private synchronized void displayMessage(String message)
    {
        Label finalMessage = new Label(message);
        finalMessage.prefWidthProperty().bind(messagePane.widthProperty());
        finalMessage.setWrapText(true);
        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(125, finalMessage);
    }

    private synchronized void displayMessage(String message, String color, String username)
    {
        char[] charWords = message.toCharArray();
        FlowPane flowPane = new FlowPane();
        Label userName = new Label(username);
        StringBuilder sb = new StringBuilder();

        flowPane.setOrientation(Orientation.HORIZONTAL);
        flowPane.prefWidthProperty().bind(messagePane.widthProperty());
        userName.setTextFill(Paint.valueOf(color));

        flowPane.getChildren().add(userName);

        int lastChar = charWords.length - 1;
        int index = 0;
        for (char c : charWords)
        {
            sb.append(c);
            if (c == 32 || index == lastChar)
            {
                flowPane.getChildren().add(new Label(sb.toString()));
                sb = new StringBuilder();
            }
            index++;
        }

        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(125, flowPane);
    }

    public static void main(String[] args)
    {
        launchArgs = args;
        launch(args);
    }
}

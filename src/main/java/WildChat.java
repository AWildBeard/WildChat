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

    private TwitchConnect socketRunner = null;

    private ExecutorService executor = Executors.newCachedThreadPool();

    static Client client = null;

    static Session session = new Session();

    private BorderPane titleBar = new BorderPane();

    private GridPane mainContent = new GridPane();

    private MenuBar menuBar  = new MenuBar();

    private Menu connections = new Menu("Connections"),
            settings = new Menu("Settings");

    private MenuItem connect = new MenuItem("Connect"),
            uiSettings = new MenuItem("UI"),
            disconnect = new MenuItem("Disconnect");

    private static ScrollPane messagePane = new ScrollPane(),
            userListPane = new ScrollPane();

    static UserList userList = new UserList();

    private Text title = new Text("Wild Chat");

    private static VBox messageHolder = new VBox();

    private HBox windowControlsHolder = new HBox(7.0);

    private Circle closeButton = new Circle(),
            maximizeButton = new Circle(),
            minimizeButton = new Circle(),
            closeButtonSealer = new Circle(),
            maximizeButtonSealer = new Circle(),
            minimizeButtonSealer = new Circle();

    private StackPane closeButtonStackPane = new StackPane(),
            maximizeButtonStackPane = new StackPane(),
            minimizeButtonStackPane = new StackPane();

    private Line closeLine1 = new Line(),
            closeLine2 = new Line(),
            minimizeLine1 = new Line();

    private Polygon maximizeTriangle1 = new Polygon(),
            maximizeTriangle2 = new Polygon();

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

    private static double mouseDragStartX, mouseDragStartY, maximizedWindowFactor;

    private boolean windowMaximizedOperation = false, wasMaxOp = false;

    static volatile boolean connected = false,
        connectionMessageReceived = false,
        connectedToChannel = false,
        credentialsAvailable = false,
        hasUserState = false;

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

        closeButton.setStyle("-fx-fill: red");
        closeButton.setRadius(8.5);
        closeButtonSealer.setStyle("-fx-fill: transparent");
        closeButtonSealer.setRadius(8.5);
        maximizeButton.setStyle("-fx-fill: limegreen");
        maximizeButton.setRadius(8.5);
        maximizeButtonSealer.setStyle("-fx-fill: transparent");
        maximizeButtonSealer.setRadius(8.5);
        minimizeButton.setStyle("-fx-fill: orange");
        minimizeButton.setRadius(8.5);
        minimizeButtonSealer.setStyle("-fx-fill: transparent");
        minimizeButtonSealer.setRadius(8.5);
        closeLine1.setStartX(-4.0);
        closeLine1.setStartY(4.0);
        closeLine1.setEndX(4.0);
        closeLine1.setEndY(-4.0);
        closeLine2.setStartX(-4.0);
        closeLine2.setStartY(-4.0);
        closeLine2.setEndX(4.0);
        closeLine2.setEndY(4.0);
        // triangle stuff here
        minimizeLine1.setStartX(-4.0);
        minimizeLine1.setStartY(0.0);
        minimizeLine1.setEndX(4.0);
        minimizeLine1.setEndY(0.0);

        messagePane.setPannable(false);
        connections.getItems().addAll(connect, disconnect);
        settings.getItems().add(uiSettings);
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
        row2Constraints.setVgrow(Priority.NEVER);
        row2Constraints.setValignment(VPos.TOP);
        row2Constraints.setFillHeight(false);
        row3Constraints.setVgrow(Priority.ALWAYS);
        row3Constraints.setValignment(VPos.BOTTOM);
        row3Constraints.setFillHeight(true);
        mainContent.getColumnConstraints().addAll(column1Constraints, column2Constraints);
        mainContent.getRowConstraints().addAll(row1Constraints, row2Constraints, row3Constraints);
        titleBar.setMinHeight(22.0);

        messagePane.setContent(messageHolder);
        userListPane.setContent(userList);
        userList.setSpacing(3.0);
        titleBar.setCenter(title);
        titleBar.setRight(windowControlsHolder);
        windowControlsHolder.setAlignment(Pos.CENTER_RIGHT);
        windowControlsHolder.getChildren().addAll(minimizeButtonStackPane, maximizeButtonStackPane, closeButtonStackPane);
        closeButtonStackPane.getChildren().addAll(closeButton, closeLine1, closeLine2, closeButtonSealer);
        maximizeButtonStackPane.getChildren().addAll(maximizeButton, maximizeTriangle1, maximizeTriangle2, maximizeButtonSealer);
        minimizeButtonStackPane.getChildren().addAll(minimizeButton, minimizeLine1, minimizeButtonSealer);
        BorderPane.setMargin(title, new Insets(7.0, 0, 0, 0));
        BorderPane.setMargin(windowControlsHolder, new Insets(3.5, 3.5, 0, 0));
        VBox.setMargin(userList, new Insets(4, 0, 4, 4));
        mainContent.add(titleBar, 0, 0, 2, 1);
        mainContent.add(menuBar, 0, 1, 2, 1);
        mainContent.add(messagePane, 0, 2);
        mainContent.add(userListPane, 1, 2);
        mainContent.add(messageField, 0, 3, 2, 1);

        Scene root = new Scene(mainContent, 650, 400);

        for (int count = 0 ; count <= 125 ; count++)
        {
            messageHolder.getChildren().add(count, new Text(" "));
        }

        disconnect.setOnAction(e -> disconnectFromChannel());

        connect.setOnAction(e -> showConnectWindow());

        closeButtonSealer.setOnMouseClicked(e ->
        {
            Platform.exit();
        });

        minimizeButtonSealer.setOnMouseClicked(e ->
        {
            this.primaryStage.setIconified(true);
        });

        maximizeButtonSealer.setOnMouseClicked(e ->
        {
            if (this.primaryStage.isMaximized())
                this.primaryStage.setMaximized(false);

            else
                this.primaryStage.setMaximized(true);
        });

        titleBar.setOnMousePressed(e ->
        {
            if (e.getButton() != MouseButton.MIDDLE)
            {
                windowMaximizedOperation = false;
                mouseDragStartX = e.getX();
                mouseDragStartY = e.getY();
                if (this.primaryStage.isMaximized())
                {
                    maximizedWindowFactor = titleBar.getScene().getWindow().getWidth();
                    windowMaximizedOperation = true;
                }
            }
        });
        titleBar.setOnMouseDragged(e ->
        {
            if (e.getButton() != MouseButton.MIDDLE)
            {
                if (windowMaximizedOperation)
                {
                    this.primaryStage.setMaximized(false);
                    wasMaxOp = true;
                }
                else
                {
                    titleBar.getScene().getWindow().setX(e.getScreenX() - mouseDragStartX);
                    wasMaxOp = false;
                }
                if (wasMaxOp)
                {
                    double orig = mouseDragStartX; // Don't wan't to change this.
                    mouseDragStartX =
                        (titleBar.getScene().getWindow().getWidth() / maximizedWindowFactor) * mouseDragStartX;
                    titleBar.getScene().getWindow().setX(e.getScreenX() - mouseDragStartX);
                    mouseDragStartX = orig;
                }
                titleBar.getScene().getWindow().setY(e.getScreenY() - mouseDragStartY);
            }
        });

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

                            displayMessage(messageNodes, session.getClientColor(),
                                session.getClientDisplayName(), clientImageBadges);
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

        this.primaryStage.setScene(root);
        this.primaryStage.setTitle("WildChat");
        this.primaryStage.setOnCloseRequest(e ->
        {
            log("Primary Stage Close");
        });
        this.primaryStage.initStyle(StageStyle.UNDECORATED);
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

    static synchronized void displayMessage(String message)
    {
        Label newMessage = new Label(message);
        newMessage.setWrapText(true);

        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(125, newMessage);
    }

    static synchronized void displayMessage(ArrayList<Node> message, String color, String username, ArrayList<Image> badges)
    {
        if (message == null)
        {
            log("Got no message for displayMessage!");
            return;
        }

        FlowPane flowPane = new FlowPane();
        Label userName = null;

        flowPane.setOrientation(Orientation.HORIZONTAL);
        flowPane.setHgap(4.0);
        flowPane.prefWidthProperty().bind(messagePane.widthProperty());

        flowPane.getChildren().add(new Label(">"));

        if (badges != null)
            for (Image icon : badges)
                flowPane.getChildren().add(new ImageView(icon));

        if (username != null)
        {
            userName = new Label(username);
            flowPane.getChildren().addAll(userName, new Label(":"));
        }

        if (color != null && username != null)
            userName.setTextFill(Paint.valueOf(color));


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
